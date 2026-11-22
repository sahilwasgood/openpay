// The 123Pay dialer keys DTMF tones, so a decimal amount produces an
// undialable link. UPI intent links can carry paise and must stay unchanged.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'site', 'webapp.js'), 'utf8');
const START = '// @rail-start';
const END = '// @rail-end';
const from = source.indexOf(START);
const to = source.indexOf(END);

if (from < 0 || to < 0 || to < from) {
  test('rail markers exist in webapp.js', () => {
    assert.fail('missing @rail-start / @rail-end markers');
  });
} else {
  const sandbox = { encodeURIComponent };
  vm.createContext(sandbox);
  vm.runInContext(source.slice(from + START.length, to) + '\nthis.railLink = railLink;', sandbox);
  const railLink = sandbox.railLink;

  test('IVR link carries whole rupees only', () => {
    assert.equal(railLink('ivr', '', 'Meera', '45', '9876543210').href,
      'tel:08045163666,,1,9876543210,,45,,1');
    assert.equal(railLink('ivr', '', 'Meera', '45.5', '9876543210'), null);
  });

  test('UPI intent link still allows paise', () => {
    const link = railLink('intent', 'meera@okhdfcbank', 'Meera', '45.50', '');
    assert.equal(link.href, 'upi://pay?pa=meera%40okhdfcbank&pn=Meera&am=45.50&cu=INR');
  });
}
