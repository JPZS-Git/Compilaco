#!/usr/bin/env sh

set -eu

cd "$(dirname "$0")"

GRAMMAR="src/main/javacc/parser/Compilaco.jj"
OUTPUT="src/main/java/parser"

if ! command -v javacc >/dev/null 2>&1; then
    echo "Erro: JavaCC não encontrado no PATH."
    exit 1
fi

echo "Iniciando geração do parser..."

javacc -OUTPUT_DIRECTORY="$OUTPUT" "$GRAMMAR"

echo "Geração concluída."