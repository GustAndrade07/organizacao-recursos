package br.com.senac.recursos.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos de modelo que todas as telas (inclusive {@code /error}) recebem. Hoje só a regra de caminho do site:
 * o Thymeleaf não deixa chamar método estático dentro de fragmento, então os templates usam
 * {@code ${caminhoDoSite.valido(x)}}.
 */
@ControllerAdvice
class AtributosDasTelas {

    @ModelAttribute("caminhoDoSite")
    CaminhoDoSite caminhoDoSite() {
        return new CaminhoDoSite();
    }
}
