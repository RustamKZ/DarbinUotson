# medical data analyzer

## project structure

```
DarbinUotson/
├── composeApp/
│   └── src/jvmMain/kotlin/org/example/project_dw/
│       ├── shared/      # data layer (datasources, models)
│       ├── presentation/# ui layer (screens, viewmodels)
│       └── di/          # dependency injection
├── python_engine/       # python statistical engine
│   ├── algorithms/      # statistical algorithms
│   ├── api/             # analyzer orchestrator
│   ├── models/          # data models (responses)
│   ├── tests/           # pytest tests
│   ├── main.py          # entry point
│   ├── check.sh         # test runner
│   └── venv/            # python virtual environment (gitignored)
├── python_runtime/      # compiled python binaries
│   ├── windows/         # stats_engine.exe
│   └── linux/           # stats_engine
└── build_scripts/       # build automation scripts
```

**note:** 
- `python_engine/venv/` is gitignored
- `python_runtime/` binaries must be rebuilt after adding dependencies

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
- uses `python_engine/venv/bin/python main.py`
- activated when `python_runtime/` doesn't exist
- shows: "PythonPathResolver: DEV mode"

**PROD Mode**:
- uses `python_runtime/{os}/stats_engine` binary
- activated when runtime binary exists
- shows: "PythonPathResolver: PROD mode"

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

3. run application
```bash
cd ..
./gradlew run
```

### running tests
```bash
cd python_engine
source venv/bin/activate
./check.sh           # if want to run easily
pytest tests/ -v -s  # start all tests with output manually
deactivate
```

### subsequent runs
```bash
./gradlew run
```

### building production linux package

1. build python binary
```bash
cd build_scripts
./build_python_linux.sh
```

2. verify binary
```bash
cd ..
./python_runtime/linux/stats_engine '{"y": [1.2, 1.5, 1.3, 1.8, 1.6, 2.1]}'
```

3. build package
```bash
./gradlew packageReleaseAppImage --no-configuration-cache
```

<br/>

## windows

### first time setup
1. clone repository
```powershell
git clone 
cd DarbinUotson
```

2. setup python environment
```powershell
cd python_engine
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
deactivate
```

3. run application
```powershell
cd ..
.\gradlew.bat run
```

### running tests
```powershell
cd python_engine
venv\Scripts\activate
pytest tests\ -v -s
deactivate
```

### building production windows package

1. build python binary
```powershell
cd build_scripts
build_python_windows.bat
```

2. verify binary
```powershell
cd ..
python_runtime\windows\stats_engine.exe "{\"y\": [1.2, 1.5, 1.3]}"
```

3. create distribution
```powershell
.\gradlew.bat createDistributable --no-configuration-cache
```

<br/>
