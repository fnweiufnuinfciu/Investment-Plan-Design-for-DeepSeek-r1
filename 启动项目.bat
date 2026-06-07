@echo off
title DeepSeek-R1 Investment System
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo ==============================================
echo   DeepSeek-R1 Intelligent Investment System
echo ==============================================
echo.

REM ===== Find Java =====
set "JAVA_DIR="
if exist "C:\Program Files\Java\jdk-17\bin\java.exe"      set "JAVA_DIR=C:\Program Files\Java\jdk-17\bin"
if exist "C:\Program Files\Java\jdk-21\bin\java.exe"      set "JAVA_DIR=C:\Program Files\Java\jdk-21\bin"
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.7.7-hotspot\bin\java.exe" set "JAVA_DIR=C:\Program Files\Eclipse Adoptium\jdk-17.0.7.7-hotspot\bin"
if not defined JAVA_DIR (
    where java >nul 2>&1
    if %errorlevel% neq 0 ( echo [ERROR] Java JDK 17+ not found & pause & exit /b 1 )
    for /f "delims=" %%i in ('where java 2^>nul') do set "JAVA_DIR=%%~dpi"
)
if "%JAVA_DIR:~-1%"=="\" set "JAVA_DIR=%JAVA_DIR:~0,-1%"
echo   Java  : %JAVA_DIR%

REM ===== Find Maven =====
set "MVN_DIR="
if exist "C:\Program Files\apache-maven-3.9.9\bin\mvn.cmd"  set "MVN_DIR=C:\Program Files\apache-maven-3.9.9\bin"
if exist "C:\Program Files\apache-maven-3.9.8\bin\mvn.cmd"  set "MVN_DIR=C:\Program Files\apache-maven-3.9.8\bin"
if exist "C:\Program Files\apache-maven-3.9.7\bin\mvn.cmd"  set "MVN_DIR=C:\Program Files\apache-maven-3.9.7\bin"
if exist "C:\Program Files\apache-maven-3.8.8\bin\mvn.cmd"  set "MVN_DIR=C:\Program Files\apache-maven-3.8.8\bin"
if exist "C:\Program Files\apache-maven-3.8.6\bin\mvn.cmd"  set "MVN_DIR=C:\Program Files\apache-maven-3.8.6\bin"
if not defined MVN_DIR (
    where mvn >nul 2>&1
    if %errorlevel% neq 0 ( echo [ERROR] Maven not found & pause & exit /b 1 )
    for /f "delims=" %%i in ('where mvn 2^>nul') do set "MVN_DIR=%%~dpi"
)
if "%MVN_DIR:~-1%"=="\" set "MVN_DIR=%MVN_DIR:~0,-1%"
echo   Maven : %MVN_DIR%

REM ===== Find Node =====
set "NODE_DIR="
if exist "C:\Program Files\nodejs\node.exe"           set "NODE_DIR=C:\Program Files\nodejs"
if not defined NODE_DIR (
    where node >nul 2>&1
    if %errorlevel% neq 0 ( echo [ERROR] Node.js not found & pause & exit /b 1 )
    for /f "delims=" %%i in ('where node 2^>nul') do set "NODE_DIR=%%~dpi"
)
if "%NODE_DIR:~-1%"=="\" set "NODE_DIR=%NODE_DIR:~0,-1%"
echo   Node  : %NODE_DIR%
echo.

REM ===== Generate _run_backend.bat =====
(
echo @echo off
echo set "PATH=%MVN_DIR%;%JAVA_DIR%;%%PATH%%"
echo cd /d "%~dp0backend"
echo title Backend :8080 - Spring Boot
echo echo.
echo echo ==============================================
echo echo   Spring Boot Backend :8080
echo echo ==============================================
echo echo.
echo echo Maven : %MVN_DIR%
echo echo Java  : %JAVA_DIR%
echo echo Dir   : %%cd%%
echo echo.
echo call "%MVN_DIR%\mvn.cmd" spring-boot:run
echo pause
) > "%cd%\_run_backend.bat"

REM ===== Generate _run_frontend.bat =====
(
echo @echo off
echo set "PATH=%NODE_DIR%;%%PATH%%"
echo cd /d "%~dp0frontend"
echo title Frontend :5173 - Vite
echo echo.
echo echo ==============================================
echo echo   Vue Frontend :5173
echo echo ==============================================
echo echo.
echo echo Node : %NODE_DIR%
echo echo Dir  : %%cd%%
echo echo.
echo npm run dev
echo pause
) > "%cd%\_run_frontend.bat"

echo Generated launcher scripts OK.
echo.

REM ===== Start backend =====
echo [1/2] Starting backend (Spring Boot :8080)...
start _run_backend.bat
if %errorlevel% neq 0 ( echo [ERROR] Cannot start backend & pause & exit /b 1 )

echo   Waiting for port 8080...
set /a N=0
:wait_be
timeout /t 5 /nobreak >nul
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% equ 0 goto be_ok
set /a N+=1
echo   ...%N% x 5s
if %N% lss 24 goto wait_be
echo   [WARNING] Timeout! Check the Backend window.
pause & exit /b 1
:be_ok
echo   Backend ready!
echo.

REM ===== Start frontend =====
echo [2/2] Starting frontend (Vite :5173)...
start _run_frontend.bat
if %errorlevel% neq 0 ( echo [ERROR] Cannot start frontend & pause & exit /b 1 )
echo   Frontend ready!
echo.

REM ===== Browser =====
start http://localhost:5173 2>nul

REM ===== Done =====
echo ==============================================
echo.
echo   >>> System started! <<<
echo.
echo   Frontend  : http://localhost:5173
echo   Backend   : http://localhost:8080
echo   H2 Console: http://localhost:8080/h2-console
echo.
echo   Login: admin / admin123
echo.
echo   Keep Backend and Frontend windows open.
echo ==============================================
pause
