// Relatório de utilização: bloqueia fim anterior ao início antes do envio (RN-13), com o erro em texto
// junto ao campo. Sem JavaScript o formulário envia normalmente e o servidor devolve o mesmo erro.
(function () {
  var form = document.querySelector('[data-testid="relatorio-filtros"]');
  if (!form) {
    return;
  }
  var inicio = form.querySelector('[data-testid="relatorio-inicio"]');
  var fim = form.querySelector('[data-testid="relatorio-fim"]');
  var erro = document.getElementById('relatorio-fim-erro');
  var mensagem = 'A data de fim não pode ser anterior à data de início.';

  function periodoInvalido() {
    return inicio.value !== '' && fim.value !== '' && fim.value < inicio.value;
  }

  function limparErro() {
    fim.classList.remove('campo__entrada--erro');
    fim.removeAttribute('aria-invalid');
    fim.removeAttribute('aria-describedby');
    erro.textContent = '';
    erro.hidden = true;
  }

  function mostrarErro() {
    fim.classList.add('campo__entrada--erro');
    fim.setAttribute('aria-invalid', 'true');
    fim.setAttribute('aria-describedby', erro.id);
    erro.textContent = mensagem;
    erro.hidden = false;
    fim.focus();
  }

  form.addEventListener('submit', function (evento) {
    if (periodoInvalido()) {
      evento.preventDefault();
      mostrarErro();
    }
  });

  [inicio, fim].forEach(function (campo) {
    campo.addEventListener('input', function () {
      if (fim.getAttribute('aria-invalid') === 'true' && !periodoInvalido()) {
        limparErro();
      }
    });
  });

  // Erro devolvido pelo servidor: o foco vai para o campo com problema
  if (!erro.hidden) {
    fim.focus();
  }
})();
