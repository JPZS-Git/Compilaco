@echo off
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\main\java\parser\*.java
if errorlevel 1 exit /b 1
echo.
echo Build concluido. Classe principal: parser.Compilaco
