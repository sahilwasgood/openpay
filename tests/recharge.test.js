// Regression tests for the pure recharge handoff rules in site/webapp.js.
// The builders are deliberately kept inside the no-build PWA. The markers in
// webapp.js define the pure block; this test evaluates that block in isolation
// so a change to an SMS body or a cap fails here before it reaches a handset.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '..', 'site', 'webapp.js'), 'utf8');
const START = '// @recharge-core-start';
const END = '// @recharge-core-end';
const from = source.indexOf(START);
const to = source.indexOf(END);

if (from < 0 || to < 0 || to < from) {
  test('recharge core markers exist in webapp.js', () => {
    assert.fail('missing @recharge-core-start / @recharge-core-end markers');
  });
} else {
  const core = source.slice(from + START.length, to);
  const sandbox = {
    t: (en) => en,
    SBI_TARGET: '917208933145',
    SBI_REG_TARGET: '07208933148',
    SBI_DAILY_CAP: 700,
    navigator: { userAgent: 'Android', maxTouchPoints: 0 },
    encodeURIComponent,
    console
  };
  vm.createContext(sandbox);
  vm.runInContext(core + `
    this.api = { rcDigits, smsHref, buildSbiRegister, buildSbiStep1,
      buildSbiStep2, buildAxisSms, buildIvr, buildOperator };
  `, sandbox);
  const api = sandbox.api;

  test('SBI registration builds REG message to the bank short code', () => {
    const d = api.buildSbiRegister('12345678901');
    assert.equal(d.body, 'REG 12345678901');
    assert.equal(d.links[0].href, 'sms:07208933148?body=REG%2012345678901');
    assert.equal(api.buildSbiRegister('abc').ok, false);
  });

  test('SBI step one uses operator token and rejects amounts over 700', () => {
    assert.equal(api.buildSbiStep1('9876543210', '199', 'JIO').body, 'MOBRC JIO 199');
    assert.equal(api.buildSbiStep1('9876543210', '700', 'VI').body, 'MOBRC VI 700');
    const over = api.buildSbiStep1('9876543210', '701', 'VI');
    assert.equal(over.ok, false);
    assert.equal(over.links, undefined);
    assert.equal(api.buildSbiStep1('123', '100', 'VI').ok, false);
  });

  test('SBI step two validates the six digit OTP', () => {
    const d = api.buildSbiStep2('12 34 56');
    assert.equal(d.body, 'MOBACC 123456');
    assert.equal(api.buildSbiStep2('1234').ok, false);
  });

  test('Axis SMS has both published targets and validates input', () => {
    const d = api.buildAxisSms('+91 98765 43210', 'jio', '199', '123456');
    assert.equal(d.body, 'MOBILE 9876543210 JIO 199 123456');
    assert.equal(d.links.map((l) => l.href.split('?')[0]).join(','), 'sms:9951860002,sms:56161600');
    assert.equal(api.buildAxisSms('1234567890', 'JIO', '199', '123456').ok, false);
    assert.equal(api.buildAxisSms('9876543210', 'JIO', '199', '12345').ok, false);
  });

  test('fractional rupees are rejected in every bank handoff', () => {
    assert.equal(api.buildSbiStep1('9876543210', '199.5', 'VI').ok, false);
    assert.equal(api.buildAxisSms('9876543210', 'JIO', '199.5', '123456').ok, false);
    assert.equal(api.buildIvr('9876543210', '199.5').ok, false);
  });

  test('UPI 123Pay hands off to the IVR number', () => {
    const d = api.buildIvr('9876543210', '199');
    assert.equal(d.links[0].href, 'tel:08045163666');
    assert.equal(api.buildIvr('9876543210', '0').ok, false);
  });

  test('operator menus never invent codes and BSNL voucher is not doubled', () => {
    assert.equal(api.buildOperator('vi', '').links[0].href, 'tel:*199%23');
    assert.equal(api.buildOperator('airtel', '').links.length, 2);
    const bsnl = api.buildOperator('bsnl', 'RC123456789012345678');
    assert.equal(bsnl.links[1].href, 'sms:123?body=RC123456789012345678');
    assert.equal(api.buildOperator('bsnl', '').links.length, 1);
    assert.equal(api.buildOperator('jio', '').links[0].href, 'tel:1299');
  });

  test('SMS URI uses the iOS body separator on Apple devices', () => {
    sandbox.navigator.userAgent = 'iPhone';
    assert.match(api.smsHref('917208933145', 'MOBRC JIO 199'), /^sms:917208933145&body=/);
    sandbox.navigator.userAgent = 'Android';
    assert.match(api.smsHref('917208933145', 'MOBRC JIO 199'), /^sms:917208933145\?body=/);
  });

  test('iPadOS desktop user agent still gets the iOS separator', () => {
    sandbox.navigator.userAgent = 'Macintosh; Intel Mac OS X 10_15_7';
    sandbox.navigator.maxTouchPoints = 5;
    assert.match(api.smsHref('917208933145', 'MOBRC JIO 199'), /^sms:917208933145&body=/);
    sandbox.navigator.maxTouchPoints = 0;
    assert.match(api.smsHref('917208933145', 'MOBRC JIO 199'), /^sms:917208933145\?body=/);
    sandbox.navigator.userAgent = 'Android';
  });
}
