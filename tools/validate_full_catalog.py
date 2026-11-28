#!/usr/bin/env python3
"""Validate the bundled full catalog without building Android."""

from __future__ import annotations

import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "seed"
ALLOWED_UNITS = {
    "GRAM", "KILOGRAM", "MILLILITER", "LITER", "TEASPOON", "TABLESPOON",
    "CUP", "PIECE", "CLOVE", "BUNCH", "SLICE", "PINCH", "CAN",
}


def fail(message: str) -> None:
    raise SystemExit("FAIL: " + message)


def load_json(path: Path):
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def gzip_lines(path: Path):
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                yield json.loads(line)


def main() -> None:
    editorial_recipes = load_json(ASSETS / "recipes.json")["recipes"]
    editorial_ingredients = load_json(ASSETS / "ingredients.json")
    categories = {item["id"] for item in load_json(ASSETS / "categories.json")}
    manifest = load_json(ASSETS / "catalog_manifest.json")

    with gzip.open(ASSETS / "catalog_ingredients.json.gz", "rt", encoding="utf-8") as handle:
        catalog_ingredients = json.load(handle)

    if len(editorial_recipes) != 2000:
        fail(f"editorial recipe count is {len(editorial_recipes)}, expected 2000")
    if len(editorial_ingredients) != 224:
        fail(f"editorial ingredient count is {len(editorial_ingredients)}, expected 224")

    editorial_recipe_ids = {item["id"] for item in editorial_recipes}
    editorial_ingredient_ids = {item["id"] for item in editorial_ingredients}
    catalog_ingredient_ids = {item["id"] for item in catalog_ingredients}
    if len(catalog_ingredient_ids) != len(catalog_ingredients):
        fail("duplicate catalog ingredient id")
    if catalog_ingredient_ids & editorial_ingredient_ids:
        fail("catalog ingredient id collides with editorial seed")

    recipes = list(gzip_lines(ASSETS / "catalog_recipes.jsonl.gz"))
    if len(recipes) != 5790:
        fail(f"catalog recipe count is {len(recipes)}, expected 5790")
    if len({item["id"] for item in recipes}) != len(recipes):
        fail("duplicate catalog recipe id")
    if {item["id"] for item in recipes} & editorial_recipe_ids:
        fail("catalog recipe id collides with editorial seed")

    recipe_ingredient_refs = set()
    for recipe in recipes:
        recipe_id = recipe["id"]
        if not recipe.get("title", "").strip():
            fail(f"{recipe_id} has an empty title")
        if recipe.get("steps") != []:
            fail(f"{recipe_id} unexpectedly has instructions")
        if recipe.get("validationStatus") != "REVIEW_REQUIRED":
            fail(f"{recipe_id} is not REVIEW_REQUIRED")
        if recipe.get("sourceName") != "Persian_Cooking (train.csv)":
            fail(f"{recipe_id} has an unexpected source")
        if not recipe.get("ingredients"):
            fail(f"{recipe_id} has no ingredients")
        for category_id in recipe.get("categoryIds", []):
            if category_id not in categories:
                fail(f"{recipe_id} references unknown category {category_id}")
        for ingredient in recipe["ingredients"]:
            ingredient_id = ingredient["ingredientId"]
            if ingredient_id not in catalog_ingredient_ids:
                fail(f"{recipe_id} references unknown ingredient {ingredient_id}")
            if ingredient["unit"] not in ALLOWED_UNITS:
                fail(f"{recipe_id} uses invalid unit {ingredient['unit']}")
            if float(ingredient["amount"]) < 0:
                fail(f"{recipe_id} has a negative amount")
            if float(ingredient["amount"]) == 0 and not ingredient.get("note", "").strip():
                fail(f"{recipe_id} has a qualitative ingredient without a note")
            recipe_ingredient_refs.add(ingredient_id)

    unused_ingredients = catalog_ingredient_ids - recipe_ingredient_refs
    if unused_ingredients:
        fail(f"unused catalog ingredients: {sorted(unused_ingredients)[:5]}")

    if manifest.get("catalog_recipes") != len(recipes):
        fail("manifest recipe count does not match")
    if manifest.get("catalog_ingredients") != len(catalog_ingredients):
        fail("manifest ingredient count does not match")
    if manifest.get("total_recipes_after_seed") != 7790:
        fail("manifest total is not 7790")

    recipes_raw = (ASSETS / "catalog_recipes.jsonl.gz").read_bytes()
    ingredients_raw = (ASSETS / "catalog_ingredients.json.gz").read_bytes()
    if hashlib.sha256(recipes_raw).hexdigest() != manifest.get("catalog_recipes_sha256"):
        fail("catalog recipes gzip hash mismatch")
    if hashlib.sha256(ingredients_raw).hexdigest() != manifest.get("catalog_ingredients_sha256"):
        fail("catalog ingredients gzip hash mismatch")

    print(json.dumps({
        "status": "PASS",
        "editorial_recipes": len(editorial_recipes),
        "catalog_recipes": len(recipes),
        "total_recipes": len(editorial_recipes) + len(recipes),
        "catalog_ingredients": len(catalog_ingredients),
        "catalog_with_instructions": sum(1 for item in recipes if item.get("steps")),
        "catalog_validation_status": "REVIEW_REQUIRED",
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
