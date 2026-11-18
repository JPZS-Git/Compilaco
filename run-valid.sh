#!/usr/bin/env sh

set -eu

cd "$(dirname "$0")"

./build.sh

java -cp out parser.Main --debug < examples/codigo_valido.txt