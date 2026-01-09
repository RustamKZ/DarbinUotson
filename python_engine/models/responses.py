from dataclasses import dataclass

@dataclass
class CriticalValues:
  one_percent: float
  five_percent: float
  ten_percent: float

@dataclass
class AdfTestResult:
  test_statistic: float
  p_value: float
  used_lag: int
  n_obs: int
  critical_values: CriticalValues
  is_stationary: bool

@dataclass
class ErrorResponse:
  error: str
  message: str
