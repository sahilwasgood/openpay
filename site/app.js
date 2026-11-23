/* PWA: service worker plus the browser install sheet */

(function () {
  if ('serviceWorker' in navigator) {
    var hadController = !!navigator.serviceWorker.controller;
    navigator.serviceWorker.register('./sw.js', { updateViaCache: 'none' }).catch(function () {});

    function checkForUpdate() {
      navigator.serviceWorker.getRegistration().then(function (reg) {
        if (reg) reg.update().catch(function () {});
      }).catch(function () {});
    }
    window.addEventListener('online', checkForUpdate);
    window.addEventListener('focus', checkForUpdate);
    document.addEventListener('visibilitychange', function () {
      if (!document.hidden) checkForUpdate();
    });
    navigator.serviceWorker.addEventListener('controllerchange', function () {
      // The first install takes control without needing a reload.
      if (!hadController) { hadController = true; return; }
      try {
        if (sessionStorage.getItem('op_fresh_reloaded')) return;
        sessionStorage.setItem('op_fresh_reloaded', '1');
      } catch (e) {}
      location.reload();
    });
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
