@echo off
REM build_scripts/build_python_windows.bat

echo === Building Python engine for Windows ===

cd /d %~dp0\..\python_engine

REM Активируем venv
call venv\Scripts\activate.bat

REM Устанавливаем PyInstaller
pip install pyinstaller

REM Переходим в build_scripts
cd ..\build_scripts

REM Собираем бинарник
pyinstaller stats_engine.spec --clean

REM Создаем папку для runtime
if not exist "..\python_runtime\windows" mkdir ..\python_runtime\windows

REM Копируем собранный бинарник
copy dist\stats_engine.exe ..\python_runtime\windows\

echo === Build complete ===
echo Binary: python_runtime\windows\stats_engine.exe

REM Тестируем
echo === Testing binary ===
..\python_runtime\windows\stats_engine.exe adf_test "{\"values\": [1.2, 1.5, 1.3, 1.8, 1.6, 2.1, 1.9, 2.3, 2.0, 2.4]}"

pause
