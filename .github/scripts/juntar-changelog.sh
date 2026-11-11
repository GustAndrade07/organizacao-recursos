#!/usr/bin/env bash
# Junta as entradas de changelog.d/*.md (menos o README) no CHANGELOG.md, logo abaixo de "## [Não lançado]",
# e apaga os arquivos juntados. Uso antes da tag; ver changelog.d/README.md.
# Uso: juntar-changelog.sh [changelog] [pasta]   (padrão: CHANGELOG.md changelog.d)
# Saída: 0 = juntou (ou não havia nada); 1 = CHANGELOG.md sem a seção "## [Não lançado]" ou junção incompleta.
set -euo pipefail

changelog="${1:-CHANGELOG.md}"
pasta="${2:-changelog.d}"
secao='## [Não lançado]'

shopt -s nullglob
arquivos=()
for f in "${pasta}"/*.md; do
  [ "$(basename "$f")" = "README.md" ] || arquivos+=("$f")
done

if [ "${#arquivos[@]}" -eq 0 ]; then
  echo "Nada para juntar em ${pasta}."
  exit 0
fi

if ! grep -qF "${secao}" "${changelog}"; then
  echo "ERRO: ${changelog} não tem a seção '${secao}'." >&2
  exit 1
fi

entradas="$(mktemp)"
novo="$(mktemp)"
trap 'rm -f "${entradas}" "${novo}"' EXIT

# Linhas sem CR e sem linhas em branco. Sai tudo em LF, como o Git guarda o arquivo
# (no Windows, o core.autocrlf volta a pôr CRLF na cópia de trabalho).
for f in "${arquivos[@]}"; do
  tr -d '\r' < "$f" | grep -v '^[[:space:]]*$' || true
done > "${entradas}"

tr -d '\r' < "${changelog}" | awk -v ent="${entradas}" -v secao="${secao}" '
  { print }
  !feito && index($0, secao) == 1 { while ((getline l < ent) > 0) print l; feito = 1 }
' > "${novo}"

# Só apaga os arquivos se o CHANGELOG cresceu exatamente o número de linhas juntadas.
antes=$(tr -d '\r' < "${changelog}" | wc -l)
depois=$(wc -l < "${novo}")
esperado=$(wc -l < "${entradas}")
if [ "$((depois - antes))" -ne "${esperado}" ]; then
  echo "ERRO: esperava ${esperado} linha(s) nova(s) em ${changelog}, vieram $((depois - antes)). Nada foi alterado." >&2
  exit 1
fi

cp "${novo}" "${changelog}"
rm -f "${arquivos[@]}"
echo "Juntadas ${#arquivos[@]} entrada(s) (${esperado} linha(s)) de ${pasta} em ${changelog}."
