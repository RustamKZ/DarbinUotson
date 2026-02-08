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

def _is_constant(data: np.ndarray, tolerance: float = 1e-10) -> bool:
  """Check if series is constant (no variation)"""
  return np.std(data) < tolerance or np.ptp(data) < tolerance 

def adf_test(data: np.ndarray) -> AdfTestResult:
  if _is_constant(data):
    log("series is constant, treating as I(0) stationary")
    return AdfTestResult(
      test_statistic=np.nan,
      p_value=0.0,
      used_lag=0,
      n_obs=len(data),
      critical_values=AdfCriticalValues(
        one_percent=np.nan,
        five_percent=np.nan,
        ten_percent=np.nan
      ),
      is_stationary=True
    )
  
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
  if _is_constant(data):
    log("series is constant, treating as I(0) stationary")
    return KpssTestResult(
      kpss_stat=0.0,
      p_value=1.0,
      lags=0,
      crit=KpssCriticalValues(
        one_percent=np.nan,
        two_and_half_percent=np.nan,
        five_percent=np.nan,
        ten_percent=np.nan
      ),
      is_stationary=True
    )
  
  result = kpss(data, nlags = "auto", regression = regression)
  is_stationary = result[1] > 0.05
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
    crit = critical,
    is_stationary = is_stationary
  )

# not less than 20 elements in ds
def zivot_andrews_test(data: np.ndarray, trend: str = 'c') -> ZivotAndrewsResult:
  if _is_constant(data):
    log("series is constant, ZA test skipped")
    return ZivotAndrewsResult(
      test_statistic=np.nan,
      p_value=0.0,
      used_lag=0,
      breakpoint=len(data) // 2,
      critical_values=AdfCriticalValues(
        one_percent=np.nan,
        five_percent=np.nan,
        ten_percent=np.nan
      ),
      is_stationary=True
    )
  
  result = zivot_andrews(data, trim=0.15, maxlag=None, regression=trend)
  log(f"ZA: stat={result[0]:.3f}, p={result[1]:.4f}, break={result[4]}")
  
  critical = AdfCriticalValues(
    one_percent=float(result[2]["1%"]),   
    five_percent=float(result[2]["5%"]), 
    ten_percent=float(result[2]["10%"])  
  )
  
  return ZivotAndrewsResult(
    test_statistic=float(result[0]),
    p_value=float(result[1]),
    used_lag=int(result[3]),             
    breakpoint=int(result[4]),
    critical_values=critical,
    is_stationary=result[1] < 0.05
  )