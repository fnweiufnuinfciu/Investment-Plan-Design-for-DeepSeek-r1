@echo off
title DeepSeek-R1 Investment System
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo ==============================================
echo   DeepSeek-R1 Intelligent Investment System
echo ==============================================
echo.

REM ============================================================
REM  Detect Java (priority: JAVA_HOME > scan > where)
REM ============================================================
set "JAVA_DIR="

REM 1) JAVA_HOME environment variable
if defined JAVA_HOME (
    if exist "!JAVA_HOME!\bin\java.exe" set "JAVA_DIR=!JAVA_HOME!\bin"
)

REM 2) Scan common JDK install locations
if not defined JAVA_DIR for /d %%d in (
    "C:\Program Files\Java\jdk-*"
    "C:\Program Files\Eclipse Adoptium\jdk-*"
    "C:\Program Files\Microsoft\jdk-*"
    "C:\Program Files (x86)\Java\jdk-*"
) do if exist "%%~d\bin\java.exe" ( set "JAVA_DIR=%%~d\bin" & goto :java_ok )

REM 3) Fallback: PATH search
if not defined JAVA_DIR (
    where java >nul 2>&1
    if !errorlevel! neq 0 (
        echo [ERROR] Java JDK 17+ not found.
        echo   Install JDK 17+ from: https://adoptium.net/
        echo   Or set JAVA_HOME environment variable.
        pause & exit /b 1
    )
    for /f "delims=" %%i in ('where java 2^>nul') do set "JAVA_DIR=%%~dpi"
)
:java_ok
if "%JAVA_DIR:~-1%"=="\" set "JAVA_DIR=%JAVA_DIR:~0,-1%"

REM Verify it's JDK (has javac), not just JRE
if not exist "%JAVA_DIR%\javac.exe" (
    echo [WARNING] Found Java but missing javac.exe - may be JRE not JDK.
    echo   JAVA_DIR=%JAVA_DIR%
    echo   Please install JDK 17+ (not just JRE).
    pause & exit /b 1
)
echo   Java  : %JAVA_DIR%

REM ============================================================
REM  Detect Maven (priority: MAVEN_HOME > scan > where)
REM ============================================================
set "MVN_DIR="

REM 1) MAVEN_HOME environment variable
if defined MAVEN_HOME (
    if exist "!MAVEN_HOME!\bin\mvn.cmd" set "MVN_DIR=!MAVEN_HOME!\bin"
)

REM 2) Scan common Maven install locations
if not defined MVN_DIR for /d %%d in (
    "C:\Program Files\apache-maven-*"
    "C:\Program Files (x86)\apache-maven-*"
    "C:\apache-maven-*"
    "%USERPROFILE%\apache-maven-*"
) do if exist "%%~d\bin\mvn.cmd" ( set "MVN_DIR=%%~d\bin" & goto :mvn_ok )

REM 3) Fallback: PATH search
if not defined MVN_DIR (
    where mvn >nul 2>&1
    if !errorlevel! neq 0 (
        echo [ERROR] Maven not found.
        echo   Install Maven 3.8+ from: https://maven.apache.org/
        echo   Or set MAVEN_HOME environment variable.
        pause & exit /b 1
    )
    for /f "delims=" %%i in ('where mvn 2^>nul') do set "MVN_DIR=%%~dpi"
)
:mvn_ok
if "%MVN_DIR:~-1%"=="\" set "MVN_DIR=%MVN_DIR:~0,-1%"

if not exist "%MVN_DIR%\mvn.cmd" (
    echo [ERROR] mvn.cmd not found at: %MVN_DIR%\mvn.cmd
    pause & exit /b 1
)
echo   Maven : %MVN_DIR%

REM ============================================================
REM  Detect Node.js (priority: NODE_HOME > scan > where)
REM ============================================================
set "NODE_DIR="

REM 1) Common install locations
for %%d in (
    "C:\Program Files\nodejs"
    "C:\Program Files (x86)\nodejs"
    "%USERPROFILE%\AppData\Roaming\npm"
) do if exist "%%~d\node.exe" ( set "NODE_DIR=%%~d" & goto :node_ok )
for /d %%d in ("C:\Program Files\nodejs*") do if exist "%%~d\node.exe" ( set "NODE_DIR=%%~d" & goto :node_ok )

REM 2) Fallback: PATH search
where node >nul 2>&1
if !errorlevel! neq 0 (
    echo [ERROR] Node.js not found.
    echo   Install Node.js 18+ from: https://nodejs.org/
    pause & exit /b 1
)
for /f "delims=" %%i in ('where node 2^>nul') do set "NODE_DIR=%%~dpi"
:node_ok
if "%NODE_DIR:~-1%"=="\" set "NODE_DIR=%NODE_DIR:~0,-1%"
echo   Node  : %NODE_DIR%
echo.

REM ============================================================
REM  Check project folders exist
REM ============================================================
if not exist "%~dp0backend\pom.xml" (
    echo [ERROR] backend\pom.xml not found - repo may be incomplete.
    pause & exit /b 1
)
if not exist "%~dp0frontend\package.json" (
    echo [ERROR] frontend\package.json not found - repo may be incomplete.
    pause & exit /b 1
)

REM ============================================================
REM  First-run: install frontend dependencies if needed
REM ============================================================
if not exist "%~dp0frontend\node_modules" (
    echo.
    echo ==============================================
    echo   First run detected - installing dependencies
    echo ==============================================
    echo.
    echo Installing frontend npm packages...
    cd /d "%~dp0frontend"
    call npm install
    if !errorlevel! neq 0 (
        echo [ERROR] npm install failed. Check your network connection.
        pause & exit /b 1
    )
    cd /d "%~dp0"
    echo   npm install completed.
    echo.
)

REM ============================================================
REM  Generate _run_backend.bat (auto-regenerated each run)
REM ============================================================
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
echo echo Starting Spring Boot ^(first run may take 1-2 min to download dependencies^)...
echo echo.
echo call "%MVN_DIR%\mvn.cmd" spring-boot:run
echo if %%errorlevel%% neq 0 ^(
echo   echo.
echo   echo [FAILED] Backend exited with error.
echo   echo Check the output above for details.
echo ^)
echo pause
) > "%cd%\_run_backend.bat"

REM ============================================================
REM  Generate _run_frontend.bat (auto-regenerated each run)
REM ============================================================
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
echo if %%errorlevel%% neq 0 ^(
echo   echo.
echo   echo [FAILED] Frontend exited with error.
echo ^)
echo pause
) > "%cd%\_run_frontend.bat"

echo.

REM ============================================================
REM  Start backend
REM ============================================================
echo [1/2] Starting backend (Spring Boot :8080)...
start _run_backend.bat
if %errorlevel% neq 0 (
    echo [ERROR] Cannot start backend window.
    pause & exit /b 1
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
echo   [WARNING] Backend did not start within 2 minutes.
echo.
echo   Troubleshooting:
echo   1. Check the Backend window for error messages
echo   2. First run downloads Maven dependencies ^(1-2 minutes^)
echo   3. Make sure port 8080 is not in use
echo   4. Try running _run_backend.bat directly to see errors
pause & exit /b 1
:be_ok
echo   Backend ready!
echo.

REM ============================================================
REM  Start frontend
REM ============================================================
echo [2/2] Starting frontend (Vite :5173)...
start _run_frontend.bat
if %errorlevel% neq 0 (
    echo [ERROR] Cannot start frontend window.
    pause & exit /b 1
)
echo   Frontend ready!
echo.

REM ============================================================
REM  Open browser
REM ============================================================
echo Opening http://localhost:5173 ...
start http://localhost:5173 2>nul

REM ============================================================
REM  Done
REM ============================================================
echo ==============================================
echo.
echo   >>> System started successfully! <<<
echo.
echo   Frontend  : http://localhost:5173
echo   Backend   : http://localhost:8080
echo   H2 Console: http://localhost:8080/h2-console
echo.
echo   Login: admin / admin123
echo.
echo   Keep the Backend and Frontend windows open.
echo   Close THIS window to exit.
echo ==============================================
pause
