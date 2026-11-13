-- V2: o login do usuário passa a ser um e-mail (DEF-010, ADR-0010). A V1 não é editada (regra do AGENTS.md).
--
-- O que muda no esquema:
--   1. índice único em lower(login): o e-mail é único sem diferenciar maiúsculas de minúsculas, mesmo que algum caminho
--      um dia grave sem normalizar. A UNIQUE da V1 (case-sensitive) continua e não atrapalha.
--   2. CHECK usuario_login_e_email: o banco recusa, em gravações NOVAS, login que não seja e-mail em minúsculas, com
--      até 254 caracteres e parte local de até 64. É a mesma regra de br.com.senac.recursos.validation.Emails; o teste
--      EsquemaBancoTest confere que as duas concordam.
--
-- O que esta migração NÃO faz de propósito:
--   * não converte nem apaga usuário antigo e não inventa e-mail: o CHECK é NOT VALID, isto é, vale para INSERT e
--     UPDATE novos e não olha as linhas que já existem. Banco local antigo (admin, ana.souza...) migra sem erro; quem
--     tinha login sem "@" não consegue mais entrar (a tela e a API só aceitam e-mail). Recrie o banco local
--     (docker compose down -v) ou o Administrador cadastra de novo. Conversão automática ficou como pergunta em
--     aberto (docs/status/2026-11-13-feat-login-email.md).
--   * a criação do índice falha se o banco já tiver dois logins iguais exceto pela caixa (ex.: Ana e ana). Isso só
--     existiria em dado antigo escrito à mão; a UNIQUE da V1 já barrava repetição exata.

CREATE UNIQUE INDEX usuario_login_minusculo_uq ON usuario (lower(login));

ALTER TABLE usuario
    ADD CONSTRAINT usuario_login_e_email CHECK (
        login ~ '^[a-z0-9_%+-]+(\.[a-z0-9_%+-]+)*@([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$'
        AND length(login) <= 254
        AND position('@' in login) <= 65
    ) NOT VALID;
