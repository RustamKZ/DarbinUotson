#!/bin/bash

## kpss test
# pytest -s -v tests/test_stationarity_test.py::TestKpssTest

## integration order test
#pytest -s -v tests/test_integration.py

## cointegration test
#pytest -s -v tests/test_cointegration_test.py

## structural breaks test
# pytest -s -v tests/test_structural_breaks.py

## regression test
# pytest -s -v tests/test_regression.py::TestRegression

## stl decomposition test
# pytest -s -v tests/test_stl_decomposition.py::TestSTLDecomposition

## FINAL regression analysis test
# pytest -s -v tests/test_analysis.py
#pytest -s -v tests/test_real_data_analysis.py::TestRealDataAnalysis::test_full_analysis_with_real_data
#pytest tests/test_real_data_analysis.py::TestRealDataAnalysis::test_different_health_outcomes -v -s


#pytest -s -v tests/test_multiple_countries.py::TestMultipleCountries::test_analyze_10_countries

pytest tests/test_economic_analysis.py -v -s
