#!/usr/bin/env python3
"""Build the bundled full-catalog seed assets from ashpazyar_dataset.

Reads the processed Persian_Cooking JSONL, preserves the original recipe
names, categories/subcategories, ingredient lines and source metadata, and
writes compact gzip assets that SeedLoader can stream into Room.
"""

from __future__ import annotations

import gzip
import hashlib
import io
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "ashpazyar_dataset"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "seed"

SEED_RECIPES = ASSETS / "recipes.json"
SEED_INGREDIENTS = ASSETS / "ingredients.json"
SEED_CATEGORIES = ASSETS / "categories.json"
CATALOG_RECIPES = ASSETS / "catalog_recipes.jsonl.gzip"
CATALOG_INGREDIENTS = ASSETS / "catalog_ingredients.json.gzip"
CATALOG_MANIFEST = ASSETS / "catalog_manifest.json"

UNIT_MAP = {
    "GRAM": "GRAM",
    "KILOGRAM": "KILOGRAM",
    "MILLILITER": "MILLILITER",
    "LITER": "LITER",
    "TEASPOON": "TEASPOON",
    "TABLESPOON": "TABLESPOON",
    "CUP": "CUP",
    "PIECE": "PIECE",
    "CLOVE": "CLOVE",
    "BUNCH": "BUNCH",
    "SLICE": "SLICE",
    "PINCH": "PINCH",
    "CAN": "CAN",
    "GLASS": "CUP",
    "DESSERT_SPOON": "TABLESPOON",
    "SPOON": "TABLESPOON",
    "PACKAGE": "PIECE",
    "CONTAINER": "PIECE",
    "BOTTLE": "PIECE",
    "SPRIG": "PIECE",
    "HANDFUL": "BUNCH",
    "LEAF": "PIECE",
    "METER": "PIECE",
    "UNSPECIFIED": "PIECE",
    "QUALITATIVE": "PIECE",
}

UNIT_NOTE = {
    "GLASS": "لیوان",
    "DESSERT_SPOON": "قاشق دسرخوری",
    "SPOON": "قاشق",
    "PACKAGE": "بسته",
    "CONTAINER": "ظرف",
    "BOTTLE": "بطری",
    "SPRIG": "شاخه",
    "HANDFUL": "مشت",
    "LEAF": "برگ",
    "METER": "متر",
    "UNSPECIFIED": "واحد نامشخص",
}

INGREDIENT_CATEGORY_TITLE = {
    "VEGETABLE": "سبزیجات",
    "FRUIT": "میوه‌ها",
    "MEAT": "گوشت و مرغ",
    "POULTRY": "گوشت و مرغ",
    "FISH": "ماهی و غذاهای دریایی",
    "SEAFOOD": "ماهی و غذاهای دریایی",
    "DAIRY": "لبنیات",
    "GRAIN": "غلات و نشاسته",
    "LEGUME": "حبوبات",
    "NUT": "خشکبار",
    "SPICE": "ادویه و چاشنی",
    "HERB": "سبزی و گیاهان معطر",
    "OIL": "روغن و چربی",
    "SWEETENER": "شیرین‌کننده",
    "BEVERAGE": "نوشیدنی",
    "OTHER": "سایر",
}

HERO_STYLES = ["PEACH", "SAGE", "CREAM", "TERRACOTTA", "DARK"]


def load_json(path: Path):
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def write_gzip(path: Path, raw: bytes) -> None:
    buffer = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=buffer, compresslevel=9, mtime=0) as handle:
        handle.write(raw)
    path.write_bytes(buffer.getvalue())


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def map_categories(primary: str, sub: str) -> list[str]:
    ids: list[str] = []
    if "خارجی" in sub:
        ids.append("international")
    elif primary == "غذای اصلی":
        ids.append("iranian")
    elif primary == "پیش غذا":
        ids.append("appetizer")
    elif primary == "کیک، شیرینی و نوشیدنی":
        ids.append("drink" if "نوشیدنی" in sub else "dessert")
    elif primary == "ساندویچ، پیتزا و ماکارونی،پاستا":
        ids.append("international")
    elif primary == "سلامت غذایی":
        ids.append("family")

    if sub == "خورش":
        ids.append("stew")
    elif sub in ("پلویی و چلویی",):
        ids.append("rice")
    elif "کباب" in sub or "کوفته" in sub or "کوکو" in sub or "کتلت" in sub:
        ids.append("kabab")
    elif "سوپ" in sub or "آش" in sub:
        ids.append("soup")
    elif "سالاد" in sub:
        ids.append("salad")
    elif "گیاهی" in sub:
        ids.append("vegetarian")
    elif "نوشیدنی" in sub:
        ids.append("drink")
    elif any(word in sub for word in ("شیرینی", "کیک", "دسر", "حلوا", "فرنی", "بستنی", "مربا", "شکلات", "ژله")):
        ids.append("dessert")

    result = list(dict.fromkeys(ids))
    return (result or ["family"])[:3]


def clean_text(value) -> str:
    return "" if value is None else str(value).strip()


def recipe_ingredients(source: dict, ingredient_ids: set[str]) -> list[dict]:
    result = []
    for item in source.get("ingredients") or []:
        ingredient_id = clean_text(item.get("ingredient_id"))
        if ingredient_id not in ingredient_ids:
            raise ValueError(f"unknown ingredient {ingredient_id!r} in {source.get('id')}")
        unit_id = clean_text(item.get("unit_id")).upper()
        quantity = item.get("quantity")
        qualitative = bool(item.get("qualitative")) or unit_id == "QUALITATIVE" or quantity is None or float(quantity) <= 0
        if qualitative:
            amount = 0.0
            unit = "PIECE"
            note = clean_text(item.get("quantity_text")) or clean_text(item.get("amount_raw")) or "به میزان لازم"
        else:
            amount = float(quantity)
            unit = UNIT_MAP.get(unit_id, "PIECE")
            note_parts = []
            if unit_id in UNIT_NOTE:
                note_parts.append("واحد اصلی: " + UNIT_NOTE[unit_id])
            prep_note = re.sub(r"\s+", " ", clean_text(item.get("preparation_note")))
            unit_raw = re.sub(r"\s+", " ", clean_text(item.get("unit_raw")))
            unit_note = re.sub(r"\s+", " ", UNIT_NOTE.get(unit_id, ""))
            if prep_note and prep_note not in (unit_raw, unit_note):
                note_parts.append(prep_note)
            note = "؛ ".join(dict.fromkeys(note_parts))
        result.append({
            "ingredientId": ingredient_id,
            "amount": amount,
            "unit": unit,
            "note": note,
            "optional": bool(item.get("optional")),
        })
    return result


def main() -> None:
    editorial = load_json(SEED_RECIPES)["recipes"]
    editorial_ingredients = load_json(SEED_INGREDIENTS)
    editorial_categories = load_json(SEED_CATEGORIES)
    if len(editorial) != 2000:
        raise SystemExit(f"expected 2000 editorial recipes, found {len(editorial)}")

    editorial_recipe_ids = {item["id"] for item in editorial}
    editorial_ingredient_ids = {item["id"] for item in editorial_ingredients}
    editorial_category_ids = {item["id"] for item in editorial_categories}
    if len(editorial_recipe_ids) != len(editorial):
        raise SystemExit("duplicate editorial recipe ids")
    if len(editorial_ingredient_ids) != len(editorial_ingredients):
        raise SystemExit("duplicate editorial ingredient ids")

    all_ingredients = load_json(DATASET / "ingredients" / "ingredients.json")
    ingredient_by_id = {}
    for item in all_ingredients:
        ingredient_id = clean_text(item.get("id"))
        if not ingredient_id:
            continue
        if ingredient_id in editorial_ingredient_ids:
            raise SystemExit(f"catalog ingredient id collides with editorial seed: {ingredient_id}")
        if ingredient_id in ingredient_by_id:
            raise SystemExit(f"duplicate catalog ingredient id: {ingredient_id}")
        ingredient_by_id[ingredient_id] = item

    recipe_lines: list[str] = []
    used_ingredients: set[str] = set()
    seen_recipe_ids: set[str] = set()
    source_rows = 0

    with (DATASET / "jsonl" / "recipes.jsonl").open(encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line:
                continue
            source = json.loads(line)
            source_rows += 1
            recipe_id = clean_text(source.get("id"))
            if not recipe_id:
                raise ValueError("recipe without id")
            if recipe_id in editorial_recipe_ids:
                raise ValueError(f"catalog recipe id collides with editorial seed: {recipe_id}")
            if recipe_id in seen_recipe_ids:
                raise ValueError(f"duplicate catalog recipe id: {recipe_id}")
            seen_recipe_ids.add(recipe_id)

            title = clean_text(source.get("title_fa")) or clean_text(source.get("title_original"))
            if not title:
                raise ValueError(f"recipe without title: {recipe_id}")
            primary = clean_text(source.get("primary_category_original")) or "نامشخص"
            sub = clean_text(source.get("subcategory_original")) or "نامشخص"
            category_ids = map_categories(primary, sub)
            for category_id in category_ids:
                if category_id not in editorial_category_ids:
                    raise ValueError(f"unknown mapped category {category_id} for {recipe_id}")

            tags = []
            for tag in source.get("tags") or []:
                name = clean_text(tag.get("name_fa"))
                if name and name not in tags:
                    tags.append(name)
            tags.extend(["بازبینی لازم", "دسته: " + primary, "زیردسته: " + sub])

            ingredients = recipe_ingredients(source, set(ingredient_by_id))
            for item in ingredients:
                used_ingredients.add(item["ingredientId"])

            source_row = source.get("source_row")
            history = (
                f"این رکورد از فایل Persian_Cooking (train.csv)، ردیف {source_row}، "
                f"با دسته اصلی «{primary}» و زیردسته «{sub}» آمده است. "
                "متن مراحل پخت در منبع ورودی وجود ندارد و رکورد در وضعیت نیازمند بازبینی است."
            )
            difficulty = {"EASY": "EASY", "MEDIUM": "MEDIUM", "HARD": "HARD", "EXPERT": "HARD"}.get(
                clean_text(source.get("difficulty")).upper(), "MEDIUM"
            )
            region_parts = [clean_text(source.get("origin_country")), clean_text(source.get("origin_region"))]
            region = " / ".join(part for part in region_parts if part) or None

            seed = {
                "id": recipe_id,
                "title": title,
                "description": (
                    f"دستور «{title}» از دسته «{primary}» و زیردسته «{sub}». "
                    "این رکورد از فایل Persian_Cooking (train.csv) گرفته شده و متن مراحل پخت در منبع ورودی وجود ندارد."
                ),
                "categoryIds": category_ids,
                "tags": tags,
                "servings": max(1, int(source.get("default_servings") or 1)),
                "prepMinutes": max(0, int(source.get("preparation_minutes") or 0)),
                "cookMinutes": max(0, int(source.get("cooking_minutes") or 0)),
                "difficulty": difficulty,
                "calories": None,
                "season": "ALL",
                "budgetLevel": "MEDIUM",
                "heroStyle": HERO_STYLES[len(recipe_lines) % len(HERO_STYLES)],
                "isVegetarian": False,
                "isVegan": False,
                "isGlutenFree": False,
                "isDairyFree": False,
                "isFamilyFriendly": True,
                "allergens": [],
                "ingredients": ingredients,
                "steps": [],
                "tips": [],
                "region": region,
                "history": history,
                "resultDescription": None,
                "restMinutes": 0,
                "equipment": [],
                "ovenTemperatureC": None,
                "prepGuide": [],
                "servingGuide": [],
                "storageGuide": None,
                "troubleshooting": [],
                "sourceName": "Persian_Cooking (train.csv)",
                "sourceUrl": None,
                "license": "نامشخص — بازنشر تجاری تأییدنشده",
                "validationStatus": "REVIEW_REQUIRED",
            }
            recipe_lines.append(json.dumps(seed, ensure_ascii=False, separators=(",", ":")))

    if len(recipe_lines) != 5790:
        raise SystemExit(f"expected 5790 catalog recipes, found {len(recipe_lines)}")
    if used_ingredients != set(ingredient_by_id):
        missing = sorted(set(ingredient_by_id) - used_ingredients)
        raise SystemExit(f"unused catalog ingredient(s): {missing[:5]}")

    catalog_ingredients = []
    for item in all_ingredients:
        ingredient_id = clean_text(item.get("id"))
        if ingredient_id not in used_ingredients:
            continue
        category = clean_text(item.get("ingredient_type")).upper() or "OTHER"
        aliases = [clean_text(alias) for alias in item.get("aliases") or [] if clean_text(alias)]
        name = clean_text(item.get("name_fa")) or clean_text(item.get("name_original"))
        catalog_ingredients.append({
            "id": ingredient_id,
            "name": name,
            "nameEn": "",
            "category": category,
            "categoryTitle": INGREDIENT_CATEGORY_TITLE.get(category, "سایر"),
            "defaultUnit": "GRAM",
            "aliases": list(dict.fromkeys(aliases)),
            "cupWeightGrams": None,
            "pieceWeightGrams": None,
            "referencePricePerKg": None,
        })

    recipes_raw = ("\n".join(recipe_lines) + "\n").encode("utf-8")
    ingredients_raw = (json.dumps(catalog_ingredients, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    write_gzip(CATALOG_RECIPES, recipes_raw)
    write_gzip(CATALOG_INGREDIENTS, ingredients_raw)

    manifest = {
        "format": 1,
        "source": "Persian_Cooking train.csv (processed ashpazyar_dataset)",
        "source_rows": source_rows,
        "editorial_recipes": len(editorial),
        "catalog_recipes": len(recipe_lines),
        "total_recipes_after_seed": len(editorial) + len(recipe_lines),
        "catalog_ingredients": len(catalog_ingredients),
        "non_food_records_not_loaded": {"articles": 1, "tutorials": 28, "decorations": 23},
        "validation_status": "REVIEW_REQUIRED",
        "instructions_available": False,
        "license": "UNKNOWN",
        "catalog_recipes_sha256": sha256(CATALOG_RECIPES.read_bytes()),
        "catalog_recipes_raw_sha256": sha256(recipes_raw),
        "catalog_ingredients_sha256": sha256(CATALOG_INGREDIENTS.read_bytes()),
        "catalog_ingredients_raw_sha256": sha256(ingredients_raw),
    }
    CATALOG_MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "catalog_recipes": len(recipe_lines),
        "catalog_ingredients": len(catalog_ingredients),
        "total_recipes": len(editorial) + len(recipe_lines),
        "catalog_recipes_bytes": CATALOG_RECIPES.stat().st_size,
        "catalog_ingredients_bytes": CATALOG_INGREDIENTS.stat().st_size,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
