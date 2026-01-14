import sys
import numpy as np
from statsmodels.tsa.stattools import (
  adfuller,
  kpss,
  zivot_andrews
)
from models.responses import (
  AdfTestResult,
  AdfCriticalValues,
  KpssCriticalValues,
  KpssTestResult,
  ZivotAndrewsResult
)

def log(msg):
  print(msg, file=sys.stderr)

def adf_test(data: np.ndarray) -> AdfTestResult:
  result = adfuller(data, autolag='AIC')

  log(f"adf: stat={result[0]:.3f}, p={result[1]:.4f}")

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

"""
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⢻⣦⡀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⢀⣤⣤⣤⣀⣀⡀⠀⠀⠀⠹⣿⣦⡀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⢀⣴⣿⣻⣞⣷⡻⠉⠀⠀⠀⠀⠀⠱⣟⣿⣦⠀⠀⠀
⠀⠀⠀⢀⣴⣿⣻⣾⣽⣻⣎⠀⠀⠀⠀⠀⠀⠀⠀⠸⣷⢯⣧⠀⠀
⠀⠀⠐⢿⣯⡷⣟⡾⠳⡿⣽⡷⣄⠀⠀⠀⠀⠀⠀⠀⢻⣯⣟⣇⠀
⠀⠀⠀⠀⠙⠽⠋⠀⠀⠈⠻⣽⣟⡷⣄⠀⠀⠀⠀⠀⠘⣷⣯⢿⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠻⣽⣟⡷⣄⠀⠀⠀⢠⣿⢾⡿⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠻⣽⣟⡷⣄⢀⣾⡽⣯⡏⠀
⠀⠀⠀⠀⠀⣴⡶⣷⢿⡶⣄⣀⠀⠀⠀⠈⣻⣽⣟⣯⣷⢿⡛⠀⠀
⠀⠀⣀⣴⣾⢯⠿⠍⠻⣟⣿⣻⣟⣿⣻⢿⣽⡾⣯⣷⣻⢿⣄⠀⠀
⢀⣾⢿⣽⡾⠏⠀⠀⠀⠀⠉⠓⠋⠷⠿⠯⠗⠛⠁⠉⢻⣟⣾⢷⡄
⠸⢯⡿⠾⠃⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⠫⠋⠀
communists will be happy - KPSS will help to find out the truth
"""
def kpss_test(data: np.ndarray, regression: str = "c") -> KpssTestResult:
  result = kpss(data, nlags = "auto", regression = regression)

  log(f"kpss: stat={result[0]:.3f}, p={result[1]:.4f}")

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

# not less than 20 elements in ds
def zivot_andrews_test(data: np.ndarray, trend: str = 'c') -> ZivotAndrewsResult:
  result = zivot_andrews(data, trim=0.15, maxlag=None, regression=trend)

  log(f"ZA: stat={result[0]:.3f}, p={result[1]:.4f}, break={result[4]}")

  critical = AdfCriticalValues(
    one_percent=float(result[3]["1%"]),
    five_percent=float(result[3]["5%"]),
    ten_percent=float(result[3]["10%"])
  )

  return ZivotAndrewsResult(
    test_statistic=float(result[0]),
    p_value=float(result[1]),
    used_lag=int(result[2]),
    breakpoint=int(result[4]),
    critical_values=critical,
    is_stationary=result[1] < 0.05
  )
