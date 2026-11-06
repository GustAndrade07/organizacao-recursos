// Bloqueio por manutenção: barra fim igual ou anterior ao início antes do envio (RN-04) e leva o foco ao que
// o servidor devolveu (aviso das reservas afetadas ou primeiro campo com erro).
// Sem JavaScript o formulário envia normalmente e o back-end valida (a tela mostra o que ele devolver).
(function () {
  var form = document.querySelector('[data-testid="manutencao-form"]');
  if (!form) {
    return;
  }
  var inicio = document.getElementById('manutencao-inicio');
  var fim = document.getElementById('manutencao-fim');
  var erro = document.getElementById('manutencao-fim-erro');
  var mensagem = 'O fim do bloqueio precisa ser depois do início.';
  // Só apaga o erro que o próprio script mostrou: o que veio do servidor fica até o envio seguinte
  var erroDoScript = false;

  function fimAntesDoInicio() {
    return Boolean(inicio.value && fim.value) && new Date(fim.value) <= new Date(inicio.value);
  }

  function limparErro() {
    fim.classList.remove('campo__entrada--erro');
    fim.removeAttribute('aria-invalid');
    fim.removeAttribute('aria-describedby');
    erro.textContent = '';
    erro.hidden = true;
    erroDoScript = false;
  }

  function mostrarErro() {
    fim.classList.add('campo__entrada--erro');
    fim.setAttribute('aria-invalid', 'true');
    fim.setAttribute('aria-describedby', erro.id);
    erro.textContent = mensagem;
    erro.hidden = false;
    erroDoScript = true;
    fim.focus();
  }

  // Vale para "Bloquear recurso" e para "Confirmar bloqueio" (botão fora do formulário, ligado por form=)
  form.addEventListener('submit', function (evento) {
    if (fimAntesDoInicio()) {
      evento.preventDefault();
      mostrarErro();
    }
  });

  [inicio, fim].forEach(function (campo) {
    campo.addEventListener('input', function () {
      if (erroDoScript && !fimAntesDoInicio()) {
        limparErro();
      }
    });
  });

  // Erro devolvido pelo servidor: o foco vai para o aviso das reservas afetadas ou para o primeiro campo inválido
  var alvo = document.querySelector('[data-testid="manutencao-afetadas"]') || form.querySelector('[aria-invalid="true"]');
  if (alvo) {
    alvo.focus();
  }
})();
