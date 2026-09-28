#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/parser/*.java
echo
echo "Build concluído. Classe principal: parser.Compilaco"
