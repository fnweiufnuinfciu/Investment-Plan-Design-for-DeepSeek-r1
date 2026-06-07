@echo off
chcp 65001 >nul
title FinBERT Classifier Server
cd /d "%~dp0"
echo.
echo ========================================
echo   FinBERT 主客观分类器安装
echo ========================================
echo.

REM Check Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Python，请先安装 Python 3.10+
    echo 下载地址: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo [1/2] 安装 Python 依赖...
pip install -r requirements.txt -q
if %errorlevel% neq 0 (
    echo [错误] 依赖安装失败
    pause
    exit /b 1
)

echo [2/2] 下载 FinBERT 模型（首次运行需联网下载，约 500MB）...
python finbert_classifier.py < nul 2>&1 | findstr "ready"
if %errorlevel% equ 0 (
    echo [完成] FinBERT 模型就绪
) else (
    echo [完成] 模型将使用启发式降级模式
)

echo.
echo 安装完成。
pause
