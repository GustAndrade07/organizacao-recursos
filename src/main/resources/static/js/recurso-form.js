// Formulário de recurso: leva o foco ao resumo quando a página volta com erros do servidor.
// A troca de tipo não é automática (WCAG 3.2.2): quem escolhe o tipo usa o botão "Atualizar campos".
// Sem JavaScript o formulário funciona do mesmo jeito; o back-end valida.
(function () {
  var resumo = document.querySelector('[data-testid="recurso-form-erros"]');
  if (resumo) {
    resumo.focus();
  }
})();
