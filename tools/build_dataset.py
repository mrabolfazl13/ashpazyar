#!/usr/bin/env python3
"""Build AshpazYar knowledge-base artifacts from train.csv without inventing data."""

from __future__ import annotations

import csv
import hashlib
import json
import re
import shutil
import sqlite3
import unicodedata
import zipfile
from collections import Counter, defaultdict
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "train.csv"
OUT = ROOT / "ashpazyar_dataset"
DIST = ROOT / "dist"
ZIP_PATH = DIST / "ashpazyar_dataset.zip"
SOURCE_ID = "src-persian-cooking-train"
LICENSE_ID = "lic-unknown"
SOURCE_NAME = "Persian_Cooking train.csv"
BUILD_TIME = datetime.now(timezone.utc).replace(microsecond=0).isoformat()

UNITS = [
    ("GRAM", "گرم", "gram", "mass", ["گرم", "گرمی", "gr", "g"]),
    ("KILOGRAM", "کیلوگرم", "kilogram", "mass", ["کیلوگرم", "کیلو گرم", "کیلو"]),
    ("MILLILITER", "میلی‌لیتر", "milliliter", "volume", ["میلی‌لیتر", "میلی لیتر", "میلیلیتر", "سی سی", "سیسی", "میل", "ml", "cc"]),
    ("LITER", "لیتر", "liter", "volume", ["لیتر", "l"]),
    ("CUP", "پیمانه/فنجان", "cup", "volume", ["پیمانه", "فنجان"]),
    ("GLASS", "لیوان/استکان", "glass", "volume", ["لیوان", "استکان"]),
    ("TABLESPOON", "قاشق غذاخوری", "tablespoon", "spoon", ["قاشق غذاخوری", "قاشق غذا خوری", "قاشق غذاخور", "قاشق سوپ خوری", "قاظق سوپ خوری", "ق غ", "قغ"]),
    ("TEASPOON", "قاشق چای‌خوری", "teaspoon", "spoon", ["قاشق چای‌خوری", "قاشق چای خوری", "قاشق چایخور", "چای خوری", "چایخوری", "ق چ", "قچ"]),
    ("DESSERT_SPOON", "قاشق مرباخوری", "dessert spoon", "spoon", ["قاشق مرباخوری", "قاشق مربا خوری", "قاشق مرباخور"]),
    ("SPOON", "قاشق (نامشخص)", "spoon, unspecified", "spoon", ["قاشق"]),
    ("PIECE", "عدد/تکه", "piece", "count", ["عدد", "تکه", "تیکه", "قطعه", "بوته", "بته", "ورق", "قرص"]),
    ("CLOVE", "حبه", "clove", "count", ["حبه"]),
    ("BUNCH", "دسته", "bunch", "count", ["دسته"]),
    ("LEAF", "برگ", "leaf", "count", ["برگ"]),
    ("SLICE", "برش", "slice", "count", ["برش"]),
    ("PACKAGE", "بسته", "package", "package", ["بسته", "پاکت"]),
    ("CAN", "قوطی", "can", "package", ["قوطی"]),
    ("BOTTLE", "شیشه", "bottle", "package", ["شیشه"]),
    ("SPRIG", "شاخه/ساقه", "sprig", "count", ["شاخه", "ساقه"]),
    ("SKEWER", "سیخ", "skewer", "count", ["سیخ"]),
    ("HANDFUL", "مشت", "handful", "count", ["مشت"]),
    ("METER", "متر", "meter", "length", ["متر"]),
    ("CONTAINER", "ظرف/قابلمه/پارچ/پیاله", "container", "container", ["قابلمه", "پارچ", "پیاله", "بشقاب", "ظرف"]),
    ("PINCH", "سر انگشت", "pinch", "count", ["سر انگشت", "سرانگشت", "پنس"]),
    ("QUALITATIVE", "به میزان لازم/دلخواه", "as needed", "qualitative", []),
    ("UNSPECIFIED", "واحد نامشخص", "unspecified", "unknown", []),
]
UNIT_SEARCH = []
for uid, fa, en, kind, aliases in UNITS:
    if uid in {"QUALITATIVE", "UNSPECIFIED"}:
        continue
    for alias in aliases:
        UNIT_SEARCH.append((uid, alias))
UNIT_SEARCH.sort(key=lambda pair: len(pair[1]), reverse=True)

QUAL_RE = re.compile(r"(?:لازم|کافی|دلخواه|مقدار|مقداری|کمی|تزئین|تعداد|چند|اندازه|بمیزان|بهمقدار|مقدرا|یهمیزان)")
RANGE_RE = re.compile(r"^(\d+(?:[./]\d+)?)\s*(?:-|–|—|تا|الی|یا)\s*(\d+(?:[./]\d+)?)")
MIXED_DASH_RE = re.compile(r"^(\d+)\s*-\s*(\d+)\s*/\s*(\d+)")
MIXED_RE = re.compile(r"^(\d+)\s+و\s+(?:(\d+)\s*/\s*(\d+)|(نیم|ربع))")
FRACTION_RE = re.compile(r"^(\d+)\s*/\s*(\d+)")
NUMBER_RE = re.compile(r"^(\d+(?:[.,]\d+)?)")
WORD_RE = re.compile(r"^(نصف|نیم|ربع|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|یازده|دوازده|سیزده|چهارده|پانزده|شانزده|هفده|هجده|نوزده|بیست)(?:\s+و\s+(نیم|ربع|\d+\s*/\s*\d+))?")
WORD_NUM = {"نصف": 0.5, "نیم": 0.5, "ربع": 0.25, "یک": 1, "دو": 2, "سه": 3, "چهار": 4, "پنج": 5, "شش": 6, "هفت": 7, "هشت": 8, "نه": 9, "ده": 10, "یازده": 11, "دوازده": 12, "سیزده": 13, "چهارده": 14, "پانزده": 15, "شانزده": 16, "هفده": 17, "هجده": 18, "نوزده": 19, "بیست": 20}

CATEGORY_CONTENT_TYPES = {
    "آموزش آشپزی تصویری": "COOKING_TUTORIAL",
    "تزئینات غذا": "FOOD_DECORATION",
    "مقالات": "TECHNIQUE_GUIDE",
    "غذای اصلی": "RECIPE",
    "پیش غذا": "RECIPE",
    "کیک، شیرینی و نوشیدنی": "RECIPE",
    "ساندویچ، پیتزا و ماکارونی،پاستا": "RECIPE",
    "سلامت غذایی": "RECIPE",
}
CONTENT_FILES = {"TECHNIQUE_GUIDE": "articles.jsonl", "COOKING_TUTORIAL": "tutorials.jsonl", "FOOD_DECORATION": "decorations.jsonl"}

INGREDIENT_TYPES = [
    ("DAIRY", ["شیر", "ماست", "پنیر", "خامه", "کره", "دوغ", "کشک", "بستنی", "لبنیات"]),
    ("EGG", ["تخم مرغ", "تخممرغ", "زرده تخم", "سفیده تخم"]),
    ("NUT", ["بادام", "گردو", "پسته", "کنجد", "فندق", "بادام زمینی", "دانه کاج"]),
    ("LEGUME", ["عدس", "لوبیا", "نخود", "لپه", "ماش", "سویا"]),
    ("GRAIN", ["برنج", "آرد", "نان", "ماکارونی", "پاستا", "جو", "گندم", "ذرت", "بلغور", "کینوا", "نودل", "بیسکویت"]),
    ("PROTEIN", ["مرغ", "گوشت", "ماهی", "میگو", "بوقلمون", "ژامبون", "سوسیس", "جگر", "قزل", "تن"]),
    ("FAT", ["روغن", "مارگارین", "شورتنینگ"]),
    ("SWEETENER", ["شکر", "عسل", "شیره", "قند", "مربا", "شکلات", "کاکائو", "وانیل"]),
    ("SPICE", ["نمک", "فلفل", "زردچوبه", "دارچین", "زعفران", "زیره", "ادویه", "هل", "جوز", "پاپریکا", "خردل"]),
    ("HERB", ["جعفری", "گشنیز", "ریحان", "نعناع", "شوید", "آویشن", "رزماری", "مریم گلی", "ترخون", "برگ بو"]),
    ("VEGETABLE", ["پیاز", "سیر", "گوجه", "خیار", "کدو", "بادمجان", "سیب زمینی", "هویج", "فلفل دلمه", "قارچ", "اسفناج", "کلم", "کرفس", "تره", "لوبیا سبز", "چغندر", "شلغم", "ذرت"]),
    ("FRUIT", ["سیب", "موز", "پرتقال", "لیمو", "توت", "انار", "خرما", "انگور", "هلو", "زردآلو", "آلو", "گیلاس", "انجیر", "گلابی", "کیوی", "خرمالو", "تمشک", "بلوبری"]),
    ("WATER", ["آب"]),
]
ALLERGENS = {
    "gluten": ["آرد", "نان", "ماکارونی", "پاستا", "جو", "گندم", "بیسکویت", "کیک", "خمیر"],
    "dairy": ["شیر", "ماست", "پنیر", "خامه", "کره", "دوغ", "کشک", "بستنی"],
    "egg": ["تخم مرغ", "تخممرغ", "زرده تخم", "سفیده تخم"],
    "nuts": ["بادام", "گردو", "پسته", "فندق", "بادام زمینی", "دانه کاج"],
    "sesame": ["کنجد"],
    "fish": ["ماهی", "قزل", "تن"],
    "shellfish": ["میگو", "خرچنگ"],
    "soy": ["سویا"],
    "celery": ["کرفس"],
}


class IngredientTableParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.rows: list[list[tuple[str, str]]] = []
        self.row: list[tuple[str, str]] | None = None
        self.cell: list[str] | None = None
        self.cell_tag: str | None = None

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        if tag == "tr":
            self.row = []
        elif tag in ("td", "th") and self.row is not None:
            self.cell = []
            self.cell_tag = tag

    def handle_data(self, data: str) -> None:
        if self.cell is not None:
            self.cell.append(data)

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in ("td", "th") and self.cell is not None and self.row is not None:
            text = re.sub(r"\s+", " ", "".join(self.cell)).strip()
            self.row.append((self.cell_tag or tag, text))
            self.cell = None
            self.cell_tag = None
        elif tag == "tr" and self.row is not None:
            if self.row:
                self.rows.append(self.row)
            self.row = None


def normalize_fa(text: str) -> str:
    text = unicodedata.normalize("NFC", text or "")
    text = text.replace("ي", "ی").replace("ك", "ک").replace("ۀ", "ه").replace("ة", "ه")
    text = text.replace("\u200c", " ").replace("\u200f", "").replace("\u200e", "")
    return re.sub(r"\s+", " ", text).strip()


def normalize_key(text: str) -> str:
    return re.sub(r"[^\w\u0600-\u06FF]+", "", normalize_fa(text).lower())


def normalize_digits(text: str) -> str:
    return (text or "").translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789"))


def sha1(text: str, length: int = 16) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:length]


def stable_id(prefix: str, key: str, length: int = 12) -> str:
    return f"{prefix}-{sha1(key, length)}"


def parse_table(html: str) -> list[dict[str, str]]:
    parser = IngredientTableParser()
    parser.feed(html or "")
    items: list[dict[str, str]] = []
    for row in parser.rows:
        if len(row) < 2:
            continue
        name = re.sub(r"\s+", " ", row[0][1]).strip()
        amount = re.sub(r"\s+", " ", row[1][1]).strip()
        if name:
            items.append({"name_original": name, "name_fa": normalize_fa(name), "amount_raw": amount})
    return items


def parse_int(value: str) -> int | None:
    value = normalize_digits((value or "").strip())
    return int(value) if re.fullmatch(r"\d+", value) else None


def parse_quantity(amount: str) -> dict[str, Any]:
    raw = re.sub(r"\s+", " ", amount or "").strip()
    text = normalize_digits(normalize_fa(raw))
    text = re.sub(r"(\d)\s*([½¼¾])", lambda m: f"{float(m.group(1)) + {'½': 0.5, '¼': 0.25, '¾': 0.75}[m.group(2)]:g}", text)
    for ch, value in {"½": "0.5", "¼": "0.25", "¾": "0.75", "⅓": "0.3333", "⅔": "0.6667"}.items():
        text = text.replace(ch, value)
    out = {"quantity": None, "quantity_max": None, "quantity_text": raw, "unit_id": "UNSPECIFIED", "unit_raw": "", "qualitative": 0, "preparation_note": "", "confidence": "LOW"}
    if not text:
        return out

    q: float | None = None
    qmax: float | None = None
    consumed = 0

    def amount_float(token: str) -> float:
        token = token.replace(",", ".")
        if "/" in token:
            numerator, denominator = token.split("/", 1)
            return float(numerator) / float(denominator)
        return float(token)

    m = MIXED_DASH_RE.match(text)
    if m:
        q = float(m.group(1)) + (float(m.group(2)) / float(m.group(3)) if float(m.group(3)) else 0.0)
        consumed = m.end()
    else:
        m = RANGE_RE.match(text)
    if m and q is None:
        q = amount_float(m.group(1))
        qmax = amount_float(m.group(2))
        consumed = m.end()
    elif q is None:
        m = MIXED_RE.match(text)
        if m:
            q = float(m.group(1))
            if m.group(2) and m.group(3) and float(m.group(3)):
                q += float(m.group(2)) / float(m.group(3))
            elif m.group(4):
                q += WORD_NUM[m.group(4)]
            consumed = m.end()
        else:
            m = FRACTION_RE.match(text)
            if m:
                q = float(m.group(1)) / float(m.group(2)) if float(m.group(2)) else None
                consumed = m.end()
            else:
                m = NUMBER_RE.match(text)
                if m:
                    q = float(m.group(1).replace(",", "."))
                    consumed = m.end()
                else:
                    m = WORD_RE.match(text)
                    if m:
                        q = WORD_NUM.get(m.group(1), 0.0)
                        consumed = m.end()
                        tail = m.group(2)
                        if tail:
                            if tail in ("نیم", "ربع"):
                                q += WORD_NUM[tail]
                            else:
                                fm = re.match(r"(\d+)\s*/\s*(\d+)", tail)
                                if fm and float(fm.group(2)):
                                    q += float(fm.group(1)) / float(fm.group(2))

    if q is not None:
        if qmax is not None and qmax < q:
            q, qmax = qmax, q
        out["quantity"] = round(q, 4)
        out["quantity_max"] = round(qmax, 4) if qmax is not None else None

    remainder = text[consumed:].strip()
    unit_id = None
    unit_raw = ""
    for candidate in (remainder, text):
        key = normalize_key(candidate)
        for uid, alias in UNIT_SEARCH:
            if normalize_key(alias) in key:
                unit_id = uid
                unit_raw = alias
                break
        if unit_id:
            break
    if unit_id:
        out["unit_id"] = unit_id
        out["unit_raw"] = unit_raw
    if q is not None and unit_id:
        out["confidence"] = "HIGH"
    elif q is not None or unit_id:
        out["confidence"] = "MEDIUM"
    if q is None:
        # No numeric amount could be extracted.  Preserve the exact text and
        # mark it as qualitative instead of inventing a number.
        out["qualitative"] = 1
        if out["unit_id"] == "UNSPECIFIED":
            out["unit_id"] = "QUALITATIVE"
        if out["confidence"] == "LOW":
            out["confidence"] = "MEDIUM"
    out["preparation_note"] = remainder[:160]
    return out


def classify(row: dict[str, str]) -> str:
    return CATEGORY_CONTENT_TYPES.get(row.get("cat", "").strip(), "RECIPE")


def ingredient_type(name: str) -> str:
    key = normalize_key(name)
    for itype, words in INGREDIENT_TYPES:
        if any(normalize_key(word) in key for word in words):
            return itype
    return "OTHER"


def possible_allergens(name: str) -> list[str]:
    key = normalize_key(name)
    return [allergen for allergen, words in ALLERGENS.items() if any(normalize_key(word) in key for word in words)]


def difficulty(value: str) -> str | None:
    return {"مبتدی": "EASY", "متوسط": "MEDIUM", "پیشرفته": "HARD", "حرفه ای": "EXPERT", "حرفهای": "EXPERT"}.get(normalize_fa(value))


def json_dump(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def jsonl_dump(path: Path, rows: Iterable[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        for row in rows:
            fh.write(json.dumps(row, ensure_ascii=False) + "\n")


def main() -> None:
    if not INPUT.exists():
        raise SystemExit(f"input not found: {INPUT}")
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    DIST.mkdir(parents=True, exist_ok=True)
    if ZIP_PATH.exists():
        ZIP_PATH.unlink()

    input_hash = hashlib.sha256(INPUT.read_bytes()).hexdigest()
    with INPUT.open(encoding="utf-8-sig", newline="") as fh:
        reader = csv.DictReader(fh)
        fieldnames = reader.fieldnames or []
        raw_rows = list(reader)
    expected = ["name", "preparation_time", "cooking_time", "cooking_method", "cook_skill", "meal", "suitable_for", "cat", "subcat", "table"]
    if fieldnames != expected:
        raise SystemExit(f"unexpected columns: {fieldnames!r}")

    rows = []
    for i, raw in enumerate(raw_rows, 1):
        original = {k: (raw.get(k, "") or "") for k in fieldnames}
        row = {k: normalize_fa(original[k]) for k in fieldnames}
        row["_row"] = str(i)
        row["_original_name"] = original["name"]
        row["_original_cat"] = original["cat"]
        row["_original_subcat"] = original["subcat"]
        row["_original_method"] = original["cooking_method"]
        row["_original_skill"] = original["cook_skill"]
        row["_original_meal"] = original["meal"]
        row["_original_suitable"] = original["suitable_for"]
        row["_raw_json"] = json.dumps(original, ensure_ascii=False)
        rows.append(row)
    exact_names = [r["_original_name"] for r in rows]

    blank_counts = {k: sum(1 for r in rows if not r[k]) for k in expected}
    names = [r["name"] for r in rows]
    name_counts = Counter(normalize_key(n) for n in names)
    cat_counts = Counter(r["cat"] for r in rows)
    subcat_counts = Counter((r["cat"], r["subcat"]) for r in rows)

    recipes: list[dict[str, Any]] = []
    contents: list[dict[str, Any]] = []
    source_records: list[dict[str, Any]] = []
    exact_duplicates: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    recipe_by_sig: dict[str, dict[str, Any]] = {}
    content_by_sig: dict[str, dict[str, Any]] = {}
    ingredients: dict[str, dict[str, Any]] = {}
    unit_occurrences: Counter[str] = Counter()
    ingredient_line_count = 0

    def add_source(row: dict[str, str], disposition: str, canonical: str | None, reason: str) -> None:
        source_records.append({"row_number": int(row["_row"]), "raw_json": row["_raw_json"], "disposition": disposition, "canonical_id": canonical, "reason": reason})

    for row in rows:
        row_no = int(row["_row"])
        ctype = classify(row)
        parsed = parse_table(row["table"])
        if not row["name"].strip():
            rejected.append({"row_number": row_no, "name": "", "reason": "EMPTY_NAME", "cat": row["cat"], "subcat": row["subcat"]})
            add_source(row, "REJECTED", None, "EMPTY_NAME")
            continue
        if not parsed:
            rejected.append({"row_number": row_no, "name": row["name"], "reason": "NO_INGREDIENTS", "cat": row["cat"], "subcat": row["subcat"]})
            add_source(row, "REJECTED", None, "NO_INGREDIENTS")
            continue

        table_key = "|".join(normalize_key(x["name_original"]) + "=" + normalize_key(x["amount_raw"]) for x in parsed)
        sig_base = "|".join([normalize_key(row["name"]), normalize_digits(row["preparation_time"]), normalize_digits(row["cooking_time"]), normalize_key(row["cooking_method"]), normalize_key(row["cook_skill"]), normalize_key(row["meal"]), normalize_digits(row["suitable_for"]), normalize_key(row["cat"]), normalize_key(row["subcat"]), table_key])
        sig = hashlib.sha256(sig_base.encode()).hexdigest()

        if ctype == "RECIPE":
            if sig in recipe_by_sig:
                canonical = recipe_by_sig[sig]
                exact_duplicates.append({"source_row": row_no, "canonical_id": canonical["id"], "title": row["name"], "duplicate_type": "EXACT", "similarity_score": 1.0, "note": "تمام فیلدها و مواد نرمال‌شده یکسان است."})
                add_source(row, "EXACT_DUPLICATE", canonical["id"], "EXACT_DUPLICATE")
                continue
            rid = stable_id("rcp", sig, 14)
            if any(r["id"] == rid for r in recipes):
                rid = stable_id("rcp", sig + str(row_no), 14)
            lines = []
            for pos, item in enumerate(parsed, 1):
                key = normalize_key(item["name_original"])
                if not key:
                    continue
                if key not in ingredients:
                    ingredients[key] = {"id": stable_id("ing", key, 12), "name_fa": item["name_fa"], "name_original": item["name_original"], "normalized_name": key, "aliases": [], "ingredient_type": ingredient_type(item["name_original"]), "type_confidence": "HEURISTIC", "possible_allergens": possible_allergens(item["name_original"]), "occurrence_count": 0}
                ing = ingredients[key]
                if item["name_original"] not in ing["aliases"]:
                    ing["aliases"].append(item["name_original"])
                ing["occurrence_count"] += 1
                q = parse_quantity(item["amount_raw"])
                unit_occurrences[q["unit_id"]] += 1
                lines.append({"position": pos, "ingredient_id": ing["id"], "name_original": item["name_original"], "name_fa": item["name_fa"], "quantity": q["quantity"], "quantity_max": q["quantity_max"], "unit_id": q["unit_id"], "unit_raw": q["unit_raw"], "quantity_text": q["quantity_text"], "qualitative": q["qualitative"], "optional": None, "preparation_note": q["preparation_note"], "extraction_confidence": q["confidence"], "raw_text": f'{item["name_original"]} — {item["amount_raw"]}'})
                ingredient_line_count += 1
            prep, cook = parse_int(row["preparation_time"]), parse_int(row["cooking_time"])
            total = prep + cook if prep is not None and cook is not None else None
            diff = difficulty(row["cook_skill"])
            flags = ["MISSING_INSTRUCTIONS", "SOURCE_LICENSE_UNKNOWN", "NO_NUTRITION"]
            if prep is None:
                flags.append("PREPARATION_TIME_MISSING")
            if diff is None:
                flags.append("DIFFICULTY_UNKNOWN")
            if parse_int(row["suitable_for"]) is not None:
                flags.append("SERVINGS_INTERPRETED_FROM_SUITABLE_FOR")
            if any(x["extraction_confidence"] == "LOW" for x in lines):
                flags.append("INGREDIENT_UNITS_PARTIAL")
            recipe = {"id": rid, "slug": "r-" + sha1(sig, 10), "title_fa": row["name"], "title_original": row["_original_name"], "description": f'دستور «{row["name"]}» از دسته «{row["_original_cat"]}» و زیردسته «{row["_original_subcat"]}»، ردیف {row_no}. متن مراحل پخت در منبع ورودی وجود ندارد.', "origin_country": None, "origin_region": None, "content_type": "RECIPE", "primary_category_id": "cat-" + sha1(normalize_key(row["cat"]), 10), "primary_category_original": row["_original_cat"], "subcategory_id": "sub-" + sha1(normalize_key(row["cat"]) + "|" + normalize_key(row["subcat"]), 10), "subcategory_original": row["_original_subcat"], "difficulty": diff, "preparation_minutes": prep, "cooking_minutes": cook, "total_minutes": total, "default_servings": parse_int(row["suitable_for"]), "servings_status": "SOURCE_VALUE_UNVERIFIED" if parse_int(row["suitable_for"]) is not None else "UNKNOWN", "cooking_method": row["_original_method"], "cook_skill_original": row["_original_skill"], "meal": row["_original_meal"], "suitable_for_original": row["_original_suitable"], "instructions_status": "MISSING", "verification_status": "REVIEW_REQUIRED", "source_id": SOURCE_ID, "source_license_id": LICENSE_ID, "created_at": BUILD_TIME, "updated_at": BUILD_TIME, "name_group": stable_id("grp", normalize_key(row["name"]), 10), "source_row": row_no, "ingredient_signature": hashlib.sha1("|".join(sorted(x["ingredient_id"] for x in lines)).encode()).hexdigest(), "quality_flags": flags, "ingredients": lines, "steps": [], "tags": [{"name_fa": row["_original_meal"], "tag_type": "MEAL"}, {"name_fa": row["_original_method"], "tag_type": "METHOD"}, {"name_fa": row["_original_skill"], "tag_type": "SKILL"}, {"name_fa": row["_original_cat"], "tag_type": "SOURCE_CATEGORY"}, {"name_fa": row["_original_subcat"], "tag_type": "SOURCE_SUBCATEGORY"}]}
            recipes.append(recipe)
            recipe_by_sig[sig] = recipe
            add_source(row, "RECIPE_PRIMARY", rid, "UNIQUE_EXACT_CONTENT")
        else:
            if sig in content_by_sig:
                canonical = content_by_sig[sig]
                exact_duplicates.append({"source_row": row_no, "canonical_id": canonical["id"], "title": row["name"], "duplicate_type": "EXACT", "similarity_score": 1.0, "note": "رکورد محتوای غیرغذایی تکراری است."})
                add_source(row, "EXACT_DUPLICATE", canonical["id"], "EXACT_DUPLICATE")
                continue
            cid = stable_id("cnt", sig, 14)
            content_ingredients = []
            for pos, item in enumerate(parsed, 1):
                q = parse_quantity(item["amount_raw"])
                content_ingredients.append({"position": pos, "name_original": item["name_original"], "amount_raw": item["amount_raw"], "quantity": q["quantity"], "quantity_max": q["quantity_max"], "unit_id": q["unit_id"], "qualitative": q["qualitative"], "confidence": q["confidence"]})
            content = {"id": cid, "title_fa": row["name"], "title_original": row["_original_name"], "content_type": ctype, "primary_category_id": "cat-" + sha1(normalize_key(row["cat"]), 10), "primary_category_original": row["_original_cat"], "subcategory_id": "sub-" + sha1(normalize_key(row["cat"]) + "|" + normalize_key(row["subcat"]), 10), "subcategory_original": row["_original_subcat"], "summary": f'محتوای «{row["name"]}» در دسته «{row["_original_cat"]}» و زیردسته «{row["_original_subcat"]}»، ردیف {row_no}.', "body": "متن آموزش یا مقاله در فایل ورودی موجود نیست؛ فقط فراداده و فهرست مواد ثبت شده است.", "source_id": SOURCE_ID, "source_license_id": LICENSE_ID, "verification_status": "REVIEW_REQUIRED", "metadata": {"preparation_time": row["preparation_time"], "cooking_time": row["cooking_time"], "cooking_method": row["_original_method"], "cook_skill": row["_original_skill"], "meal": row["_original_meal"], "suitable_for": row["_original_suitable"]}, "ingredients": content_ingredients, "steps": [], "source_row": row_no, "quality_flags": ["MISSING_INSTRUCTIONS", "SOURCE_LICENSE_UNKNOWN", "CONTENT_BODY_MISSING"]}
            contents.append(content)
            content_by_sig[sig] = content
            add_source(row, "NON_FOOD", cid, ctype)

    # Report same-name variants; never merge them.
    duplicates = list(exact_duplicates)
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for recipe in recipes:
        groups[recipe["name_group"]].append(recipe)
    semantic_pairs = 0
    for group in groups.values():
        if len(group) < 2:
            continue
        group.sort(key=lambda r: r["source_row"])
        for i in range(len(group)):
            for j in range(i + 1, len(group)):
                a, b = group[i], group[j]
                sa = {x["ingredient_id"] for x in a["ingredients"]}
                sb = {x["ingredient_id"] for x in b["ingredients"]}
                union = sa | sb
                score = len(sa & sb) / len(union) if union else 0.0
                if score >= 0.7 or (a["preparation_minutes"] == b["preparation_minutes"] and a["cooking_minutes"] == b["cooking_minutes"] and a["cooking_method"] == b["cooking_method"]):
                    semantic_pairs += 1
                    duplicates.append({"source_row": b["source_row"], "canonical_id": a["id"], "duplicate_id": b["id"], "title": a["title_fa"], "duplicate_type": "SEMANTIC_NAME_VARIANT", "similarity_score": round(score, 4), "note": "نام یکسان/مواد مشابه؛ برای بازبینی انسانی نگه داشته شد."})

    same_name_rows = []
    same_name_groups = 0
    for group_id, group in groups.items():
        if len(group) < 2:
            continue
        same_name_groups += 1
        group.sort(key=lambda r: r["source_row"])
        first = group[0]
        first_set = {x["ingredient_id"] for x in first["ingredients"]}
        max_score = 0.0
        for i in range(len(group)):
            for j in range(i + 1, len(group)):
                a = {x["ingredient_id"] for x in group[i]["ingredients"]}
                b = {x["ingredient_id"] for x in group[j]["ingredients"]}
                union = a | b
                max_score = max(max_score, len(a & b) / len(union) if union else 0.0)
        for r in group:
            if "SAME_NAME_VARIANT" not in r["quality_flags"]:
                r["quality_flags"].append("SAME_NAME_VARIANT")
            current = {x["ingredient_id"] for x in r["ingredients"]}
            union = first_set | current
            score = len(first_set & current) / len(union) if union else 0.0
            same_name_rows.append({"group_id": group_id, "group_size": len(group), "recipe_id": r["id"], "title_fa": r["title_fa"], "source_row": r["source_row"], "preparation_minutes": r["preparation_minutes"], "cooking_minutes": r["cooking_minutes"], "cooking_method": r["cooking_method"], "jaccard_to_first": round(score, 4), "max_pair_similarity": round(max_score, 4), "review_status": "UNREVIEWED"})

    cat_ids = {}
    cat_original = {}
    subcat_original = {}
    for row in rows:
        cat_original.setdefault(row["cat"], row["_original_cat"])
        subcat_original.setdefault((row["cat"], row["subcat"]), row["_original_subcat"])
    main_categories = []
    for order, (cat, count) in enumerate(cat_counts.most_common()):
        cid = "cat-" + sha1(normalize_key(cat), 10)
        cat_ids[cat] = cid
        main_categories.append({"id": cid, "name_fa": cat, "name_original": cat_original[cat], "parent_id": None, "content_type": classify({"cat": cat}), "description": f"دسته اصلی منبع با {count} ردیف.", "sort_order": order, "source_count": count})
    subcategories = []
    for order, ((cat, subcat), count) in enumerate(subcat_counts.most_common()):
        subcategories.append({"id": "sub-" + sha1(normalize_key(cat) + "|" + normalize_key(subcat), 10), "name_fa": subcat, "name_original": subcat_original[(cat, subcat)], "parent_id": cat_ids[cat], "content_type": classify({"cat": cat}), "description": f"زیردسته «{subcat_original[(cat, subcat)]}» از «{cat_original[cat]}» با {count} ردیف.", "sort_order": order, "source_count": count})

    ingredient_list = sorted(ingredients.values(), key=lambda x: (-x["occurrence_count"], x["name_fa"]))
    units_out = [{"id": uid, "name_fa": fa, "name_en": en, "kind": kind, "aliases": aliases, "occurrences": unit_occurrences.get(uid, 0)} for uid, fa, en, kind, aliases in UNITS]
    sources = [{"id": SOURCE_ID, "source_name": SOURCE_NAME, "source_url": None, "collection_name": "Persian_Cooking", "access_date": BUILD_TIME[:10], "license_id": LICENSE_ID, "attribution": "ورودی کاربر؛ وضعیت مجوز و حق بازنشر نامشخص است.", "commercial_use_allowed": 0, "redistribution_allowed": 0, "verification_status": "UNRESOLVED", "input_sha256": input_hash, "input_rows": len(rows)}]
    licenses = [{"id": LICENSE_ID, "license_name": "UNKNOWN — نیازمند بررسی حقوقی", "license_url": None, "commercial_use_allowed": 0, "redistribution_allowed": 0, "attribution_required": 1, "notes": "هیچ ادعای مجوزی برای متن دستورها یا فایل ورودی ثبت نشده است. انتشار تجاری تا احراز حقوق مجاز نیست."}]

    stats = {"input_rows": len(rows), "unique_raw_names": len(set(exact_names)), "unique_normalized_names": len(name_counts), "duplicate_name_rows": sum(c - 1 for c in name_counts.values()), "exact_duplicate_rows": len(exact_duplicates), "exact_duplicate_food_rows": sum(1 for d in exact_duplicates if str(d.get("canonical_id", "")).startswith("rcp-")), "exact_duplicate_content_rows": sum(1 for d in exact_duplicates if str(d.get("canonical_id", "")).startswith("cnt-")), "food_rows": sum(1 for r in rows if classify(r) == "RECIPE"), "non_food_rows": sum(1 for r in rows if classify(r) != "RECIPE"), "recipes": len(recipes), "verified_recipes": 0, "review_required_recipes": len(recipes), "rejected_rows": len(rejected), "content_records": len(contents), "content_tutorials": sum(1 for c in contents if c["content_type"] == "COOKING_TUTORIAL"), "content_decorations": sum(1 for c in contents if c["content_type"] == "FOOD_DECORATION"), "content_techniques": sum(1 for c in contents if c["content_type"] == "TECHNIQUE_GUIDE"), "ingredients": len(ingredient_list), "recipe_ingredient_lines": ingredient_line_count, "qualitative_ingredient_lines": sum(1 for r in recipes for x in r["ingredients"] if x["qualitative"] == 1), "numeric_ingredient_lines": sum(1 for r in recipes for x in r["ingredients"] if x["quantity"] is not None), "unknown_unit_lines": sum(1 for r in recipes for x in r["ingredients"] if x["unit_id"] == "UNSPECIFIED"), "high_confidence_lines": sum(1 for r in recipes for x in r["ingredients"] if x["extraction_confidence"] == "HIGH"), "medium_confidence_lines": sum(1 for r in recipes for x in r["ingredients"] if x["extraction_confidence"] == "MEDIUM"), "categories": len(main_categories), "subcategories": len(subcategories), "semantic_duplicate_pairs": semantic_pairs, "same_name_variant_groups": same_name_groups, "same_name_variant_recipes": len(same_name_rows), "missing_instructions": sum(1 for r in recipes if r["instructions_status"] == "MISSING"), "blank_fields": blank_counts, "invalid_time_rows": sum(1 for r in rows if (r["preparation_time"] and parse_int(r["preparation_time"]) is None) or (r["cooking_time"] and parse_int(r["cooking_time"]) is None)), "invalid_servings_rows": sum(1 for r in rows if r["suitable_for"] and parse_int(r["suitable_for"]) is None), "target_verified": 50000, "gap_to_target": 50000}

    jsonl_dump(OUT / "jsonl" / "recipes.jsonl", recipes)
    buckets: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for c in contents:
        buckets[c["content_type"]].append(c)
    for ctype, filename in CONTENT_FILES.items():
        jsonl_dump(OUT / "jsonl" / filename, buckets.get(ctype, []))
    jsonl_dump(OUT / "reports" / "review_required.jsonl", [dict(r, review_reasons=r["quality_flags"]) for r in recipes] + [dict(c, review_reasons=c["quality_flags"]) for c in contents])
    json_dump(OUT / "categories" / "main_categories.json", main_categories)
    json_dump(OUT / "categories" / "subcategories.json", subcategories)
    json_dump(OUT / "categories" / "categories.json", main_categories + subcategories)
    json_dump(OUT / "ingredients" / "ingredients.json", ingredient_list)
    json_dump(OUT / "ingredients" / "units.json", units_out)
    json_dump(OUT / "sources" / "sources.json", sources)
    json_dump(OUT / "sources" / "licenses.json", licenses)
    json_dump(OUT / "reports" / "stats.json", stats)

    with (OUT / "reports" / "duplicate_report.csv").open("w", encoding="utf-8-sig", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["source_row", "canonical_id", "duplicate_id", "title", "duplicate_type", "similarity_score", "note"])
        w.writeheader()
        for d in duplicates:
            w.writerow({k: d.get(k, "") for k in w.fieldnames})
    with (OUT / "reports" / "same_name_variants.csv").open("w", encoding="utf-8-sig", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["group_id", "group_size", "recipe_id", "title_fa", "source_row", "preparation_minutes", "cooking_minutes", "cooking_method", "jaccard_to_first", "max_pair_similarity", "review_status"])
        w.writeheader()
        for x in same_name_rows:
            w.writerow(x)
    with (OUT / "reports" / "rejected_records.csv").open("w", encoding="utf-8-sig", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["row_number", "name", "reason", "cat", "subcat"])
        w.writeheader()
        for x in rejected:
            w.writerow(x)

    db_path = OUT / "database" / "ashpazyar.db"
    db_path.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(db_path)
    con.execute("PRAGMA foreign_keys = ON")
    con.executescript("""
    CREATE TABLE licenses (id TEXT PRIMARY KEY, license_name TEXT NOT NULL, license_url TEXT, commercial_use_allowed INTEGER NOT NULL, redistribution_allowed INTEGER NOT NULL, attribution_required INTEGER NOT NULL, notes TEXT);
    CREATE TABLE sources (id TEXT PRIMARY KEY, source_name TEXT NOT NULL, source_url TEXT, collection_name TEXT, access_date TEXT, license_id TEXT NOT NULL REFERENCES licenses(id), attribution TEXT, commercial_use_allowed INTEGER NOT NULL, redistribution_allowed INTEGER NOT NULL, verification_status TEXT NOT NULL);
    CREATE TABLE categories (id TEXT PRIMARY KEY, name_fa TEXT NOT NULL, name_original TEXT NOT NULL, parent_id TEXT REFERENCES categories(id), content_type TEXT NOT NULL, description TEXT, sort_order INTEGER NOT NULL);
    CREATE TABLE units (id TEXT PRIMARY KEY, name_fa TEXT NOT NULL, name_en TEXT, kind TEXT, aliases_json TEXT);
    CREATE TABLE ingredients (id TEXT PRIMARY KEY, name_fa TEXT NOT NULL, name_original TEXT, normalized_name TEXT NOT NULL, ingredient_type TEXT, type_confidence TEXT, possible_allergens_json TEXT, aliases_json TEXT, occurrence_count INTEGER NOT NULL);
    CREATE TABLE recipes (id TEXT PRIMARY KEY, slug TEXT UNIQUE NOT NULL, title_fa TEXT NOT NULL, title_original TEXT NOT NULL, description TEXT, origin_country TEXT, origin_region TEXT, content_type TEXT NOT NULL, primary_category_id TEXT REFERENCES categories(id), subcategory_id TEXT REFERENCES categories(id), difficulty TEXT, preparation_minutes INTEGER, cooking_minutes INTEGER, total_minutes INTEGER, default_servings INTEGER, servings_status TEXT, cooking_method TEXT, cook_skill_original TEXT, meal TEXT, suitable_for_original TEXT, instructions_status TEXT NOT NULL, verification_status TEXT NOT NULL, source_id TEXT REFERENCES sources(id), source_license_id TEXT REFERENCES licenses(id), created_at TEXT, updated_at TEXT, name_group TEXT, source_row INTEGER, ingredient_signature TEXT, quality_flags_json TEXT);
    CREATE TABLE recipe_ingredients (recipe_id TEXT NOT NULL REFERENCES recipes(id), position INTEGER NOT NULL, ingredient_id TEXT NOT NULL REFERENCES ingredients(id), quantity REAL, quantity_max REAL, unit_id TEXT NOT NULL REFERENCES units(id), unit_raw TEXT, quantity_text TEXT, qualitative INTEGER NOT NULL DEFAULT 0, optional INTEGER, preparation_note TEXT, extraction_confidence TEXT, raw_text TEXT, PRIMARY KEY (recipe_id, position));
    CREATE TABLE recipe_steps (id TEXT PRIMARY KEY, recipe_id TEXT NOT NULL REFERENCES recipes(id), step_number INTEGER NOT NULL, title TEXT, instruction TEXT, ingredients_used_json TEXT, duration_seconds INTEGER, temperature_value REAL, temperature_unit TEXT, heat_level TEXT, equipment_json TEXT, success_indicators_json TEXT, common_mistakes_json TEXT, recovery_instructions TEXT, safety_notes_json TEXT, image_reference TEXT, source_reference TEXT);
    CREATE TABLE tags (id TEXT PRIMARY KEY, name_fa TEXT NOT NULL, tag_type TEXT NOT NULL);
    CREATE TABLE recipe_tags (recipe_id TEXT NOT NULL REFERENCES recipes(id), tag_id TEXT NOT NULL REFERENCES tags(id), PRIMARY KEY (recipe_id, tag_id));
    CREATE TABLE content_articles (id TEXT PRIMARY KEY, title_fa TEXT NOT NULL, title_original TEXT NOT NULL, content_type TEXT NOT NULL, primary_category_id TEXT REFERENCES categories(id), subcategory_id TEXT REFERENCES categories(id), summary TEXT, body TEXT, source_id TEXT REFERENCES sources(id), source_license_id TEXT REFERENCES licenses(id), verification_status TEXT NOT NULL, metadata_json TEXT, ingredients_json TEXT, source_row INTEGER, quality_flags_json TEXT);
    CREATE TABLE recipe_duplicates (primary_recipe_id TEXT NOT NULL REFERENCES recipes(id), duplicate_recipe_id TEXT NOT NULL REFERENCES recipes(id), duplicate_type TEXT NOT NULL, similarity_score REAL, resolution_status TEXT, note TEXT, PRIMARY KEY (primary_recipe_id, duplicate_recipe_id));
    CREATE TABLE recipe_nutrition (recipe_id TEXT PRIMARY KEY REFERENCES recipes(id), calories REAL, protein REAL, carbohydrates REAL, fat REAL, fiber REAL, sodium REAL, calculation_method TEXT, confidence TEXT, source_id TEXT REFERENCES sources(id));
    CREATE TABLE recipe_media (id TEXT PRIMARY KEY, recipe_id TEXT REFERENCES recipes(id), media_type TEXT, media_reference TEXT, license_id TEXT REFERENCES licenses(id), attribution TEXT);
    CREATE TABLE source_records (row_number INTEGER PRIMARY KEY, raw_json TEXT NOT NULL, disposition TEXT NOT NULL, canonical_id TEXT, reason TEXT);
    CREATE INDEX idx_recipes_category ON recipes(primary_category_id);
    CREATE INDEX idx_recipes_status ON recipes(verification_status);
    CREATE INDEX idx_recipes_name_group ON recipes(name_group);
    CREATE INDEX idx_recipes_title ON recipes(title_fa);
    CREATE INDEX idx_ingredients_name ON ingredients(normalized_name);
    CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients(ingredient_id);
    CREATE INDEX idx_content_type ON content_articles(content_type);
    CREATE INDEX idx_source_records_disposition ON source_records(disposition);
    CREATE VIRTUAL TABLE recipes_fts USING fts5(title_fa, title_original, description, ingredients_text, category_text, tags_text, tokenize='unicode61');
    CREATE VIRTUAL TABLE content_fts USING fts5(title_fa, title_original, summary, body, category_text, tokenize='unicode61');
    """)

    con.executemany("INSERT INTO licenses VALUES (?,?,?,?,?,?,?)", [(x["id"], x["license_name"], x["license_url"], x["commercial_use_allowed"], x["redistribution_allowed"], x["attribution_required"], x["notes"]) for x in licenses])
    con.executemany("INSERT INTO sources VALUES (?,?,?,?,?,?,?,?,?,?)", [(x["id"], x["source_name"], x["source_url"], x["collection_name"], x["access_date"], x["license_id"], x["attribution"], x["commercial_use_allowed"], x["redistribution_allowed"], x["verification_status"]) for x in sources])
    con.executemany("INSERT INTO categories VALUES (?,?,?,?,?,?,?)", [(x["id"], x["name_fa"], x["name_original"], x["parent_id"], x["content_type"], x["description"], x["sort_order"]) for x in main_categories + subcategories])
    con.executemany("INSERT INTO units VALUES (?,?,?,?,?)", [(x["id"], x["name_fa"], x["name_en"], x["kind"], json.dumps(x["aliases"], ensure_ascii=False)) for x in units_out])
    con.executemany("INSERT INTO ingredients VALUES (?,?,?,?,?,?,?,?,?)", [(x["id"], x["name_fa"], x["name_original"], x["normalized_name"], x["ingredient_type"], x["type_confidence"], json.dumps(x["possible_allergens"], ensure_ascii=False), json.dumps(x["aliases"], ensure_ascii=False), x["occurrence_count"]) for x in ingredient_list])
    con.executemany("INSERT INTO recipes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", [(r["id"], r["slug"], r["title_fa"], r["title_original"], r["description"], r["origin_country"], r["origin_region"], r["content_type"], r["primary_category_id"], r["subcategory_id"], r["difficulty"], r["preparation_minutes"], r["cooking_minutes"], r["total_minutes"], r["default_servings"], r["servings_status"], r["cooking_method"], r["cook_skill_original"], r["meal"], r["suitable_for_original"], r["instructions_status"], r["verification_status"], r["source_id"], r["source_license_id"], r["created_at"], r["updated_at"], r["name_group"], r["source_row"], r["ingredient_signature"], json.dumps(r["quality_flags"], ensure_ascii=False)) for r in recipes])
    con.executemany("INSERT INTO recipe_ingredients VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", [(r["id"], x["position"], x["ingredient_id"], x["quantity"], x["quantity_max"], x["unit_id"], x["unit_raw"], x["quantity_text"], x["qualitative"], x["optional"], x["preparation_note"], x["extraction_confidence"], x["raw_text"]) for r in recipes for x in r["ingredients"]])
    tag_map = {}
    recipe_tags = set()
    for r in recipes:
        for t in r["tags"]:
            if not t["name_fa"].strip():
                continue
            tid = "tag-" + sha1(t["tag_type"] + "|" + normalize_key(t["name_fa"]), 10)
            tag_map[tid] = (tid, t["name_fa"], t["tag_type"])
            recipe_tags.add((r["id"], tid))
    con.executemany("INSERT INTO tags VALUES (?,?,?)", list(tag_map.values()))
    con.executemany("INSERT INTO recipe_tags VALUES (?,?)", sorted(recipe_tags))
    con.executemany("INSERT INTO content_articles VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", [(c["id"], c["title_fa"], c["title_original"], c["content_type"], c["primary_category_id"], c["subcategory_id"], c["summary"], c["body"], c["source_id"], c["source_license_id"], c["verification_status"], json.dumps(c["metadata"], ensure_ascii=False), json.dumps(c["ingredients"], ensure_ascii=False), c["source_row"], json.dumps(c["quality_flags"], ensure_ascii=False)) for c in contents])
    con.executemany("INSERT INTO recipe_duplicates VALUES (?,?,?,?,?,?)", [(d["canonical_id"], d["duplicate_id"], d["duplicate_type"], d["similarity_score"], "UNREVIEWED", d["note"]) for d in duplicates if d.get("duplicate_id") and d.get("duplicate_type") != "EXACT"])
    con.executemany("INSERT INTO source_records VALUES (?,?,?,?,?)", [(x["row_number"], x["raw_json"], x["disposition"], x["canonical_id"], x["reason"]) for x in source_records])

    for r in recipes:
        con.execute("INSERT INTO recipes_fts(rowid, title_fa, title_original, description, ingredients_text, category_text, tags_text) SELECT rowid, ?, ?, ?, ?, ?, ? FROM recipes WHERE id = ?", (r["title_fa"], r["title_original"], r["description"], " ".join(x["name_fa"] for x in r["ingredients"]), r["primary_category_original"] + " " + r["subcategory_original"], " ".join(x["name_fa"] for x in r["tags"]), r["id"]))
    for c in contents:
        con.execute("INSERT INTO content_fts(rowid, title_fa, title_original, summary, body, category_text) SELECT rowid, ?, ?, ?, ?, ? FROM content_articles WHERE id = ?", (c["title_fa"], c["title_original"], c["summary"], c["body"], c["primary_category_original"] + " " + c["subcategory_original"], c["id"]))
    con.commit()

    integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
    fk_issues = con.execute("PRAGMA foreign_key_check").fetchall()
    recipe_count_db = con.execute("SELECT COUNT(*) FROM recipes").fetchone()[0]
    content_count_db = con.execute("SELECT COUNT(*) FROM content_articles").fetchone()[0]
    source_count_db = con.execute("SELECT COUNT(*) FROM source_records").fetchone()[0]
    fts_hits = con.execute("SELECT COUNT(*) FROM recipes_fts WHERE recipes_fts MATCH ?", ("کیک",)).fetchone()[0]
    con.close()
    if integrity != "ok" or fk_issues or recipe_count_db != len(recipes) or content_count_db != len(contents) or source_count_db != len(rows):
        raise SystemExit(f"build checks failed: integrity={integrity} fk={fk_issues} counts={recipe_count_db}/{content_count_db}/{source_count_db}")

    docs = OUT / "docs"
    docs.mkdir(parents=True, exist_ok=True)
    quality = f"""# گزارش کیفیت داده آشپزیار

## دامنه
- ورودی: train.csv
- SHA-256: {input_hash}
- ردیف ورودی: {stats['input_rows']}
- زمان پردازش: {BUILD_TIME}

## آمار واقعی
| شاخص | مقدار |
|---|---:|
| ردیف ورودی | {stats['input_rows']} |
| نام خام یکتا | {stats['unique_raw_names']} |
| نام نرمال‌شده یکتا | {stats['unique_normalized_names']} |
| ردیف دارای نام تکراری | {stats['duplicate_name_rows']} |
| ردیف تکراری دقیق | {stats['exact_duplicate_rows']} |
| تکراری دقیق غذایی | {stats['exact_duplicate_food_rows']} |
| تکراری دقیق غیرغذایی | {stats['exact_duplicate_content_rows']} |
| ردیف غذایی | {stats['food_rows']} |
| ردیف غیرغذایی | {stats['non_food_rows']} |
| دستور مستقل | {stats['recipes']} |
| دستور VERIFIED | **0** |
| دستور REVIEW_REQUIRED | {stats['review_required_recipes']} |
| ردیف REJECTED | {stats['rejected_rows']} |
| محتوای غیرغذایی | {stats['content_records']} |
| آموزش تصویری | {stats['content_tutorials']} |
| تزئینات | {stats['content_decorations']} |
| تکنیک/مقاله | {stats['content_techniques']} |
| ماده یکتا | {stats['ingredients']} |
| خط ماده | {stats['recipe_ingredient_lines']} |
| ماده با مقدار عددی | {stats['numeric_ingredient_lines']} |
| ماده کیفی (بدون عدد) | {stats['qualitative_ingredient_lines']} |
| واحد نامشخص | {stats['unknown_unit_lines']} |
| اطمینان بالا | {stats['high_confidence_lines']} |
| اطمینان متوسط | {stats['medium_confidence_lines']} |
| دسته اصلی | {stats['categories']} |
| زیردسته | {stats['subcategories']} |
| جفت مشابه اسمی | {stats['semantic_duplicate_pairs']} |
| گروه نام یکسان | {stats['same_name_variant_groups']} |
| رکورد نام یکسان | {stats['same_name_variant_recipes']} |
| دستور فاقد مراحل | {stats['missing_instructions']} |
| زمان نامعتبر | {stats['invalid_time_rows']} |
| تعداد نفرات نامعتبر | {stats['invalid_servings_rows']} |
| هدف | 50000 |
| فاصله | **50000** |

## نتیجه
هیچ ستون مراحل پخت در فایل ورودی وجود ندارد؛ بنابراین هیچ دستور غذایی VERIFIED نشد. زمان، دما، تغذیه و تعداد نفرات مستقل ساخته نشدند؛ مقدار default_servings فقط کپی عددی ستون suitable_for با وضعیت SOURCE_VALUE_UNVERIFIED است. مجوز منبع نامشخص است و استفاده تجاری مجاز نیست. رکوردهای غیرغذایی به‌عنوان Recipe ثبت نشدند.
"""
    (OUT / "reports" / "DATA_QUALITY_REPORT.md").write_text(quality, encoding="utf-8")
    (docs / "DATA_QUALITY_REPORT.md").write_text(quality, encoding="utf-8")
    (ROOT / "DATA_QUALITY_REPORT.md").write_text(quality, encoding="utf-8")
    sources_doc = f"""# منابع داده

- منبع: {SOURCE_NAME}
- فایل: train.csv
- SHA-256: {input_hash}
- ردیف: {len(rows)}
- مجوز: نامشخص / نیازمند بررسی حقوقی
- استفاده تجاری: مجاز نیست
- بازنشر: مجاز نیست
- نتیجه: فقط برای پردازش داخلی استفاده شد؛ هیچ رکوردی VERIFIED نیست.
"""
    (docs / "DATA_SOURCES.md").write_text(sources_doc, encoding="utf-8")
    licenses_doc = f"""# مجوزها

- شناسه: {LICENSE_ID}
- نام: UNKNOWN
- استفاده تجاری: 0
- بازنشر: 0
- انتساب: الزامی تا تعیین وضعیت
- تمام خروجی‌ها REVIEW_REQUIRED هستند و برای انتشار تجاری آماده نیستند.
"""
    (docs / "DATA_LICENSES.md").write_text(licenses_doc, encoding="utf-8")
    schema_doc = f"""# طرح داده خروجی

- recipes: {len(recipes)} رکورد
- recipe_ingredients: {ingredient_line_count} خط
- ingredients: {len(ingredient_list)} ماده
- categories: {len(main_categories)} دسته و {len(subcategories)} زیردسته
- content_articles: {len(contents)} رکورد غیرغذایی
- source_records: {len(source_records)} ردیف ورودی با وضعیت نهایی
- recipe_steps، recipe_nutrition و recipe_media آماده‌اند اما داده ساختگی ندارند.
- FTS5 با توکنایزر unicode61 برای recipes و content فعال است.
"""
    (docs / "DATA_SCHEMA.md").write_text(schema_doc, encoding="utf-8")
    progress = f"""# پیشرفت داده

| وضعیت | مقدار |
|---|---:|
| ورودی | {len(rows)} |
| دستور مستقل | {len(recipes)} |
| VERIFIED | 0 |
| REVIEW_REQUIRED | {len(recipes) + len(contents)} |
| REJECTED | {len(rejected)} |
| غیرغذایی | {len(contents)} |
| فاصله تا 50000 | **50000** |

هیچ رکوردی برای پر کردن عدد ساخته نشد.
"""
    (OUT / "reports" / "DATA_PROGRESS.md").write_text(progress, encoding="utf-8")
    (docs / "DATA_PROGRESS.md").write_text(progress, encoding="utf-8")
    (ROOT / "DATA_PROGRESS.md").write_text(progress, encoding="utf-8")
    readme = f"""# AshpazYar Recipe Knowledge Base

بسته ساخته‌شده از train.csv: {len(rows)} ردیف ورودی، {len(recipes)} دستور مستقل و {len(contents)} محتوای غیرغذایی.

## هشدار
هیچ دستور VERIFIED وجود ندارد. منبع، مجوز و حق بازنشر نامشخص است. ستون مراحل پخت در ورودی نیست. زمان، دما، تغذیه و تعداد نفرات حدس زده نشده‌اند.

## ساختار
- database/ashpazyar.db
- jsonl/recipes.jsonl
- jsonl/articles.jsonl, tutorials.jsonl, decorations.jsonl
- categories/, ingredients/, sources/, reports/, docs/

## جستجو
SELECT r.title_fa FROM recipes_fts f JOIN recipes r ON r.rowid = f.rowid WHERE recipes_fts MATCH 'کیک' LIMIT 10;

## بازتولید
python tools/build_dataset.py
python tools/validate_dataset.py
"""
    (OUT / "README.md").write_text(readme, encoding="utf-8")
    (docs / "README.md").write_text(readme, encoding="utf-8")
    log = f"""# گزارش پردازش

- زمان: {BUILD_TIME}
- ورودی: train.csv
- SHA-256: {input_hash}
- ردیف: {len(rows)}
- غذایی: {stats['food_rows']}
- غیرغذایی: {stats['non_food_rows']}
- تکراری دقیق: {len(exact_duplicates)}
- دستور مستقل: {len(recipes)}
- محتوا: {len(contents)}
- خط ماده: {ingredient_line_count}
- ماده یکتا: {len(ingredient_list)}
- دسته/زیردسته: {len(main_categories)}/{len(subcategories)}
- VERIFIED: 0
- REVIEW_REQUIRED: {len(recipes) + len(contents)}
- REJECTED: {len(rejected)}
- integrity_check: {integrity}
- foreign_key_check: {len(fk_issues)}
- FTS5 (کیک): {fts_hits}
"""
    (OUT / "PROCESSING_LOG.md").write_text(log, encoding="utf-8")
    (ROOT / "PROCESSING_LOG.md").write_text(log, encoding="utf-8")

    manifest_files = []
    for path in sorted(OUT.rglob("*")):
        if path.is_file():
            manifest_files.append({"path": path.relative_to(OUT).as_posix(), "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    json_dump(OUT / "MANIFEST.json", {"generated_at": BUILD_TIME, "input_file": "train.csv", "input_sha256": input_hash, "input_rows": len(rows), "recipes": len(recipes), "content_records": len(contents), "verified_recipes": 0, "files": manifest_files})

    with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in sorted(OUT.rglob("*")):
            if path.is_file():
                zf.write(path, Path("ashpazyar_dataset") / path.relative_to(OUT))
    zip_hash = hashlib.sha256(ZIP_PATH.read_bytes()).hexdigest()
    (DIST / "ashpazyar_dataset.sha256").write_text(zip_hash + "  ashpazyar_dataset.zip\n", encoding="utf-8")
    print(json.dumps({**stats, "db": str(db_path), "zip": str(ZIP_PATH), "zip_sha256": zip_hash, "fts_hits": fts_hits}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
