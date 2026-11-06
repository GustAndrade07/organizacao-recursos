// Nova reserva: bloqueia término igual ou anterior ao início antes do envio (RN-01) e evita envio duplicado.
// Sem JavaScript o formulário envia normalmente e o back-end valida (a tela mostra o que ele devolver).
(function () {
  var form = document.querySelector('[data-testid="reserva-form"]');
  if (!form) {
    return;
  }
  var inicio = form.querySelector('[data-testid="reserva-form-inicio"]');
  var termino = form.querySelector('[data-testid="reserva-form-termino"]');
  var botao = form.querySelector('[data-testid="reserva-form-confirmar"]');
  var erro = document.getElementById('reserva-form-erro-termino');
  var resumo = document.querySelector('[data-testid="reserva-form-erro-resumo"]');
  var titulo = resumo.querySelector('[data-resumo-titulo]');
  var lista = resumo.querySelector('[data-resumo-lista]');
  var textoOriginal = botao.textContent;
  var mensagem = 'O término precisa ser depois do início';

  function limpar() {
    termino.classList.remove('campo__entrada--erro');
    termino.removeAttribute('aria-invalid');
    termino.removeAttribute('aria-describedby');
    erro.textContent = '';
    erro.hidden = true;
    titulo.textContent = '';
    lista.textContent = '';
    resumo.hidden = true;
  }

  function mostrar() {
    termino.classList.add('campo__entrada--erro');
    termino.setAttribute('aria-invalid', 'true');
    termino.setAttribute('aria-describedby', erro.id);
    erro.textContent = mensagem;
    erro.hidden = false;
    titulo.textContent = 'Corrija 1 campo para continuar';
    lista.textContent = '';
    var item = document.createElement('li');
    var link = document.createElement('a');
    link.href = '#' + termino.id;
    link.textContent = 'Hora de término: ' + mensagem;
    item.appendChild(link);
    lista.appendChild(item);
    resumo.hidden = false;
    resumo.focus();
  }

  form.addEventListener('submit', function (evento) {
    if (inicio.value && termino.value && termino.value <= inicio.value) {
      evento.preventDefault();
      mostrar();
      return;
    }
    limpar();
    botao.disabled = true;
    botao.textContent = botao.getAttribute('data-enviando');
  });

  [inicio, termino].forEach(function (campo) {
    campo.addEventListener('input', function () {
      if (termino.getAttribute('aria-invalid') === 'true') {
        limpar();
      }
    });
  });

  // Voltar pelo histórico do navegador não pode deixar o botão travado
  window.addEventListener('pageshow', function (evento) {
    if (evento.persisted) {
      botao.disabled = false;
      botao.textContent = textoOriginal;
    }
  });
})();
