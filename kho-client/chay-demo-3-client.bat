@echo off
chcp 65001 >nul
rem Demo 3 client cùng lúc và 1 client mất kết nối. Ứng dụng Spring Boot phải đang chạy.
rem Cách dùng: chay-demo-3-client.bat [máy chủ] [cổng]
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 KiemThuBaClient.java %*
