from dataclasses import dataclass

@dataclass
class AdfCriticalValues:
  one_percent: float
  five_percent: float
  ten_percent: float

@dataclass
class AdfTestResult:
  test_statistic: float
  p_value: float
  used_lag: int
  n_obs: int
  critical_values: AdfCriticalValues
  is_stationary: bool

@dataclass
class KpssCriticalValues:
  one_percent: float
  two_and_half_percent: float
  five_percent: float
  ten_percent: float


@dataclass
class KpssTestResult:
  kpss_stat: float
  p_value: float
  lags: int
  crit: KpssCriticalValues

@dataclass
class ErrorResponse:
  error: str
  message: str
