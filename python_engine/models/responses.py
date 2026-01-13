from __future__ import annotations
from dataclasses import dataclass
from typing import Optional

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

@dataclass
class IntegrationOrderResult:
  order: int # 0 = I(0), 1 = I(1), 2 = I(2)
  adf_result: AdfTestResult
  kpss_result: KpssTestResult
  has_conflict: bool = False

@dataclass
class ZivotAndrewsResult:
  test_statistic: float
  p_value: float
  used_lag: int
  breakpoint: int # a point of structural shift
  critical_values: AdfCriticalValues
  is_stationary: bool
