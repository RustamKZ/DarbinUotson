import statsmodels.api as sm
from statsmodels.tsa.stattools import coint
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

def johansen_test(data: list[np.ndarray], regression = "c") -> JohansenTestResult:
  if len(data) < 2:
    raise TypeError("johansen tests requires at least 2 series")

  ts_matrix = np.column_stack(data)

  det_order_map = {
    "nc": "-1",
    "c": 0,
    "ct": 1
  }
  det_order = det_order_map.get(regression, 0)

  # number of lagged differences in the model
  # TODO: possibly can add AIC/BIC for autolag like in adfuller test
  k_ar_diff = 1

  result = coint_johansen(
    ts_matrix,
    det_order = det_order,
    k_ar_diff = k_ar_diff
  )

  log(f"johansen test result: eig = {result.eig}, trace = {result.lr1}")

  return result
