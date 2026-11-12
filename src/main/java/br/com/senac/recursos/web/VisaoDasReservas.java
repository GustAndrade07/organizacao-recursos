package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.response.ManutencaoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.service.ManutencaoService;
import br.com.senac.recursos.service.UsuarioService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Monta, a partir do que os services devolvem, as linhas que as telas de reserva mostram (contratos em
 * docs/frontend/README.md). Fica fora dos controllers porque as telas 7, 8, 9, 10 e 11 usam as mesmas peças: nome do
 * solicitante, recursos, data e horário prontos e o estado por extenso.
 */
@Component
public class VisaoDasReservas {

    /** Localização ainda não existe no esquema (fica para a V2, ver Recurso); a tela mostra este texto. */
    static final String SEM_LOCAL = "Não informado";

    private final FormatoDasTelas formato;
    private final UsuarioService usuarios;
    private final ManutencaoService manutencoes;

    public VisaoDasReservas(FormatoDasTelas formato, UsuarioService usuarios, ManutencaoService manutencoes) {
        this.formato = formato;
        this.usuarios = usuarios;
        this.manutencoes = manutencoes;
    }

    public FormatoDasTelas formato() {
        return formato;
    }

    /** Nomes dos solicitantes, buscados uma vez por id durante a montagem de uma página. */
    public Map<Long, String> nomes() {
        return new HashMap<>();
    }

    public String nomeDe(Long usuarioId, Map<Long, String> nomes) {
        return nomes.computeIfAbsent(usuarioId, id -> {
            try {
                UsuarioResponse usuario = usuarios.buscar(id);
                return usuario == null ? "Usuário " + id : usuario.nome();
            } catch (EntidadeNaoEncontradaException naoExiste) {
                return "Usuário " + id;
            }
        });
    }

    /** Linha das listas (telas 7, 9 e 10): {@code data} e {@code horario} já em texto. */
    public Map<String, Object> linha(ReservaResponse reserva, Map<Long, String> nomes) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", reserva.id());
        linha.put("recurso", FormatoDasTelas.recursos(reserva));
        linha.put("local", SEM_LOCAL);
        linha.put("solicitante", nomeDe(reserva.solicitanteId(), nomes));
        linha.put("data", formato.textoData(reserva.inicio()));
        linha.put("horario", formato.horario(reserva.inicio(), reserva.fim()));
        linha.put("estado", FormatoDasTelas.estado(reserva.estado()));
        linha.put("iniciada", formato.iniciada(reserva));
        linha.put("horarioPassou", formato.terminada(reserva));
        return linha;
    }

    /** "Sala 203, de Rafael Costa", como o aviso de confirmação das telas 9 e 10 mostra. */
    public String rotulo(ReservaResponse reserva, Map<Long, String> nomes) {
        return FormatoDasTelas.recursos(reserva) + ", de " + nomeDe(reserva.solicitanteId(), nomes);
    }

    /** Aviso de confirmação ({@code fragments/confirmacao}): {@code titulo}, {@code reserva} e {@code estado}. */
    public Map<String, String> confirmacao(String titulo, String reserva, EstadoReserva estado) {
        return Map.of("titulo", titulo, "reserva", reserva, "estado", FormatoDasTelas.estado(estado));
    }

    /** Próximas primeiro: as que ainda não terminaram por início crescente, depois as passadas, das mais recentes. */
    public Comparator<ReservaResponse> proximasPrimeiro() {
        Instant agora = formato.agora();
        Comparator<ReservaResponse> passadas = Comparator.comparing(r -> r.fim().isAfter(agora) ? 0 : 1);
        return passadas.thenComparing((a, b) -> a.fim().isAfter(agora)
                ? a.inicio().compareTo(b.inicio())
                : b.inicio().compareTo(a.inicio()));
    }

    /**
     * O período da manutenção que barrou a reserva (RN-04), para o aviso da tela 6 e da alteração na tela 8. O
     * service só diz quais recursos estão em manutenção; o período vem da lista de manutenções do primeiro deles.
     */
    public Optional<Map<String, LocalDateTime>> manutencao(List<Long> recursoIds, Instant inicio, Instant fim) {
        for (Long recursoId : recursoIds) {
            for (ManutencaoResponse manutencao : manutencoes.listar(recursoId)) {
                if (manutencao.inicio().isBefore(fim) && inicio.isBefore(manutencao.fim())) {
                    return Optional.of(Map.of("inicio", formato.dataHora(manutencao.inicio()),
                            "fim", formato.dataHora(manutencao.fim())));
                }
            }
        }
        return Optional.empty();
    }
}
