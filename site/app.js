/* OpenPay · ladder demo + honest network chip */

(function () {
  var seg = document.getElementById('netseg');
  var board = document.getElementById('railboard');
  var label = document.getElementById('raillabel');
  if (seg && board) {
    var picks = {
      full: 'UPI Intent',
      signal: 'USSD, or IVR on Jio',
      offline: 'OpenPay Note'
    };
    seg.addEventListener('click', function (e) {
      var b = e.target.closest('button[data-state]');
      if (!b) return;
      seg.dataset.state = b.dataset.state;
      board.dataset.state = b.dataset.state;
      seg.querySelectorAll('button').forEach(function (x) {
        x.setAttribute('aria-pressed', x === b ? 'true' : 'false');
      });
      if (label) label.textContent = picks[b.dataset.state];
    });
  }

  var chip = document.getElementById('netchip');
  if (chip) {
    var set = function () {
      chip.textContent = navigator.onLine
        ? 'Your network: online'
        : 'Your network: offline, OpenPay would use a Note';
    };
    set();
    window.addEventListener('online', set);
    window.addEventListener('offline', set);
  }
})();
