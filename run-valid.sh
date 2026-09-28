#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
./build.sh
java -cp out parser.Compilaco --debug < examples/codigo_valido.txt
