#!/bin/bash

# kpss test
# pytest -s -v tests/test_stationarity_test.py::TestKpssTest

# integration order test
#pytest -s -v tests/test_integration.py

# cointegration test
# pytest -s -v tests/test_cointegration_test.py

# structural breaks test
# pytest -s -v tests/test_structural_breaks.py

## regression test
pytest -s -v tests/test_regression.py::TestRegression
