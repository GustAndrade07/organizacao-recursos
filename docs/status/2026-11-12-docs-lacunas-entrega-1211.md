# Status em 12/11/2026 (quinta, branch `docs/lacunas-entrega-1211`)

Só documentação, a partir da `main` (`2610d6a`, CI verde no run 211).

O que muda:
- **Plano, seção 4.1 nova:** conferência da `main` contra os materiais das Aulas 01 a 17.
  - CI e SonarCloud atendem as Aulas 06 e 07: ruleset `proteger-main` lido em Settings e Quality Gate OK no `2610d6a`.
  - Tabela com 12 lacunas, cada uma com aula, dono (seção 6) e se bloqueia a tag.
- **Plano, Etapa 4 (SonarCloud):** CVE-2026-92877 marcado como Accepted no painel em 12/11 e o gate do `2610d6a`. A caixa continua aberta até a análise do commit da tag.
- **Plano, Etapa 5 (concorrência):** o `ConcorrenciaReservaApiTest` roda no CI (run 43276426460). A caixa continua aberta: falta o vermelho sem a proteção no nível da API.
- **`docs/seguranca/riscos-dependencias.md`:** marcada a caixa do Accepted do `jaxb-core`.
- **Specs no Notion, atualizadas no mesmo dia:**
  - Banco de Dados: estado da `main`, sem migração nova, teste de API na CI e lacunas que tocam o banco.
  - Spec principal: riscos de CI e de reserva dupla em 12/11, mais uma linha nova com as lacunas da seção 4.1.

**Não alterado:**
- Os 3 riscos do Spring Security 6.5.11: avaliados em 12/11, mas o Gustavo decidiu não registrar ainda.
- As lacunas de outras áreas: ficam na tabela da seção 4.1 para cada dono.

**Não verificado:** a suíte não rodou, porque a mudança é só de texto. Os números do JaCoCo do run 211 foram lidos só no SonarCloud.
