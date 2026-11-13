# Status — 13/11/2026 — `docs/ajustes-pos-login-email`

**Quem:** Gustavo (com IA), na área dele (banco, ADRs e plano). Só documentação; nenhum código de produção e nenhuma migração nova. Base: `main` em `5e50d70` (merge do PR #67).

## O que mudou
- **`docs/banco/schema.sql` gerado de novo** com a V1 e a V2. Antes ele tinha só a V1 e dizia "ao criar a V2, gere de novo". O dump novo tem só dois itens a mais, os dois da V2: o índice `usuario_login_minusculo_uq` em `lower(login)` e o `CHECK usuario_login_e_email` (`NOT VALID`). O cabeçalho passa a dizer V1 + V2 e "ao criar a V3, gere de novo".
- **V2 → V3** para a migração futura de localização, patrimônio, competências e `movimentacao_material`. A V2 agora é `V2__login_e_email.sql` (PR #67, ADR-0010). Mudou em: `docs/plano-execucao.md` (caixa "Entidades" e linha D13 da seção 9, cada uma com uma nota de por que mudou) e no ADR-0009 (consequências). As menções a "V2" em `docs/decisao-qualidade.md`, `docs/relatorio-qualidade.md`, `docs/rtm.md` e `docs/status-requisitos.md` são da área do Leandro e **não** foram mexidas.
- **ADR-0010 aceito.** Ele dizia "proposto; vira aceito na revisão do PR" (regra 6). A revisão do PR #67 foi feita: aprovado pelo Gustavo em 13/11/2026, 21:22 UTC, e mergeado em `5e50d70`. A pendência "nome da migração" do ADR ganhou a nota de que foi ajustada.
- **Plano:**
  - D14: registra o OK do Gustavo, que é a aprovação do PR #67 com a V2 de login na rc1. **Falta registrar o OK do Pedro.**
  - Caixa "Login por e-mail": agora cita o merge do #67, mas **continua aberta**. O texto dela diz que fecha com o OK do Pedro e do Gustavo, e o do Pedro não está registrado.
  - Caixa "Páginas de política": "PR 1 aberto" passou a "PR 1 mergeado (PR #66, `bf2584a`)". Continua aberta, porque o texto final (PR 2) ainda espera as respostas do grupo.

## Verificado
- `schema.sql`: num contêiner `postgres:16` descartável (PostgreSQL 16.15, banco vazio `recursos`, apagado depois), a V1 e a V2 foram aplicadas com `psql -v ON_ERROR_STOP=1`, sem erro, e depois veio `pg_dump --schema-only --no-owner --no-privileges` (13/11/2026). As linhas `\restrict`/`\unrestrict` foram removidas, como na versão de 07/11. O diff contra o arquivo antigo é só o cabeçalho e os dois itens da V2.
- Aprovação e merge do PR #67: API pública do GitHub (`pulls/67/reviews` e `pulls/67`), lida em 13/11/2026.
- `.\mvnw.cmd -B verify` nesta branch: resultado na descrição do PR.

## Não verificado
- OK do Pedro na D14: não está escrito em nenhum lugar do repositório que eu tenha achado, então ficou como pendência.
- Spec do Notion "Specs do Banco de Dados": não conferi se ela cita "V2" para localização. **A atualizar em até 24 h** (regra 7), pelo Gustavo.
