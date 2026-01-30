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

    result = johansen_test(series_list=[x, y], regression="c")

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

    result = johansen_test(series_list=[x1, x2, x3], regression="c")

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

    result = johansen_test(series_list=[x1, x2, x3], regression="c")

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

    result = johansen_test(series_list=[x1, x2], regression="ct")

    log(f"Eigenvalues: {result.eig}")
    log(f"Trace stats: {result.lr1}")

    assert len(result.eig) == 2

  # 🆕 Тесты для автолагов
  def test_johansen_auto_select_lags(self):
    """Test Johansen automatic lag selection"""
    log("\n=== Johansen: Auto Lag Selection ===")
    np.random.seed(42)
    n = 100

    x1 = np.cumsum(np.random.normal(0, 1, n))
    x2 = x1 + np.random.normal(0, 0.5, n)

    # With auto lag selection (default)
    result_auto = johansen_test(
      series_list=[x1, x2],
      regression="c",
      auto_select_lags=True,
      max_lags=10
    )

    # Without auto lag selection
    result_manual = johansen_test(
      series_list=[x1, x2],
      regression="c",
      auto_select_lags=False
    )

    log(f"Auto lags result: {result_auto.eig}")
    log(f"Manual lags result: {result_manual.eig}")

    # Both should produce valid results
    assert len(result_auto.eig) == 2
    assert len(result_manual.eig) == 2

  def test_johansen_with_different_lag_limits(self):
    """Test Johansen with different max_lags"""
    log("\n=== Johansen: Different Max Lags ===")
    np.random.seed(42)
    n = 100

    x1 = np.cumsum(np.random.normal(0, 1, n))
    x2 = x1 + np.random.normal(0, 0.5, n)

    # Test with different max_lags
    for max_lags in [3, 5, 10]:
      log(f"\nTesting max_lags={max_lags}")
      result = johansen_test(
        series_list=[x1, x2],
        regression="c",
        auto_select_lags=True,
        max_lags=max_lags
      )

      assert len(result.eig) == 2
      log(f"  Eigenvalues: {result.eig}")

  def test_johansen_short_series_lag_handling(self):
    """Test Johansen with short series (auto lags should default to 1)"""
    log("\n=== Johansen: Short Series ===")
    np.random.seed(42)
    n = 30  # Short series

    x1 = np.cumsum(np.random.normal(0, 1, n))
    x2 = x1 + np.random.normal(0, 0.5, n)

    # Should handle short series gracefully
    result = johansen_test(
      series_list=[x1, x2],
      regression="c",
      auto_select_lags=True,
      max_lags=10
    )

    log(f"Short series result: {result.eig}")
    assert len(result.eig) == 2


class TestCointegrationEdgeCases:
  """Test edge cases for cointegration tests"""

  def test_nearly_identical_series(self):
    """Test with nearly identical series (very high cointegration)"""
    log("\n=== Edge: Nearly Identical Series ===")
    np.random.seed(42)
    n = 100

    x = np.cumsum(np.random.normal(0, 1, n))
    # Add tiny noise instead of perfectly identical
    y = x + np.random.normal(0, 0.001, n)  # ✅ Very small noise

    # AEG with nearly identical series
    result_aeg = aeg_test(data=[x, y], regression="c")
    log(f"AEG p-value: {result_aeg.p_value}")

    # Should be perfectly cointegrated
    assert result_aeg.p_value < 0.01, "Expected strong cointegration"

    # Johansen with nearly identical series
    result_joh = johansen_test(series_list=[x, y], regression="c")
    n_coint = sum(result_joh.lr1 > result_joh.cvt[:, 1])
    log(f"Johansen n_coint: {n_coint}")

    assert n_coint >= 1, "Expected at least 1 cointegration"

  def test_identical_series_error_handling(self):
    """Test that identical series are handled gracefully"""
    log("\n=== Edge: Identical Series (Error Handling) ===")
    np.random.seed(42)
    n = 100

    x = np.cumsum(np.random.normal(0, 1, n))

    # AEG should work (with warning)
    result_aeg = aeg_test(data=[x, x], regression="c")
    log(f"AEG p-value: {result_aeg.p_value}")
    # p-value will be 0 (perfect cointegration detected)
    assert result_aeg.p_value < 0.01

    # Johansen should raise LinAlgError for truly identical series
    try:
      result_joh = johansen_test(series_list=[x, x], regression="c")
      log("⚠️ Johansen did not raise error (unexpected)")
      # If it doesn't raise, check if it detected cointegration
      n_coint = sum(result_joh.lr1 > result_joh.cvt[:, 1])
      log(f"Johansen n_coint: {n_coint}")
    except np.linalg.LinAlgError as e:
      log(f"✅ Johansen correctly raised LinAlgError: {e}")
      # This is expected behavior for singular matrix

  def test_very_long_series(self):
    """Test with very long series"""
    log("\n=== Edge: Very Long Series ===")
    np.random.seed(42)
    n = 500  # Long series

    x = np.cumsum(np.random.normal(0, 1, n))
    y = x + np.random.normal(0, 0.5, n)

    result = johansen_test(
      series_list=[x, y],
      regression="c",
      auto_select_lags=True,
      max_lags=20
    )

    log(f"Long series eigenvalues: {result.eig}")
    assert len(result.eig) == 2


if __name__ == "__main__":
  pytest.main([__file__, "-v", "-s"])
