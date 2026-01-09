import numpy as np
from statsmodels.tsa.stattools import adfuller
from models.responses import AdfTestResult, CriticalValues

def adf_test(data: np.ndarray) -> AdfTestResult:
  result = adfuller(data, autolag='AIC')

  critical = CriticalValues(
    one_percent = float(result[4]['1%']),
    five_percent = float(result[4]['5%']),
    ten_percent = float(result[4]['10%'])
  )

  return AdfTestResult(
    test_statistic = float(result[0]),
    p_value = float(result[1]),
    used_lag = int(result[2]),
    n_obs = int(result[3]),
    critical_values = critical,
    is_stationary = result[1] < 0.05
  )
