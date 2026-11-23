/* OpenPay web app: the product in the browser.
   The rail ladder as links (upi:// and tel:), plus offline recharge handoff.
   Everything stored locally. Browsers cannot read SMS: confirmation is manual. */

(function () {
  'use strict';

  // ---------- store ----------

  var LS = {
    get: function (k, d) {
      try { var v = localStorage.getItem('op_' + k); return v ? JSON.parse(v) : d; }
      catch (e) { return d; }
    },
    set: function (k, v) { try { localStorage.setItem('op_' + k, JSON.stringify(v)); } catch (e) {} }
  };

  var state = {
    carrier: LS.get('carrier', null),        // 'jio' | 'other'
    payments: LS.get('payments', [])
  };

  function save() {
    LS.set('carrier', state.carrier);
    LS.set('payments', state.payments);
  }

  function rupees(paise) {
    var r = Math.floor(paise / 100), p = paise % 100;
    return p === 0 ? String(r) : r + '.' + String(p).padStart(2, '0');
  }

  // ---------- rail ladder ----------

  function netKind() {
    return navigator.onLine ? 'online' : 'offline';
  }

  function pickRail(vpa, phone) {
    var net = netKind();
    if (net === 'online') return 'intent';
    if (state.carrier === 'jio') return phone ? 'ivr' : 'none';
    return vpa ? 'ussd' : (phone ? 'ivr' : 'none');
  }

  var RAILTXT = {
    intent: 'UPI Intent · needs data · hands off to your UPI app',
    ussd: 'USSD *99# · needs signal · you finish the carrier menu',
    ivr: 'IVR 123Pay · needs signal · the dialer keys it in',
    none: 'No rail here yet. Add a UPI ID or a phone number for the 123Pay call.'
  };
  var RAILHI = {
    intent: 'UPI Intent · डेटा चाहिए · आपके UPI ऐप को भेजता है',
    ussd: 'USSD *99# · सिग्नल चाहिए · बाकी कैरियर मेन्यू में करें',
    ivr: 'IVR 123Pay · सिग्नल चाहिए · डायलर नंबर दबाता है',
    none: 'अभी कोई रास्ता नहीं। UPI ID या फ़ोन नंबर जोड़ें।'
  };
  function railText(rail) {
    return lang === 'hi'
      ? (RAILHI[rail] || RAILHI.none)
      : (RAILTXT[rail] || RAILTXT.none);
  }
  function railShort(rail) {
    var en = { intent: 'UPI', ussd: 'USSD *99#', ivr: '123Pay' };
    var hi = { intent: 'UPI', ussd: 'USSD *99#', ivr: '123Pay' };
    return lang === 'hi' ? (hi[rail] || '') : (en[rail] || '');
  }

  // @rail-start
  function railLink(rail, vpa, name, amt, phone) {
    if (rail === 'intent') {
      var u = 'upi://pay?pa=' + encodeURIComponent(vpa) +
        '&pn=' + encodeURIComponent(name || 'Payee') +
        '&am=' + amt + '&cu=INR';
      return {
        href: u,
        text: lang === 'hi' ? '₹' + amt + ' UPI से भुगतान करें' : 'Pay Rs ' + amt + ' by UPI'
      };
    }
    if (rail === 'ussd') return {
      href: 'tel:*99*1*3%23',
      text: lang === 'hi' ? '*99*1*3# डायल करें' : 'Dial *99*1*3#'
    };
    if (rail === 'ivr') {
      if (!Number.isInteger(Number(amt))) return null;
      return {
        href: 'tel:08045163666,,1,' + phone + ',,' + amt + ',,1',
        text: lang === 'hi' ? '123Pay कॉल, नंबर दबेगा' : 'Call 123Pay, keyed in'
      };
    }
    return null;
  }
  // @rail-end

  // ---------- pay tab ----------

  var $ = function (id) { return document.getElementById(id); };

  // ---------- i18n ----------

  var HI = {
    tab_pay: 'भुगतान', tab_history: 'इतिहास', tab_settings: 'सेटिंग्स',
    lbl_payee: 'प्राप्तकर्ता का नाम', lbl_vpa: 'UPI ID', lbl_phone: 'या फ़ोन, 123Pay कॉल के लिए',
    lbl_amt: 'रुपये में रक़म', lbl_from: 'आपका नाम, प्राप्तकर्ता को दिखेगा',
    btn_scan: 'UPI QR स्कैन', btn_contact: 'संपर्क चुनें', btn_balance: 'बैलेंस *99*3#',
    first99_s: '*99# पहली बार?',
    first99_p: 'एक बार <a href="tel:*99%23">*99#</a> डायल करें। अपने बैंक का शॉर्ट नाम भेजें, जैसे SBI के लिए SBIN या HDFC के लिए HDFC, या IFSC के पहले चार अक्षर। खाता जोड़ें, फिर पूछे जाने पर डेबिट कार्ड से अपना UPI PIN सेट करें। यही एक सेटअप आगे के हर *99# भुगतान को खोल देता है। आपका PIN कैरियर मेन्यू पर रहता है, इस पेज पर कभी नहीं।',
    btn_export: 'टेक्स्ट में निर्यात', lbl_lang: 'भाषा',
    lbl_elder: 'बड़ा टेक्स्ट मोड', lbl_elder_sub: 'पढ़ने में आसानी के लिए हर स्क्रीन बड़ी',
    lbl_backup: 'बैकअप और पुनर्स्थापना', lbl_backup_sub: 'ब्राउज़र स्टोरेज मिट सकता है। एक्सपोर्ट में आपका इतिहास और सेटिंग्स सुरक्षित रहते हैं।',
    btn_export_json: 'बैकअप निर्यात करें', btn_import_json: 'बैकअप पुनर्स्थापित करें',
    btn_wipe: 'सब मिटाएँ', lbl_wipe_h: 'इस ब्राउज़र को साफ़ करें',
    set_foot: 'OpenPay वेब सब कुछ सिर्फ़ इसी ब्राउज़र में रखता है। कोई सर्वर नहीं, कोई खाता नहीं।',
    tab_recharge: 'रिचार्ज',
    rc_eyebrow: 'रिचार्ज हैंडऑफ़', rc_title: 'रिचार्ज, बिना पैसे रखे।', rc_badge: 'कोई वॉलेट नहीं · कोई सर्वर नहीं',
    rc_intro: 'OpenPay कभी पैसे नहीं रखता। हम सिर्फ़ आपके बैंक का SMS ऐप या डायलर खोलते हैं। बाकी आप करते हैं, और आपका बैंक कन्फर्मेशन भेजता है।',
    rc_method_label: 'तरीका चुनें', rc_sbi: 'SBI Quick', rc_axis: 'Axis SMS', rc_ivr: 'UPI 123Pay', rc_op: 'ऑपरेटर कोड',
    rc_details_h: '1 · आपकी जानकारी', rc_handoff_h: '2 · हैंडऑफ़',
    rc_number: 'रिचार्ज करने वाला मोबाइल नंबर', rc_amount: 'रक़म, रुपयों में',
    rc_acct: 'SBI खाता नंबर, एक बार पंजीकरण के लिए', rc_token: 'ऑपरेटर टोकन',
    rc_otp: 'SBI OTP, 6 अंक, SMS से', rc_token_edit: 'ऑपरेटर टोकन, ज़रूरत हो तो बदलें',
    rc_axis_note: 'Axis सटीक Jio/Airtel/Vi टोकन प्रकाशित नहीं करता। भेजने से पहले टोकन Axis से तय करें।',
    rc_last6: 'अपने Axis खाते के आख़िरी 6 अंक', rc_opsel: 'ऑपरेटर',
    rc_voucher: 'वाउचर कोड, BSNL RC कोड के लिए',
    rc_avoid_h: 'इनसे बचें',
    rc_avoid: 'Vi+Paytm @paytm VPA (Paytm ने August 2024 में हैंडल रिटायर किए) और ICICI SMS रिचार्ज। इनसे रिचार्ज भरोसेमंद नहीं है।'
  };

  var lang = LS.get('lang', 'en');
  document.querySelectorAll('[data-i]').forEach(function (el) {
    if (!el.dataset.en) el.dataset.en = el.innerHTML;
  });
  function applyLang() {
    document.documentElement.lang = lang;
    document.querySelectorAll('[data-i]').forEach(function (el) {
      el.innerHTML = lang === 'hi' ? (HI[el.dataset.i] || el.dataset.en) : el.dataset.en;
    });
    $('langEn').classList.toggle('on', lang === 'en');
    $('langHi').classList.toggle('on', lang === 'hi');
    LS.set('lang', lang);
    $('carrierBtn').textContent = carrierLabel();
    renderRecharge();
    renderPayees();
    renderHistory();
    refreshPay();
  }
  $('langEn').onclick = function () { lang = 'en'; applyLang(); };
  $('langHi').onclick = function () { lang = 'hi'; applyLang(); };

  function t(en, hi) { return lang === 'hi' ? (hi || en) : en; }

  // ---------- settings ----------

  var elder = LS.get('elder', false);
  function applyElder() {
    document.documentElement.classList.toggle('elder', elder);
    $('elderBtn').textContent = elder ? t('On', 'चालू') : t('Off', 'बंद');
    $('elderBtn').classList.toggle('on', elder);
    LS.set('elder', elder);
  }
  $('elderBtn').onclick = function () { elder = !elder; applyElder(); };

  var payerName = LS.get('name', '');
  $('setName').value = payerName;
  $('setName').addEventListener('input', function () {
    payerName = $('setName').value.trim();
    LS.set('name', payerName);
  });
  $('backupBtn').onclick = function () {
    var dump = {};
    Object.keys(localStorage).forEach(function (k) {
      if (k.indexOf('op_') === 0) dump[k] = localStorage.getItem(k);
    });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([JSON.stringify(dump, null, 1)], { type: 'application/json' }));
    a.download = 'openpay-backup.json';
    a.click();
    toast(t('Backup downloaded, keep it private', 'बैकअप डाउनलोड हुआ, इसे निजी रखें'));
  };
  $('restoreBtn').onclick = function () { $('restoreFile').click(); };
  $('restoreFile').addEventListener('change', function (e) {
    var f = e.target.files[0];
    if (!f) return;
    var r = new FileReader();
    r.onload = function () {
      try {
        var dump = JSON.parse(r.result);
        var n = 0;
        Object.keys(dump).forEach(function (k) {
          if (k.indexOf('op_') === 0) { localStorage.setItem(k, dump[k]); n++; }
        });
        toast(t('Restored, reloading', 'पुनर्स्थापित, रीलोड हो रहा है'));
        setTimeout(function () { location.reload(); }, 800);
      } catch (err) { toast(t('Not a backup file', 'यह बैकअप फ़ाइल नहीं है')); }
    };
    r.readAsText(f);
  });
  $('wipeBtn').onclick = function () {
    if (!confirm(t('Wipe your name and history from this browser?', 'इस ब्राउज़र से आपका नाम और इतिहास मिटाएँ?'))) return;
    Object.keys(localStorage).forEach(function (k) {
      if (k.indexOf('op_') === 0) localStorage.removeItem(k);
    });
    location.reload();
  };

  // ---------- share target and hash routing ----------

  function routeIncomingSafe() {
    try { routeIncoming(); } catch (e) {}
  }
  function routeIncoming() {
    var q = new URLSearchParams(location.search);
    var shared = (q.get('text') || '') + ' ' + (q.get('url') || '') + ' ' + (q.get('title') || '');
    shared = shared.trim();
    if (shared) {
      var f = parseUpi(shared);
      if (f) { autofill(f); return; }
    }
    var h = (location.hash || '').replace('#', '');
    if (['pay', 'recharge', 'history', 'settings'].indexOf(h) !== -1) switchTab(h);
  }

  // ---------- recent payees ----------

  function renderPayees() {
    var box = $('payeelist');
    box.innerHTML = '';
    (LS.get('payees', []) || []).slice(0, 6).forEach(function (p) {
      var c = document.createElement('button');
      c.className = 'payee-chip';
      c.type = 'button';
      c.textContent = p.name || p.vpa || p.phone;
      c.onclick = function () {
        $('pName').value = p.name || '';
        $('pVpa').value = p.vpa || '';
        $('pPhone').value = p.phone || '';
        refreshPay();
        toast(t('Filled', 'भर दिया'));
      };
      box.appendChild(c);
    });
  }
  function rememberPayee(name, vpa, phone) {
    var list = LS.get('payees', []) || [];
    var entry = { name: name || '', vpa: vpa || '', phone: phone || '' };
    list = list.filter(function (x) {
      return !((entry.vpa && x.vpa === entry.vpa) || (!entry.vpa && entry.phone && x.phone === entry.phone));
    });
    if (entry.name || entry.vpa || entry.phone) list.unshift(entry);
    LS.set('payees', list.slice(0, 6));
    renderPayees();
  }



  function refreshPay() {
    var name = $('pName').value.trim();
    var vpa = $('pVpa').value.trim();
    var phone = $('pPhone').value.trim().replace(/\D/g, '');
    var amt = $('pAmt').value.trim();
    var ok = amt && Number(amt) > 0 && (vpa || phone);
    var rail = ok ? pickRail(vpa, phone) : 'none';
    $('railbox').textContent = railText(rail);
    var link = ok ? railLink(rail, vpa, name, amt, phone) : null;
    if (!link && ok && rail === 'ivr') {
      $('railbox').textContent = t(
        'IVR 123Pay needs whole rupees. Drop the paise or use a UPI ID with data.',
        '123Pay कॉल में पूरे रुपये चाहिए। पैसे हटाएँ या डेटा पर UPI ID इस्तेमाल करें।'
      );
    }
    if (link) {
      $('payBtn').href = link.href;
      $('payBtn').textContent = link.text;
      $('payBtn').classList.remove('dead');
    } else {
      $('payBtn').removeAttribute('href');
      $('payBtn').textContent = t('Choose a rail', 'रास्ता चुनें');
      $('payBtn').classList.add('dead');
    }
    $('netline').firstChild.textContent =
      t('net: ', 'नेट: ') + netKind() + ' · ' + t('carrier: ', 'कैरियर: ');
  }

  function recordPayment(rail, name, vpa, phone, amt) {
    state.payments.unshift({
      at: Date.now(), rail: rail, name: name, vpa: vpa || '', phone: phone || '',
      paise: Math.round(Number(amt) * 100), state: 'pending'
    });
    state.payments = state.payments.slice(0, 300);
    save();
    rememberPayee(name, vpa, phone);
  }

  // ---------- QR autofill, contacts, balance ----------

  // @parse-upi-start
  function parseUpi(text) {
    var t = (text || '').trim();
    var m = t.match(/upi:\/\/pay\?([^#\s]*)/i);
    if (m) {
      var q = {};
      try {
        m[1].split('&').forEach(function (kv) {
          var p = kv.split('=');
          q[decodeURIComponent(p[0]).toLowerCase()] = decodeURIComponent((p[1] || '').replace(/\+/g, ' '));
        });
      } catch (e) { return null; }
      if (q.pa) {
        return { vpa: q.pa, name: q.pn || '', amt: q.am ? String(Number(q.am)) : '' };
      }
    }
    var v = t.match(/[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}/);
    if (v) return { vpa: v[0], name: '', amt: '' };
    return null;
  }
  // @parse-upi-end

  function autofill(f) {
    $('pVpa').value = f.vpa || '';
    if (f.name) $('pName').value = f.name;
    if (f.amt) $('pAmt').value = f.amt;
    switchTab('pay');
    refreshPay();
    toast(t('Filled from QR', 'QR से भर दिया'));
  }

  $('qrScanBtn').addEventListener('click', function () {
    if (!('BarcodeDetector' in window)) {
      $('qrPasteWrap').classList.remove('hidden');
      $('qrPaste').focus();
      toast(t('This browser cannot scan, paste instead', 'यह ब्राउज़र स्कैन नहीं कर सकता, पेस्ट करें'));
      return;
    }
    var video = $('payScanVideo');
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      .then(function (stream) {
        video.srcObject = stream;
        video.classList.remove('hidden');
        video.play();
        var det = new BarcodeDetector({ formats: ['qr_code'] });
        var timer = setInterval(function () {
          det.detect(video).then(function (codes) {
            if (!codes.length) return;
            var f = parseUpi(codes[0].rawValue);
            clearInterval(timer);
            stream.getTracks().forEach(function (t) { t.stop(); });
            video.classList.add('hidden');
            if (f) autofill(f);
            else toast(t('Not a UPI QR code', 'यह UPI QR नहीं है'));
          }).catch(function () {});
        }, 400);
      })
      .catch(function () {
        $('qrPasteWrap').classList.remove('hidden');
        toast(t('Camera refused, paste instead', 'कैमरा अनुमति नहीं मिली, पेस्ट करें'));
      });
  });

  $('qrPasteBtn').addEventListener('click', function () {
    var f = parseUpi($('qrPaste').value);
    if (f) { $('qrPasteWrap').classList.add('hidden'); autofill(f); }
    else toast(t('No UPI ID found in that text', 'इस टेक्स्ट में UPI ID नहीं मिली'));
  });

  if ('contacts' in navigator && navigator.contacts && navigator.contacts.select) {
    $('contactBtn').hidden = false;
    $('contactBtn').addEventListener('click', function () {
      navigator.contacts.select(['name', 'tel'], false)
        .then(function (list) {
          if (!list.length) return;
          var c = list[0];
          $('pName').value = (c.name && c.name[0]) || '';
          var tel = (c.tel && c.tel[0] || '').replace(/\D/g, '');
          if (tel.length === 12 && tel.indexOf('91') === 0) tel = tel.slice(2);
          if (tel.length === 10) { $('pPhone').value = tel; }
          refreshPay();
          toast(t('Contact filled', 'कॉन्टैक्ट भरा गया'));
        })
        .catch(function () {});
    });
  }


  // ---------- history tab ----------

  function dateLabel(epoch) {
    return new Date(epoch).toLocaleString(lang === 'hi' ? 'hi-IN' : 'en-IN');
  }
  function stateLabel(s) {
    return t(s, s === 'pending' ? 'लंबित' : 'चुकता');
  }
  function stateAction(s) {
    return t(s === 'pending' ? 'mark paid' : 'mark pending',
      s === 'pending' ? 'भुगतान चिह्नित करें' : 'लंबित चिह्नित करें');
  }

  function renderHistory() {
    var box = $('histList');
    box.innerHTML = '';
    if (!state.payments.length) {
      box.innerHTML = '<p class="app-note">' + t('Nothing yet.', 'अभी कुछ नहीं।') + '</p>';
      return;
    }
    state.payments.slice(0, 100).forEach(function (p) {
      var row = document.createElement('div');
      row.className = 'hrow';
      var left = document.createElement('div');
      var b = document.createElement('strong');
      b.textContent = t('Rs ', '₹ ') + rupees(p.paise) + ' · ' + (p.name || p.vpa || p.phone);
      var sub = document.createElement('span');
      sub.className = 'mono dim';
      sub.textContent = dateLabel(p.at) + ' · ' + railShort(p.rail) + ' · ' + stateLabel(p.state);
      left.appendChild(b); left.appendChild(sub);
      if (p.state === 'pending') {
        var note = document.createElement('p');
        note.className = 'app-note';
        note.textContent = t(
          "Your bank has the request. Don't pay again. If the payee was not credited, the bank auto-reverses by T+1. Merchant confirmation failures are refunded by T+5.",
          'आपके बैंक को अनुरोध मिल गया है। दोबारा भुगतान न करें। यदि प्राप्तकर्ता को राशि नहीं मिली, बैंक T+1 तक स्वतः वापस करता है। व्यापारी पुष्टि न मिलने पर T+5 तक रिफंड मिलता है।'
        );
        left.appendChild(note);
      }
      var act = document.createElement('button');
      act.className = 'linklike';
      act.textContent = stateAction(p.state);
      act.onclick = function () {
        p.state = p.state === 'pending' ? 'paid' : 'pending';
        save(); renderHistory();
      };
      left.style.cursor = 'pointer';
      left.onclick = function () {
        $('pName').value = p.name || '';
        $('pVpa').value = p.vpa || '';
        $('pPhone').value = p.phone || '';
        $('pAmt').value = p.paise ? String(Math.round(p.paise / 100)) : '';
        switchTab('pay');
        refreshPay();
        toast(t('Filled, pay again', 'भर गया, फिर से भुगतान करें'));
      };
      row.appendChild(left); row.appendChild(act);
      box.appendChild(row);
    });
  }

  $('exportBtn').onclick = function () {
    var text = state.payments.map(function (p) {
      return dateLabel(p.at) + ' · Rs ' + rupees(p.paise) + ' · ' +
        (p.name || p.vpa || p.phone) + ' · ' + p.rail + ' · ' + p.state;
    }).join('\n') || 'No payments yet.';
    if (navigator.share) navigator.share({ text: text }).catch(function () {});
    else {
      var a = document.createElement('a');
      a.href = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
      a.download = 'openpay-history.txt';
      a.click();
    }
  };

  // ---------- recharge handoff ----------
  // OpenPay holds no money and has no server. Builders below are pure: they
  // return descriptors only. The DOM layer turns them into sms:/tel: links
  // the handset opens. Nothing here can send a message or read a bank SMS.

  var SBI_TARGET = '917208933145';
  var SBI_REG_TARGET = '07208933148';
  var SBI_DAILY_CAP = 700;

  var recharge = {
    method: LS.get('rc_method', 'sbi'),
    number: LS.get('rc_number', '')
  };

  // @recharge-core-start
  function rcDigits(s) { return (s || '').replace(/\D/g, ''); }
  function mobileDigits(s) {
    var d = rcDigits(s);
    if (d.length === 12 && d.indexOf('91') === 0) d = d.slice(2);
    return d;
  }
  function validMobile(s) { return /^[6-9]\d{9}$/.test(mobileDigits(s)); }
  function validAmount(a) { return Number.isFinite(a) && a > 0 && Math.floor(a) === a; }
  function smsHref(target, body) {
    var ua = (typeof navigator !== 'undefined' && navigator.userAgent) || '';
    var ios = /iP(hone|ad|od)/.test(ua) ||
      (/Macintosh/.test(ua) && (navigator.maxTouchPoints || 0) > 1);
    return 'sms:' + target + (ios ? '&body=' : '?body=') + encodeURIComponent(body);
  }
  function smsLinks(targets, body) {
    return targets.map(function (x) { return { label: x, href: smsHref(x, body) }; });
  }
  function step(title, instruction, body, links, note) {
    return { ok: true, title: title, instruction: instruction, body: body, links: links, note: note || '' };
  }
  function fail(error) { return { ok: false, error: error }; }

  // SBI Quick: one-time REG, then a two-step MOBRC / MOBACC flow.
  function buildSbiRegister(account) {
    var acc = (account || '').trim();
    if (!/^\d+$/.test(acc)) return fail(t('Enter your SBI account number to see the one-time registration SMS', 'एक बार पंजीकरण का SMS देखने के लिए अपना SBI खाता नंबर दर्ज करें'));
    var body = 'REG ' + acc;
    return step(
      t('One-time registration', 'एक बार पंजीकरण'),
      t('Send this once from the mobile registered with your SBI account. Skip it if you already registered.', 'अपने SBI खाते से जुड़े मोबाइल से यह एक बार भेजें। अगर पहले हो चुका है तो छोड़ दें।'),
      body, smsLinks([SBI_REG_TARGET], body),
      t('Register to ' + SBI_REG_TARGET + ' · same registered mobile only.', SBI_REG_TARGET + ' पर पंजीकरण · सिर्फ़ उसी पंजीकृत मोबाइल से।')
    );
  }

  function buildSbiStep1(number, amount, token) {
    var n = mobileDigits(number), a = Number(amount);
    if (!validMobile(n)) return fail(t('Enter a valid 10-digit mobile number', 'मान्य 10 अंकों का मोबाइल नंबर दर्ज करें'));
    if (!validAmount(a)) return fail(t('Enter a whole number of rupees above zero', 'शून्य से बड़ी पूरी रक़म रुपयों में दर्ज करें'));
    if (a > SBI_DAILY_CAP) return fail(t('SBI Quick caps at Rs 700 per day (Rs 1,000 a month). Lower the amount.', 'SBI Quick की सीमा Rs 700 प्रति दिन (Rs 1,000 प्रति माह) है। रक़म घटाएँ।'));
    var tok = (token || '').trim().toUpperCase();
    if (!tok) return fail(t('Choose an operator token', 'ऑपरेटर टोकन चुनें'));
    var body = 'MOBRC ' + tok + ' ' + a;
    return step(
      t('Step 1 · Start recharge', 'चरण 1 · रिचार्ज शुरू करें'),
      t('Send this from the mobile registered with your SBI account. Your bank replies with a 6-digit OTP.', 'अपने SBI खाते से जुड़े मोबाइल से भेजें। आपका बैंक 6 अंकों का OTP भेजेगा।'),
      body, smsLinks([SBI_TARGET], body),
      t('Rs 700 a day, Rs 1,000 a month. Same registered mobile only.', 'प्रति दिन Rs 700, प्रति माह Rs 1,000। सिर्फ़ उसी पंजीकृत मोबाइल से।')
    );
  }

  function buildSbiStep2(otp) {
    var o = rcDigits(otp);
    if (!/^\d{6}$/.test(o)) return fail(t('Enter the 6-digit OTP your bank just sent', 'बैंक ने जो 6 अंकों का OTP भेजा है वह दर्ज करें'));
    var body = 'MOBACC ' + o;
    return step(
      t('Step 2 · Confirm with the OTP', 'चरण 2 · OTP से पुष्टि करें'),
      t('Send this within 10 minutes of the OTP. Same registered mobile only.', 'OTP मिलने के 10 मिनट के भीतर भेजें। सिर्फ़ उसी पंजीकृत मोबाइल से।'),
      body, smsLinks([SBI_TARGET], body)
    );
  }

  // Axis: single SMS, no internet needed, any number. Token is editable.
  function buildAxisSms(number, operator, amount, last6) {
    var n = mobileDigits(number), a = Number(amount), l6 = rcDigits(last6);
    var op = (operator || '').trim().toUpperCase();
    if (!validMobile(n)) return fail(t('Enter a valid 10-digit mobile number', 'मान्य 10 अंकों का मोबाइल नंबर दर्ज करें'));
    if (!op) return fail(t('Enter the operator token', 'ऑपरेटर टोकन दर्ज करें'));
    if (!validAmount(a)) return fail(t('Enter a whole number of rupees above zero', 'शून्य से बड़ी पूरी रक़म रुपयों में दर्ज करें'));
    if (!/^\d{6}$/.test(l6)) return fail(t('Enter the last 6 digits of your Axis account', 'अपने Axis खाते के आख़िरी 6 अंक दर्ज करें'));
    var body = 'MOBILE ' + n + ' ' + op + ' ' + a + ' ' + l6;
    return step(
      t('Send the Axis SMS', 'Axis SMS भेजें'),
      t('Works without internet or GPRS, and can recharge any number. Confirm the operator token with Axis before sending.', 'इंटरनेट या GPRS के बिना चलता है और किसी भी नंबर का रिचार्ज कर सकता है। भेजने से पहले ऑपरेटर टोकन Axis से तय करें।'),
      body, smsLinks(['9951860002', '56161600'], body),
      t('If one number does not work, try the other.', 'अगर एक नंबर काम न करे तो दूसरा आज़माएँ।')
    );
  }

  // UPI 123Pay: a phone call, the keypad drives the bank menu.
  function buildIvr(number, amount) {
    var n = mobileDigits(number), a = Number(amount);
    if (!validMobile(n)) return fail(t('Enter a valid 10-digit mobile number', 'मान्य 10 अंकों का मोबाइल नंबर दर्ज करें'));
    if (!validAmount(a)) return fail(t('Enter a whole number of rupees above zero', 'शून्य से बड़ी पूरी रक़म रुपयों में दर्ज करें'));
    return step(
      t('Call UPI 123Pay', 'UPI 123Pay पर कॉल करें'),
      t('Choose Mobile Recharge, say or confirm the operator, then enter the number and amount. Approve with your UPI PIN. Money moves bank → UPI → operator, no wallet.', 'मोबाइल रिचार्ज चुनें, ऑपरेटर बोलें या पुष्टि करें, फिर नंबर और रक़म दर्ज करें। UPI PIN से स्वीकृत करें। पैसे बैंक → UPI → ऑपरेटर जाते हैं, कोई वॉलेट नहीं।'),
      '', [{ label: t('Dial 08045163666', '08045163666 डायल करें'), href: 'tel:08045163666' }],
      t('Menu items vary by bank. Your number: ' + n + ' · Rs ' + a, 'मेन्यू बैंक अनुसार बदलता है। आपका नंबर: ' + n + ' · Rs ' + a)
    );
  }

  // Operator self-service: spends existing balance or a voucher, no bank money.
  function buildOperator(op, voucher) {
    var v = (voucher || '').trim().toUpperCase();
    if (v.indexOf('RC') === 0) v = v.slice(2);
    function dial(label, code) { return { label: label, href: 'tel:' + code }; }

    if (op === 'vi') return step('Vi',
      t('Dial *199# and pick a plan. This spends your existing Vi balance, not bank money.', '*199# डायल करें और प्लान चुनें। यह आपके मौजूदा Vi बैलेंस से कटेगा, बैंक के पैसे से नहीं।'),
      '', [dial(t('Dial *199#', '*199# डायल करें'), '*199%23')]);

    if (op === 'airtel') return step('Airtel',
      t('Dial *121# for recharge offers. For a data loan dial *567*3#.', 'रिचार्ज ऑफ़र के लिए *121# डायल करें। डेटा लोन के लिए *567*3# डायल करें।'),
      '', [dial(t('Dial *121#', '*121# डायल करें'), '*121%23'), dial(t('Data loan *567*3#', 'डेटा लोन *567*3#'), '*567*3%23')]);

    if (op === 'bsnl') {
      var links = [dial(t('Dial *123#', '*123# डायल करें'), '*123%23')];
      if (v) links.push({ label: t('SMS RC code to 123', 'RC कोड 123 पर भेजें'), href: smsHref('123', 'RC' + v) });
      return step('BSNL',
        t('Dial *123# to browse STV codes, or SMS RC followed by your voucher code to 123.', 'STV कोड देखने के लिए *123# डायल करें, या RC के बाद वाउचर कोड 123 पर SMS करें।'),
        '', links,
        v ? t('Voucher SMS: RC' + v, 'वाउचर SMS: RC' + v)
          : t('Enter a voucher code above to enable the RC SMS link.', 'RC SMS लिंक चालू करने के लिए ऊपर वाउचर कोड दर्ज करें।'));
    }

    if (op === 'mtnl') return step('MTNL',
      t('Dial *444#, then choose 4, then 1, to recharge.', '*444# डायल करें, फिर 4, फिर 1 चुनें।'),
      '', [dial(t('Dial *444#', '*444# डायल करें'), '*444%23')]);

    return step('Jio',
      t('Jio has no USSD code. Use the MyJio app, or call the 1299 IVR and pick recharge.', 'Jio का कोई USSD कोड नहीं है। MyJio ऐप इस्तेमाल करें, या 1299 IVR पर कॉल कर रिचार्ज चुनें।'),
      '', [dial(t('Call 1299', '1299 पर कॉल करें'), '1299')]);
  }

  // @recharge-core-end
  // DOM layer: user values go in via textContent, never innerHTML.
  function rcEl(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function rcStepCard(d, n) {
    var card = rcEl('div', 'rc-step' + (d.ok ? '' : ' is-pending'));
    var head = rcEl('div', 'rc-step-head');
    head.appendChild(rcEl('span', 'rc-step-no', d.ok ? String(n) : '·'));
    head.appendChild(rcEl('strong', 'rc-step-title', d.ok ? d.title : t('Waiting for details', 'जानकारी का इंतज़ार')));
    card.appendChild(head);
    if (!d.ok) {
      card.appendChild(rcEl('p', 'rc-step-inst rc-pending', d.error));
      return card;
    }
    if (d.instruction) card.appendChild(rcEl('p', 'rc-step-inst', d.instruction));
    if (d.body) card.appendChild(rcEl('code', 'rc-code', d.body));
    var acts = rcEl('div', 'rc-acts');
    d.links.forEach(function (l) {
      var a = rcEl('a', 'btn btn-primary btn-sm rc-handoff', l.label);
      a.href = l.href;
      acts.appendChild(a);
    });
    card.appendChild(acts);
    if (d.note) card.appendChild(rcEl('p', 'rc-mini', d.note));
    return card;
  }

  function renderRecharge() {
    if (!$('rcOutput')) return;
    document.querySelectorAll('.rc-method').forEach(function (b) {
      var on = b.dataset.method === recharge.method;
      b.classList.toggle('on', on);
      b.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
    document.querySelectorAll('.rc-field[data-for]').forEach(function (f) {
      f.classList.toggle('hidden', f.dataset.for !== recharge.method);
    });

    var number = $('rcNumber').value;
    var amount = $('rcAmt').value;
    var steps = [];
    var over = recharge.method === 'sbi' && Number(amount) > SBI_DAILY_CAP;
    $('rcCapWarn').classList.toggle('hidden', !over);
    if (over) {
      $('rcCapWarn').textContent = t(
        'SBI Quick allows only Rs 700 per day (Rs 1,000 a month). This amount will not be sent.',
        'SBI Quick सिर्फ़ Rs 700 प्रति दिन (Rs 1,000 प्रति माह) देता है। यह रक़म भेजी नहीं जाएगी।'
      );
    }

    if (recharge.method === 'sbi') {
      steps.push(buildSbiRegister($('rcAcct').value));
      steps.push(buildSbiStep1(number, amount, $('rcSbiToken').value));
      steps.push(buildSbiStep2($('rcOtp').value));
    } else if (recharge.method === 'axis') {
      steps.push(buildAxisSms(number, $('rcAxisOp').value, amount, $('rcLast6').value));
    } else if (recharge.method === 'ivr') {
      steps.push(buildIvr(number, amount));
    } else {
      steps.push(buildOperator($('rcOpSel').value, $('rcVoucher').value));
    }

    var out = $('rcOutput');
    out.innerHTML = '';
    steps.forEach(function (d, i) { out.appendChild(rcStepCard(d, i + 1)); });
  }

  $('rcNumber').value = recharge.number;
  document.querySelectorAll('.rc-method').forEach(function (b) {
    b.addEventListener('click', function () {
      recharge.method = b.dataset.method;
      LS.set('rc_method', recharge.method);
      renderRecharge();
    });
  });
  ['rcNumber', 'rcAmt', 'rcAcct', 'rcSbiToken', 'rcOtp', 'rcAxisOp', 'rcLast6', 'rcOpSel', 'rcVoucher'].forEach(function (id) {
    var node = $(id);
    node.addEventListener('input', function () {
      if (id === 'rcNumber') { recharge.number = rcDigits(node.value); LS.set('rc_number', recharge.number); }
      renderRecharge();
    });
    node.addEventListener('change', renderRecharge);
  });

  // ---------- glue ----------

  function switchTab(name) {
    document.querySelectorAll('.tab').forEach(function (t) {
      var active = t.dataset.tab === name;
      t.classList.toggle('on', active);
      t.setAttribute('aria-selected', active ? 'true' : 'false');
      t.setAttribute('tabindex', active ? '0' : '-1');
    });
    document.querySelectorAll('.pane').forEach(function (p) { p.classList.add('hidden'); });
    $('pane-' + name).classList.remove('hidden');
    if (name === 'history') renderHistory();
    if (name === 'recharge') renderRecharge();
  }

  var tabs = Array.prototype.slice.call(document.querySelectorAll('.tab'));
  tabs.forEach(function (t) {
    t.addEventListener('click', function () { switchTab(t.dataset.tab); });
    t.addEventListener('keydown', function (e) {
      var i = tabs.indexOf(t);
      var next = null;
      if (e.key === 'ArrowRight') next = tabs[(i + 1) % tabs.length];
      else if (e.key === 'ArrowLeft') next = tabs[(i - 1 + tabs.length) % tabs.length];
      else if (e.key === 'Home') next = tabs[0];
      else if (e.key === 'End') next = tabs[tabs.length - 1];
      if (!next) return;
      e.preventDefault();
      switchTab(next.dataset.tab);
      next.focus();
    });
  });

  ['pName', 'pVpa', 'pPhone', 'pAmt'].forEach(function (id) {
    $(id).addEventListener('input', refreshPay);
  });

  $('payBtn').addEventListener('click', function () {
    if (!$('payBtn').getAttribute('href')) return;
    recordPayment(
      pickRail($('pVpa').value.trim(), $('pPhone').value.trim()),
      $('pName').value.trim(), $('pVpa').value.trim(), $('pPhone').value.trim(), $('pAmt').value.trim()
    );
  });

  function carrierLabel() {
    return state.carrier === 'jio' ? 'Jio' : t('not Jio', 'Jio नहीं');
  }
  $('carrierBtn').addEventListener('click', function () {
    state.carrier = state.carrier === 'jio' ? 'other' : 'jio';
    $('carrierBtn').textContent = carrierLabel();
    save(); refreshPay();
  });
  $('carrierBtn').textContent = carrierLabel();

  window.addEventListener('online', refreshPay);
  window.addEventListener('offline', refreshPay);

  var toastTimer = null;
  function toast(msg) {
    var t = document.createElement('div');
    t.className = 'wtoast';
    t.textContent = msg;
    document.body.appendChild(t);
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.remove(); }, 2600);
  }

  routeIncomingSafe();
  applyLang();
  applyElder();
  renderPayees();
  refreshPay();
})();
