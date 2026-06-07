@echo off
set "PATH=C:\Program Files\apache-maven-3.9.9\bin;C:\Program Files\Java\jdk-17\bin;%PATH%"
cd /d "C:\Users\31908\Desktop\µ‰Œ§\workspace\backend"
title Backend :8080 - Spring Boot
echo.
echo ==============================================
echo   Spring Boot Backend :8080
echo ==============================================
echo.
echo Maven : C:\Program Files\apache-maven-3.9.9\bin
echo Java  : C:\Program Files\Java\jdk-17\bin
echo Dir   : %cd%
echo.
call "C:\Program Files\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
pause
