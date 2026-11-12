package br.com.senac.recursos.web.sessao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limite de tentativas do login das telas (ressalva 1 do PR #48, ADR-0009): {@code limite} falhas do mesmo login
 * dentro de {@code bloqueio} bloqueiam esse login por {@code bloqueio}, contado do instante da última falha. Login
 * certo zera o contador. Tentativa durante o bloqueio não estende o bloqueio. O login não precisa existir: o contador
 * só olha o texto digitado (sem espaços nas pontas e em minúsculas), para não diferenciar logins que existem.
 *
 * <p>O tempo vem do {@link Clock} injetado (AGENTS.md, seção 5). O contador vive na memória desta instância
 * ({@link ConcurrentHashMap}): com mais de uma instância, cada uma conta as suas tentativas (limitação escrita no
 * ADR-0009). Registros vencidos são apagados quando o mapa passa de {@link #LIMPAR_ACIMA_DE} logins, para a memória
 * não crescer sem fim com logins inventados.
 */
public class TentativasDeLogin {

    static final int LIMPAR_ACIMA_DE = 10_000;

    private final Clock relogio;
    private final int limite;
    private final Duration bloqueio;
    private final ConcurrentMap<String, Registro> registros = new ConcurrentHashMap<>();

    public TentativasDeLogin(Clock relogio, int limite, Duration bloqueio) {
        if (limite < 1) {
            throw new IllegalArgumentException("recursos.telas.login.tentativas precisa ser pelo menos 1");
        }
        if (bloqueio == null || bloqueio.isNegative() || bloqueio.isZero()) {
            throw new IllegalArgumentException("recursos.telas.login.bloqueio precisa ser uma duração positiva");
        }
        this.relogio = relogio;
        this.limite = limite;
        this.bloqueio = bloqueio;
    }

    /** O login está bloqueado agora? */
    public boolean bloqueado(String login) {
        Registro registro = registros.get(chave(login));
        return registro != null && registro.bloqueadoAte(relogio.instant());
    }

    /** Uma falha de senha (ou de login inexistente). Durante o bloqueio, não muda nada. */
    public void falhou(String login) {
        Instant agora = relogio.instant();
        registros.compute(chave(login), (chave, registro) -> proximo(registro, agora));
        if (registros.size() > LIMPAR_ACIMA_DE) {
            registros.values().removeIf(registro -> registro.vencido(agora, bloqueio));
        }
    }

    /** Login certo: o contador do login volta a zero. */
    public void entrou(String login) {
        registros.remove(chave(login));
    }

    int registrosNaMemoria() {
        return registros.size();
    }

    private Registro proximo(Registro registro, Instant agora) {
        if (registro != null && registro.bloqueadoAte(agora)) {
            return registro;
        }
        Instant inicioDaJanela = agora.minus(bloqueio);
        List<Instant> falhas = new ArrayList<>();
        if (registro != null) {
            registro.falhas().stream().filter(falha -> falha.isAfter(inicioDaJanela)).forEach(falhas::add);
        }
        falhas.add(agora);
        if (falhas.size() >= limite) {
            return new Registro(List.of(), agora.plus(bloqueio));
        }
        return new Registro(List.copyOf(falhas), null);
    }

    private static String chave(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    /** Falhas dentro da janela, ou o fim do bloqueio. */
    private record Registro(List<Instant> falhas, Instant fimDoBloqueio) {

        boolean bloqueadoAte(Instant agora) {
            return fimDoBloqueio != null && agora.isBefore(fimDoBloqueio);
        }

        boolean vencido(Instant agora, Duration janela) {
            if (fimDoBloqueio != null) {
                return !agora.isBefore(fimDoBloqueio);
            }
            return falhas.stream().noneMatch(falha -> falha.isAfter(agora.minus(janela)));
        }
    }
}
