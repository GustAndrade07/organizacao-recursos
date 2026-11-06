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
  var tempoLimite = null;

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

  // Volta ao estado normal e leva o foco ao botão (o navegador o tirou dele ao desabilitar)
  function restaurar() {
    window.clearTimeout(tempoLimite);
    botao.textContent = textoOriginal;
    botao.classList.remove('botao--carregando');
    botao.removeAttribute('aria-busy');
    botao.removeAttribute('aria-disabled');
    botao.disabled = false;
    botao.focus();
  }

  // O botão não tem name/value: desabilitá-lo aqui não tira nada do que o formulário envia
  form.addEventListener('submit', function () {
    mostrarEnvio();
    // Esc/Parar, rede caída ou resposta sem navegação (204, download) não podem deixar o botão preso
    window.clearTimeout(tempoLimite);
    tempoLimite = window.setTimeout(restaurar, 30000);
  });

  // Voltar pelo histórico do navegador (ou qualquer volta a esta página) não pode deixar o botão travado
  window.addEventListener('pageshow', function () {
    if (botao.disabled) {
      restaurar();
    }
  });
})();
