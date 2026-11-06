// Login: leva o foco ao aviso (credencial inválida ou sessão expirada) para teclado e leitor de tela e,
// no envio, deixa o botão em "Entrando…". Nunca cancela o envio: sem JavaScript o formulário funciona igual.
(function () {
  var aviso = document.querySelector('[data-testid="login-erro"], [data-testid="login-sessao-expirada"]');
  if (aviso) {
    aviso.focus();
  }

  var form = document.querySelector('[data-testid="login-formulario"]');
  var botao = form && form.querySelector('[data-testid="login-entrar"]');
  if (!botao) {
    return;
  }
  var textoOriginal = botao.textContent;
  var enviando = false;

  function mostrarEnvio() {
    var anel = document.createElement('span');
    anel.className = 'carregando';
    anel.setAttribute('aria-hidden', 'true');
    botao.textContent = '';
    botao.appendChild(anel);
    botao.appendChild(document.createTextNode(botao.getAttribute('data-enviando')));
    botao.classList.add('botao--carregando');
    botao.setAttribute('aria-busy', 'true');
    botao.setAttribute('aria-disabled', 'true');
    botao.disabled = true;
  }

  function restaurar() {
    enviando = false;
    botao.textContent = textoOriginal;
    botao.classList.remove('botao--carregando');
    botao.removeAttribute('aria-busy');
    botao.removeAttribute('aria-disabled');
    botao.disabled = false;
  }

  // O botão não tem name/value: desabilitá-lo aqui não tira nada do que o formulário envia
  form.addEventListener('submit', function () {
    enviando = true;
    mostrarEnvio();
  });

  // Voltar pelo histórico do navegador não pode deixar o botão travado
  window.addEventListener('pageshow', function (evento) {
    if (evento.persisted && enviando) {
      restaurar();
    }
  });
})();
