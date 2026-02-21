from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
from enum import Enum

@dataclass
class StlResult:
  has_trend: bool
  has_seasonality: bool
  trend_strength: float
  seasonal_strength: float
  trend_component: Optional[np.ndarray] = None
  seasonal_component: Optional[np.ndarray] = None
  residual_component: Optional[np.ndarray] = None

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
  is_stationary: bool

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
  structural_break: Optional[int] = None # structural break index
  za_result: Optional[ZivotAndrewsResult] = None

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
  za: Optional[ZivotAndrewsResult] = None
  structural_break: Optional[int] = None
  has_trend: bool = False
  has_seasonality: bool = False
  trend_strength: float = 0.0
  seasonal_strength: float = 0.0

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
class DurbinWatsonResult:
  statistic: float
  has_autocorrelation: bool

@dataclass
class RegressionResult:
  coefficients: list[CoefficientInfo]
  r_squared: float
  adj_r_squared: float
  f_statistic: float
  f_pvalue: float
  durbin_watson: DurbinWatsonResult
  n_obs: int
  has_lags: bool = False
  uses_newey_west: bool = False

class PeriodType(Enum):
  BEFORE_BREAK = "before_break"
  AFTER_BREAK = "after_break"
  CUSTOM = "custom" # for the cases with several structural breaks

@dataclass
class PeriodModelResult:
  period_type: PeriodType
  model_type: str # ModelType.value
  data_size: int
  series_orders: list[SeriesOrder]
  period_number: Optional[int] = None
  start_index: Optional[int] = None
  end_index: Optional[int] = None
  cointegration: Optional[CointegrationResult] = None
  regression: Optional[RegressionResult] = None
  error_message: Optional[str] = None

@dataclass
class StructuralBreak:
  index: int
  series_index: int

@dataclass
class PeriodInfo:
  period_number: int
  start_index: int
  end_index: int
  data_size: int

@dataclass
class ModelResults:
  cointegration: Optional[CointegrationResult] = None
  regression: Optional[RegressionResult] = None
  error_message: Optional[str ] = None
  has_structural_break: bool = False
  # for several structural breaks
  structural_breaks: Optional[list[StructuralBreak]] = None
  periods: Optional[list[PeriodInfo]] = None
  period_results: Optional[list[PeriodModelResult]] = None

@dataclass
class AnalysisResult:
  series_count: int
  variable_names: list[str]
  target_variable: str
  series_orders: list[SeriesOrder]
  model_type: str
  model_results: Optional[ModelResults] = None
  has_structural_break: bool = False
  structural_breaks: Optional[list[StructuralBreak]] = None
  transformations: Optional[list[TransformationInfo]] = None

class TransformationType(Enum):
  NONE = "none"
  FIRST_DIFFERENCE = "first_difference"
  SECOND_DIFFERENCE = "second_difference"

@dataclass
class TransformationInfo:
  series_index: int
  variable_name: str
  original_order: int
  transformation: TransformationType

@dataclass
class CoefficientInfo:
  name: str
  value: float
  std_error: float
  t_value: float
  p_value: float
  is_significant: bool
