from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
from enum import Enum

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
  structural_break: Optional[int] = None # structual break index

@dataclass
class ZivotAndrewsResult:
  test_statistic: float
  p_value: float
  used_lag: int
  breakpoint: int # a point of structural shift
  critical_values: AdfCriticalValues
  is_stationary: bool

class ModelType(Enum):
  FULL_STATIONARY = "full_stationary"
  FULL_NON_STATIONARY = "full_non_stationary"
  MIXED = "mixed"

@dataclass
class SeriesOrder:
  order: int
  has_conflict: bool
  adf: AdfTestResult
  kpss: KpssTestResult
  structural_break: Optional[int] = None

@dataclass
class AnalysisResult:
  series_count: int
  series_orders: list[SeriesOrder]
  model_type: str # ModelType.value
  model_results: Optional[dict] = None

@dataclass
class AegCritValues:
  one_percent: float
  five_percent: float
  ten_percent: float

@dataclass
class AegTestResult:
  coint_t: float
  p_value: float
  crit_values: AegCritValues

@dataclass
class CointegrationTestType(Enum):
  AEG = "aeg"
  JOHANSEN = "johansen"

@dataclass
class CointegrationResult:
  test_type: CointegrationTestType
  n_series: int
  is_cointegrated: bool
  aeg_result: Optional[AegTestResult] = None
  johansen_eigenvalues: Optional[list[float]] = None
  johansen_trace_stats: Optional[list[float]] = None
  n_cointegration_relations: Optional[int] = None

@dataclass
class RegressionResult:
  pass

@dataclass
class ModelResults:
  cointegration: Optional[CointegrationResult] = None
  regression: Optional[RegressionResult] = None
  error_message: Optional[str ] = None
