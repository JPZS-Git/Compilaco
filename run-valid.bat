@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
type examples\codigo_valido.txt | java -cp out parser.Compilaco --debug
