@echo off
setlocal

cd /d "%~dp0"

set "OUTPUT=out"
set "SOURCE=src\main\java\parser"

if exist "%OUTPUT%" rmdir /s /q "%OUTPUT%"

mkdir "%OUTPUT%"

echo Compilando projeto...

javac -encoding UTF-8 -d "%OUTPUT%" "%SOURCE%\*.java"

if errorlevel 1 (
    echo Erro durante a compilacao.
    exit /b 1
)

echo Compilacao concluida.