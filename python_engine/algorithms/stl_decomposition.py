import numpy as np
from algorithms.integration import log
from models.responses import StlResult
from statsmodels.tsa.seasonal import STL

def detect_trend_and_seasonality(
  data: np.ndarray,
  period: int = 52
) -> StlResult:
  n = len(data)

  if n < 24:
    log("insufficient data for STL decomposition (n < 24)")
    return StlResult(
      has_trend = False,
      has_seasonality = False,
      trend_strength = 0.0,
      seasonal_strength = 0.0
    )

  if period < 2:
    period = 2
  if period >= n // 2:
    period = n // 3

  log(f"STL using period: {period}")

  try:
    stl = STL(data, period=period, seasonal=13)
    result = stl.fit()

    trend_var = np.var(result.trend)
    residual_var = np.var(result.resid)

    if trend_var + residual_var > 0:
      trend_strength = trend_var / (trend_var + residual_var)
    else:
      trend_strength = 0.0

    seasonal_var = np.var(result.seasonal)

    if seasonal_var + residual_var > 0:
      seasonal_strength = seasonal_var / (seasonal_var + residual_var)
    else:
      seasonal_strength = 0.0

    has_trend = bool(trend_strength > 0.2)
    has_seasonality = bool(seasonal_strength > 0.2)

    log(f"STL: trend_strength={trend_strength:.3f}, seasonal_strength={seasonal_strength:.3f}")
    log(f"STL: has_trend={has_trend}, has_seasonality={has_seasonality}")

    return StlResult(
      has_trend = has_trend,
      has_seasonality = has_seasonality,
      trend_strength = float(trend_strength),
      seasonal_strength = float(seasonal_strength),
      trend_component = result.trend,
      seasonal_component = result.seasonal,
      residual_component = result.resid
    )

  except Exception as e:
    log(f"STL decomposition failed: {e}")
    return StlResult(
      has_trend = False,
      has_seasonality = False,
      trend_strength = 0.0,
      seasonal_strength = 0.0
    )
