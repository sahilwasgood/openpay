// The product promises no network dependencies. These files must not pull
// fonts, scripts or styles from a third party; a font request is also a
// tracking request, and the landing page must render offline after install.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const site = path.join(__dirname, '..', 'site');
const files = ['index.html', 'app.html', 'styles.css', 'webapp.js', 'app.js', 'sw.js'];
const blocked = /fonts\.googleapis\.com|fonts\.gstatic\.com|cdn\.jsdelivr\.net|unpkg\.com|cdnjs\.cloudflare\.com/;

for (const name of files) {
  test(`${name} has no third-party asset requests`, () => {
    const text = fs.readFileSync(path.join(site, name), 'utf8');
    assert.equal(blocked.test(text), false, `${name} references a third-party asset host`);
  });
}

test('local font files back the display and body faces', () => {
  for (const f of ['Inter-Regular.ttf', 'SpaceGrotesk-Medium.ttf', 'SpaceGrotesk-Bold.ttf']) {
    assert.ok(fs.existsSync(path.join(site, 'fonts', f)), `missing site/fonts/${f}`);
  }
  const css = fs.readFileSync(path.join(site, 'styles.css'), 'utf8');
  assert.match(css, /@font-face[^}]*Inter-Regular\.ttf/s);
  assert.match(css, /@font-face[^}]*SpaceGrotesk-Bold\.ttf/s);
});
