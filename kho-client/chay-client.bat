@echo off
chcp 65001 >nul
rem Client Tra cứu kho. Cần cài JDK 21 trở lên; ứng dụng Spring Boot phải đang chạy.
rem Cách dùng: chay-client.bat [máy chủ] [cổng]
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 TraCuuKhoClient.java %*
