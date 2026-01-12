import numpy as np
from statsmodels.tsa.stattools import adfuller, kpss
from models.responses import AdfTestResult, AdfCriticalValues, KpssCriticalValues, KpssTestResult

def adf_test(data: np.ndarray) -> AdfTestResult:
  result = adfuller(data, autolag='AIC')

  print(f"adf test result:\n\t->{result}")

  critical = AdfCriticalValues(
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

def kpss_test(data: np.ndarray) -> KpssTestResult :
  result = kpss(data, nlags = "auto", regression = "c")

  print(f"kpss test result\n{result}")

  critical = KpssCriticalValues(
    one_percent = result[3]["1%"],
    two_and_half_percent = result[3]["2.5%"],
    five_percent = result[3]["5%"],
    ten_percent = result[3]["10%"],
  )

  return KpssTestResult(
    kpss_stat = float(result[0]),
    p_value = float(result[1]),
    lags = int(result[2]),
    crit = critical
  )
