// Nova reserva: bloqueia término igual ou anterior ao início antes do envio (RN-01), evita envio duplicado
// e leva o foco ao aviso quando a página volta com erro do servidor.
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

  function itemDoTermino() {
    return lista.querySelector('a[href="#' + termino.id + '"]');
  }

  // O resumo reflete os itens que sobraram: some só quando não resta nenhum
  function atualizarResumo() {
    var total = lista.children.length;
    if (total === 0) {
      titulo.textContent = '';
      resumo.hidden = true;
      return;
    }
    titulo.textContent = 'Corrija ' + total + (total === 1 ? ' campo' : ' campos') + ' para continuar';
    resumo.hidden = false;
  }

  function limparTermino() {
    termino.classList.remove('campo__entrada--erro');
    termino.removeAttribute('aria-invalid');
    termino.removeAttribute('aria-describedby');
    erro.textContent = '';
    erro.hidden = true;
    var link = itemDoTermino();
    if (link) {
      lista.removeChild(link.parentNode);
    }
    atualizarResumo();
  }

  function mostrarErroDoTermino() {
    termino.classList.add('campo__entrada--erro');
    termino.setAttribute('aria-invalid', 'true');
    termino.setAttribute('aria-describedby', erro.id);
    erro.textContent = mensagem;
    erro.hidden = false;
    if (!itemDoTermino()) {
      var item = document.createElement('li');
      var link = document.createElement('a');
      link.href = '#' + termino.id;
      link.textContent = 'Hora de término: ' + mensagem.charAt(0).toLowerCase() + mensagem.slice(1);
      item.appendChild(link);
      lista.appendChild(item);
    }
    atualizarResumo();
    resumo.focus();
  }

  form.addEventListener('submit', function (evento) {
    if (inicio.value && termino.value && termino.value <= inicio.value) {
      evento.preventDefault();
      mostrarErroDoTermino();
      return;
    }
    botao.disabled = true;
    botao.textContent = botao.getAttribute('data-enviando');
  });

  [inicio, termino].forEach(function (campo) {
    campo.addEventListener('input', function () {
      if (termino.getAttribute('aria-invalid') === 'true') {
        limparTermino();
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

  // Erro devolvido pelo servidor (resumo, conflito ou manutenção): o foco vai para o aviso visível
  var aviso = document.querySelector('main [role="alert"]:not([hidden])');
  if (aviso) {
    aviso.focus();
  }
})();
