// Abre e fecha o menu no mobile. Sem JavaScript o painel fica sempre visível (ver app.css).
(function () {
  var barra = document.querySelector('[data-menu]');
  var botao = barra && barra.querySelector('[data-testid="menu-abrir"]');
  if (!botao) {
    return;
  }

  function definir(aberto) {
    barra.setAttribute('data-aberto', String(aberto));
    botao.setAttribute('aria-expanded', String(aberto));
    botao.setAttribute('aria-label', aberto ? 'Fechar menu' : 'Abrir menu');
  }

  botao.addEventListener('click', function () {
    definir(barra.getAttribute('data-aberto') !== 'true');
  });

  document.addEventListener('keydown', function (evento) {
    if (evento.key === 'Escape' && barra.getAttribute('data-aberto') === 'true') {
      definir(false);
      botao.focus();
    }
  });
})();
