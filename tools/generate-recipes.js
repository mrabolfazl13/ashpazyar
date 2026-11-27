#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SEED = path.join(ROOT, 'app/src/main/assets/seed');
const ingredients = JSON.parse(fs.readFileSync(path.join(SEED, 'ingredients.json'), 'utf8'));
const categories = JSON.parse(fs.readFileSync(path.join(SEED, 'categories.json'), 'utf8'));
const basePath = path.join(ROOT, 'tools/base-recipes.json');
const baseFile = JSON.parse(fs.readFileSync(fs.existsSync(basePath) ? basePath : path.join(SEED, 'recipes.json'), 'utf8'));
const byId = new Map(ingredients.map((item) => [item.id, item]));
const categoryIds = new Set(categories.map((item) => item.id));
const units = new Set(['GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'TEASPOON', 'TABLESPOON', 'PINCH', 'CUP', 'PIECE', 'CLOVE', 'BUNCH', 'SLICE', 'CAN']);
const TARGET = 2000;

const ANIMAL = new Set(['lamb', 'lamb-shank', 'beef', 'veal', 'ground-beef', 'ground-lamb', 'chicken', 'chicken-breast', 'chicken-thigh', 'turkey', 'liver', 'sausage', 'fish', 'trout', 'salmon', 'shrimp', 'canned-tuna', 'fish-fillet', 'gelatin', 'fish-sauce']);
const DAIRY = new Set(['milk', 'yogurt', 'greek-yogurt', 'doogh', 'butter', 'cream', 'sour-cream', 'cheese', 'feta', 'mozzarella', 'parmesan', 'kashk', 'condensed-milk', 'ice-cream', 'cream-cheese']);
const GLUTEN = new Set(['wheat-flour', 'bread', 'lavash', 'pita', 'pasta', 'spaghetti', 'noodles', 'vermicelli', 'bulgur', 'semolina', 'couscous', 'lasagna-sheets', 'pizza-dough', 'burger-bun', 'tortilla', 'breadcrumbs', 'phyllo', 'puff-pastry', 'barley']);
const NUTS = new Set(['walnut', 'almond', 'pistachio', 'hazelnut', 'peanut', 'pine-nut', 'tahini', 'peanut-butter']);
const FISH = new Set(['fish', 'trout', 'salmon', 'canned-tuna', 'fish-fillet', 'fish-sauce']);
const SHELLFISH = new Set(['shrimp']);
const SOY = new Set(['tofu', 'soy-granules', 'soy-sauce', 'miso']);
const SESAME = new Set(['sesame', 'tahini']);
const EGGS = new Set(['eggs', 'mayonnaise']);

const PROTEIN = {
  chicken: ['chicken-thigh', 'مرغ', 150],
  beef: ['beef', 'گوشت گوساله', 150],
  lamb: ['lamb', 'گوشت گوسفندی', 150],
  fish: ['fish-fillet', 'فیله ماهی', 170],
  shrimp: ['shrimp', 'میگو', 120],
  mushroom: ['mushroom', 'قارچ', 120],
  chickpeas: ['chickpeas', 'نخود', 80],
  lentils: ['lentils', 'عدس', 80],
  whiteBeans: ['white-beans', 'لوبیا سفید', 80],
  spinach: ['spinach', 'اسفناج', 80],
  tofu: ['tofu', 'توفو', 130],
  soy: ['soy-granules', 'سویا', 60],
  eggplant: ['eggplant', 'بادمجان', 100],
  potato: ['potato', 'سیب‌زمینی', 150]
};

const ADD = {
  saffron: ['saffron', 1, 'PINCH'], roseWater: ['rose-water', 1, 'TABLESPOON'],
  lime: ['lime', 1, 'PIECE'], lemon: ['lemon', 1, 'PIECE'],
  pomegranate: ['pomegranate-paste', 2, 'TABLESPOON'], walnut: ['walnut', 50, 'GRAM'],
  chili: ['chili-pepper', 1, 'PIECE'], pepperFlakes: ['red-pepper-flakes', 1, 'TEASPOON'],
  kashk: ['kashk', 100, 'GRAM'], driedMint: ['dried-mint', 1, 'TEASPOON'],
  prune: ['prune', 100, 'GRAM'], mushroom: ['mushroom', 250, 'GRAM'],
  spinach: ['spinach', 200, 'GRAM'], eggplant: ['eggplant', 1, 'PIECE'],
  zucchini: ['zucchini', 1, 'PIECE'], potato: ['potato', 2, 'PIECE'],
  carrot: ['carrot', 2, 'PIECE'], chickpeas: ['chickpeas', 150, 'GRAM'],
  lentils: ['lentils', 150, 'GRAM'], whiteBeans: ['white-beans', 150, 'GRAM'],
  raisin: ['raisin', 40, 'GRAM'], date: ['date', 6, 'PIECE'],
  almond: ['almond', 50, 'GRAM'], pistachio: ['pistachio', 40, 'GRAM'],
  honey: ['honey', 2, 'TABLESPOON'], yogurt: ['yogurt', 150, 'GRAM'],
  feta: ['feta', 100, 'GRAM'], mozzarella: ['mozzarella', 100, 'GRAM'],
  parmesan: ['parmesan', 50, 'GRAM'], cream: ['cream', 100, 'MILLILITER'],
  milk: ['milk', 200, 'MILLILITER'], coconutMilk: ['coconut-milk', 200, 'MILLILITER'],
  eggs: ['eggs', 2, 'PIECE'], bread: ['bread', 2, 'PIECE'],
  rice: ['rice', 200, 'GRAM'], pasta: ['pasta', 150, 'GRAM'],
  garlic: ['garlic', 3, 'PIECE'], onion: ['onion', 1, 'PIECE'],
  tomato: ['tomato', 2, 'PIECE'], tomatoPaste: ['tomato-paste', 1, 'TABLESPOON'],
  bellPepper: ['bell-pepper', 1, 'PIECE'], corn: ['corn', 100, 'GRAM'],
  olive: ['olive', 50, 'GRAM'], oliveOil: ['olive-oil', 2, 'TABLESPOON'],
  butter: ['butter', 30, 'GRAM'], barberry: ['barberry', 30, 'GRAM'],
  cinnamon: ['cinnamon', 0.5, 'TEASPOON'], cardamom: ['cardamom', 0.5, 'TEASPOON'],
  curry: ['curry-powder', 1, 'TEASPOON'], cumin: ['cumin', 0.5, 'TEASPOON'],
  paprika: ['paprika', 1, 'TEASPOON'], ginger: ['ginger', 10, 'GRAM'],
  basil: ['basil', 20, 'GRAM'], parsley: ['parsley', 30, 'GRAM'],
  coriander: ['coriander', 30, 'GRAM'], dill: ['dill', 30, 'GRAM'],
  mint: ['mint', 20, 'GRAM'], strawberry: ['strawberry', 150, 'GRAM'],
  banana: ['banana', 1, 'PIECE'], apple: ['apple', 1, 'PIECE'],
  orange: ['orange', 1, 'PIECE'], sourCherry: ['sour-cherry', 150, 'GRAM'],
  pomegranateSeeds: ['pomegranate', 1, 'PIECE'], chocolate: ['chocolate', 50, 'GRAM'],
  cocoa: ['cocoa', 1, 'TABLESPOON'], iceCream: ['ice-cream', 200, 'GRAM'],
  mustard: ['mustard', 1, 'TEASPOON'], vinegar: ['vinegar', 1, 'TABLESPOON'],
  cheese: ['cheese', 100, 'GRAM'], tuna: ['canned-tuna', 1, 'CAN'],
  tofu: ['tofu', 200, 'GRAM'], soy: ['soy-granules', 80, 'GRAM'],
  lettuce: ['lettuce', 200, 'GRAM'], cucumber: ['cucumber', 2, 'PIECE'],
  orangeBlossom: ['orange-blossom-water', 1, 'TABLESPOON'],
  redBeans: ['red-beans', 150, 'GRAM'], ice: ['ice', 100, 'GRAM'],
  salt: ['salt', 1, 'TEASPOON'], water: ['water', 500, 'MILLILITER'],
  breadcrumbs: ['breadcrumbs', 50, 'GRAM']
};

function P(code, suffix, protein) { return [code, suffix, protein, null, {}]; }
function V(code, suffix, plant) { return [code, suffix, 'V:' + plant, null, {}]; }
function A(code, suffix, adds, opts) { return [code, suffix, null, adds, opts || {}]; }

const VARIANT_MAP = {
  stew: [
    P('morgh', 'با مرغ', 'chicken'), P('gosht', 'با گوشت گوساله', 'beef'), P('gousfand', 'با گوشت گوسفندی', 'lamb'),
    P('gharch', 'با قارچ', 'mushroom'), V('giyahi', 'گیاهی', 'mushroom'), V('nokhod', 'با نخود', 'chickpeas'),
    V('adas', 'با عدس', 'lentils'), V('lubia', 'با لوبیا سفید', 'whiteBeans'), V('bademjan', 'با بادمجان', 'eggplant'),
    V('sibzamini', 'با سیب‌زمینی', 'potato'), V('esfenaj', 'با اسفناج', 'spinach'),
    A('zaferani', 'زعفرانی', ['saffron', 'roseWater'], {}), A('limui', 'لیمویی', ['lime', 'lemon'], {}),
    A('anari', 'اناری', ['pomegranate', 'walnut'], {}), A('tond', 'تند', ['chili', 'pepperFlakes'], {}),
    A('kashki', 'کشکدار', ['kashk', 'driedMint'], {}), A('alu', 'با آلو', ['prune'], {}),
    A('nan', 'با نان', ['bread'], {}), A('berenj', 'با برنج', ['rice', 'butter'], {}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  rice: [
    P('morgh', 'با مرغ', 'chicken'), P('mahiche', 'با ماهیچه', 'lamb'), P('gosht', 'با گوشت گوساله', 'beef'),
    P('meygoo', 'با میگو', 'shrimp'), P('mahi', 'با ماهی', 'fish'), P('gharch', 'با قارچ', 'mushroom'),
    V('giyahi', 'گیاهی', 'chickpeas'), V('adas', 'با عدس', 'lentils'), V('lubia', 'با لوبیا سبز', 'whiteBeans'), V('nokhod', 'با نخود', 'chickpeas'),
    A('tokhm', 'با تخم‌مرغ', ['eggs'], {}), A('keshmish', 'با کشمش', ['raisin'], {}), A('khorma', 'با خرما', ['date'], {}),
    A('zaferani', 'زعفرانی', ['saffron', 'roseWater'], {}), A('zereshk', 'با زرشک', ['barberry', 'butter'], {}),
    A('badam', 'با بادام', ['almond'], {}), A('peste', 'با پسته', ['pistachio'], {}),
    A('mast', 'با ماست', ['yogurt', 'driedMint'], {}), A('salad', 'با سالاد', ['lettuce', 'cucumber', 'tomato'], {}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  kabab: [
    P('morgh', 'با مرغ', 'chicken'), P('gousfand', 'با گوشت گوسفندی', 'lamb'), P('gosht', 'با گوشت گوساله', 'beef'),
    P('mahi', 'با ماهی', 'fish'), P('meygoo', 'با میگو', 'shrimp'), P('gharch', 'با قارچ', 'mushroom'),
    V('giyahi', 'گیاهی', 'soy'),
    A('zaferani', 'زعفرانی', ['saffron', 'yogurt'], {}), A('limui', 'لیمویی', ['lime', 'oliveOil'], {}),
    A('anari', 'اناری', ['pomegranate', 'walnut'], {}), A('tond', 'تند', ['chili', 'pepperFlakes'], {}),
    A('mast', 'با سس ماست', ['yogurt', 'mint', 'garlic'], {}), A('nan', 'با نان', ['bread'], {}),
    A('berenj', 'با برنج', ['rice', 'butter'], {}), A('sibzamini', 'با سیب‌زمینی', ['potato', 'oliveOil'], {}),
    A('felfel', 'با فلفل دلمه‌ای', ['bellPepper', 'onion'], {}), A('goje', 'با گوجه', ['tomato'], {}),
    A('sir', 'با سس سیر', ['yogurt', 'garlic', 'lemon'], {}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  soup: [
    P('morgh', 'با مرغ', 'chicken'), P('gosht', 'با گوشت', 'beef'), P('gharch', 'با قارچ', 'mushroom'),
    V('giyahi', 'گیاهی', 'chickpeas'), V('adas', 'با عدس', 'lentils'), V('nokhod', 'با نخود', 'chickpeas'), V('lubia', 'با لوبیا', 'whiteBeans'),
    A('sabzijat', 'با سبزیجات', ['carrot', 'zucchini', 'spinach'], {}), A('shir', 'با شیر', ['milk'], {}),
    A('khame', 'با خامه', ['cream'], {}), A('tond', 'تند', ['chili', 'pepperFlakes'], {}),
    A('limui', 'لیمویی', ['lime', 'lemon'], {}), A('zaferani', 'زعفرانی', ['saffron'], {}),
    A('sibzamini', 'با سیب‌زمینی', ['potato'], {}), A('havij', 'با هویج', ['carrot'], {}),
    A('kadoo', 'با کدو', ['zucchini'], {}), A('nan', 'با نان', ['bread'], {}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  salad: [
    P('morgh', 'با مرغ', 'chicken'), A('tuna', 'با تن ماهی', ['tuna'], {}), P('meygoo', 'با میگو', 'shrimp'),
    V('nokhod', 'با نخود', 'chickpeas'), V('lubia', 'با لوبیا', 'whiteBeans'), V('giyahi', 'گیاهی', 'tofu'),
    A('feta', 'با پنیر فتا', ['feta', 'olive'], {}), A('mast', 'با ماست', ['yogurt', 'mint'], {}),
    A('tokhm', 'با تخم‌مرغ', ['eggs'], {}), A('gerdoo', 'با گردو', ['walnut'], {}),
    A('keshmish', 'با کشمش', ['raisin'], {}), A('sib', 'با سیب', ['apple', 'walnut'], {}),
    A('anar', 'با انار', ['pomegranateSeeds'], {}), A('limu', 'با لیمو', ['lemon', 'oliveOil'], {}),
    A('tond', 'تند', ['chili', 'pepperFlakes'], {}), A('mediterranean', 'مدیترانه‌ای', ['feta', 'olive', 'oliveOil'], {}),
    A('diet', 'رژیمی', ['lemon'], {diet: true}), A('berenj', 'با برنج', ['rice'], {}),
    A('pasta', 'با پاستا', ['pasta'], {}), A('mustard', 'با سس خردل', ['mustard', 'honey', 'vinegar'], {}),
    A('majlesi', 'مجلسی', ['feta', 'walnut', 'pistachio'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'})
  ],
  breakfast: [
    P('morgh', 'با مرغ', 'chicken'), A('panir', 'با پنیر', ['cheese', 'tomato'], {}),
    A('tokhm', 'با تخم‌مرغ', ['eggs'], {}), A('gharch', 'با قارچ', ['mushroom'], {}),
    A('goje', 'با گوجه', ['tomato'], {}), A('esfenaj', 'با اسفناج', ['spinach'], {}),
    A('sibzamini', 'با سیب‌زمینی', ['potato'], {}), V('adas', 'با عدس', 'lentils'), V('nokhod', 'با نخود', 'chickpeas'),
    A('khorma', 'با خرما', ['date'], {}), A('moz', 'با موز', ['banana'], {}),
    A('asal', 'با عسل', ['honey', 'walnut'], {}), A('gerdoo', 'با گردو', ['walnut'], {}),
    A('shir', 'با شیر', ['milk'], {}), A('nan', 'با نان', ['bread'], {}),
    A('diet', 'رژیمی', ['lemon'], {diet: true}),
    A('majlesi', 'مجلسی', ['date', 'pistachio', 'honey'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  dessert: [
    A('zaferani', 'زعفرانی', ['saffron', 'roseWater'], {}), A('shokolati', 'شکلاتی', ['chocolate', 'cocoa'], {}),
    A('khorma', 'با خرما', ['date'], {}), A('gerdoo', 'با گردو', ['walnut'], {}),
    A('peste', 'با پسته', ['pistachio'], {}), A('badam', 'با بادام', ['almond'], {}),
    A('nargil', 'با نارگیل', ['coconutMilk'], {}), A('toot', 'با توت‌فرنگی', ['strawberry'], {}),
    A('moz', 'با موز', ['banana'], {}), A('sib', 'با سیب', ['apple', 'cinnamon'], {}),
    A('portegal', 'با پرتقال', ['orange', 'orangeBlossom'], {}), A('asal', 'با عسل', ['honey'], {}),
    A('darchin', 'با دارچین', ['cinnamon'], {}), A('hel', 'با هل', ['cardamom', 'roseWater'], {}),
    A('golab', 'با گلاب', ['roseWater', 'pistachio'], {}), A('khame', 'با خامه', ['cream'], {}),
    A('bastani', 'با بستنی', ['iceCream'], {}), A('kam-shekar', 'کم‌شکر', [], {lowSugar: true}),
    A('majlesi', 'مجلسی', ['pistachio', 'almond', 'roseWater'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  drink: [
    A('limu', 'با لیمو', ['lemon'], {}), A('nana', 'با نعنا', ['mint'], {}),
    A('zanjabil', 'با زنجبیل', ['ginger'], {}), A('toot', 'با توت‌فرنگی', ['strawberry'], {}),
    A('moz', 'با موز', ['banana'], {}), A('khorma', 'با خرما', ['date'], {}),
    A('hel', 'با هل', ['cardamom'], {}), A('zaferan', 'با زعفران', ['saffron'], {}),
    A('asal', 'با عسل', ['honey'], {}), A('sib', 'با سیب', ['apple', 'cinnamon'], {}),
    A('portegal', 'با پرتقال', ['orange'], {}), A('anar', 'با انار', ['pomegranateSeeds'], {}),
    A('albaloo', 'با آلبالو', ['sourCherry'], {}), A('mast', 'با ماست', ['yogurt', 'mint'], {}),
    A('shir', 'با شیر', ['milk'], {}), A('shokolat', 'با شکلات', ['chocolate', 'cocoa', 'milk'], {}),
    A('nargil', 'با نارگیل', ['coconutMilk'], {}), A('khonak', 'خنک', ['ice'], {iced: true}),
    A('garm', 'گرم', ['honey'], {warm: true}),
    A('majlesi', 'مجلسی', ['saffron', 'pistachio', 'roseWater'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'})
  ],
  snack: [
    P('morgh', 'با مرغ', 'chicken'), P('gosht', 'با گوشت', 'beef'), P('mahi', 'با ماهی', 'fish'),
    P('gharch', 'با قارچ', 'mushroom'), V('giyahi', 'گیاهی', 'chickpeas'), V('nokhod', 'با نخود', 'chickpeas'),
    V('adas', 'با عدس', 'lentils'), A('sibzamini', 'با سیب‌زمینی', ['potato'], {}),
    A('bademjan', 'با بادمجان', ['eggplant'], {}), A('kadoo', 'با کدو', ['zucchini'], {}),
    A('tokhm', 'با تخم‌مرغ', ['eggs'], {}), A('panir', 'با پنیر', ['cheese', 'mozzarella'], {}),
    A('sabzijat', 'با سبزیجات', ['carrot', 'zucchini', 'spinach'], {}), A('tond', 'تند', ['chili', 'pepperFlakes'], {}),
    A('kashk', 'با کشک', ['kashk', 'driedMint'], {}), A('gerdoo', 'با گردو', ['walnut'], {}),
    A('mast', 'با ماست', ['yogurt', 'mint'], {}), A('sir', 'با سس سیر', ['yogurt', 'garlic', 'lemon'], {}),
    A('sorkh', 'سرخ‌شده', ['breadcrumbs'], {fried: true}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ],
  main: [
    P('morgh', 'با مرغ', 'chicken'), P('gosht', 'با گوشت', 'beef'), P('meygoo', 'با میگو', 'shrimp'),
    P('mahi', 'با ماهی', 'fish'), P('gharch', 'با قارچ', 'mushroom'), V('giyahi', 'گیاهی', 'tofu'),
    A('panir', 'با پنیر', ['mozzarella', 'parmesan'], {}), A('khame', 'با خامه', ['cream'], {}),
    A('goje', 'با گوجه', ['tomato', 'tomatoPaste'], {}), A('felfel', 'با فلفل دلمه‌ای', ['bellPepper', 'onion'], {}),
    A('sibzamini', 'با سیب‌زمینی', ['potato'], {}), A('tond', 'تند', ['chili', 'pepperFlakes'], {}),
    A('mediterranean', 'مدیترانه‌ای', ['feta', 'olive', 'lemon'], {cuisine: 'international'}),
    A('hendi', 'هندی', ['curry', 'coconutMilk', 'ginger'], {cuisine: 'international'}),
    A('italiani', 'ایتالیایی', ['basil', 'parmesan', 'tomato'], {cuisine: 'international'}),
    A('mexican', 'مکزیکی', ['cumin', 'paprika', 'corn', 'redBeans'], {cuisine: 'international'}),
    A('nan', 'با نان', ['bread'], {}), A('salad', 'با سالاد', ['lettuce', 'cucumber', 'tomato'], {}),
    A('majlesi', 'مجلسی', ['barberry', 'pistachio', 'butter'], {premium: true}),
    A('eqtesadi', 'اقتصادی', [], {budget: 'BUDGET'}), A('sari', 'سریع', [], {quick: true})
  ]
};

const FALLBACKS = {
  stew: ['majlesi', 'eqtesadi', 'sari', 'nan', 'berenj'],
  rice: ['majlesi', 'eqtesadi', 'sari', 'mast', 'salad'],
  kabab: ['majlesi', 'eqtesadi', 'sari', 'nan', 'berenj'],
  soup: ['majlesi', 'eqtesadi', 'sari', 'nan', 'sabzijat'],
  salad: ['majlesi', 'eqtesadi', 'diet', 'limu', 'mustard'],
  breakfast: ['majlesi', 'eqtesadi', 'sari', 'nan', 'asal'],
  dessert: ['majlesi', 'eqtesadi', 'kam-shekar', 'khame', 'darchin'],
  drink: ['majlesi', 'eqtesadi', 'khonak', 'garm', 'asal'],
  snack: ['majlesi', 'eqtesadi', 'sari', 'mast', 'sir'],
  main: ['majlesi', 'eqtesadi', 'sari', 'nan', 'salad']
};

function kindOf(recipe) {
  const cats = recipe.categoryIds || [];
  if (cats.includes('drink') && !cats.includes('dessert')) return 'drink';
  if (cats.includes('dessert')) return 'dessert';
  if (cats.includes('salad')) return 'salad';
  if (cats.includes('soup')) return 'soup';
  if (cats.includes('breakfast')) return 'breakfast';
  if (cats.includes('kabab')) return 'kabab';
  if (cats.includes('rice')) return 'rice';
  if (cats.includes('stew')) return 'stew';
  if (cats.includes('appetizer')) return 'snack';
  return 'main';
}

function grams(line) {
  const ing = byId.get(line.ingredientId);
  const n = Number(line.amount) || 0;
  const u = line.unit || 'GRAM';
  if (u === 'GRAM') return n;
  if (u === 'KILOGRAM') return n * 1000;
  if (u === 'MILLILITER') return n;
  if (u === 'LITER') return n * 1000;
  if (u === 'TEASPOON') return n * 5;
  if (u === 'TABLESPOON') return n * 15;
  if (u === 'PINCH') return n * 0.4;
  if (u === 'CUP') return n * (ing && ing.cupWeightGrams ? ing.cupWeightGrams : 200);
  if (u === 'PIECE') return n * (ing && ing.pieceWeightGrams ? ing.pieceWeightGrams : 100);
  if (u === 'CLOVE') return n * 5;
  if (u === 'BUNCH') return n * 50;
  if (u === 'SLICE') return n * 20;
  if (u === 'CAN') return n * 180;
  return n;
}

const DENSITY = {
  water: 0, ice: 0, salt: 0, tea: 1, coffee: 2, 'rose-water': 0, 'orange-blossom-water': 0,
  'olive-oil': 884, 'vegetable-oil': 884, 'sesame-oil': 884, butter: 717, ghee: 900,
  honey: 304, sugar: 387, 'date-syrup': 293, 'grape-molasses': 290, chocolate: 546,
  cocoa: 228, walnut: 654, almond: 579, pistachio: 560, hazelnut: 628, peanut: 567,
  tahini: 595, 'peanut-butter': 588, sesame: 573, coconut: 354, 'ice-cream': 207,
  milk: 61, yogurt: 61, 'greek-yogurt': 97, doogh: 35, cream: 340, 'sour-cream': 198,
  cheese: 350, feta: 264, mozzarella: 280, parmesan: 392, kashk: 150,
  rice: 365, 'basmati-rice': 365, bulgur: 342, 'wheat-flour': 364, 'rice-flour': 366,
  semolina: 360, pasta: 371, spaghetti: 371, noodles: 384, vermicelli: 384,
  bread: 265, lavash: 275, pita: 275, oats: 389, barley: 354, quinoa: 368,
  couscous: 376, cornstarch: 381, breadcrumbs: 395, 'puff-pastry': 558, phyllo: 299,
  lentils: 352, chickpeas: 364, 'white-beans': 333, 'red-beans': 333, 'pinto-beans': 347,
  'split-peas': 341, 'mung-beans': 347, 'black-eyed-peas': 336, 'fava-beans': 341,
  soy: 336, tofu: 76, 'coconut-milk': 230, 'soy-sauce': 53, mayonnaise: 680,
  mustard: 66, ketchup: 112, 'pomegranate-paste': 280, vinegar: 18,
  'balsamic-vinegar': 88, 'tomato-paste': 82, 'condensed-milk': 321,
  potato: 77, tomato: 18, onion: 40, garlic: 149, carrot: 41, cucumber: 15,
  eggplant: 25, zucchini: 17, pumpkin: 26, cabbage: 25, cauliflower: 25, broccoli: 34,
  spinach: 23, lettuce: 15, mushroom: 22, 'bell-pepper': 31, 'green-pepper': 20,
  'chili-pepper': 40, celery: 16, leek: 61, 'spring-onion': 32, parsley: 36,
  coriander: 23, dill: 43, mint: 70, basil: 23, fenugreek: 49, radish: 16,
  beetroot: 43, turnip: 28, okra: 33, 'green-beans': 31, 'green-peas': 81,
  corn: 86, artichoke: 47, asparagus: 20, ginger: 80, 'sweet-potato': 86,
  olive: 115, lemon: 29, lime: 30, orange: 47, tangerine: 53, apple: 52,
  banana: 89, grape: 69, pomegranate: 83, 'sour-cherry': 50, cherry: 63,
  peach: 39, apricot: 48, plum: 46, fig: 74, date: 282, barberry: 316,
  'dried-apricot': 241, raisin: 299, prune: 240, melon: 34, watermelon: 30,
  strawberry: 32, kiwi: 61, pear: 57, persimmon: 70, quince: 57,
  lamb: 294, 'lamb-shank': 234, beef: 250, veal: 172, 'ground-beef': 250,
  'ground-lamb': 282, chicken: 239, 'chicken-breast': 165, 'chicken-thigh': 209,
  turkey: 189, liver: 175, sausage: 301, fish: 206, trout: 148, salmon: 208,
  shrimp: 99, 'canned-tuna': 116, 'fish-fillet': 206
};

const CAT_DENSITY = {MEAT: 220, POULTRY: 190, FISH: 160, DAIRY: 120, LEGUME: 320, GRAIN: 350, VEGETABLE: 40, HERB: 40, FRUIT: 60, NUT: 600, OIL: 880, SWEETENER: 380, SPICE: 250, OTHER: 100};

function calories(recipe) {
  let total = 0;
  for (const line of recipe.ingredients) {
    const ing = byId.get(line.ingredientId);
    if (!ing) continue;
    let density = DENSITY[ing.id];
    if (density === undefined) density = CAT_DENSITY[ing.category] === undefined ? 100 : CAT_DENSITY[ing.category];
    total += grams(line) * density / 100;
  }
  return Math.max(20, Math.round((total / Math.max(1, recipe.servings)) / 10) * 10);
}

function dietFor(lines) {
  const ids = new Set(lines.map((line) => line.ingredientId));
  const has = (set) => [...ids].some((id) => set.has(id));
  const allergens = [];
  if (has(GLUTEN)) allergens.push('gluten');
  if (has(DAIRY)) allergens.push('dairy');
  if (has(EGGS)) allergens.push('egg');
  if (has(NUTS)) allergens.push('nuts');
  if (has(SESAME)) allergens.push('sesame');
  if (has(FISH)) allergens.push('fish');
  if (has(SHELLFISH)) allergens.push('shellfish');
  if (has(SOY)) allergens.push('soy');
  if (ids.has('celery')) allergens.push('celery');
  const meat = has(ANIMAL);
  return {isVegetarian: !meat, isVegan: !meat && !has(DAIRY) && !has(EGGS) && !ids.has('honey'), isGlutenFree: !has(GLUTEN), isDairyFree: !has(DAIRY), allergens};
}

function hash(value) {
  let h = 0;
  for (let i = 0; i < value.length; i += 1) h = ((h << 5) - h + value.charCodeAt(i)) | 0;
  return h;
}

function hero(recipe, kind) {
  if (kind === 'dessert') return recipe.ingredients.some((line) => ['chocolate', 'cocoa'].includes(line.ingredientId)) ? 'TERRACOTTA' : 'PEACH';
  if (kind === 'drink') return 'PEACH';
  if (kind === 'salad' || kind === 'soup') return 'SAGE';
  if (kind === 'rice') return 'CREAM';
  if (kind === 'kabab' || kind === 'stew') return 'TERRACOTTA';
  if (recipe.ingredients.some((line) => ['salmon', 'shrimp', 'fish-fillet', 'trout'].includes(line.ingredientId))) return 'SAGE';
  return ['CREAM', 'TERRACOTTA', 'SAGE', 'PEACH'][Math.abs(hash(recipe.id)) % 4];
}

function season(recipe) {
  const ids = new Set(recipe.ingredients.map((line) => line.ingredientId));
  if (ids.has('pumpkin') || ids.has('quince') || ids.has('persimmon') || ids.has('beetroot')) return 'AUTUMN';
  if (ids.has('watermelon') || ids.has('melon') || ids.has('strawberry') || ids.has('apricot')) return 'SUMMER';
  if (ids.has('asparagus') || ids.has('green-peas') || ids.has('fava-beans')) return 'SPRING';
  return recipe.season || 'ALL';
}

function line(id, amount, unit, note) {
  return {ingredientId: id, amount: Math.round(amount * 10) / 10, unit: unit || 'GRAM', note: note || '', optional: false};
}

function add(lines, spec, servings) {
  if (!spec || !byId.has(spec[0])) return false;
  if (lines.some((item) => item.ingredientId === spec[0])) return false;
  const scale = Math.max(0.5, servings / 4);
  lines.push(line(spec[0], spec[1] * scale, spec[2]));
  return true;
}

function replaceText(text, names, target) {
  let value = text;
  value = value.replace(/تخم‌مرغ/g, '\u0001');
  value = value.replace(/ماهیچه/g, '\u0002');
  for (const name of [...new Set(names)].sort((a, b) => b.length - a.length)) {
    if (name !== target) value = value.split(name).join(target);
  }
  return value.replace(/\u0001/g, 'تخم‌مرغ').replace(/\u0002/g, 'ماهیچه');
}

function pushStep(steps, title, instruction, timer) {
  if (!instruction || steps.some((step) => step.instruction === instruction)) return;
  const step = {title: title, instruction: instruction, timerSeconds: timer || null};
  if (steps.length >= 9) {
    steps[steps.length - 2].instruction = steps[steps.length - 2].instruction.replace(/\s*$/, '') + ' ' + instruction;
    return;
  }
  steps.splice(Math.max(0, steps.length - 1), 0, step);
}

function ensureSteps(recipe, kind) {
  if (!recipe.steps.length) recipe.steps.push({title: 'آماده‌سازی', instruction: 'مواد را آماده و اندازه‌گیری کنید.', timerSeconds: null});
  if (recipe.steps.length < 5) pushStep(recipe.steps, 'آماده‌سازی', 'همه مواد را آماده، شسته و اندازه‌گیری کنید تا پخت یکنواخت پیش برود.', null);
  if (recipe.steps.length < 5) pushStep(recipe.steps, 'سرو', kind === 'drink' ? 'نوشیدنی را در لیوان مناسب سرو کنید.' : 'غذا را در ظرف مناسب بکشید و گرم سرو کنید.', null);
  if (recipe.steps.length < 5) pushStep(recipe.steps, 'تکمیل', kind === 'drink' ? 'مواد را خوب هم بزنید تا یکدست شود.' : 'حرارت را کم کنید و چند دقیقه استراحت دهید تا طعم‌ها به هم برسند.', 300);
}

function ensureTips(recipe, kind, opts) {
  const tips = (recipe.tips || []).filter((tip) => typeof tip === 'string' && tip.trim());
  if (opts && opts.premium) tips.push('برای سرو مجلسی، مواد تزئینی را دقیقاً قبل از سرو اضافه کنید.');
  if (opts && opts.budget) tips.push('با خرید مواد فصل و حذف اقلام تزئینی، هزینه هر وعده را کاهش دهید.');
  if (opts && opts.quick) tips.push('مواد را از قبل آماده کنید و از زودپز یا حرارت بالا برای پخت سریع‌تر استفاده کنید.');
  if (tips.length < 2) {
    const map = {
      stew: 'حرارت ملایم، عطر و رنگ خورشت را بهتر حفظ می‌کند.',
      rice: 'برنج را از قبل خیس کنید تا دانه‌ها یکدست بپزند.',
      kabab: 'گوشت را حداقل یک ساعت در چاشنی استراحت دهید.',
      soup: 'حبوبات را از قبل خیس کنید تا سریع‌تر بپزند.',
      salad: 'سس را دقیقاً قبل از سرو اضافه کنید.',
      breakfast: 'مواد را تازه آماده کنید و بلافاصله سرو نمایید.',
      dessert: 'مواد را در دمای اتاق اندازه‌گیری کنید.',
      drink: 'نوشیدنی را خنک یا گرم، مطابق سلیقه سرو کنید.',
      snack: 'روغن را در دمای مناسب نگه دارید تا جذب غذا نشود.',
      main: 'مواد اولیه را از قبل آماده کنید تا پخت منظم پیش برود.'
    };
    tips.push(map[kind] || map.main);
  }
  return [...new Set(tips)].slice(0, 4);
}

const SAVORY_ADDS = new Set(['tomato', 'mushroom', 'spinach', 'garlic', 'onion', 'kashk', 'chili', 'pepperFlakes', 'cheese', 'feta', 'mozzarella', 'parmesan', 'olive', 'tuna', 'shrimp', 'chicken', 'beef', 'fish', 'lentils', 'chickpeas', 'whiteBeans', 'potato', 'zucchini', 'eggplant', 'bellPepper', 'corn', 'redBeans', 'breadcrumbs']);
const TITLE_MEAT = {
  chicken: ['مرغ', 'جوجه'],
  beef: ['گوشت', 'ماهیچه', 'کباب کوبیده', 'شیشلیک', 'بختیاری'],
  lamb: ['گوشت', 'ماهیچه', 'کباب کوبیده', 'شیشلیک', 'بختیاری'],
  fish: ['ماهی', 'سالمون', 'قزل‌آلا'],
  shrimp: ['میگو']
};

function build(base, def, kind) {
  const adds = def[3] || [];
  const opts = def[4] || {};
  const suffixCore = def[1].replace(/^با /, '');
  if (suffixCore && base.title.includes(suffixCore)) return null;
  const sweetBase = kind === 'dessert' || /پنکیک|کیک|شله|حلوا|بستنی|دسر|شیرین|کارامل|موس|فرنی|ژله|پودینگ/.test(base.title);
  if (sweetBase && (def[2] || adds.some((key) => SAVORY_ADDS.has(ADD[key] && ADD[key][0])))) return null;
  if (adds.includes('date') && !sweetBase && kind !== 'drink') return null;
  if (opts.cuisine && (!(base.categoryIds || []).includes('international') || /ایرانی|پاستا|اسپاگتی|لازانیا|پیتزا|ریزوتو|آلفردو|بولونز|تیکا|ماسالا|کاری|هندی|مکزیکی|مدیترانه|کاپرزه|یونانی/.test(base.title))) return null;
  const proteinKey = def[2];
  const title = base.title;
  if (proteinKey && /ماست و خیار|ماست موسیر/.test(title)) return null;
  if (proteinKey && !proteinKey.startsWith('V:') && title.includes('گیاهی')) return null;
  if (adds.some((key) => ['bread', 'rice', 'pasta'].includes(key)) && /پیتزا|پاستا|اسپاگتی|لازانیا|برگر|ساندویچ|ریزوتو|نان/.test(title)) return null;
  if (adds.includes('cream') && /برگر|ساندویچ|پیتزا/.test(title)) return null;
  if (kind === 'breakfast' && adds.some((key) => ['banana', 'strawberry', 'honey', 'date', 'apple', 'orange', 'chocolate', 'cocoa', 'iceCream'].includes(key)) && /املت|نرگسی|عدسی|حلیم|کوکو|تخم‌مرغ|قارچ|گوجه|اسفناج|پنیر|سیب‌زمینی|نان و پنیر/.test(title)) return null;
  if (kind === 'drink' && /دوغ/.test(title) && adds.some((key) => ['strawberry', 'banana', 'date', 'honey', 'chocolate', 'cocoa', 'coconutMilk', 'iceCream', 'orange', 'apple', 'sourCherry'].includes(key))) return null;
  if (kind === 'drink' && /چای/.test(title) && adds.some((key) => ['milk', 'cocoa', 'chocolate', 'coconutMilk', 'banana', 'strawberry', 'iceCream'].includes(key))) return null;
  if (/ماست و خیار|حمص|متبل|بابا غنوج/.test(title) && adds.some((key) => ['rice', 'pasta', 'bread'].includes(key))) return null;
  if (proteinKey) {
    const plant = proteinKey.startsWith('V:');
    const key = plant ? proteinKey.slice(2) : proteinKey;
    if (plant && Object.values(TITLE_MEAT).flat().some((term) => base.title.includes(term))) return null;
    if (!plant) {
      const conflicts = Object.entries(TITLE_MEAT).some(([name, terms]) => name !== key && terms.some((term) => base.title.includes(term)));
      if (conflicts) return null;
    }
  }
  const recipe = JSON.parse(JSON.stringify(base));
  const servings = Math.max(2, recipe.servings || 4);
  let lines = recipe.ingredients.map((item) => Object.assign({}, item));
  const oldNames = [];
  let targetName = null;
  let targetSpec = null;
  if (proteinKey) {
    const plant = proteinKey.startsWith('V:');
    const key = plant ? proteinKey.slice(2) : proteinKey;
    const target = PROTEIN[key];
    if (!target) throw new Error('Unknown protein target: ' + key);
    const hasTarget = lines.some((item) => item.ingredientId === target[0]);
    const otherAnimal = lines.some((item) => ANIMAL.has(item.ingredientId) && item.ingredientId !== target[0]);
    if (hasTarget && !otherAnimal) return null;
    if (plant && !lines.some((item) => ANIMAL.has(item.ingredientId))) return null;
    let insertAt = lines.length;
    for (const item of lines) {
      if (ANIMAL.has(item.ingredientId)) {
        if (insertAt === lines.length) insertAt = lines.indexOf(item);
        const ing = byId.get(item.ingredientId);
        if (ing) oldNames.push(ing.name);
      }
    }
    for (const generic of ['گوشت گوسفندی', 'گوشت گوساله', 'گوشت چرخ‌کرده', 'گوشت', 'ماهیچه', 'مرغ', 'بوقلمون', 'ماهی', 'میگو', 'سوسیس', 'جگر']) oldNames.push(generic);
    lines = lines.filter((item) => !ANIMAL.has(item.ingredientId));
    targetSpec = {id: target[0], amount: Math.max(80, servings * target[2]), note: plant ? 'خردشده' : 'آماده‌شده'};
    if (!lines.some((item) => item.ingredientId === target[0])) {
      lines.splice(Math.min(insertAt, lines.length), 0, line(targetSpec.id, targetSpec.amount, 'GRAM', targetSpec.note));
    }
    targetName = target[1];
  }
  const addedKeys = [];
  for (const key of adds) {
    if (add(lines, ADD[key], servings)) addedKeys.push(key);
  }
  if (opts.lowSugar) {
    for (const item of lines) {
      if (['sugar', 'brown-sugar', 'honey', 'date-syrup', 'grape-molasses', 'condensed-milk'].includes(item.ingredientId)) item.amount = Math.round(Math.max(0.5, item.amount * 0.5) * 10) / 10;
    }
  }
  if (opts.diet) {
    for (const item of lines) {
      if (['vegetable-oil', 'olive-oil', 'butter', 'ghee', 'cream'].includes(item.ingredientId)) item.amount = Math.round(Math.max(0.5, item.amount * 0.5) * 10) / 10;
    }
  }
  if (lines.length < 6) {
    for (const key of ['onion', 'garlic', 'oliveOil', 'salt']) add(lines, ADD[key], servings);
  }
  if (lines.length > 12) lines = lines.filter((item) => !['mint', 'parsley', 'coriander', 'dill'].includes(item.ingredientId));
  if (lines.length > 12) lines = lines.slice(0, 12);
  if (targetSpec && !lines.some((item) => item.ingredientId === targetSpec.id)) {
    lines.push(line(targetSpec.id, targetSpec.amount, 'GRAM', targetSpec.note));
  }
  const retainedAdds = addedKeys.filter((key) => ADD[key] && lines.some((item) => item.ingredientId === ADD[key][0]));
  const meaningful = proteinKey || retainedAdds.length > 0 || opts.premium || opts.budget || opts.quick || opts.diet || opts.lowSugar || opts.fried || opts.iced || opts.warm || opts.cuisine;
  if (!meaningful) return null;
  recipe.ingredients = lines;
  recipe.servings = servings;
  recipe.prepMinutes = Math.max(5, recipe.prepMinutes || 10);
  recipe.cookMinutes = Math.max(5, recipe.cookMinutes || 20);
  if (opts.quick) {
    recipe.cookMinutes = Math.max(10, Math.min(30, recipe.cookMinutes - 20));
    recipe.prepMinutes = Math.max(5, Math.min(15, recipe.prepMinutes));
    recipe.categoryIds = [...new Set([...(recipe.categoryIds || []), 'quick'])];
    for (const step of recipe.steps) if (step.timerSeconds) step.timerSeconds = Math.max(60, Math.round(step.timerSeconds * 0.5 / 60) * 60);
  }
  recipe.steps = recipe.steps.map((step) => Object.assign({}, step));
  if (targetName && oldNames.length) {
    recipe.steps = recipe.steps.map((step) => Object.assign({}, step, {
      title: replaceText(step.title || '', oldNames, targetName),
      instruction: replaceText(step.instruction, oldNames, targetName)
    }));
  }
  const addNames = retainedAdds.map((key) => ADD[key] && byId.get(ADD[key][0]) ? byId.get(ADD[key][0]).name : null).filter(Boolean);
  const stepText = recipe.steps.map((step) => (step.title || '') + ' ' + step.instruction).join(' ');
  if (opts.premium) {
    pushStep(recipe.steps, 'تزئین مجلسی', 'زرشک، پسته و کمی کره را تفت دهید و برای تزئین روی غذا استفاده کنید.', null);
  } else if (opts.fried) {
    pushStep(recipe.steps, 'سرخ کردن', 'روغن را تا دمای متوسط گرم کنید و مواد را تا طلایی شدن سرخ کنید.', 240);
  } else if (addNames.length) {
    pushStep(recipe.steps, 'افزودن چاشنی', addNames.join(' و ') + ' را در مرحله مناسب اضافه کنید و اجازه دهید طعم‌ها با غذا ترکیب شوند.', 180);
  }
  if (targetName && !stepText.includes(targetName)) {
    pushStep(recipe.steps, 'افزودن ' + targetName, targetName + ' را با کمی روغن تفت دهید و در مرحله مناسب به غذا اضافه کنید.', 240);
  }
  ensureSteps(recipe, kind);
  recipe.steps = recipe.steps.slice(0, 9);
  recipe.id = base.id + '-' + def[0];
  let joinedTitle = base.title + ' ' + def[1];
  if (def[1].startsWith('با ') && base.title.includes(' با ')) joinedTitle = base.title + ' و ' + def[1].slice(3);
  recipe.title = joinedTitle.replace(/\s+/g, ' ').trim();
  recipe.titleEn = base.titleEn || null;
  const patterns = [
    'نسخه‌ای ' + def[1] + ' از ' + base.title + ' با مواد تازه و پخت خانگی.',
    base.title + ' ' + def[1] + '؛ غذایی خوش‌عطر با دستور مرحله‌به‌مرحله و مواد در دسترس.',
    'ترکیبی تازه از ' + base.title + ' که برای وعده خانوادگی آماده می‌شود.'
  ];
  recipe.description = patterns[Math.abs(hash(recipe.id)) % patterns.length];
  recipe.tags = [...new Set([...(base.tags || []), def[1].replace(/^با /, '')])].slice(0, 6);
  recipe.difficulty = opts.premium ? 'HARD' : (base.difficulty || 'EASY');
  if (opts.budget) recipe.budgetLevel = opts.budget;
  else if (opts.premium) recipe.budgetLevel = 'PREMIUM';
  else if (['shrimp', 'salmon'].includes(proteinKey)) recipe.budgetLevel = 'PREMIUM';
  else if (proteinKey && proteinKey.startsWith('V:')) recipe.budgetLevel = 'BUDGET';
  const diet = dietFor(recipe.ingredients);
  Object.assign(recipe, diet);
  recipe.isFamilyFriendly = true;
  recipe.calories = calories(recipe);
  if (opts.lowSugar) recipe.calories = Math.max(40, Math.round(recipe.calories * 0.75 / 10) * 10);
  if (opts.diet) recipe.calories = Math.max(40, Math.round(recipe.calories * 0.85 / 10) * 10);
  recipe.heroStyle = hero(recipe, kind);
  recipe.season = season(recipe);
  const cats = new Set((base.categoryIds || []).filter((id) => categoryIds.has(id)));
  if (recipe.prepMinutes + recipe.cookMinutes <= 30) cats.add('quick');
  if (recipe.isVegetarian) cats.add('vegetarian'); else cats.delete('vegetarian');
  if (recipe.budgetLevel === 'BUDGET' && (opts.budget === 'BUDGET' || (base.categoryIds || []).includes('economical'))) cats.add('economical'); else cats.delete('economical');
  if (opts.cuisine) cats.add(opts.cuisine);
  recipe.categoryIds = [...cats].slice(0, 5);
  recipe.tips = ensureTips(recipe, kind, opts);
  return recipe;
}

function poolFor(base, kind, existingTitles) {
  const defs = [...(VARIANT_MAP[kind] || [])];
  for (const code of FALLBACKS[kind] || []) {
    if (!defs.some((def) => def[0] === code)) {
      const all = Object.values(VARIANT_MAP).flat();
      const found = all.find((def) => def[0] === code);
      if (found) defs.push(found);
    }
  }
  const pool = [];
  const seen = new Set();
  const titles = new Set(existingTitles);
  for (const def of defs) {
    if (seen.has(def[0])) continue;
    seen.add(def[0]);
    const candidate = build(base, def, kind);
    if (!candidate || !candidate.title || titles.has(candidate.title)) continue;
    if (candidate.ingredients.length < 4 || candidate.ingredients.length > 12) continue;
    if (candidate.steps.length < 4 || candidate.steps.length > 9) continue;
    if (candidate.ingredients.some((item) => !byId.has(item.ingredientId) || !units.has(item.unit))) continue;
    titles.add(candidate.title);
    pool.push(candidate);
  }
  return pool;
}

function normalize(recipe, index) {
  const fixed = JSON.parse(JSON.stringify(recipe));
  fixed.id = fixed.id || 'recipe-' + (index + 1);
  fixed.categoryIds = [...new Set((fixed.categoryIds || []).filter((id) => categoryIds.has(id)))];
  if (!fixed.categoryIds.length) fixed.categoryIds = ['family'];
  fixed.ingredients = (fixed.ingredients || []).filter((item) => byId.has(item.ingredientId) && units.has(item.unit));
  fixed.steps = (fixed.steps || []).filter((step) => step.instruction && step.instruction.trim());
  const kind = kindOf(fixed);
  ensureSteps(fixed, kind);
  fixed.steps = fixed.steps.slice(0, 9);
  fixed.tips = ensureTips(fixed, kind, null);
  Object.assign(fixed, dietFor(fixed.ingredients));
  fixed.calories = calories(fixed);
  fixed.prepMinutes = Math.max(5, fixed.prepMinutes || 10);
  fixed.cookMinutes = Math.max(5, fixed.cookMinutes || 20);
  fixed.servings = Math.max(2, fixed.servings || 4);
  fixed.heroStyle = hero(fixed, kind);
  fixed.season = season(fixed);
  fixed.budgetLevel = fixed.budgetLevel || 'MEDIUM';
  if (fixed.isVegetarian) fixed.categoryIds = [...new Set([...fixed.categoryIds, 'vegetarian'])];
  else fixed.categoryIds = fixed.categoryIds.filter((id) => id !== 'vegetarian');
  if (fixed.budgetLevel === 'BUDGET') fixed.categoryIds = [...new Set([...fixed.categoryIds, 'economical'])];
  else fixed.categoryIds = fixed.categoryIds.filter((id) => id !== 'economical');
  fixed.difficulty = fixed.difficulty || 'EASY';
  fixed.description = (fixed.description || '').trim() || fixed.title + '، غذایی خانگی و خوش‌طعم.';
  fixed.titleEn = fixed.titleEn || null;
  return fixed;
}

function main() {
  const bases = baseFile.recipes.map(normalize);
  const ids = new Set(bases.map((item) => item.id));
  const titles = new Set(bases.map((item) => item.title));
  const pools = bases.map((base) => poolFor(base, kindOf(base), titles));
  const selected = [];
  const target = TARGET - bases.length;
  let round = 0;
  while (selected.length < target && round < 40) {
    for (let i = 0; i < bases.length && selected.length < target; i += 1) {
      const candidate = pools[i][round];
      if (!candidate || ids.has(candidate.id) || titles.has(candidate.title)) continue;
      ids.add(candidate.id);
      titles.add(candidate.title);
      selected.push(candidate);
    }
    round += 1;
  }
  if (selected.length < target) throw new Error('Only ' + selected.length + ' variants; need ' + target);
  const all = bases.concat(selected).map((recipe, index) => Object.assign({}, recipe, {editorialOrder: index + 1}));
  const summary = {
    total: all.length,
    base: bases.length,
    variants: selected.length,
    vegetarian: all.filter((item) => item.isVegetarian).length,
    vegan: all.filter((item) => item.isVegan).length,
    quick: all.filter((item) => item.categoryIds.includes('quick')).length,
    poolMin: Math.min(...pools.map((pool) => pool.length)),
    poolMax: Math.max(...pools.map((pool) => pool.length)),
    ingredientsMin: Math.min(...all.map((item) => item.ingredients.length)),
    ingredientsMax: Math.max(...all.map((item) => item.ingredients.length)),
    stepsMin: Math.min(...all.map((item) => item.steps.length)),
    stepsMax: Math.max(...all.map((item) => item.steps.length))
  };
  fs.writeFileSync(path.join(SEED, 'recipes.json'), JSON.stringify({recipes: all}, null, 2) + '\n');
  console.log(JSON.stringify(summary, null, 2));
}

main();
