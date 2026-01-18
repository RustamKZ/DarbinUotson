import pytest
import numpy as np
from algorithms.cointegration_tests import (
  aeg_test,
  johansen_test
)
from algorithms.integration import log

class TestAEG:

  def test_aeg_cointegrated_series(self):
    """Test AEG with cointegrated series"""
    log("\n=== AEG: Cointegrated Series ===")
    np.random.seed(42)
    n = 100

    # Create cointegrated series: y = x + noise
    x = np.cumsum(np.random.normal(0, 1, n))
    y = x + np.random.normal(0, 0.5, n)

    result = aeg_test(data=[x, y], regression="c")

    log(f"Coint t-stat: {result.coint_t:.4f}")
    log(f"P-value: {result.p_value:.4f}")
    log(f"Critical 5%: {result.crit_values.five_percent:.4f}")

    # Should be cointegrated (p < 0.05)
    assert result.p_value < 0.05, "Expected cointegration"
    assert isinstance(result.coint_t, float)
    assert result.coint_t < result.crit_values.five_percent

  def test_aeg_non_cointegrated_series(self):
    """Test AEG with independent I(1) series"""
    log("\n=== AEG: Non-Cointegrated Series ===")
    np.random.seed(123)
    n = 100

    # Independent random walks
    x = np.cumsum(np.random.normal(0, 1, n))
    y = np.cumsum(np.random.normal(0, 1, n))

    result = aeg_test(data=[x, y], regression="c")

    log(f"Coint t-stat: {result.coint_t:.4f}")
    log(f"P-value: {result.p_value:.4f}")

    # Should NOT be cointegrated (p > 0.05)
    assert result.p_value > 0.05, "Expected no cointegration"

  def test_aeg_with_trend(self):
    """Test AEG with trend specification"""
    log("\n=== AEG: With Trend ===")
    np.random.seed(42)
    n = 100

    trend = np.arange(n) * 0.1
    x = trend + np.cumsum(np.random.normal(0, 1, n))
    y = x + np.random.normal(0, 0.5, n)

    result = aeg_test(data=[x, y], regression="ct")

    log(f"Coint t-stat: {result.coint_t:.4f}")
    log(f"P-value: {result.p_value:.4f}")

    assert isinstance(result.coint_t, float)
    assert isinstance(result.p_value, float)

  def test_aeg_error_single_series(self):
    """Test AEG error when single series provided"""
    log("\n=== AEG: Error Test ===")
    x = np.random.randn(100)

    with pytest.raises(TypeError):
      aeg_test(data=[x], regression="c")


class TestJohansen:

  def test_johansen_two_cointegrated(self):
    """Test Johansen with 2 cointegrated series"""
    log("\n=== Johansen: 2 Cointegrated Series ===")
    np.random.seed(42)
    n = 100

    x = np.cumsum(np.random.normal(0, 1, n))
    y = x + np.random.normal(0, 0.5, n)

    result = johansen_test(data=[x, y], regression="c")

    log(f"Eigenvalues: {result.eig}")
    log(f"Trace stats: {result.lr1}")
    log(f"Critical values (5%): {result.cvt[:, 1]}")

    # Check at least 1 cointegration relationship
    n_coint = sum(result.lr1 > result.cvt[:, 1])
    log(f"N cointegration relations: {n_coint}")

    assert n_coint >= 1, "Expected at least 1 cointegration"

  def test_johansen_three_series(self):
    """Test Johansen with 3 series"""
    log("\n=== Johansen: 3 Series ===")
    np.random.seed(42)
    n = 100

    x1 = np.cumsum(np.random.normal(0, 1, n))
    x2 = x1 + np.random.normal(0, 0.5, n)
    x3 = x1 + np.random.normal(0, 0.5, n)

    result = johansen_test(data=[x1, x2, x3], regression="c")

    log(f"Eigenvalues: {result.eig}")
    log(f"Trace stats: {result.lr1}")

    n_coint = sum(result.lr1 > result.cvt[:, 1])
    log(f"N cointegration relations: {n_coint}")

    assert len(result.eig) == 3
    assert len(result.lr1) == 3

  def test_johansen_non_cointegrated(self):
    """Test Johansen with independent series"""
    log("\n=== Johansen: Non-Cointegrated ===")
    np.random.seed(123)
    n = 100

    # Independent random walks
    x1 = np.cumsum(np.random.normal(0, 1, n))
    x2 = np.cumsum(np.random.normal(0, 1, n))
    x3 = np.cumsum(np.random.normal(0, 1, n))

    result = johansen_test(data=[x1, x2, x3], regression="c")

    n_coint = sum(result.lr1 > result.cvt[:, 1])
    log(f"N cointegration relations: {n_coint}")

    # Might have 0 or very few cointegrations
    assert n_coint <= 1

  def test_johansen_with_trend(self):
    """Test Johansen with trend"""
    log("\n=== Johansen: With Trend ===")
    np.random.seed(42)
    n = 100

    trend = np.arange(n) * 0.1
    x1 = trend + np.cumsum(np.random.normal(0, 1, n))
    x2 = x1 + np.random.normal(0, 0.5, n)

    result = johansen_test(data=[x1, x2], regression="ct")

    log(f"Eigenvalues: {result.eig}")
    log(f"Trace stats: {result.lr1}")

    assert len(result.eig) == 2

  def test_johansen_error_single_series(self):
    """Test Johansen error with single series"""
    log("\n=== Johansen: Error Test ===")
    x = np.random.randn(100)

    with pytest.raises(TypeError):
      johansen_test(data=[x], regression="c")


if __name__ == "__main__":
  pytest.main([__file__, "-v", "-s"])
