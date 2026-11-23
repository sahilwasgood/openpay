#!/usr/bin/env python3
"""End-to-end smoke test for the PWA. Uses system Chrome when available.

Run from the repo root: python3 tests/browser-smoke.py
Skips cleanly when Playwright or a browser is not installed.
"""
import os
import sys
from urllib.parse import quote

try:
    from playwright.sync_api import sync_playwright
except ImportError:
    print('SKIP: playwright not installed')
    sys.exit(0)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'site'))
APP = 'file://' + os.path.join(ROOT, 'app.html')
HOME = 'file://' + os.path.join(ROOT, 'index.html')
CHROME = '/usr/bin/google-chrome'


def tab(pg, name):
    pg.evaluate("document.querySelector('.tab[data-tab=\\\"" + name + "\\\"]').click()")
    pg.wait_for_timeout(80)


def main():
    with sync_playwright() as p:
        opts = {'headless': True, 'args': ['--no-sandbox', '--no-proxy-server', '--disable-dev-shm-usage']}
        if os.path.exists(CHROME):
            opts['executable_path'] = CHROME
        browser = p.chromium.launch(**opts)
        try:
            pg = browser.new_page(viewport={'width': 390, 'height': 844})
            errors = []
            pg.on('pageerror', lambda e: errors.append(str(e)))
            pg.goto(APP, wait_until='load')

            names = pg.locator('.tab').all_text_contents()
            assert names == ['Pay', 'Recharge', 'History', 'Settings'], names
            assert all('note' not in n.lower() and 'verify' not in n.lower() for n in names)

            # Language must be announced correctly by screen readers
            tab(pg, 'settings')
            pg.evaluate("document.getElementById('langHi').click()")
            assert pg.evaluate('document.documentElement.lang') == 'hi'
            assert pg.locator('#carrierBtn').text_content() == 'Jio नहीं'
            pg.evaluate("document.getElementById('langEn').click()")
            assert pg.locator('#carrierBtn').text_content() == 'not Jio'
            assert pg.evaluate('document.documentElement.lang') == 'en'
            tab(pg, 'pay')

            # QR paste autofill
            pg.evaluate("document.getElementById('qrPasteWrap').classList.remove('hidden')")
            pg.fill('#qrPaste', 'upi://pay?pa=test@ybl&pn=Test%20Shop&am=99&cu=INR')
            pg.evaluate("document.getElementById('qrPasteBtn').click()")
            pg.wait_for_timeout(100)
            assert (pg.input_value('#pVpa'), pg.input_value('#pName'), pg.input_value('#pAmt')) == \
                   ('test@ybl', 'Test Shop', '99')

            # Dynamic rail copy follows the language switch too
            tab(pg, 'settings')
            pg.evaluate("document.getElementById('langHi').click()")
            assert 'डेटा' in pg.locator('#railbox').inner_text()
            pg.evaluate("document.getElementById('langEn').click()")
            tab(pg, 'pay')

            # Pay link and history
            pg.evaluate("document.getElementById('payBtn').click()")
            pg.wait_for_timeout(100)
            assert len(pg.evaluate("localStorage.getItem('op_payments')") or '') > 2
            tab(pg, 'history')
            assert 'Test Shop' in pg.locator('#histList').inner_text()
            assert "Don't pay again" in pg.locator('#histList').inner_text()
            assert 'auto-reverses by T+1' in pg.locator('#histList').inner_text()
            pg.evaluate("document.querySelector('#histList .hrow > div').click()")
            pg.wait_for_timeout(100)
            assert any('still pending' in x for x in pg.locator('.wtoast').all_text_contents())
            assert 'hidden' in (pg.locator('#pane-pay').get_attribute('class') or '')
            assert 'hands off to your UPI app' not in pg.locator('#histList').inner_text()

            # History labels are translated too
            tab(pg, 'settings')
            pg.evaluate("document.getElementById('langHi').click()")
            tab(pg, 'history')
            assert 'लंबित' in pg.locator('#histList').inner_text()
            tab(pg, 'settings')
            pg.evaluate("document.getElementById('langEn').click()")
            tab(pg, 'pay')

            # Recharge handoff and cap guard
            tab(pg, 'recharge')
            assert pg.locator('.tab[data-tab="recharge"]').get_attribute('aria-selected') == 'true'
            assert pg.locator('.tab[data-tab="pay"]').get_attribute('aria-selected') == 'false'
            assert pg.locator('#pane-recharge').get_attribute('role') == 'tabpanel'
            assert pg.locator('#pane-recharge').get_attribute('aria-labelledby') == 'tab-recharge'
            pg.fill('#rcNumber', '9876543210')
            pg.fill('#rcAmt', '199')
            pg.fill('#rcAcct', '12345678901')
            pg.wait_for_timeout(100)
            bodies = pg.locator('#rcOutput .rc-code').all_text_contents()
            assert any('MOBRC' in b for b in bodies), bodies
            pg.fill('#rcAmt', '701')
            pg.wait_for_timeout(100)
            assert 'hidden' not in (pg.locator('#rcCapWarn').get_attribute('class') or '')
            assert not any('MOBRC' in b for b in pg.locator('#rcOutput .rc-code').all_text_contents())

            # Arrow keys move between tabs as a proper ARIA tab list does
            pg.evaluate("document.querySelector('.tab[data-tab=\\\"recharge\\\"]').dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true,cancelable:true}))")
            pg.wait_for_timeout(100)
            assert pg.locator('.tab[data-tab="history"]').get_attribute('aria-selected') == 'true'
            assert pg.evaluate('document.activeElement.dataset.tab') == 'history'

            # Share target with a UPI link inside a sentence
            shared = 'Pay me: upi://pay?pa=share@ybl&pn=Share&am=77'
            share = browser.new_page()
            share.goto(APP + '?text=' + quote(shared, safe=''), wait_until='load')
            share.wait_for_timeout(100)
            assert (share.input_value('#pVpa'), share.input_value('#pName'), share.input_value('#pAmt')) == \
                   ('share@ybl', 'Share', '77')

            # Landing rail demo
            home = browser.new_page(viewport={'width': 390, 'height': 844})
            home.goto(HOME, wait_until='load')
            home.click('.seg[data-net="signal"]')
            home.wait_for_timeout(100)
            assert '*99#' in home.locator('#boardCopy').inner_text()
            home.click('.seg[data-carrier="jio"]')
            home.wait_for_timeout(100)
            assert '123Pay' in home.locator('#boardCopy').inner_text()

            # Narrow screens must not scroll sideways; accessibility modes must not break.
            for page_url in (APP, HOME):
                narrow = browser.new_page(viewport={'width': 320, 'height': 700})
                narrow.goto(page_url, wait_until='load')
                assert narrow.evaluate('document.documentElement.scrollWidth') <= 320
                narrow.close()
            ctx = browser.new_context(viewport={'width': 390, 'height': 844},
                                      forced_colors='active', reduced_motion='reduce')
            forced = ctx.new_page()
            forced_errors = []
            forced.on('pageerror', lambda e: forced_errors.append(str(e)))
            forced.goto(APP, wait_until='load')
            assert forced.locator('.tab').count() == 4
            assert not forced_errors, forced_errors[:2]
            ctx.close()

            assert not errors, errors[:3]
        finally:
            browser.close()
    print('browser smoke PASS')


if __name__ == '__main__':
    main()
