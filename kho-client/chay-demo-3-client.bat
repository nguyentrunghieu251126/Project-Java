@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo Dang chay kich ban 3 Client tranh mua san pham...
javac KiemThuBaClient.java
java KiemThuBaClient
pause