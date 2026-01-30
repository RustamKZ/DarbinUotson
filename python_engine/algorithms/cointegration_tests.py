import statsmodels.api as sm
from statsmodels.tsa.stattools import coint
from statsmodels.tsa.vector_ar.var_model import VAR
from statsmodels.tsa.vector_ar.vecm import (
  coint_johansen,
  JohansenTestResult
)
from models.responses import (
  AegTestResult,
  AegCritValues
)
from algorithms.integration import log
import numpy as np

# augmented engle-granger test
def aeg_test(data: list[np.ndarray], regression: str = "c") -> AegTestResult:

  if len(data) != 2:
    raise TypeError("data must consists of two series for aeg test")

  coint_t, pvalue, crit_values = coint(
    data[0],
    data[1],
    trend = regression,
    method = "aeg"
  )

  log(f"RESULTS: {coint_t}, {pvalue}, {crit_values}")

  return AegTestResult(
    coint_t = coint_t,
    p_value = pvalue,
    crit_values = AegCritValues(
      one_percent = crit_values[0],
      five_percent = crit_values[1],
      ten_percent = crit_values[2]
    )
  )

def johansen_test(
    series_list: list[np.ndarray],
    regression: str = "c", #nc, c, ct
    max_lags: int = 10,
    auto_select_lags: bool = True
) -> JohansenTestResult:
  n_series = len(series_list)
  n = len(series_list[0])

  ts_matrix = np.column_stack(series_list)

  det_order_map = {
    "nc": -1,
    "c": 0,
    "ct": 1
  }
  det_order = det_order_map.get(regression, 0)
  #select optimal lags
  if auto_select_lags:
    k_ar_diff = _select_optimal_lags_johansen(ts_matrix, max_lags)
  else:
    k_ar_diff = 1
    log(f"using default k_ar_diff = {k_ar_diff}")

  result = coint_johansen(
    ts_matrix,
    det_order = det_order,
    k_ar_diff = k_ar_diff
  )

  log(f"johansen test result: eig = {result.eig}, trace = {result.lr1}")

  return result

def _select_optimal_lags_johansen(
    ts_matrix: np.ndarray,
    max_lags: int
) -> int:
  if len(ts_matrix) <= max_lags + 10:
    log(f"insufficient data for lag selection, using lag=1")
    return 1

  try:
    var_model = VAR(ts_matrix)
    lag_order = var_model.select_order(maxlags = max_lags)

    optimal_lag = lag_order.aic

    log(f"VAR lag selection: {optimal_lag} (AIC)")

    if optimal_lag < 1:
      optimal_lag = 1

    return optimal_lag

  except Exception as e:
    log(f"VAR lag selection error: {e}, using lag = 1")
    return 1
