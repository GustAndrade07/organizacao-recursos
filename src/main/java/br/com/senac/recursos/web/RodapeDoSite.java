package br.com.senac.recursos.web;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * O que o rodapé de página mostra e que muda com o tempo: o ano da linha de direitos. Vem do {@code Clock} injetado
 * (por {@link FormatoDasTelas}), no fuso das telas, nunca escrito no HTML (AGENTS.md, seção 5: nada de relógio solto).
 * O layout chama {@code ${@rodapeDoSite.ano()}} por expressão de bean, que funciona igual no login, nas telas e nas
 * páginas de erro, sem depender de um controller ter posto o valor no modelo.
 */
@Component
public class RodapeDoSite {

    private final FormatoDasTelas formato;

    public RodapeDoSite(FormatoDasTelas formato) {
        this.formato = formato;
    }

    /** Ano de hoje no fuso das telas: às 23h30 de 31/12 em São Paulo ainda é o ano que acaba, mesmo já sendo o próximo em UTC. */
    public int ano() {
        return LocalDate.ofInstant(formato.agora(), formato.fuso()).getYear();
    }
}
