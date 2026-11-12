# Status em 12/11/2026 (quinta, branch `docs/d13-aceita`)

Só documentação, a partir da `main` (`19f5e31`).

O que muda:
- **D13 decidida (Gustavo, OK do Pedro): risco aceito e registrado.** A recusa por manutenção (RN-04) continua sendo "consulta e depois grava" em `ReservaService.exigirForaDeManutencao`. Uma manutenção criada nesse intervalo deixa passar uma reserva no período bloqueado.
  - Motivos do aceite: a janela é de milissegundos, só o Administrador cria manutenção e o caso não está no PRD.
  - Responsável: Gustavo.
  - Evolução prevista: uma V2 com trigger usando a trava por recurso da D7. O lado "manutenção sobre reservas existentes" depende da D11.
  - Registrada na seção 9 do plano e no ADR-0008, que deixa de listar a D13 como pendente.
- **OK do Pedro, em 12/11/2026, nas decisões do Gustavo que dependiam dele:**
  - D3, D6 e D7 (seção 9 do plano).
  - ADR-0005, ADR-0006 e ADR-0007 passam de **proposto** para **aceito**.
  - Esse estado foi refletido em `docs/arquitetura.md` (sequência do D3 e caixas da seção de acompanhamento) e na tabela de ADRs do ciclo ATAM 01.
- A spec do Banco de Dados no Notion foi atualizada no mesmo dia (D6, D7, D13 e ADR-0006/0007).

**Não alterado:**
- D1 e D5: continuam a confirmar com o Leandro.
- `docs/frontend/README.md`: área do Leandro; os contratos dele "a confirmar com o Pedro" não são decisões do Gustavo.
- O risco da D13 entra como risco residual em `docs/decisao-qualidade.md`, que é do Leandro e ainda não existe na `main`.

**Não verificado:** a suíte não rodou, porque a mudança é só de texto. Nenhuma caixa do plano foi marcada.
