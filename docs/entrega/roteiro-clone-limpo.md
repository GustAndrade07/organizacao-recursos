# Roteiro do clone limpo

> Para quem vai **validar o README em um clone limpo**: **Leandro ou Pedro**. O Gustavo escreveu o README (histórico do arquivo), então **não** faz este roteiro; a IA também não faz o clone nem afirma que foi feito.
> Aula 14: quem faz o clone não pode ter escrito o README nem reaproveitar nada da própria máquina. **Anote o que faltou, liste os bloqueadores em ordem e só então corrija** (a correção vira PR à parte, do Gustavo).
> Situação em 07/11/2026: **o clone limpo ainda não foi feito.** Este arquivo é só o roteiro; os resultados vão na tabela da seção 4 e no [checklist](checklist.md).

## 1. Regras (valem do começo ao fim)

1. **Siga só o `README.md`.** Não use `AGENTS.md`, o plano, o Notion nem a ajuda de quem escreveu o README. Se um passo não estiver no README, ele **não existe**: anote como "faltou no README" e pare ali.
2. **Pasta nova e vazia**, fora de qualquer outro clone do projeto. Não copie `.m2`, `target`, `.env` nem nada da pasta de trabalho.
3. **Não corrija nada.** Nem o README, nem o código, nem a máquina além do que o README mandar instalar. Só anote.
4. **Anote o erro inteiro** (texto copiado, não resumo) e a hora. Se travar, pare, anote e avise o grupo.
5. Não coloque senha nem token em nenhum lugar do relatório. O `docker-compose.yml` usa uma senha de desenvolvimento local; não precisa copiá-la.

## 2. Antes de começar

Escolha **o commit que vai valer**: a `main` depois dos merges dos PRs pendentes, e **antes** da tag `v1.0.0-rc1`. O Gustavo informa o hash. Registre aqui:

| Dado | Valor |
|---|---|
| Quem fez (nome completo) | |
| Data e hora de início | |
| Sistema operacional | |
| Versão do Java (`java -version`) | |
| Versão do Docker (`docker version`) | |
| Commit testado (`git rev-parse --short HEAD` dentro do clone) | |

## 3. Passos (nesta ordem)

O comando exato é o que o README manda; abaixo está só a ordem e o que se espera. No Windows (PowerShell) o README manda usar `.\mvnw.cmd` no lugar de `./mvnw`.

| # | O que fazer (segundo o README) | O que se espera |
|---|---|---|
| 1 | Conferir os pré-requisitos do README (JDK 21, Docker, Git) | `java -version` mostra 21; Docker responde |
| 2 | `git clone` do repositório em pasta vazia e entrar na pasta | Clone termina sem erro; anotar o hash do commit |
| 3 | `docker compose up -d` | Banco sobe em `localhost:5432` (`docker compose ps` mostra o serviço de pé) |
| 4 | Subir a aplicação com o Maven Wrapper (`spring-boot:run`) | Aplicação sobe em `http://localhost:8080`, sem erro no log; anotar quanto tempo levou |
| 5 | Abrir `http://localhost:8080/actuator/health` | Responde `{"status":"UP"}` |
| 6 | Parar a aplicação (Ctrl+C) e rodar `verify` (Docker rodando) | `BUILD SUCCESS`; anotar a linha `Tests run:` e quanto tempo levou |
| 7 | Abrir o relatório de cobertura indicado no README | O arquivo abre; anotar os percentuais de linhas e branches mostrados |

## 4. Resultado

Preencha uma linha por passo, na hora, sem reescrever depois.

| # | Hora | Funcionou? (sim / não / README não diz) | O que apareceu (copie o texto do erro ou da linha final) |
|---|---|---|---|
| 1 | | | |
| 2 | | | |
| 3 | | | |
| 4 | | | |
| 5 | | | |
| 6 | | | |
| 7 | | | |

**O que faltou ou confundiu no README** (uma linha por ponto, com o passo):

-

**Bloqueadores, em ordem** (os quatro primeiros zeram a nota: aplicação que não executa, pipeline vermelho, falha de autorização, reserva dupla):

1.

## 5. Depois do clone

1. Entregue este arquivo preenchido ao Gustavo, em PR seu (sua autoria) ou colado no grupo.
2. Com base nos bloqueadores, o Gustavo corrige o README e o resto **em PR separado**, citando este roteiro.
3. Só depois disso o [checklist](checklist.md) tem as colunas "Existe" e "Atualizado" preenchidas **por quem fez o clone**; até lá, o que está no checklist é só o que foi conferido sem clone (seção "Conferido sem clone").
