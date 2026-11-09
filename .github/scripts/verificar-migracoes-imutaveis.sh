#!/usr/bin/env bash
# Migração Flyway já existente na base não pode ser modificada, renomeada nem apagada (ADR-0006, AGENTS.md seção 5).
# Erro de migração se corrige com a PRÓXIMA versão: o Flyway guarda o checksum e recusa migração editada.
#
# Uso: verificar-migracoes-imutaveis.sh [base] [pasta]
#   base  referência com o estado "já mergeado" (padrão: origin/main)
#   pasta pasta das migrações (padrão: src/main/resources/db/migration)
# Compara o que há de commitado no HEAD desde o ancestral comum com a base (git diff base...HEAD);
# mudança não commitada não entra. Só arquivos V<n>__*.sql contam: .gitkeep e migrações repetíveis (R__) ficam de fora.
# Saída: 0 = nada existente foi alterado; 1 = alteração proibida; 2 = base não encontrada.
set -euo pipefail

base="${1:-origin/main}"
pasta="${2:-src/main/resources/db/migration}"

if ! git rev-parse --verify --quiet "${base}^{commit}" >/dev/null; then
  echo "ERRO: a base '${base}' não existe neste clone (checkout sem histórico? use fetch-depth: 0)." >&2
  exit 2
fi

# --no-renames: renomear aparece como apagar + criar, e o "apagar" é o que bloqueia.
# --diff-filter=MD: modificado ou apagado; arquivo novo (A) é sempre permitido.
alteradas="$(git diff --name-status --no-renames --diff-filter=MD "${base}...HEAD" -- ":(glob)${pasta}/V*__*.sql")"

if [ -n "${alteradas}" ]; then
  echo "ERRO: migração já existente em ${base} foi alterada (M = modificada, D = apagada ou renomeada):" >&2
  echo "${alteradas}" >&2
  echo "Migração mergeada não se edita: restaure o arquivo e crie a próxima versão (ADR-0006)." >&2
  exit 1
fi

echo "OK: nenhuma migração existente em ${base} foi alterada (${pasta})."
