#!/usr/bin/env sh

set -eu

cd "$(dirname "$0")"

OUTPUT="out"
SOURCE="src/main/java/parser"

rm -rf "$OUTPUT"
mkdir -p "$OUTPUT"

echo "Compilando projeto..."

javac -encoding UTF-8 -d "$OUTPUT" "$SOURCE"/*.java src/main/java/recovery/*.java src/main/java/gui/*.java src/main/java/gerador/*.java

echo "Compilacao concluida."