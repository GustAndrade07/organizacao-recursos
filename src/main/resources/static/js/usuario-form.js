// Cadastro de usuário (tela 16): leva o foco ao resumo quando a página volta com erros do servidor.
// Sem JavaScript o formulário funciona do mesmo jeito; o back-end valida.
(function () {
  var resumo = document.querySelector('[data-testid="usuario-form-erros"]');
  if (resumo) {
    resumo.focus();
  }
})();
