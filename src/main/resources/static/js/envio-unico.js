// Envio único: em todo formulário marcado com data-envio-unico, o primeiro envio desabilita os botões de envio
// e marca o formulário com aria-busy="true", para o segundo clique não gerar um segundo POST.
// - Nunca cancela o envio: os botões só são desabilitados no ciclo seguinte ao evento, depois que o navegador
//   já coletou os campos (inclusive name/value do botão clicado, como confirmar=true). Se outro script cancelou
//   o envio (validação), nada é desabilitado.
// - Não mexe no texto do botão (login.js e reserva-form.js têm o seu próprio comportamento).
// - Restaura tudo em qualquer pageshow (voltar pelo histórico, cache de páginas) e, por segurança, depois de 30 s
//   (resposta que não navega, como download ou 204).
// - Sem o atributo, ou sem JavaScript, nada muda. O formulário precisa ter um botão de envio: com ele desabilitado
//   o Enter nos campos também não envia de novo.
(function () {
  var LIMITE_MS = 30000;
  var travados = [];

  function botoesDe(form) {
    var botoes = [];
    var i;
    for (i = 0; i < form.elements.length; i++) {
      if (form.elements[i].type === 'submit') {
        botoes.push(form.elements[i]);
      }
    }
    var imagens = form.querySelectorAll('input[type="image"]');
    for (i = 0; i < imagens.length; i++) {
      botoes.push(imagens[i]);
    }
    return botoes;
  }

  function restaurar(registro) {
    window.clearTimeout(registro.temporizador);
    registro.botoes.forEach(function (botao) {
      botao.disabled = false;
    });
    registro.form.removeAttribute('aria-busy');
    travados.splice(travados.indexOf(registro), 1);
  }

  function travar(form) {
    var registro = { form: form, botoes: [], temporizador: 0 };
    botoesDe(form).forEach(function (botao) {
      if (!botao.disabled) {
        botao.disabled = true;
        registro.botoes.push(botao);
      }
    });
    form.setAttribute('aria-busy', 'true');
    registro.temporizador = window.setTimeout(function () {
      restaurar(registro);
    }, LIMITE_MS);
    travados.push(registro);
  }

  document.addEventListener('submit', function (evento) {
    var form = evento.target;
    if (!form || !form.hasAttribute || !form.hasAttribute('data-envio-unico')) {
      return;
    }
    // Só no ciclo seguinte: o navegador monta os dados do envio logo depois do evento, ainda neste ciclo
    window.setTimeout(function () {
      var jaTravado = travados.some(function (registro) {
        return registro.form === form;
      });
      if (!evento.defaultPrevented && !jaTravado) {
        travar(form);
      }
    }, 0);
  });

  window.addEventListener('pageshow', function () {
    travados.slice().forEach(restaurar);
  });
})();
