#!/usr/bin/env bash
# Auto-teste de verificar-migracoes-imutaveis.sh: monta um repositório descartável, cria cada situação e confere
# o código de saída esperado. Uma checagem que nunca falha não prova nada; por isso os casos "deve falhar".
# Uso: testar-verificacao-migracoes.sh   (roda em qualquer pasta; não toca no repositório do projeto)
set -uo pipefail

verificador="$(cd "$(dirname "$0")" && pwd)/verificar-migracoes-imutaveis.sh"
pasta="src/main/resources/db/migration"
falhas=0
total=0

git_t() { git -c user.name=teste -c user.email=teste@exemplo.invalido -c commit.gpgsign=false -c core.autocrlf=false "$@"; }

# caso <nome> <saida esperada> <função que prepara a branch de trabalho> [base]
caso() {
  local nome="$1" esperado="$2" preparar="$3" base="${4:-main}"
  local dir
  dir="$(mktemp -d)"
  (
    cd "$dir" || exit 99
    git init -q -b main . >/dev/null 2>&1 || git init -q . >/dev/null 2>&1
    git_t checkout -q -B main
    mkdir -p "$pasta"
    echo "-- gitkeep" > "$pasta/.gitkeep"
    echo "CREATE TABLE a (id int);" > "$pasta/V1__a.sql"
    git_t add -A && git_t commit -q -m "base"
    git_t checkout -q -b trabalho
    "$preparar"
    git_t add -A && git_t commit -q -m "trabalho" --allow-empty
    bash "$verificador" "$base" >/dev/null 2>&1
  )
  local saida=$?
  rm -rf "$dir"
  total=$((total + 1))
  if [ "$saida" -eq "$esperado" ]; then
    echo "ok     - ${nome} (saída ${saida})"
  else
    echo "FALHOU - ${nome}: esperado ${esperado}, veio ${saida}"
    falhas=$((falhas + 1))
  fi
}

adiciona_v2()      { echo "CREATE TABLE b (id int);" > "$pasta/V2__b.sql"; }
modifica_v1()      { echo "CREATE TABLE a (id int, nome text);" > "$pasta/V1__a.sql"; }
apaga_v1()         { rm "$pasta/V1__a.sql"; }
renomeia_v1()      { git_t mv "$pasta/V1__a.sql" "$pasta/V1__outro_nome.sql"; }
apaga_gitkeep()    { rm "$pasta/.gitkeep"; }
mexe_em_repetivel(){ echo "SELECT 1;" > "$pasta/R__visao.sql"; }
nada_em_migracao() { echo "x" > README-teste.md; }
modifica_e_adiciona() { modifica_v1; adiciona_v2; }

caso "adicionar V2 é permitido"                          0 adiciona_v2
caso "mexer fora da pasta de migrações é permitido"      0 nada_em_migracao
caso "apagar o .gitkeep é permitido"                     0 apaga_gitkeep
caso "criar migração repetível (R__) é permitido"        0 mexe_em_repetivel
caso "modificar a V1 existente é bloqueado"              1 modifica_v1
caso "apagar a V1 existente é bloqueado"                 1 apaga_v1
caso "renomear a V1 existente é bloqueado"               1 renomeia_v1
caso "modificar a V1 junto com adicionar V2 é bloqueado" 1 modifica_e_adiciona
caso "base inexistente falha com saída 2"                2 nada_em_migracao origem/inexistente

echo "---"
echo "${total} casos, ${falhas} falha(s)"
[ "${falhas}" -eq 0 ]
