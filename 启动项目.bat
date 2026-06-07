@echo off
title DeepSeek-R1 Investment System
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM If launched with :real argument, skip the self-relaunch
if "%~1"==":real" goto :real
REM Self-relaunch with cmd /k so window NEVER auto-closes
start "" cmd /k ""%~f0" :real"
exit
:real

echo.
echo ==============================================
echo   DeepSeek-R1 Intelligent Investment System
echo ==============================================
echo.

REM ============================================================
REM  Detect Java
REM ============================================================
set "JAVA_DIR="

if defined JAVA_HOME (
    if exist "!JAVA_HOME!\bin\java.exe" set "JAVA_DIR=!JAVA_HOME!\bin"
)

if not defined JAVA_DIR for /d %%d in (
    "C:\Program Files\Java\jdk-*"
    "C:\Program Files\Eclipse Adoptium\jdk-*"
    "C:\Program Files\Microsoft\jdk-*"
) do if exist "%%~d\bin\java.exe" ( set "JAVA_DIR=%%~d\bin" & goto :java_ok )

if not defined JAVA_DIR (
    where java >nul 2>&1
    if !errorlevel! neq 0 (
        echo [ERROR] Java JDK 17+ not found.
        echo   Download: https://adoptium.net/
        goto :error
    )
    for /f "delims=" %%i in ('where java 2^>nul') do set "JAVA_DIR=%%~dpi"
)
:java_ok
if "%JAVA_DIR:~-1%"=="\" set "JAVA_DIR=%JAVA_DIR:~0,-1%"

if not exist "%JAVA_DIR%\javac.exe" (
    echo [WARNING] java found but javac.exe is missing - need JDK not JRE.
    echo   PATH: %JAVA_DIR%
    goto :error
)
echo   Java  : %JAVA_DIR%

REM ============================================================
REM  Detect Maven
REM ============================================================
set "MVN_DIR="

if defined MAVEN_HOME (
    if exist "!MAVEN_HOME!\bin\mvn.cmd" set "MVN_DIR=!MAVEN_HOME!\bin"
)

if not defined MVN_DIR for /d %%d in (
    "C:\Program Files\apache-maven-*"
    "C:\Program Files (x86)\apache-maven-*"
    "C:\apache-maven-*"
    "%USERPROFILE%\apache-maven-*"
) do if exist "%%~d\bin\mvn.cmd" ( set "MVN_DIR=%%~d\bin" & goto :mvn_ok )

if not defined MVN_DIR (
    where mvn >nul 2>&1
    if !errorlevel! neq 0 (
        echo [ERROR] Maven not found.
        echo   Download: https://maven.apache.org/
        goto :error
    )
    for /f "delims=" %%i in ('where mvn 2^>nul') do set "MVN_DIR=%%~dpi"
)
:mvn_ok
if "%MVN_DIR:~-1%"=="\" set "MVN_DIR=%MVN_DIR:~0,-1%"

if not exist "%MVN_DIR%\mvn.cmd" (
    echo [ERROR] mvn.cmd not found at: %MVN_DIR%\mvn.cmd
    goto :error
)
echo   Maven : %MVN_DIR%

REM ============================================================
REM  Detect Node.js
REM ============================================================
set "NODE_DIR="

for %%d in (
    "C:\Program Files\nodejs"
    "C:\Program Files (x86)\nodejs"
) do if exist "%%~d\node.exe" ( set "NODE_DIR=%%~d" & goto :node_ok )
for /d %%d in ("C:\Program Files\nodejs*") do if exist "%%~d\node.exe" ( set "NODE_DIR=%%~d" & goto :node_ok )

where node >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] Node.js not found.
    echo   Download: https://nodejs.org/
    goto :error
)
for /f "delims=" %%i in ('where node 2^>nul') do set "NODE_DIR=%%~dpi"
:node_ok
if "%NODE_DIR:~-1%"=="\" set "NODE_DIR=%NODE_DIR:~0,-1%"
echo   Node  : %NODE_DIR%
echo.

REM ============================================================
REM  Check project files
REM ============================================================
if not exist "%~dp0backend\pom.xml" (
    echo [ERROR] backend\pom.xml not found - repo may be incomplete.
    goto :error
)
if not exist "%~dp0frontend\package.json" (
    echo [ERROR] frontend\package.json not found - repo may be incomplete.
    goto :error
)

REM ============================================================
REM  First-run: npm install
REM ============================================================
if not exist "%~dp0frontend\node_modules" (
    echo.
    echo ==============================================
    echo   First run - installing npm dependencies...
    echo ==============================================
    echo.
    cd /d "%~dp0frontend"
    call npm install
    if !errorlevel! neq 0 (
        echo [ERROR] npm install failed. Check network.
        goto :error
    )
    cd /d "%~dp0"
    echo   Done.
    echo.
)

REM ============================================================
REM  Generate _run_backend.bat
REM ============================================================
(
echo @echo off
echo set "PATH=%MVN_DIR%;%JAVA_DIR%;%%PATH%%"
echo cd /d "%~dp0backend"
echo title Backend :8080
echo echo.
echo echo === Backend :8080 ^(Spring Boot^) ===
echo echo Maven: %MVN_DIR%
echo echo Java : %JAVA_DIR%
echo echo.
echo echo First run may take 1-2 min for Maven downloads...
echo echo.
echo call "%MVN_DIR%\mvn.cmd" spring-boot:run
echo if %%errorlevel%% neq 0 ^(echo. ^& echo [FAILED] Backend exited with error.^)
echo pause
) > "%cd%\_run_backend.bat"

REM ============================================================
REM  Generate _run_frontend.bat
REM ============================================================
(
echo @echo off
echo set "PATH=%NODE_DIR%;%%PATH%%"
echo cd /d "%~dp0frontend"
echo title Frontend :5173
echo echo.
echo echo === Frontend :5173 ^(Vite^) ===
echo echo Node: %NODE_DIR%
echo echo.
echo npm run dev
echo if %%errorlevel%% neq 0 ^(echo. ^& echo [FAILED] Frontend exited with error.^)
echo pause
) > "%cd%\_run_frontend.bat"

REM ============================================================
REM  Launch backend
REM ============================================================
echo [1/2] Starting backend ^(Spring Boot :8080^)...
start _run_backend.bat
if %errorlevel% neq 0 (
    echo [ERROR] Cannot open backend window.
    goto :error
)

echo   Waiting for port 8080...
set /a N=0
:wait_be
timeout /t 5 /nobreak >nul
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% equ 0 goto be_ok
set /a N+=1
echo   ...%N% x 5s
if %N% lss 24 goto wait_be
echo.
echo   [WARNING] Backend did not start in 2 minutes.
echo   Check the Backend window for errors.
goto :error
:be_ok
echo   Backend ready!
echo.

REM ============================================================
REM  Launch frontend
REM ============================================================
echo [2/2] Starting frontend ^(Vite :5173^)...
start _run_frontend.bat
if %errorlevel% neq 0 (
    echo [ERROR] Cannot open frontend window.
    goto :error
)
echo   Frontend ready!
echo.

REM ============================================================
REM  Open browser
REM ============================================================
echo Opening http://localhost:5173 ...
start http://localhost:5173 2>nul

REM ============================================================
REM  Success
REM ============================================================
echo ==============================================
echo.
echo   ^>^^>^> System started! ^<^<^<
echo.
echo   Frontend  : http://localhost:5173
echo   Backend   : http://localhost:8080
echo   H2 Console: http://localhost:8080/h2-console
echo.
echo   Login: admin / admin123
echo.
echo   Keep the Backend and Frontend windows open.
echo ==============================================
echo.
echo Press any key to close this launcher...
pause
exit

REM ============================================================
REM  Error handler — ALWAYS reaches here, window NEVER flashes
REM ============================================================
:error
echo.
echo ==============================================
echo   Setup incomplete - see error above.
echo ==============================================
echo.
echo Press any key to exit...
pause
exit
