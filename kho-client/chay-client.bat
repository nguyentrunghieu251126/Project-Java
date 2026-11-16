@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo Loi: khong tim thay javac. Hay cai JDK va them thu muc bin cua JDK vao PATH.
    pause
    exit /b 1
)

echo Dang bien dich TraCuuKhoClient...
javac TraCuuKhoClient.java
if errorlevel 1 (
    echo Bien dich that bai. Kiem tra loi javac o phia tren.
    pause
    exit /b 1
)

echo Dang ket noi den Socket Server tai localhost:9090...
java TraCuuKhoClient
if errorlevel 1 (
    echo Client ket thuc voi loi.
    pause
    exit /b 1
)

pause