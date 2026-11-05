// Depois de uma tentativa que falhou, leva o foco ao aviso para teclado e leitor de tela.
(function () {
  var aviso = document.querySelector('[data-testid="login-erro"]');
  if (aviso) {
    aviso.focus();
  }
})();
