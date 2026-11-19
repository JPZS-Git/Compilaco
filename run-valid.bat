@echo off
setlocal

cd /d "%~dp0"

call build.bat

if errorlevel 1 exit /b 1

java -cp out parser.Main examples\codigo_valido.txt --debug