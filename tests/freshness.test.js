// Guard the freshness contract: when online, browsers must not be left on an
// old HTML or service-worker version. These are source assertions because the
// behaviour depends on real HTTP cache headers and a deployed service worker.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const site = path.join(__dirname, '..', 'site');
const app = fs.readFileSync(path.join(site, 'app.js'), 'utf8');
const sw = fs.readFileSync(path.join(site, 'sw.js'), 'utf8');
const html = fs.readFileSync(path.join(site, 'app.html'), 'utf8');

test('service worker script bypasses the HTTP cache on update', () => {
  assert.match(app, /updateViaCache:\s*['"]none['"]/);
});

test('service worker checks for updates on reconnect and focus', () => {
  assert.match(app, /addEventListener\(['"]online['"]/);
  assert.match(app, /addEventListener\(['"]focus['"]/);
  assert.match(app, /\.update\(\)/);
});

test('a new service worker reloads the page once', () => {
  assert.match(app, /controllerchange/);
  assert.match(app, /location\.reload\(\)/);
  assert.match(app, /op_fresh_reloaded/);
});

test('network-first fetches bypass the browser cache', () => {
  assert.match(sw, /cache:\s*['"]reload['"]/);
  assert.match(sw, /cache:\s*['"]no-store['"]/);
});

test('HTML pages carry a version marker for stale-page detection', () => {
  assert.match(html, /__OP_VERSION__/);
  assert.match(html, /cache:\s*['"]reload['"]/);
});
