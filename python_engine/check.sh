#!/bin/bash

# kpss test
pytest -s -v tests/test_stationarity_test.py::TestKpssTest
