import pytest
import numpy as np
from algorithms.stationarity_tests import kpss_test, adf_test

class TestKpssTest:
  def test_compare_adf_and_kpss(self):
    np.random.seed(42)

    # stationary
    stationary = np.random.randn(100)
    print("\n1. Stationary data (white noise):")

    adf_result = adf_test(stationary)
    kpss_result = kpss_test(stationary)

    print(f"\nADF: is_stationary={adf_result.is_stationary}, p-value={adf_result.p_value:.4f}")
    print(f"KPSS: is_stationary={(kpss_result.p_value > 0.05)}, p-value={kpss_result.p_value:.4f}")
    print(f"Agreement: {adf_result.is_stationary == (kpss_result.p_value > 0.05)}")

    # non-stationary
    non_stationary = np.cumsum(np.random.randn(100))
    print("\n2. Non-stationary data (random walk):")

    adf_result = adf_test(non_stationary)
    kpss_result = kpss_test(non_stationary)

    print(f"\nADF: is_stationary={adf_result.is_stationary}, p-value={adf_result.p_value:.4f}")
    print(f"KPSS: is_stationary={(kpss_result.p_value > 0.05)}, p-value={kpss_result.p_value:.4f}")
    print(f"Agreement: {adf_result.is_stationary == (kpss_result.p_value > 0.05)}")
