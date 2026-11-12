package br.com.senac.recursos.service;

import br.com.senac.recursos.dto.response.RelatorioUtilizacaoResponse;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.repository.RelatorioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatório de utilização (RF-12, RN-13). As três partes leem o mesmo instantâneo do banco (uma transação só de
 * leitura com isolamento REPEATABLE READ), para os números não discordarem entre si se uma reserva for gravada no meio.
 * As regras de contagem são as das consultas em {@code db/consultas/} e seguem pendentes de confirmação (D8 do plano).
 */
@Service
public class RelatorioService {

    private final RelatorioRepository relatorio;

    public RelatorioService(RelatorioRepository relatorio) {
        this.relatorio = relatorio;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RelatorioUtilizacaoResponse utilizacao(Periodo janela) {
        String faixa = janela.comoTstzrange();
        return new RelatorioUtilizacaoResponse(janela.inicio(), janela.fim(), relatorio.horasPorRecurso(faixa),
                relatorio.cargaPorProfessor(faixa), relatorio.conflitosEvitados(faixa));
    }
}
