#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SEED = path.join(ROOT, 'app/src/main/assets/seed');
const MIN_RECIPES = 2000;
const UNITS = new Set(['GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'TEASPOON', 'TABLESPOON', 'PINCH', 'CUP', 'PIECE', 'CLOVE', 'BUNCH', 'SLICE', 'CAN']);
const ALLERGENS = new Set(['gluten', 'dairy', 'egg', 'nuts', 'sesame', 'fish', 'shellfish', 'soy', 'celery']);
const ANIMAL = new Set(['lamb', 'lamb-shank', 'beef', 'veal', 'ground-beef', 'ground-lamb', 'chicken', 'chicken-breast', 'chicken-thigh', 'turkey', 'liver', 'sausage', 'fish', 'fish-fillet', 'salmon', 'trout', 'shrimp', 'canned-tuna', 'gelatin', 'fish-sauce']);
const DAIRY = new Set(['milk', 'yogurt', 'greek-yogurt', 'doogh', 'butter', 'cream', 'sour-cream', 'cheese', 'feta', 'mozzarella', 'parmesan', 'kashk', 'condensed-milk', 'ice-cream', 'cream-cheese']);
const EGGS = new Set(['eggs', 'egg-yolk', 'egg-white']);
const GLUTEN = new Set(['wheat-flour', 'bread', 'lavash', 'pita', 'pasta', 'spaghetti', 'noodles', 'vermicelli', 'bulgur', 'semolina', 'couscous', 'lasagna-sheets', 'pizza-dough', 'burger-bun', 'tortilla', 'breadcrumbs', 'phyllo', 'puff-pastry', 'barley']);
const MEAT_WORDS = ['گوشت', 'مرغ', 'ماهی', 'میگو', 'بوقلمون', 'سوسیس', 'جگر'];

const recipes = JSON.parse(fs.readFileSync(path.join(SEED, 'recipes.json'), 'utf8')).recipes;
const ingredients = JSON.parse(fs.readFileSync(path.join(SEED, 'ingredients.json'), 'utf8'));
const categories = JSON.parse(fs.readFileSync(path.join(SEED, 'categories.json'), 'utf8'));
const tips = JSON.parse(fs.readFileSync(path.join(SEED, 'tips.json'), 'utf8'));
const substitutes = JSON.parse(fs.readFileSync(path.join(SEED, 'substitutes.json'), 'utf8'));

const issues = [];
const ingredientIds = new Set(ingredients.map((item) => item.id));
const categoryIds = new Set(categories.map((item) => item.id));
const recipeIds = new Set();
const titles = new Set();
const stats = {vegetarian: 0, vegan: 0, glutenFree: 0, dairyFree: 0, quick: 0, categories: {}};

function fail(message) { issues.push(message); }

if (recipes.length < MIN_RECIPES) fail('recipe count is ' + recipes.length + ', expected at least ' + MIN_RECIPES);
if (ingredients.length < 200) fail('ingredient count is ' + ingredients.length);
if (categories.length < 15) fail('category count is ' + categories.length);
if (tips.length < 20) fail('tip count is ' + tips.length);

for (const ingredient of ingredients) {
  if (!ingredient.id || !ingredient.name || !ingredient.defaultUnit) fail('invalid ingredient: ' + JSON.stringify(ingredient));
  if (!UNITS.has(ingredient.defaultUnit)) fail('invalid ingredient unit: ' + ingredient.id + ' -> ' + ingredient.defaultUnit);
  if (!(ingredient.referencePricePerKg >= 0)) fail('invalid price: ' + ingredient.id);
  if (!(ingredient.cupWeightGrams >= 0) || !(ingredient.pieceWeightGrams >= 0)) fail('invalid weight: ' + ingredient.id);
}

for (const substitute of substitutes.substitutes || substitutes) {
  if (!ingredientIds.has(substitute.ingredientId)) fail('substitute source missing: ' + substitute.ingredientId);
  if (!ingredientIds.has(substitute.substituteIngredientId)) fail('substitute target missing: ' + substitute.substituteIngredientId);
}

for (const recipe of recipes) {
  if (!recipe.id || !/^[a-z0-9-]+$/.test(recipe.id)) fail('invalid recipe id: ' + recipe.id);
  if (recipeIds.has(recipe.id)) fail('duplicate recipe id: ' + recipe.id);
  recipeIds.add(recipe.id);
  if (!recipe.title || recipe.title.length < 3 || recipe.title.length > 80) fail('invalid title: ' + recipe.id);
  if (titles.has(recipe.title)) fail('duplicate title: ' + recipe.title);
  titles.add(recipe.title);
  if (!recipe.description || recipe.description.length < 20) fail('short description: ' + recipe.id);
  if (!recipe.titleEn || /[\u0600-\u06FF]/.test(recipe.titleEn)) fail('invalid titleEn: ' + recipe.id);
  if (!Array.isArray(recipe.tags) || recipe.tags.length === 0) fail('missing tags: ' + recipe.id);
  if (!Array.isArray(recipe.categoryIds) || recipe.categoryIds.length === 0) fail('missing categories: ' + recipe.id);
  if (!(recipe.servings >= 1 && recipe.servings <= 12)) fail('servings out of range: ' + recipe.id);
  if (!(recipe.prepMinutes >= 0 && recipe.prepMinutes <= 1440)) fail('prep minutes out of range: ' + recipe.id);
  if (!(recipe.cookMinutes >= 0 && recipe.cookMinutes <= 480)) fail('cook minutes out of range: ' + recipe.id);
  if (!(recipe.calories >= 20 && recipe.calories <= 3000)) fail('calories out of range: ' + recipe.id + ' -> ' + recipe.calories);
  if (!['EASY', 'MEDIUM', 'HARD'].includes(recipe.difficulty)) fail('invalid difficulty: ' + recipe.id);
  if (!['BUDGET', 'MEDIUM', 'PREMIUM'].includes(recipe.budgetLevel)) fail('invalid budget: ' + recipe.id);
  if (!['ALL', 'SPRING', 'SUMMER', 'AUTUMN', 'WINTER'].includes(recipe.season)) fail('invalid season: ' + recipe.id);
  if (!['PEACH', 'TERRACOTTA', 'SAGE', 'CREAM', 'DARK'].includes(recipe.heroStyle)) fail('invalid hero style: ' + recipe.id);
  if (!Array.isArray(recipe.ingredients) || recipe.ingredients.length < 4 || recipe.ingredients.length > 12) fail('ingredient count out of range: ' + recipe.id + ' -> ' + recipe.ingredients.length);
  if (!Array.isArray(recipe.steps) || recipe.steps.length < 4 || recipe.steps.length > 9) fail('step count out of range: ' + recipe.id + ' -> ' + recipe.steps.length);
  if (!Array.isArray(recipe.tips) || recipe.tips.length < 2) fail('not enough tips: ' + recipe.id);
  if (!Array.isArray(recipe.allergens)) fail('missing allergens: ' + recipe.id);
  for (const allergen of recipe.allergens || []) if (!ALLERGENS.has(allergen)) fail('unknown allergen: ' + recipe.id + ' -> ' + allergen);
  for (const category of recipe.categoryIds) if (!categoryIds.has(category)) fail('unknown category: ' + recipe.id + ' -> ' + category);
  const local = new Set();
  for (const line of recipe.ingredients) {
    if (!ingredientIds.has(line.ingredientId)) fail('unknown ingredient: ' + recipe.id + ' -> ' + line.ingredientId);
    if (!UNITS.has(line.unit)) fail('unknown unit: ' + recipe.id + ' -> ' + line.unit);
    if (!(line.amount > 0)) fail('invalid amount: ' + recipe.id + ' -> ' + line.ingredientId);
    if (local.has(line.ingredientId)) fail('duplicate ingredient line: ' + recipe.id + ' -> ' + line.ingredientId);
    local.add(line.ingredientId);
  }
  for (const step of recipe.steps) {
    if (!step.title || !step.instruction) fail('empty step: ' + recipe.id);
    if (step.timerSeconds !== null && !(step.timerSeconds > 0)) fail('invalid timer: ' + recipe.id);
  }
  if (JSON.stringify(recipe).includes('undefined') || JSON.stringify(recipe).includes('null,null')) fail('placeholder value: ' + recipe.id);
  const hasAnimal = [...local].some((id) => ANIMAL.has(id));
  if (recipe.isVegetarian === hasAnimal) fail('vegetarian flag mismatch: ' + recipe.id);
  const hasDairy = [...local].some((id) => DAIRY.has(id));
  const hasEgg = [...local].some((id) => EGGS.has(id));
  if (recipe.isVegan && (hasAnimal || hasDairy || hasEgg || local.has('honey'))) fail('vegan flag mismatch: ' + recipe.id);
  if (recipe.isDairyFree && hasDairy) fail('dairy-free flag mismatch: ' + recipe.id);
  if (recipe.isGlutenFree && [...local].some((id) => GLUTEN.has(id))) fail('gluten-free flag mismatch: ' + recipe.id);
  if (recipe.isVegetarian) stats.vegetarian += 1;
  if (recipe.isVegan) stats.vegan += 1;
  if (recipe.isGlutenFree) stats.glutenFree += 1;
  if (recipe.isDairyFree) stats.dairyFree += 1;
  if (recipe.categoryIds.includes('quick')) stats.quick += 1;
  for (const category of recipe.categoryIds) stats.categories[category] = (stats.categories[category] || 0) + 1;
  const stepText = recipe.steps.map((step) => step.title + ' ' + step.instruction).join(' ');
  if (!hasAnimal && MEAT_WORDS.some((word) => stepText.includes(word)) && !stepText.includes('تخم‌مرغ') && !stepText.includes('ماهیچه')) fail('meat word in vegetarian steps: ' + recipe.id);
  if (recipe.categoryIds.includes('vegetarian') !== recipe.isVegetarian) fail('vegetarian category mismatch: ' + recipe.id);
  if (recipe.title.endsWith(' گیاهی') && !recipe.isVegetarian) fail('vegetarian title mismatch: ' + recipe.id);
  if (!hasAnimal && recipe.title.includes('با مرغ')) fail('chicken title without chicken: ' + recipe.id);
}

console.log(JSON.stringify({recipes: recipes.length, ingredients: ingredients.length, categories: categories.length, tips: tips.length, substitutes: (substitutes.substitutes || substitutes).length, ...stats}, null, 2));
if (issues.length > 0) {
  console.error('Validation failed with ' + issues.length + ' issue(s):');
  for (const issue of issues.slice(0, 100)) console.error('- ' + issue);
  process.exitCode = 1;
} else {
  console.log('All recipe seed validations passed.');
}
