// Guard the accessible colour pairs used by the redesign. If a token is
// changed later, this fails before the site ships unreadable text.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const css = fs.readFileSync(path.join(__dirname, '..', 'site', 'styles.css'), 'utf8');
const tokens = {};
for (const m of css.matchAll(/--([\w-]+):\s*light-dark\((#[0-9a-fA-F]{6}),\s*(#[0-9a-fA-F]{6})\)/g)) {
  tokens[m[1]] = [m[2], m[3]];
}
for (const m of css.matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]{6})/g)) {
  if (!tokens[m[1]]) tokens[m[1]] = [m[2], m[2]];
}
tokens['glass-stage'] = ['#0B1210', '#0B1210'];

function luminance(hex) {
  const c = [1, 3, 5]
    .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((v) => (v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)));
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}
function contrast(a, b) {
  const x = luminance(a), y = luminance(b);
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
}

const bodyPairs = [
  ['on-background', 'background'],
  ['on-surface', 'surface'],
  ['on-surface-variant', 'surface'],
  ['on-surface-variant', 'surface-container'],
  ['primary', 'surface'],
  ['on-primary', 'primary'],
  ['on-primary-container', 'primary-container'],
  ['on-error-container', 'error-container'],
  ['pending', 'pending-bg'],
  ['success', 'success-bg'],
  ['glass-text', 'glass-stage'],
  ['glass-muted', 'glass-stage'],
  ['glass-signal', 'glass-stage']
];

for (const [fg, bg] of bodyPairs) {
  for (const [name, index] of [['light', 0], ['dark', 1]]) {
    test(`${fg} on ${bg} meets 4.5:1 in ${name}`, () => {
      assert.ok(tokens[fg] && tokens[bg], `missing token ${fg} or ${bg}`);
      assert.ok(contrast(tokens[fg][index], tokens[bg][index]) >= 4.5,
        `${fg} on ${bg} is ${contrast(tokens[fg][index], tokens[bg][index]).toFixed(2)}:1`);
    });
  }
}
