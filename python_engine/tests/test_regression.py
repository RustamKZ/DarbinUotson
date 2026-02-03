import sys
import numpy as np
import pytest
from algorithms.regression import ols_regression


def log_test(msg):
    print(f"[TEST] {msg}", file=sys.stderr)


class TestRegression:
    """Тесты для OLS регрессии на уровнях"""

    def test_simple_ols_no_autocorrelation(self):
        """
        Тест простой OLS без автокорреляции
        """
        log_test("=== Test 1: Simple OLS (No Autocorrelation) ===")

        np.random.seed(42)
        n = 100

        # Генерируем данные: Y = 5 + 2*X1 + 3*X2 + noise
        X1 = np.random.normal(10, 2, n)
        X2 = np.random.normal(5, 1, n)
        noise = np.random.normal(0, 1, n)

        Y = 5 + 2 * X1 + 3 * X2 + noise

        # Запускаем регрессию
        result = ols_regression(Y, [X1, X2], add_constant=True)

        log_test(f"R²: {result.r_squared:.4f}")
        log_test(f"Coefficients: {[f'{c:.2f}' for c in result.coefficients]}")
        log_test(f"Durbin-Watson: {result.durbin_watson.statistic:.4f}")
        log_test(f"Has autocorrelation: {result.durbin_watson.has_autocorrelation}")
        log_test(f"Has lags: {result.has_lags}")
        log_test(f"Uses Newey-West: {result.uses_newey_west}")

        # Проверки
        assert result is not None
        assert result.r_squared > 0.9, "R² should be high for good linear relationship"
        assert len(result.coefficients) == 3, "Should have 3 coefficients (const + 2 vars)"
        assert len(result.p_values) == 3, "Should have 3 p-values"
        assert result.n_obs == n, f"Should have {n} observations"

        # Проверяем коэффициенты (примерно 5, 2, 3)
        const = result.coefficients[0]
        coef1 = result.coefficients[1]
        coef2 = result.coefficients[2]

        assert 4 < const < 6, f"Constant should be ~5, got {const:.2f}"
        assert 1.5 < coef1 < 2.5, f"Coef for X1 should be ~2, got {coef1:.2f}"
        assert 2.5 < coef2 < 3.5, f"Coef for X2 should be ~3, got {coef2:.2f}"

        # Не должно быть автокорреляции
        assert result.durbin_watson.has_autocorrelation == False
        assert result.has_lags == False
        assert result.uses_newey_west == False

        log_test("✅ Test 1 PASSED\n")

    def test_ols_with_autocorrelation(self):
        """
        Тест OLS с автокорреляцией - должен добавить лаги
        """
        log_test("=== Test 2: OLS with Autocorrelation ===")

        np.random.seed(123)
        n = 100

        # Генерируем данные с автокорреляцией
        X = np.random.normal(10, 2, n)

        # Y с автокорреляцией: Y[t] = 5 + 2*X[t] + 0.7*Y[t-1] + noise
        Y = np.zeros(n)
        Y[0] = 5 + 2 * X[0] + np.random.normal(0, 1)

        for t in range(1, n):
            Y[t] = 5 + 2 * X[t] + 0.7 * Y[t-1] + np.random.normal(0, 1)

        # Запускаем регрессию
        result = ols_regression(Y, [X], add_constant=True, auto_select_lags=True)

        log_test(f"R²: {result.r_squared:.4f}")
        log_test(f"Durbin-Watson: {result.durbin_watson.statistic:.4f}")
        log_test(f"Has autocorrelation: {result.durbin_watson.has_autocorrelation}")
        log_test(f"Has lags: {result.has_lags}")
        log_test(f"Uses Newey-West: {result.uses_newey_west}")

        # Проверки
        assert result is not None
        assert result.r_squared > 0.5, "R² should be reasonable"

        # Должны использоваться либо лаги, либо Newey-West
        assert result.has_lags == True or result.uses_newey_west == True, \
            "Should use lags or Newey-West for autocorrelated data"

        if result.has_lags:
            log_test("✅ System correctly added lags to handle autocorrelation")
        elif result.uses_newey_west:
            log_test("✅ System correctly used Newey-West correction")

        log_test("✅ Test 2 PASSED\n")

    def test_single_variable_regression(self):
        """
        Тест регрессии с одной независимой переменной
        """
        log_test("=== Test 3: Single Variable Regression ===")

        np.random.seed(999)
        n = 100

        # Y = 10 + 5*X + noise
        X = np.random.normal(0, 1, n)
        Y = 10 + 5 * X + np.random.normal(0, 0.5, n)

        result = ols_regression(Y, [X], add_constant=True)

        log_test(f"R²: {result.r_squared:.4f}")
        log_test(f"Coefficients: {[f'{c:.2f}' for c in result.coefficients]}")

        # Проверки
        assert len(result.coefficients) == 2, "Should have 2 coefficients (const + 1 var)"
        assert result.r_squared > 0.95, "R² should be very high for clean linear data"

        const = result.coefficients[0]
        slope = result.coefficients[1]

        assert 9 < const < 11, f"Constant should be ~10, got {const:.2f}"
        assert 4.5 < slope < 5.5, f"Slope should be ~5, got {slope:.2f}"

        log_test("✅ Test 3 PASSED\n")

    def test_regression_without_constant(self):
        """
        Тест регрессии без константы (через начало координат)
        """
        log_test("=== Test 4: Regression Without Constant ===")

        np.random.seed(777)
        n = 100

        # Y = 3*X (без константы)
        X = np.random.normal(5, 1, n)
        Y = 3 * X + np.random.normal(0, 0.2, n)

        result = ols_regression(Y, [X], add_constant=False)

        log_test(f"R²: {result.r_squared:.4f}")
        log_test(f"Coefficients: {[f'{c:.2f}' for c in result.coefficients]}")

        # Проверки
        assert len(result.coefficients) == 1, "Should have 1 coefficient (no constant)"
        assert result.r_squared > 0.98, "R² should be very high"

        slope = result.coefficients[0]
        assert 2.8 < slope < 3.2, f"Slope should be ~3, got {slope:.2f}"

        log_test("✅ Test 4 PASSED\n")

    def test_multiple_variables_regression(self):
        """
        Тест регрессии с 3+ переменными
        """
        log_test("=== Test 5: Multiple Variables (3+) Regression ===")

        np.random.seed(555)
        n = 100

        # Y = 2 + 1*X1 + 2*X2 + 3*X3 + 4*X4 + noise
        X1 = np.random.normal(0, 1, n)
        X2 = np.random.normal(5, 2, n)
        X3 = np.random.normal(-2, 1, n)
        X4 = np.random.normal(10, 3, n)

        Y = 2 + 1*X1 + 2*X2 + 3*X3 + 4*X4 + np.random.normal(0, 1, n)

        result = ols_regression(Y, [X1, X2, X3, X4], add_constant=True)

        log_test(f"R²: {result.r_squared:.4f}")
        log_test(f"Number of coefficients: {len(result.coefficients)}")
        log_test(f"F-statistic: {result.f_statistic:.2f}")
        log_test(f"F p-value: {result.f_pvalue:.4e}")

        # Проверки
        assert len(result.coefficients) == 5, "Should have 5 coefficients (const + 4 vars)"
        assert result.r_squared > 0.9, "R² should be high for good fit"
        assert result.f_pvalue < 0.001, "F-test should be highly significant"

        # Все p-values должны быть значимыми
        significant_count = sum(1 for p in result.p_values if p < 0.05)
        assert significant_count >= 4, "At least 4 variables should be significant"

        log_test("✅ Test 5 PASSED\n")

    def test_short_series_handling(self):
        """
        Тест обработки коротких рядов
        """
        log_test("=== Test 6: Short Series Handling ===")

        np.random.seed(333)
        n = 20  # Короткий ряд

        X = np.random.normal(5, 1, n)
        Y = 3 + 2*X + np.random.normal(0, 0.5, n)

        # Даже с автокорреляцией, для коротких рядов не должно быть лагов
        result = ols_regression(Y, [X], add_constant=True, auto_select_lags=True)

        log_test(f"n={n}, R²: {result.r_squared:.4f}")
        log_test(f"Has lags: {result.has_lags}")

        # Проверки
        assert result is not None
        assert result.n_obs <= n

        log_test("✅ Test 6 PASSED: Short series handled correctly\n")

    def test_data_structure_integrity(self):
        """
        Тест целостности структуры данных в результате
        """
        log_test("=== Test 7: Data Structure Integrity ===")

        np.random.seed(111)
        n = 50

        X = np.random.normal(0, 1, n)
        Y = 5 + 2*X + np.random.normal(0, 0.5, n)

        result = ols_regression(Y, [X], add_constant=True)

        # Проверяем все обязательные поля
        required_fields = [
            'coefficients', 'std_errors', 't_values', 'p_values',
            'r_squared', 'adj_r_squared', 'f_statistic', 'f_pvalue',
            'durbin_watson', 'n_obs', 'has_lags', 'uses_newey_west'
        ]

        for field in required_fields:
            assert hasattr(result, field), f"Missing field: {field}"
            log_test(f"✅ Field '{field}' present")

        # Проверяем типы
        assert isinstance(result.coefficients, list)
        assert isinstance(result.r_squared, float)
        assert isinstance(result.n_obs, int)
        assert isinstance(result.has_lags, bool)
        assert isinstance(result.uses_newey_west, bool)

        # Проверяем длины массивов
        n_coeffs = len(result.coefficients)
        assert len(result.std_errors) == n_coeffs
        assert len(result.t_values) == n_coeffs
        assert len(result.p_values) == n_coeffs

        log_test("✅ All data structures are valid")
        log_test("✅ Test 7 PASSED\n")
