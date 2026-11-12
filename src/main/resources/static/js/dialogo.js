// Diálogos das telas 8, 9 e 10 (cancelar reserva, rejeitar pedido, marcar como não compareceu). O servidor entrega o <dialog>
// já aberto, então a tela funciona sem JavaScript; aqui ele vira modal (fundo escurecido, foco preso,
// Esc fecha). Ao fechar com Esc, o endereço volta ao da lista (recarregar a página não reabre o diálogo)
// e o foco volta ao link que abriu o diálogo (data-origem guarda o data-testid dele).
(function () {
  if (typeof HTMLDialogElement === 'undefined') {
    return;
  }
  Array.prototype.forEach.call(document.querySelectorAll('dialog.dialogo[open]'), function (dialogo) {
    var voltar = dialogo.querySelector('a[data-fechar]');
    var origem = dialogo.getAttribute('data-origem');
    dialogo.removeAttribute('open');
    dialogo.showModal();
    dialogo.addEventListener('close', function () {
      if (voltar) {
        window.history.replaceState(null, '', voltar.href);
      }
      var gatilho = origem && document.querySelector('[data-testid="' + origem + '"]');
      var destino = gatilho || document.getElementById('conteudo');
      if (destino) {
        destino.focus();
      }
    });
  });
})();
