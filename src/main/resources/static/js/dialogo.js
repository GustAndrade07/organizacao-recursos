// Diálogos das telas 9 e 10 (rejeitar pedido, marcar como não compareceu). O servidor entrega o <dialog>
// já aberto, então a tela funciona sem JavaScript; aqui ele vira modal (fundo escurecido, foco preso,
// Esc fecha). Ao fechar com Esc, o endereço volta ao da lista, para recarregar a página não reabrir o diálogo.
(function () {
  if (typeof HTMLDialogElement === 'undefined') {
    return;
  }
  Array.prototype.forEach.call(document.querySelectorAll('dialog.dialogo[open]'), function (dialogo) {
    var voltar = dialogo.querySelector('a[data-fechar]');
    dialogo.removeAttribute('open');
    dialogo.showModal();
    dialogo.addEventListener('close', function () {
      if (voltar) {
        window.history.replaceState(null, '', voltar.href);
      }
      var principal = document.getElementById('conteudo');
      if (principal) {
        principal.focus();
      }
    });
  });
})();
