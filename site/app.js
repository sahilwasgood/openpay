/* PWA: service worker plus the browser install sheet */

(function () {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(function () {});
  }

  var deferred = null;
  var board = document.getElementById('pwaboard');
  var entry = document.getElementById('installEntry');
  var isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
  var standalone = window.matchMedia && window.matchMedia('(display-mode: standalone)').matches;

  function refreshBoardMode() {
    var iosList = document.getElementById('pwbIos');
    var menuList = document.getElementById('pwbMenu');
    var actions = document.getElementById('pwbActions');
    if (!iosList || !menuList || !actions) return;
    iosList.hidden = !(isIOS && !deferred);
    menuList.hidden = !(!isIOS && !deferred);
    actions.hidden = !deferred;
  }

  function openBoard() {
    if (!board) return;
    refreshBoardMode();
    board.hidden = false;
  }

  function closeBoard() {
    if (board) board.hidden = true;
  }

  if (!standalone && entry) {
    entry.hidden = false;
    entry.addEventListener('click', function () {
      // one click: browser prompt when available, the sheet is only the fallback
      if (deferred) {
        deferred.prompt();
        deferred.userChoice.then(function () { deferred = null; });
        return;
      }
      openBoard();
    });
  }

  document.addEventListener('click', function (e) {
    if (e.target && e.target.closest && e.target.closest('[data-close]')) closeBoard();
  });

  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault();
    deferred = e;
    if (entry && !standalone) entry.hidden = false;
  });

  window.addEventListener('appinstalled', function () {
    deferred = null;
    if (entry) entry.hidden = true;
    closeBoard();
  });

  var install = document.getElementById('pwbInstall');
  if (install) {
    install.addEventListener('click', function () {
      if (!deferred) { refreshBoardMode(); return; }
      deferred.prompt();
      deferred.userChoice.then(function () { deferred = null; });
      closeBoard();
    });
  }

  window.addEventListener('beforeinstallprompt', function () {
    if (!board.hidden) refreshBoardMode();
  });
})();
