@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Dang bien dich va chay TraCuuKhoClient...
javac TraCuuKhoClient.java
java TraCuuKhoClient
pause