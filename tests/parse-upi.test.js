// A pasted QR is untrusted input. Bad percent-encoding must be rejected
// cleanly so the paste handler can show its "no UPI ID" message.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'site', 'webapp.js'), 'utf8');
const START = '// @parse-upi-start';
const END = '// @parse-upi-end';
const from = source.indexOf(START);
const to = source.indexOf(END);

if (from < 0 || to < 0 || to < from) {
  test('parseUpi markers exist in webapp.js', () => {
    assert.fail('missing @parse-upi-start / @parse-upi-end markers');
  });
} else {
  const sandbox = { decodeURIComponent };
  vm.createContext(sandbox);
  vm.runInContext(source.slice(from + START.length, to) + '\nthis.parseUpi = parseUpi;', sandbox);
  const parse = sandbox.parseUpi;

  test('valid UPI QR payload is parsed', () => {
    assert.deepEqual(JSON.parse(JSON.stringify(parse('upi://pay?pa=test%40ybl&pn=Test%20Shop&am=99&cu=INR'))),
      { vpa: 'test@ybl', name: 'Test Shop', amt: '99' });
  });

  test('malformed percent-encoding returns null instead of throwing', () => {
    assert.doesNotThrow(() => assert.equal(parse('upi://pay?pa=%ZZ&pn=Bad'), null));
    assert.equal(parse('upi://pay?pa=ok%40ybl&pn=%E0%A4%A'), null);
  });

  test('a bare VPA still autofills', () => {
    assert.deepEqual(JSON.parse(JSON.stringify(parse('meera@okhdfcbank'))),
      { vpa: 'meera@okhdfcbank', name: '', amt: '' });
  });
}
