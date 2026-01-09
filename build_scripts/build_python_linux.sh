#!/bin/bash
# build_scripts/build_python_linux.sh

set -e

echo "=== Building Python engine for Linux ==="

cd "$(dirname "$0")/../python_engine"

# Активируем venv
source venv/bin/activate

# Устанавливаем PyInstaller
pip install pyinstaller

# Переходим в папку build_scripts
cd ../build_scripts

# Собираем бинарник
pyinstaller stats_engine.spec --clean

# Создаем папку для runtime
mkdir -p ../python_runtime/linux

# Копируем собранный бинарник
cp dist/stats_engine ../python_runtime/linux/

# Делаем исполняемым
chmod +x ../python_runtime/linux/stats_engine

echo "=== Build complete ==="
echo "Binary: python_runtime/linux/stats_engine"

# Тестируем
echo "=== Testing binary ==="
../python_runtime/linux/stats_engine adf_test \
  '{"values": [1.2, 1.5, 1.3, 1.8, 1.6, 2.1, 1.9, 2.3, 2.0, 2.4]}'
