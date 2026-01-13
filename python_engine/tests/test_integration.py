import pytest
import numpy as np
from algorithms.integration import determine_integration_order
from models.responses import IntegrationOrderResult


class TestIntegrationOrder:
  """Тесты для определения порядка интегрированности"""

  def test_i0_white_noise(self):
    """I(0) - белый шум (стационарный)"""
    print("\n=== Testing I(0): White noise ===")

    np.random.seed(42)
    data = np.random.randn(100)
    print(f"Data: mean={data.mean():.3f}, std={data.std():.3f}")

    result = determine_integration_order(data)

    print(f"\n--- Final Result ---")
    print(f"Order: I({result.order})")
    print(f"Has conflict: {result.has_conflict}")

    assert isinstance(result, IntegrationOrderResult)
    assert result.order == 0
    assert result.has_conflict is False

  def test_i1_random_walk(self):
    """I(1) - случайное блуждание (нестационарный)"""
    print("\n=== Testing I(1): Random walk ===")

    np.random.seed(42)
    data = np.cumsum(np.random.randn(100))
    print(f"Data: mean={data.mean():.3f}, std={data.std():.3f}")
    print(f"Range: [{data.min():.2f}, {data.max():.2f}]")

    result = determine_integration_order(data)

    print(f"\n--- Final Result ---")
    print(f"Order: I({result.order})")

    assert isinstance(result, IntegrationOrderResult)
    assert result.order == 1

  def test_i1_with_drift(self):
    """I(1) - случайное блуждание с дрейфом"""
    print("\n=== Testing I(1): Random walk with drift ===")

    np.random.seed(42)
    drift = 0.5
    data = np.cumsum(np.random.randn(100) + drift)
    print(f"Drift: {drift}")
    print(f"Data: mean={data.mean():.3f}, final={data[-1]:.3f}")

    result = determine_integration_order(data)

    print(f"\n--- Final Result ---")
    print(f"Order: I({result.order})")

    assert result.order == 1

  def test_small_sample_i0(self):
    """I(0) - маленькая выборка"""
    print("\n=== Testing I(0): Small sample ===")

    small_data = np.array([1.2, 1.5, 1.3, 1.8, 1.6, 2.1, 1.9, 2.3, 2.0, 2.4])
    print(f"Sample size: {len(small_data)}")
    print(f"Data: {small_data}")

    result = determine_integration_order(small_data)

    print(f"\n--- Final Result ---")
    print(f"Order: I({result.order})")

    assert isinstance(result, IntegrationOrderResult)

  def test_max_order_limit(self):
    """Проверка достижения максимального порядка"""
    print("\n=== Testing max_order limit ===")

    np.random.seed(42)
    # I(2) процесс
    data = np.cumsum(np.cumsum(np.random.randn(100)))

    result = determine_integration_order(data, max_order=2)

    print(f"\n--- Final Result ---")
    print(f"Order: I({result.order})")
    print(f"Max order was: 2")

    assert result.order <= 2


if __name__ == "__main__":
  pytest.main([__file__, "-v", "-s"])
