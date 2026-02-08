import sys
import numpy as np
import pandas as pd
import pytest
from pathlib import Path
from algorithms.stl_decomposition import (
    detect_trend_and_seasonality,
    find_period_via_acf,
    _fallback_period
)
from algorithms.integration import log


# Path to test dataset
DATASET_PATH = Path(__file__).parent.parent / "datasets" / "global_climate_health_impact_tracker_2015_2025.csv"


def log_test(msg):
    print(f"[TEST] {msg}", file=sys.stderr)


class TestSTLDecomposition:
    """Tests for STL decomposition and ACF period detection"""

    @pytest.fixture
    def sample_data(self):
        """Load real dataset for testing"""
        if not DATASET_PATH.exists():
            pytest.skip(f"Dataset not found at {DATASET_PATH}")

        df = pd.read_csv(DATASET_PATH)

        # Filter USA data only
        usa_data = df[df['country_code'] == 'USA'].copy()
        usa_data = usa_data.sort_values('date')

        log_test(f"Loaded {len(usa_data)} USA records")

        return usa_data

    def test_acf_period_detection_temperature(self, sample_data):
        """Test ACF period detection on temperature data"""
        log_test("\n=== Test 1: ACF Period Detection (Temperature) ===")

        # Extract temperature column
        temperature = sample_data['temperature_celsius'].values
        n = len(temperature)

        log_test(f"Data size: {n}")

        # Test ACF period detection
        detected_period = find_period_via_acf(
            temperature,
            max_period=None,  # Auto-calculate
            expected_period=52  # Weekly data, expect yearly seasonality
        )

        log_test(f"Detected period: {detected_period}")

        # Assertions
        assert detected_period > 0, "Period should be positive"
        assert detected_period < n // 2, "Period should be less than half data size"

        # For weekly data, period should be close to 52 (yearly seasonality)
        # Allow tolerance of ±10
        assert 42 <= detected_period <= 62, f"Expected period ~52, got {detected_period}"

        log_test("✅ Test 1 PASSED\n")

    def test_acf_period_detection_multiple_variables(self, sample_data):
        """Test ACF on multiple health variables"""
        log_test("\n=== Test 2: ACF Multiple Variables ===")

        variables = [
            'temperature_celsius',
            'precipitation_mm',
            'pm25_ugm3',
            'respiratory_disease_rate'
        ]

        results = {}

        for var in variables:
            data = sample_data[var].values

            # Skip if too many NaN values
            if np.isnan(data).sum() > len(data) * 0.1:
                log_test(f"⚠️ Skipping {var}: too many NaN values")
                continue

            # Remove NaN values
            data = data[~np.isnan(data)]

            if len(data) < 52:
                log_test(f"⚠️ Skipping {var}: insufficient data")
                continue

            period = find_period_via_acf(data, expected_period=52)
            results[var] = period

            log_test(f"{var}: detected period = {period}")

        # Check that we detected at least some periods
        assert len(results) > 0, "Should detect periods for at least one variable"

        # All periods should be reasonable
        for var, period in results.items():
            assert 10 <= period <= 156, f"{var}: period {period} outside reasonable range"

        log_test("✅ Test 2 PASSED\n")

    def test_stl_decomposition_full(self, sample_data):
        """Test full STL decomposition pipeline"""
        log_test("\n=== Test 3: Full STL Decomposition ===")

        temperature = sample_data['temperature_celsius'].values

        # Run STL decomposition
        stl_result = detect_trend_and_seasonality(
            temperature,
            expected_period=52
        )

        log_test(f"Has trend: {stl_result.has_trend}")
        log_test(f"Has seasonality: {stl_result.has_seasonality}")
        log_test(f"Trend strength: {stl_result.trend_strength:.3f}")
        log_test(f"Seasonal strength: {stl_result.seasonal_strength:.3f}")

        # Assertions
        assert isinstance(stl_result.has_trend, bool)
        assert isinstance(stl_result.has_seasonality, bool)
        assert 0.0 <= stl_result.trend_strength <= 1.0
        assert 0.0 <= stl_result.seasonal_strength <= 1.0

        # Temperature should have seasonality (yearly pattern)
        assert stl_result.has_seasonality, "Temperature should show seasonal pattern"

        # Check components are present
        if stl_result.trend_component is not None:
            assert len(stl_result.trend_component) > 0
            log_test(f"Trend component size: {len(stl_result.trend_component)}")

        if stl_result.seasonal_component is not None:
            assert len(stl_result.seasonal_component) > 0
            log_test(f"Seasonal component size: {len(stl_result.seasonal_component)}")

        log_test("✅ Test 3 PASSED\n")

    def test_stl_trend_detection(self, sample_data):
        """Test trend detection capability"""
        log_test("\n=== Test 4: Trend Detection ===")

        # Create synthetic data with known trend
        n = 200
        time = np.arange(n)

        # Data with trend
        trend_data = 0.05 * time + 10 * np.sin(2 * np.pi * time / 52) + np.random.normal(0, 0.5, n)

        # Data without trend (pure seasonality)
        no_trend_data = 10 * np.sin(2 * np.pi * time / 52) + np.random.normal(0, 0.5, n)

        # Test with trend
        result_with_trend = detect_trend_and_seasonality(trend_data, expected_period=52)
        log_test(f"With trend: trend_strength={result_with_trend.trend_strength:.3f}")

        # Test without trend
        result_no_trend = detect_trend_and_seasonality(no_trend_data, expected_period=52)
        log_test(f"No trend: trend_strength={result_no_trend.trend_strength:.3f}")

        # Assertions
        assert result_with_trend.trend_strength > result_no_trend.trend_strength, \
            "Data with trend should have higher trend strength"

        assert result_with_trend.has_trend, "Should detect trend in trending data"

        log_test("✅ Test 4 PASSED\n")

    def test_fallback_period(self):
        """Test fallback period selection"""
        log_test("\n=== Test 5: Fallback Period ===")

        test_cases = [
            (200, 52),   # 200 points → 52 (yearly for weekly)
            (50, 30),    # 50 points → 30 (monthly)
            (20, 7),     # 20 points → 7 (weekly)
            (10, 3)      # 10 points → n//3
        ]

        for n, expected in test_cases:
            result = _fallback_period(n)
            log_test(f"n={n} → period={result} (expected ~{expected})")
            assert result > 0, "Period should be positive"
            assert result <= n, "Period should not exceed data size"

        log_test("✅ Test 5 PASSED\n")

    def test_short_series_handling(self):
        """Test handling of short time series"""
        log_test("\n=== Test 6: Short Series Handling ===")

        # Very short series
        short_data = np.random.randn(20)

        result = detect_trend_and_seasonality(short_data, expected_period=52)

        log_test(f"Short series result: has_trend={result.has_trend}, has_seasonality={result.has_seasonality}")

        # Should handle gracefully
        assert isinstance(result.has_trend, bool)
        assert isinstance(result.has_seasonality, bool)

        log_test("✅ Test 6 PASSED\n")

    def test_data_with_missing_values(self, sample_data):
        """Test handling of data with missing values"""
        log_test("\n=== Test 7: Missing Values Handling ===")

        # Get data with some NaN values
        pm25 = sample_data['pm25_ugm3'].values

        log_test(f"Original data size: {len(pm25)}")
        log_test(f"NaN count: {np.isnan(pm25).sum()}")

        # Remove NaN values
        clean_data = pm25[~np.isnan(pm25)]

        if len(clean_data) < 52:
            pytest.skip("Not enough clean data after removing NaN")

        # Should work on clean data
        result = detect_trend_and_seasonality(clean_data, expected_period=52)

        log_test(f"Result: has_seasonality={result.has_seasonality}")

        assert isinstance(result.has_seasonality, bool)

        log_test("✅ Test 7 PASSED\n")

    def test_india_data(self, sample_data):
        """Test on India data (different country)"""
        log_test("\n=== Test 8: India Data ===")

        # Load full dataset and filter India
        df = pd.read_csv(DATASET_PATH)
        india_data = df[df['country_code'] == 'IND'].copy()
        india_data = india_data.sort_values('date')

        log_test(f"India records: {len(india_data)}")

        if len(india_data) < 52:
            pytest.skip("Not enough India data")

        temperature = india_data['temperature_celsius'].values

        # Run STL
        result = detect_trend_and_seasonality(temperature, expected_period=52)

        log_test(f"India - trend_strength: {result.trend_strength:.3f}")
        log_test(f"India - seasonal_strength: {result.seasonal_strength:.3f}")

        assert isinstance(result.has_trend, bool)
        assert isinstance(result.has_seasonality, bool)

        log_test("✅ Test 8 PASSED\n")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "-s"])
