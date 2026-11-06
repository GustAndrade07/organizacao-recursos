// Formulário de recurso: trocar o tipo recarrega a página com os campos do tipo escolhido, levando o que já
// foi digitado. Sem JavaScript o mesmo vale pelo botão "Atualizar campos", que este script tira da tela.
// Também leva o foco ao resumo quando a página volta com erros do servidor.
(function () {
  var form = document.querySelector('[data-testid="recurso-form"]');
  if (!form) {
    return;
  }
  var tipo = form.querySelector('select[name="tipo"]');
  var trocar = form.querySelector('[data-trocar-tipo]');
  var padrao = form.querySelector('[data-padrao]');

  if (tipo && trocar) {
    var destino = trocar.getAttribute('formaction');
    var metodo = form.getAttribute('method');
    var acao = form.getAttribute('action');
    trocar.parentNode.removeChild(trocar);
    if (padrao) {
      padrao.parentNode.removeChild(padrao);
    }
    tipo.addEventListener('change', function () {
      form.setAttribute('method', 'get');
      form.setAttribute('action', destino);
      form.submit();
      // Voltar pelo histórico do navegador não pode deixar o formulário de salvar enviando por GET
      form.setAttribute('method', metodo);
      form.setAttribute('action', acao);
    });
  }

  var resumo = document.querySelector('[data-testid="recurso-form-erros"]');
  if (resumo) {
    resumo.focus();
  }
})();
