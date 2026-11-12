# Status em 12/11/2026 (quinta, branch `docs/riscos-spring-security-1211`)

Só documentação, a partir da `main` (`2610d6a`).

O que muda:
- **`docs/seguranca/riscos-dependencias.md`, seção 8 nova:** os 3 riscos do Spring Security 6.5.11 que entraram com o back-end (PR #44), avaliados e **aceitos com justificativa** (responsável Gustavo):
  - CVE-2026-41707 (`oauth2-jose`, DPoP): não atinge, porque o token não tem `cnf.jkt`.
  - CVE-2026-47842 (`crypto`, AES com IV zerado): não atinge, porque o projeto não usa `Encryptors`.
  - CVE-2026-59276 (`core`, comparação com tempo variável): provavelmente não atinge; **não verificado**.
- Não há correção na linha 6.5 (a última é a 6.5.11); a correção existe só na 7.x, com o Spring Boot 4.
- Os 3 foram marcados como **Accepted** no painel do SonarCloud em 12/11/2026, com o motivo de cada um, e conferidos no filtro "Accepted" (6 riscos aceitos no total).

**Não alterado, para não dar conflito com os PRs #53 e #54 abertos:**
- A caixa da linha 65 de `riscos-dependencias.md` ("reler os riscos restantes") e a Etapa 4 do plano. Quem fizer o último merge entre este PR e o #53 marca as duas.

**Pendente:**
- A entrada dos 3 como risco residual em `docs/decisao-qualidade.md` (Leandro).

**Não verificado:** a suíte não rodou (mudança só de texto). No CVE-2026-59276, não foi conferido quais componentes do Spring Security fazem a comparação vulnerável.
