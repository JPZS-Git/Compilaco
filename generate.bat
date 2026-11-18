@echo off
setlocal

cd /d "%~dp0"

set "GRAMMAR=src\main\javacc\parser\Compilaco.jj"
set "OUTPUT=src\main\java\parser"

where javacc >nul 2>nul

if errorlevel 1 (
    echo Erro: JavaCC nao encontrado no PATH.
    exit /b 1
)

echo Iniciando geracao do parser...

javacc -OUTPUT_DIRECTORY=%OUTPUT% %GRAMMAR%

if errorlevel 1 (
    echo Erro durante a geracao do parser.
    exit /b 1
)

echo Geracao concluida.