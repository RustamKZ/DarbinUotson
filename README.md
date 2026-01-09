# medical data analyzer

## project structure

```
DarbinUotson/
├── composeApp/
│   └── src/jvmMain/kotlin/org/example/project_dw/
│       ├── domain/      # business logic & use cases
│       ├── shared/      # data layer (repositories, datasources, models)
│       ├── presentation/# ui layer (screens, viewmodels)
│       └── di/          # dependency injection
├── python_engine/       # python statistical engine
│   ├── algorithms/      # statistical tests implementations
│   ├── api/             # CLI interface
│   ├── models/          # data models (requests/responses)
│   ├── main.py          # entry point
│   └── venv/            # python virtual environment (gitignored)
├── python_runtime/      # compiled python binaries
│   ├── windows/         # stats_engine.exe (build on Windows)
│   └── linux/           # stats_engine (build on Linux)
└── build_scripts/       # build automation scripts
    ├── build_python_linux.sh
    ├── build_python_windows.bat
    └── stats_engine.spec
```

**note:** 
- `python_engine/venv/` is gitignored
- `python_runtime/` binaries should be committed or built locally

## commit rules

- **feat**: new feature
- **fix**: bug fix
- **test**: adding or updating tests
- **refactor**: code refactoring
- **docs**: documentation changes
- **build**: build systems or dependencies
- **chore**: maintenance tasks (routine that doesn't change functionality)

<br/>

## build modes

**DEV Mode** (default):
- Uses `python_engine/venv/bin/python main.py`
- activated when `python_runtime/` doesn't exist

**PROD Mode**:
- Uses `python_runtime/{os}/stats_engine` binary
- activated after running build scripts

<br/>

## linux

### first time setup
1. clone repository
```bash
git clone 
cd DarbinUotson
```

2. setup python environment
```bash
cd python_engine
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
deactivate
```

3. make gradlew executable
``` bash
cd ..
chmod +x gradlew
```

4. run application (DEV mode uses venv automatically)
``` bash
./gradlew run
```

### subsequent runs (development)
```bash
cd ~/code/kotlin/DarbinUotson
./gradlew run
```

app will auto use python venv in DEV mode no need to enable it

### building production linux package

1. build python binary
```bash
cd build_scripts
chmod +x build_python_linux.sh
./build_python_linux.sh
```

2. verify binary works
``` bash
cd ..
./python_runtime/linux/stats_engine adf_test \
  '{"values": [1.2, 1.5, 1.3, 1.8, 1.6, 2.1, 1.9, 2.3, 2.0, 2.4]}'
```

3. test PROD mode
should show: "PythonBridge: PROD mode"
``` bash
./gradlew run
```

4. build .AppImage package
``` bash
./gradlew packageReleaseAppImage --no-configuration-cache
```

5. create distribution archive
```bash
cd composeApp/build/compose/binaries/main-release/app
tar -czf ~/medicaldataanalyzer-1.0.0-linux-x64.tar.gz medicaldataanalyzer/
```

**Result:** `~/medicaldataanalyzer-1.0.0-linux-x64.tar.gz`

**Usage:** Extract and run `medicaldataanalyzer/bin/medicaldataanalyzer`

<br/>

## windows

### first time setup
1. clone repository
```powershell
git clone 
cd DarbinUotson
```

2. setup python environment
``` bash
cd python_engine
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
deactivate
```

3. run application (DEV mode uses venv automatically)
``` bash
cd ..
.\gradlew.bat run
```

### subsequent runs (development)
```powershell
cd C:\path\to\DarbinUotson
.\gradlew.bat run
```

app will auto use python venv in DEV mode.

### building production windows installer

1. build python binary
``` bash
cd build_scripts
build_python_windows.bat
```

2. Verify binary works
``` bash
cd ..
python_runtime\windows\stats_engine.exe adf_test "{\"values\": [1.2, 1.5, 1.3, 1.8, 1.6, 2.1, 1.9, 2.3, 2.0, 2.4]}"
```

3. Test PROD mode
should show: "PythonBridge: PROD mode"
``` bash
.\gradlew.bat run
```

4. create portable distribution 
``` bash
.\gradlew.bat createDistributable --no-configuration-cache
```

5. create zip archive 
create ZIP archive for portable distribution
```powershell
cd composeApp\build\compose\binaries\main\app
Compress-Archive -Path medicaldataanalyzer -DestinationPath medicaldataanalyzer-windows-x64.zip
```

**Result:** `composeApp\build\compose\binaries\main\app\medicaldataanalyzer-windows-x64.zip`

**Usage:** Extract and run `medicaldataanalyzer\bin\medicaldataanalyzer.bat`
