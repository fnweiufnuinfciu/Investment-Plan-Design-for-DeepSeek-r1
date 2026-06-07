# DeepSeek-R1 Investment System Launcher
# Double-click 启动项目.bat to run this script

$ErrorActionPreference = "Continue"
$workspace = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $workspace

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  DeepSeek-R1 Intelligent Investment System" -ForegroundColor White
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# Find Java
# ============================================================
$javaBin = $null
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $javaBin = "$env:JAVA_HOME\bin"
}
if (-not $javaBin) {
    foreach ($p in @("$env:ProgramFiles\Java\jdk-*", "$env:ProgramFiles\Eclipse Adoptium\jdk-*")) {
        $f = Get-Item "$p\bin\javac.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($f) { $javaBin = Split-Path $f.FullName -Parent; break }
    }
}
if (-not $javaBin) {
    $c = Get-Command java -ErrorAction SilentlyContinue
    if ($c) { $javaBin = Split-Path $c.Source -Parent }
}
if (-not $javaBin) {
    Write-Host "[ERROR] Java JDK 17+ not found." -ForegroundColor Red
    Write-Host "  Download: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"; exit 1
}
Write-Host "  Java  : $javaBin" -ForegroundColor Green

# ============================================================
# Find Maven
# ============================================================
$mvnBin = $null
if ($env:MAVEN_HOME -and (Test-Path "$env:MAVEN_HOME\bin\mvn.cmd")) {
    $mvnBin = "$env:MAVEN_HOME\bin"
}
if (-not $mvnBin) {
    foreach ($p in @("$env:ProgramFiles\apache-maven-*", "${env:ProgramFiles(x86)}\apache-maven-*")) {
        $f = Get-Item "$p\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($f) { $mvnBin = Split-Path $f.FullName -Parent; break }
    }
}
if (-not $mvnBin) {
    $c = Get-Command mvn -ErrorAction SilentlyContinue
    if ($c) { $mvnBin = Split-Path $c.Source -Parent }
}
if (-not $mvnBin) {
    Write-Host "[ERROR] Maven not found." -ForegroundColor Red
    Write-Host "  Download: https://maven.apache.org/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"; exit 1
}
Write-Host "  Maven : $mvnBin" -ForegroundColor Green

# ============================================================
# Find Node.js
# ============================================================
$nodeDir = $null
foreach ($p in @("$env:ProgramFiles\nodejs", "${env:ProgramFiles(x86)}\nodejs")) {
    if (Test-Path "$p\node.exe") { $nodeDir = $p; break }
}
if (-not $nodeDir) {
    $c = Get-Command node -ErrorAction SilentlyContinue
    if ($c) { $nodeDir = Split-Path $c.Source -Parent }
}
if (-not $nodeDir) {
    Write-Host "[ERROR] Node.js not found." -ForegroundColor Red
    Write-Host "  Download: https://nodejs.org/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"; exit 1
}
Write-Host "  Node  : $nodeDir" -ForegroundColor Green
Write-Host ""

# ============================================================
# Verify project
# ============================================================
if (-not (Test-Path "$workspace\backend\pom.xml")) {
    Write-Host "[ERROR] backend\pom.xml not found" -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}
if (-not (Test-Path "$workspace\frontend\package.json")) {
    Write-Host "[ERROR] frontend\package.json not found" -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}

# ============================================================
# First-run: npm install (if needed)
# ============================================================
if (-not (Test-Path "$workspace\frontend\node_modules")) {
    Write-Host "==============================================" -ForegroundColor Yellow
    Write-Host "  First run - installing npm dependencies..." -ForegroundColor Yellow
    Write-Host "==============================================" -ForegroundColor Yellow
    $env:Path = "$nodeDir;$env:Path"
    Push-Location "$workspace\frontend"
    npm install
    Pop-Location
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] npm install failed" -ForegroundColor Red
        Read-Host "Press Enter to exit"; exit 1
    }
    Write-Host "  Done." -ForegroundColor Green
    Write-Host ""
}

# ============================================================
# Start BACKEND
# ============================================================
Write-Host "[1/2] Starting backend (Spring Boot :8080)..." -ForegroundColor White

# Write a temp bat with NO Chinese characters (all paths are ASCII)
# Working directory is set by PowerShell, so no "cd" needed
$beBat = [System.IO.Path]::GetTempFileName() + ".bat"
@"
@echo off
title Backend :8080 - Spring Boot
echo.
echo === Backend :8080 (Spring Boot) ===
echo.
echo First run may take 1-2 min for Maven downloads...
echo.
call "$mvnBin\mvn.cmd" spring-boot:run
pause
"@ | Out-File -FilePath $beBat -Encoding ASCII

$beProc = Start-Process cmd.exe -ArgumentList "/k `"$beBat`"" -WorkingDirectory "$workspace\backend" -PassThru

Write-Host "  Waiting for port 8080..." -ForegroundColor Gray
$ok = $false
for ($i = 1; $i -le 24; $i++) {
    Start-Sleep -Seconds 5
    $ln = netstat -ano 2>$null | Select-String ":8080 " | Select-String "LISTENING"
    if ($ln) { $ok = $true; break }
    Write-Host "  ...$($i * 5)s" -ForegroundColor Gray
}
if (-not $ok) {
    Write-Host "[WARNING] Backend did not start in 2 minutes." -ForegroundColor Yellow
    Write-Host "  Check the Backend window for errors." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"; exit 1
}
Write-Host "  Backend ready!" -ForegroundColor Green
Write-Host ""

# ============================================================
# Start FRONTEND
# ============================================================
Write-Host "[2/2] Starting frontend (Vite :5173)..." -ForegroundColor White

$feBat = [System.IO.Path]::GetTempFileName() + ".bat"
@"
@echo off
title Frontend :5173 - Vite
echo.
echo === Frontend :5173 (Vite) ===
echo.
npm run dev
pause
"@ | Out-File -FilePath $feBat -Encoding ASCII

Start-Process cmd.exe -ArgumentList "/k `"$feBat`"" -WorkingDirectory "$workspace\frontend"

Write-Host "  Frontend ready!" -ForegroundColor Green
Write-Host ""

# ============================================================
# Open browser
# ============================================================
Start-Process "http://localhost:5173"

# ============================================================
# Done
# ============================================================
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  >>> System started! <<<" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend  : http://localhost:5173" -ForegroundColor White
Write-Host "  Backend   : http://localhost:8080" -ForegroundColor White
Write-Host "  H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host ""
Write-Host "  Login: admin / admin123" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Keep Backend and Frontend windows open." -ForegroundColor Gray
Write-Host "==============================================" -ForegroundColor Cyan
Read-Host "Press Enter to exit"
