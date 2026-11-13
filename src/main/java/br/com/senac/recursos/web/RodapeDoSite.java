package br.com.senac.recursos.web;

import org.springframework.stereotype.Component;

/** Esqueleto do TDD (commit {@code test(red)}): ainda não calcula o ano; o comportamento vem no {@code feat(green)}. */
@Component
public class RodapeDoSite {

    private final FormatoDasTelas formato;

    public RodapeDoSite(FormatoDasTelas formato) {
        this.formato = formato;
    }

    public int ano() {
        return 0;
    }
}
