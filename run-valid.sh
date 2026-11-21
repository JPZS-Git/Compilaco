#!/usr/bin/env sh

set -eu

cd "$(dirname "$0")"

./build.sh

java -cp out parser.Main examples/codigo_valido.txt --debug