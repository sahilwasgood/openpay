/* OpenPay web app: the product in the browser.
   The rail ladder as links (upi:// and tel:), notes signed with WebCrypto
   Ed25519 using the exact canonical string the Android app uses, everything
   stored locally. Browsers cannot read SMS: confirmation is manual. */

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
    payments: LS.get('payments', []),
    issued: LS.get('issued', []),
    receivables: LS.get('receivables', []),
    seen: LS.get('seen_serials', []),
    pinned: LS.get('pinned_keys', {}),
    blocked: LS.get('blocked_keys', []),
    key: LS.get('keypair', null)
  };

  function save() {
    LS.set('carrier', state.carrier);
    LS.set('payments', state.payments);
    LS.set('issued', state.issued);
    LS.set('receivables', state.receivables);
    LS.set('seen_serials', state.seen);
    LS.set('pinned_keys', state.pinned);
    LS.set('blocked_keys', state.blocked);
  }

  // ---------- canonical note string, byte identical to the Android app ----------

  function canonical(d) {
    return ['openpay-note', d.v, d.from, d.payerKey, d.to,
      d.vpa || '', d.phone || '', d.amountPaise, d.serial, d.nonce,
      d.expiresEpoch, d.memo || ''].join('|');
  }

  function b64(buf) {
    var bytes = new Uint8Array(buf), s = '', i;
    for (i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
    return btoa(s);
  }
  function unb64(s) {
    var bin = atob(s.replace(/-/g, '+').replace(/_/g, '/')), bytes = new Uint8Array(bin.length), i;
    for (i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return bytes;
  }
  function rupees(paise) {
    var r = Math.floor(paise / 100), p = paise % 100;
    return p === 0 ? String(r) : r + '.' + String(p).padStart(2, '0');
  }
  function serial() {
    var a = new Uint8Array(5); crypto.getRandomValues(a);
    return 'NB' + Array.from(a).map(function (b) { return b.toString(16).padStart(2, '0').toUpperCase(); }).join('');
  }
  function nonce() {
    var a = new Uint8Array(4); crypto.getRandomValues(a);
    return Array.from(a).map(function (b) { return b.toString(16).padStart(2, '0').toUpperCase(); }).join('');
  }

  function ensureKey() {
    if (state.key) return Promise.resolve(state.key);
    return crypto.subtle.generateKey({ name: 'Ed25519' }, true, ['sign', 'verify'])
      .then(function (kp) {
        return Promise.all([
          crypto.subtle.exportKey('raw', kp.publicKey),
          crypto.subtle.exportKey('jwk', kp.privateKey)
        ]).then(function (out) {
          state.key = { pub: b64(out[0]), priv: out[1] };
          LS.set('keypair', state.key);
          return state.key;
        });
      });
  }

  function signNote(d) {
    return ensureKey().then(function (k) {
      d.payerKey = k.pub;
      return crypto.subtle.importKey('jwk', k.priv, { name: 'Ed25519' }, false, ['sign'])
        .then(function (sk) {
          return crypto.subtle.sign('Ed25519', sk, new TextEncoder().encode(canonical(d)));
        })
        .then(function (sig) {
          return { op: 'openpay-note', d: d, sig: b64(sig) };
        });
    });
  }

  // ---------- rail ladder ----------

  function netKind() {
    return navigator.onLine ? 'online' : 'offline';
  }

  function pickRail(vpa, phone) {
    var net = netKind();
    if (net === 'online') return 'intent';
    if (state.carrier === 'jio') return phone ? 'ivr' : 'nonote';
    return vpa ? 'ussd' : (phone ? 'ivr' : 'none');
  }

  var RAILTXT = {
    intent: 'UPI Intent · needs data · hands off to your UPI app',
    ussd: 'USSD *99# · needs signal · you finish the carrier menu',
    ivr: 'IVR 123Pay · needs signal · the dialer keys it in',
    nonote: 'Jio has no USSD and the 123Pay call needs a phone number. Add one, or write a Note.',
    none: 'Add a UPI ID or phone first.'
  };

  function railLink(rail, vpa, name, amt, phone) {
    if (rail === 'intent') {
      var u = 'upi://pay?pa=' + encodeURIComponent(vpa) +
        '&pn=' + encodeURIComponent(name || 'Payee') +
        '&am=' + amt + '&cu=INR';
      return { href: u, text: 'Pay Rs ' + amt + ' by UPI' };
    }
    if (rail === 'ussd') return { href: 'tel:*99*1*3%23', text: 'Dial *99*1*3#' };
    if (rail === 'ivr') {
      return { href: 'tel:08045163666,,1,' + phone + ',,' + amt + ',,1', text: 'Call 123Pay, keyed in' };
    }
    return null;
  }

  // ---------- pay tab ----------

  var $ = function (id) { return document.getElementById(id); };

  function refreshPay() {
    var name = $('pName').value.trim();
    var vpa = $('pVpa').value.trim();
    var phone = $('pPhone').value.trim().replace(/\D/g, '');
    var amt = $('pAmt').value.trim();
    var ok = amt && Number(amt) > 0 && (vpa || phone);
    var rail = ok ? pickRail(vpa, phone) : 'none';
    $('railbox').textContent = RAILTXT[rail] || RAILTXT.none;
    var link = ok ? railLink(rail, vpa, name, amt, phone) : null;
    if (link) {
      $('payBtn').href = link.href;
      $('payBtn').textContent = link.text;
      $('payBtn').classList.remove('dead');
    } else {
      $('payBtn').removeAttribute('href');
      $('payBtn').textContent = rail === 'nonote' ? 'Write a Note instead' : 'Choose a rail';
      $('payBtn').classList.toggle('dead', rail !== 'nonote');
      $('payBtn').onclick = rail === 'nonote' ? function () { switchTab('notes'); } : null;
    }
    $('netline').firstChild.textContent =
      'net: ' + netKind() + ' · carrier: ';
  }

  function recordPayment(rail, name, vpa, phone, amt) {
    state.payments.unshift({
      at: Date.now(), rail: rail, name: name, vpa: vpa || '', phone: phone || '',
      paise: Math.round(Number(amt) * 100), state: 'pending'
    });
    state.payments = state.payments.slice(0, 300);
    save();
  }

  // ---------- notes tab ----------

  function openPaise() {
    return state.issued.filter(function (n) { return !n.settledAt; })
      .reduce(function (a, n) { return a + n.d.amountPaise; }, 0);
  }

  function renderOpenNotes() {
    $('noteOpenLine').textContent = 'Rs ' + rupees(openPaise()) + ' of Rs 10,000 open';
    var box = $('openNotes');
    box.innerHTML = '';
    state.issued.slice(0, 30).forEach(function (n) {
      var row = document.createElement('div');
      row.className = 'hrow';
      var left = document.createElement('div');
      var b = document.createElement('strong');
      b.textContent = 'Rs ' + rupees(n.d.amountPaise) + ' to ' + n.d.to +
        (n.settledAt ? ' · settled' : ' · ' + new Date(n.d.expiresEpoch).toLocaleTimeString());
      var sub = document.createElement('span');
      sub.className = 'mono dim';
      sub.textContent = n.d.serial;
      left.appendChild(b); left.appendChild(sub);
      var act = document.createElement('button');
      act.className = 'linklike';
      act.textContent = n.settledAt ? 'hide' : 'mark settled';
      act.onclick = function () {
        if (n.settledAt) { row.remove(); return; }
        n.settledAt = Date.now(); save(); renderOpenNotes();
      };
      row.appendChild(left); row.appendChild(act);
      box.appendChild(row);
    });
  }

  var expMinutes = 30;

  function createNote() {
    var from = $('nFrom').value.trim() || 'OpenPay payer';
    var to = $('nTo').value.trim();
    var vpa = $('nVpa').value.trim();
    var amt = Number($('nAmt').value);
    if (!to || !vpa || !amt || amt <= 0) { toast('Name, UPI ID and amount are needed'); return; }
    if (amt > 2000) { toast('Notes cap at Rs 2,000 each'); return; }
    if (openPaise() + amt * 100 > 1000000) { toast('Rs 10,000 open at once is the cap'); return; }
    var d = {
      v: 1, from: from, payerKey: '', to: to, vpa: vpa, phone: '',
      amountPaise: Math.round(amt * 100), serial: serial(), nonce: nonce(),
      expiresEpoch: Date.now() + expMinutes * 60000, memo: ''
    };
    signNote(d).then(function (env) {
      state.issued.unshift({ d: env.d, issuedAt: Date.now(), envelope: env, settledAt: null });
      state.seen.push(env.d.serial);
      save();
      renderOpenNotes();
      showQr(env);
    }).catch(function (e) { toast('This browser cannot sign Ed25519 notes'); });
  }

  function showQr(env) {
    var qr = qrcode(0, 'M');
    qr.addData(JSON.stringify(env));
    qr.make();
    $('qrbox').innerHTML = qr.createSvgTag({ cellSize: 4, margin: 2 });
    $('qrserial').textContent = env.d.serial + ' · expires ' + new Date(env.d.expiresEpoch).toLocaleTimeString();
    $('qrwrap').classList.remove('hidden');
    $('noteCopy').onclick = function () {
      navigator.clipboard.writeText(JSON.stringify(env)).then(function () { toast('Note text copied'); });
    };
    $('noteShare').onclick = function () {
      if (navigator.share) navigator.share({ text: JSON.stringify(env) }).catch(function () {});
      else { $('noteCopy').click(); }
    };
  }

  // ---------- verify tab ----------

  function verdictHtml(ok, title, pairs, foot) {
    var h = '<div class="vres ' + (ok ? 'ok' : 'bad') + '"><div class="vhead">' + title + '</div>';
    pairs.forEach(function (p) {
      h += '<div class="vrow"><span>' + p[0] + '</span><strong>' + p[1] + '</strong></div>';
    });
    h += '<p class="vfoot">' + (foot || '') + '</p></div>';
    return h;
  }

  function verifyNoteText(text) {
    var env;
    try { env = JSON.parse(text.trim()); } catch (e) {
      $('verifyResult').innerHTML = verdictHtml(false, 'Not a Note', [['Reason', 'not valid JSON']], ''); return;
    }
    if (!env || env.op !== 'openpay-note' || !env.d || !env.sig) {
      $('verifyResult').innerHTML = verdictHtml(false, 'Not a Note', [['Reason', 'missing fields']], ''); return;
    }
    var d = env.d;
    var pairs = [
      ['From', d.from || '?'], ['To', d.to || '?'],
      ['Amount', 'Rs ' + rupees(Number(d.amountPaise) || 0)], ['Serial', d.serial || '?'],
      ['Expires', d.expiresEpoch ? new Date(Number(d.expiresEpoch)).toLocaleString() : '?']
    ];
    var known = !!state.pinned[d.payerKey];
    var blocked = state.blocked.indexOf(d.payerKey) !== -1;
    if (known) pairs.push(['Payer', 'known']);
    if (blocked) pairs.push(['Payer', 'BLOCKED']);

    crypto.subtle.importKey('raw', unb64(d.payerKey), { name: 'Ed25519' }, false, ['verify'])
      .then(function (k) {
        return crypto.subtle.verify('Ed25519', k, unb64(env.sig),
          new TextEncoder().encode(canonical(d)));
      })
      .then(function (sigOk) {
        if (!sigOk) {
          $('verifyResult').innerHTML = verdictHtml(false, 'Signature does not match. Refuse.', pairs, ''); return;
        }
        if (blocked) {
          $('verifyResult').innerHTML = verdictHtml(false, 'Blacklisted payer. Refuse.', pairs, ''); return;
        }
        if (state.seen.indexOf(d.serial) !== -1) {
          $('verifyResult').innerHTML = verdictHtml(false, 'Duplicate serial, already seen.', pairs, ''); return;
        }
        if (Number(d.amountPaise) <= 0 || Number(d.amountPaise) > 200000) {
          $('verifyResult').innerHTML = verdictHtml(false, 'Amount outside note limits.', pairs, ''); return;
        }
        if (Date.now() >= Number(d.expiresEpoch)) {
          $('verifyResult').innerHTML = verdictHtml(false, 'Note expired.', pairs, ''); return;
        }
        state.seen.push(d.serial);
        state.receivables.unshift({ d: d, at: Date.now(), known: known });
        save();
        renderReceivables();
        var first = !known && Number(d.amountPaise) > 50000;
        $('verifyResult').innerHTML = verdictHtml(!first,
          first ? 'New payer, above Rs 500. Your call.' : 'Verified. Signature checks out.',
          pairs,
          first ? 'Notes are promissory notes, not money. Accept like a first time customer, or refuse.' :
            'Recorded in Received below. Pin the payer to trust this key next time.');
        if (!known) {
          var btn = document.createElement('button');
          btn.className = 'btn btn-ghost btn-sm';
          btn.textContent = 'Pin this payer';
          btn.onclick = function () {
            state.pinned[d.payerKey] = d.from; save();
            $('verifyResult').appendChild(document.createTextNode(' pinned'));
          };
          $('verifyResult').querySelector('.vres').appendChild(btn);
        }
      })
      .catch(function (e) {
        $('verifyResult').innerHTML = verdictHtml(false, 'Could not verify', [['Reason', String(e && e.message || e)]], '');
      });
  }

  function renderReceivables() {
    var box = $('receivables');
    box.innerHTML = '';
    state.receivables.slice(0, 50).forEach(function (r) {
      var row = document.createElement('div');
      row.className = 'hrow';
      var left = document.createElement('div');
      var b = document.createElement('strong');
      b.textContent = 'Rs ' + rupees(r.d.amountPaise) + ' from ' + r.d.from + (r.known ? ' · known' : '');
      var sub = document.createElement('span');
      sub.className = 'mono dim';
      sub.textContent = new Date(r.at).toLocaleString() + ' · ' + r.d.serial;
      left.appendChild(b); left.appendChild(sub);
      var act = document.createElement('button');
      act.className = 'linklike warn';
      act.textContent = 'block';
      act.onclick = function () {
        state.blocked.push(r.d.payerKey); save();
        act.textContent = 'blocked';
      };
      row.appendChild(left); row.appendChild(act);
      box.appendChild(row);
    });
  }

  // camera scan where the browser allows it
  function startScan() {
    if (!('BarcodeDetector' in window)) { toast('This browser cannot scan, paste instead'); $('pasteBtn').click(); return; }
    var video = $('scanVideo');
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      .then(function (stream) {
        video.srcObject = stream;
        video.classList.remove('hidden');
        video.play();
        var det = new BarcodeDetector({ formats: ['qr_code'] });
        var timer = setInterval(function () {
          det.detect(video).then(function (codes) {
            if (codes.length) {
              clearInterval(timer);
              stream.getTracks().forEach(function (t) { t.stop(); });
              video.classList.add('hidden');
              verifyNoteText(codes[0].rawValue);
            }
          }).catch(function () {});
        }, 400);
      })
      .catch(function () { toast('Camera refused, paste instead'); $('pasteBtn').click(); });
  }


  // ---------- QR autofill, contacts, balance ----------

  function parseUpi(text) {
    var t = (text || '').trim();
    var m = t.match(/^upi:\/\/pay\?([^#]*)/i);
    if (m) {
      var q = {};
      m[1].split('&').forEach(function (kv) {
        var p = kv.split('=');
        q[decodeURIComponent(p[0]).toLowerCase()] = decodeURIComponent((p[1] || '').replace(/\+/g, ' '));
      });
      if (q.pa) {
        return { vpa: q.pa, name: q.pn || '', amt: q.am ? String(Number(q.am)) : '' };
      }
    }
    var v = t.match(/[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}/);
    if (v) return { vpa: v[0], name: '', amt: '' };
    return null;
  }

  function autofill(f) {
    $('pVpa').value = f.vpa || '';
    if (f.name) $('pName').value = f.name;
    if (f.amt) $('pAmt').value = f.amt;
    switchTab('pay');
    refreshPay();
    toast('Filled from QR');
  }

  $('qrScanBtn').addEventListener('click', function () {
    if (!('BarcodeDetector' in window)) {
      $('qrPasteWrap').classList.remove('hidden');
      $('qrPaste').focus();
      toast('This browser cannot scan, paste instead');
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
            else toast('Not a UPI QR code');
          }).catch(function () {});
        }, 400);
      })
      .catch(function () {
        $('qrPasteWrap').classList.remove('hidden');
        toast('Camera refused, paste instead');
      });
  });

  $('qrPasteBtn').addEventListener('click', function () {
    var f = parseUpi($('qrPaste').value);
    if (f) { $('qrPasteWrap').classList.add('hidden'); autofill(f); }
    else toast('No UPI ID found in that text');
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
          toast('Contact filled');
        })
        .catch(function () {});
    });
  }

  // ---------- history tab ----------

  function renderHistory() {
    var box = $('histList');
    box.innerHTML = '';
    if (!state.payments.length) {
      box.innerHTML = '<p class="app-note">Nothing yet.</p>';
      return;
    }
    state.payments.slice(0, 100).forEach(function (p) {
      var row = document.createElement('div');
      row.className = 'hrow';
      var left = document.createElement('div');
      var b = document.createElement('strong');
      b.textContent = 'Rs ' + rupees(p.paise) + ' · ' + (p.name || p.vpa || p.phone);
      var sub = document.createElement('span');
      sub.className = 'mono dim';
      sub.textContent = new Date(p.at).toLocaleString() + ' · ' + p.rail + ' · ' + p.state;
      left.appendChild(b); left.appendChild(sub);
      var act = document.createElement('button');
      act.className = 'linklike';
      act.textContent = p.state === 'pending' ? 'mark paid' : 'mark pending';
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
        toast('Filled, pay again');
      };
      row.appendChild(left); row.appendChild(act);
      box.appendChild(row);
    });
  }

  $('exportBtn').onclick = function () {
    var text = state.payments.map(function (p) {
      return new Date(p.at).toLocaleString() + ' · Rs ' + rupees(p.paise) + ' · ' +
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

  // ---------- glue ----------

  function switchTab(name) {
    document.querySelectorAll('.tab').forEach(function (t) {
      t.classList.toggle('on', t.dataset.tab === name);
    });
    document.querySelectorAll('.pane').forEach(function (p) { p.classList.add('hidden'); });
    $('pane-' + name).classList.remove('hidden');
    if (name === 'history') renderHistory();
    if (name === 'notes') renderOpenNotes();
    if (name === 'verify') renderReceivables();
  }

  document.querySelectorAll('.tab').forEach(function (t) {
    t.addEventListener('click', function () { switchTab(t.dataset.tab); });
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

  $('carrierBtn').addEventListener('click', function () {
    state.carrier = state.carrier === 'jio' ? 'other' : 'jio';
    $('carrierBtn').textContent = state.carrier === 'jio' ? 'Jio' : 'not Jio';
    save(); refreshPay();
  });
  if (state.carrier) $('carrierBtn').textContent = state.carrier === 'jio' ? 'Jio' : 'not Jio';

  $('expChips').querySelectorAll('.chip').forEach(function (c) {
    c.addEventListener('click', function () {
      $('expChips').querySelectorAll('.chip').forEach(function (x) { x.classList.remove('on'); });
      c.classList.add('on');
      expMinutes = Number(c.dataset.min);
    });
  });

  $('noteBtn').addEventListener('click', createNote);
  $('scanBtn').addEventListener('click', startScan);
  $('pasteBtn').addEventListener('click', function () {
    $('notePaste').classList.remove('hidden');
    $('pasteVerify').classList.remove('hidden');
    $('scanVideo').classList.add('hidden');
    $('notePaste').focus();
  });
  $('pasteVerify').addEventListener('click', function () { verifyNoteText($('notePaste').value); });

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

  refreshPay();
  renderOpenNotes();
})();
