#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SEED = path.join(ROOT, 'app/src/main/assets/seed/recipes.json');
const TARGET = 50000;
const STATUSES = new Set(['VERIFIED', 'REVIEW_REQUIRED', 'REJECTED']);
const recipes = JSON.parse(fs.readFileSync(SEED, 'utf8')).recipes || [];

const counts = {
  total: recipes.length,
  verified: 0,
  reviewRequired: 0,
  rejected: 0,
  withSourceName: 0,
  withSourceUrl: 0,
  withLicense: 0,
  fullEducationalSteps: 0,
  duplicateSignatures: 0
};
const violations = [];
const signatures = new Map();
const titles = new Map();

function value(value) {
  return typeof value === 'string' ? value.trim() : value;
}

function hasText(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

for (const recipe of recipes) {
  const id = recipe.id || '( بدون شناسه )';
  const status = recipe.validationStatus || 'REVIEW_REQUIRED';
  if (!STATUSES.has(status)) violations.push(id + ': وضعیت نامعتبر ' + status);
  if (status === 'VERIFIED') counts.verified += 1;
  else if (status === 'REJECTED') counts.rejected += 1;
  else counts.reviewRequired += 1;

  if (hasText(recipe.sourceName)) counts.withSourceName += 1;
  if (hasText(recipe.sourceUrl)) counts.withSourceUrl += 1;
  if (hasText(recipe.license)) counts.withLicense += 1;

  const steps = Array.isArray(recipe.steps) ? recipe.steps : [];
  const hasSuccessSignal = steps.some((step) => Array.isArray(step.successIndicators) && step.successIndicators.length > 0);
  const hasMistakes = steps.some((step) => Array.isArray(step.commonMistakes) && step.commonMistakes.length > 0);
  const allStepsWritten = steps.length >= 4 && steps.every((step) => hasText(step.title) && hasText(step.instruction));
  if (allStepsWritten && hasSuccessSignal && hasMistakes && hasText(recipe.troubleshooting)) counts.fullEducationalSteps += 1;

  const key = (recipe.title || '').trim() + '|' + (recipe.ingredients || []).map((line) => line.ingredientId).sort().join(',');
  signatures.set(key, (signatures.get(key) || 0) + 1);
  const titleKey = (recipe.title || '').trim();
  titles.set(titleKey, (titles.get(titleKey) || 0) + 1);

  if (status !== 'VERIFIED') continue;

  const required = ['title', 'titleEn', 'description', 'region', 'sourceName', 'sourceUrl', 'license', 'troubleshooting'];
  for (const field of required) {
    if (!hasText(recipe[field])) violations.push(id + ': فیلد تأییدشده خالی است: ' + field);
  }
  if (!Array.isArray(recipe.tips) || recipe.tips.length < 2) violations.push(id + ': نکات حرفه‌ای کافی ندارد');
  if (!Array.isArray(recipe.equipment) || recipe.equipment.length === 0) violations.push(id + ': تجهیزات ثبت نشده است');
  if (steps.length < 4) violations.push(id + ': کمتر از ۴ مرحله دارد');
  for (const step of steps) {
    if (!hasText(step.title) || !hasText(step.instruction)) violations.push(id + ': مرحله بدون عنوان یا دستور اجرایی');
  }
  if (!hasSuccessSignal) violations.push(id + ': نشانه موفقیت مرحله‌ای ندارد');
  if (!hasMistakes) violations.push(id + ': خطای رایج مرحله‌ای ندارد');
}

for (const count of signatures.values()) if (count > 1) counts.duplicateSignatures += count - 1;

const report = {
  targetVerified: TARGET,
  ...counts,
  gapToTarget: Math.max(0, TARGET - counts.verified),
  duplicateTitles: [...titles.values()].filter((count) => count > 1).reduce((sum, count) => sum + count - 1, 0),
  verifiedViolations: violations.length
};

console.log(JSON.stringify(report, null, 2));
if (counts.verified < TARGET) {
  console.warn('هدف تأییدشده محقق نشده است: ' + counts.verified + ' از ' + TARGET + ' دستور.');
}
if (violations.length > 0) {
  console.error('رکوردهای VERIFIED فاقد شرایط انتشار:');
  for (const violation of violations.slice(0, 100)) console.error('- ' + violation);
  process.exitCode = 1;
} else if (process.argv.includes('--require-target') && counts.verified < TARGET) {
  process.exitCode = 1;
} else {
  console.log('هیچ رکورد تأییدشده‌ای فاقد منبع/آموزش کامل نیست.');
}
