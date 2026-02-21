import json
import sys
import math
from enum import Enum
import numpy as np
from dataclasses import asdict
from typing import Optional
from statsmodels.tsa.vector_ar.vecm import JohansenTestResult
from algorithms.integration import determine_integration_order, log
from algorithms.cointegration_tests import aeg_test, johansen_test
from algorithms.stl_decomposition import detect_trend_and_seasonality
from algorithms.ecm import build_ecm_model
from algorithms.var import build_var_on_differences
from algorithms.mixed_regression import build_mixed_regression
from algorithms.regression import ols_regression
from models.responses import (
  SeriesOrder,
  AnalysisResult,
  ModelType,
  CointegrationResult,
  CointegrationTestType,
  ModelResults,
  IntegrationOrderResult,
  PeriodType,
  PeriodModelResult,
  StructuralBreak,
  PeriodInfo,
  AegTestResult,
  StlResult,
  TransformationType,
  TransformationInfo
)
from models.domain import (
  PreparedData,
  PeriodAnalysis,
  PeriodData
)

def analyze_time_series(input_json: str) -> str:
  try:
    input_data = json.loads(input_json)
  except json.JSONDecodeError as e:
    error = {
      "error": "INVALID_JSON",
      "message": f"Failed to parse JSON input: {str(e)}"
    }
    return json.dumps(error)

  if "series" not in input_data:
    error = {
      "error": "MISSING_SERIES",
      "message": "Input must contain 'series' field"
    }
    return json.dumps(error)

  if not isinstance(input_data["series"], list):
    error = {
      "error": "INVALID_SERIES",
      "message": "'series' must be an array"
    }
    return json.dumps(error)

  if len(input_data["series"]) == 0:
    error = {
      "error": "EMPTY_SERIES",
      "message": "'series' array is empty"
    }
    return json.dumps(error)

  if len(input_data["series"]) < 2:
    error = {
      "error": "INSUFFICIENT_SERIES",
      "message": "At least 2 time series required for regression analysis (1 dependent + 1 independent)"
    }
    return json.dumps(error)

  series_list = []
  variable_names = []

  for idx, s in enumerate(input_data["series"]):
    if not isinstance(s, dict):
      error = {
        "error": "INVALID_SERIES_FORMAT",
        "message": f"Series at index {idx} must be an object with 'name' and 'data' fields"
      }
      return json.dumps(error)

    if "data" not in s:
      error = {
        "error": "MISSING_DATA",
        "message": f"Series at index {idx} is missing 'data' field"
      }
      return json.dumps(error)

    if not isinstance(s["data"], list):
      error = {
        "error": "INVALID_DATA_FORMAT",
        "message": f"Series at index {idx}: 'data' must be an array"
      }
      return json.dumps(error)

    if len(s["data"]) < 20:
      error = {
        "error": "INSUFFICIENT_DATA",
        "message": f"Series at index {idx} has only {len(s['data'])} observations (minimum: 20)"
      }
      return json.dumps(error)

    series_list.append(np.array(s["data"]))
    variable_names.append(s.get("name", f"series_{idx}"))

  target_index = input_data.get("target_index")

  if target_index is not None:
    if not isinstance(target_index, int):
      error = {
        "error": "INVALID_TARGET_INDEX",
        "message": "'target_index' must be an integer"
      }
      return json.dumps(error)

    if target_index < 0 or target_index >= len(series_list):
      error = {
        "error": "TARGET_INDEX_OUT_OF_RANGE",
        "message": f"'target_index' must be between 0 and {len(series_list) - 1}"
      }
      return json.dumps(error)

    log(f"user-specified target: {variable_names[target_index]}")
  else:
    target_index = _auto_detect_target(variable_names)
    log(f"auto-detected target: {variable_names[target_index]}")

  target_variable = variable_names[target_index]

  if target_index != 0:
    _swap_series(series_list, variable_names, 0, target_index)

  try:
    series_orders = _analyze_series_orders(series_list)

    model_type = _decide_model_type(series_orders)
    log(f"model type: {model_type.value}")

    prepared_data = _prepare_data(series_list, series_orders, model_type)

    transformations = None
    if model_type == ModelType.MIXED:
      transformations = _create_transformation_info(series_orders, variable_names)

    model_results = _build_model(prepared_data)

    result = AnalysisResult(
      series_count = len(series_list),
      variable_names = variable_names,
      target_variable = target_variable,
      series_orders = series_orders,
      model_type = model_type.value,
      model_results = model_results,
      has_structural_break = prepared_data.has_structural_break,
      structural_breaks = prepared_data.structural_breaks,
      transformations = transformations
    )

    result_dict = _clean_nans(asdict(result))
    return json.dumps(result_dict, default = str)

  except Exception as e:
    log(f"[ERROR] Analysis failed: {e}")
    import traceback
    traceback.print_exc(file=sys.stderr)
    
    error = {
      "error": "ANALYSIS_FAILED",
      "message": f"Time series analysis failed: {str(e)}"
    }
    return json.dumps(error)

def _auto_detect_target(names: list[str]) -> int:
  health_keywords = [
    "disease", "illness", "mortality", "death", "infection",
    "hospital", "patient", "symptom", "diagnosis",
    "respiratory", "cardiovascular", "cancer", "rate",
    "cases", "incidence", "prevalence",
    "admission", "health", "incident", "risk"
  ]

  for i in range(len(names)):
    name_lower = names[i].lower()

    for keyword in health_keywords:
      if keyword in name_lower:
        log(f"found health variable: {names[i]}")
        return i

  log("no health variable detected, using first series")
  return 0


def _swap_series(
  series_list: list[np.ndarray],
  names: list[str],
  idx1: int,
  idx2: int
):
  series_list[idx1], series_list[idx2] = series_list[idx2], series_list[idx1]
  names[idx1], names[idx2] = names[idx2], names[idx1]


def _analyze_series_orders(series_list: list[np.ndarray]) -> list[SeriesOrder]:
  series_orders = []
  i = 0

  for series in series_list:
    log(f"\nseries {i + 1}")
    i = i + 1

    stl_result: StlResult = detect_trend_and_seasonality(series)

    if stl_result.has_trend:
      log("stl detected trend: using 'ct' regression")
      kpss_regression = "ct"
      za_regression = "ct"
    else:
      log("stl no trend: using 'c' regression")
      kpss_regression = "c"
      za_regression = "c"

    order_result: IntegrationOrderResult = determine_integration_order(
      data = series,
      kpss_regression = kpss_regression,
      za_regression = za_regression
    )

    series_orders.append(
      SeriesOrder(
        order = order_result.order,
        has_conflict = order_result.has_conflict,
        adf = order_result.adf_result,
        kpss = order_result.kpss_result,
        za = order_result.za_result,
        structural_break = order_result.structural_break,
        has_trend = stl_result.has_trend,
        has_seasonality = stl_result.has_seasonality,
        trend_strength = stl_result.trend_strength,
        seasonal_strength = stl_result.seasonal_strength
      )
    )

  return series_orders


def _decide_model_type(series_orders: list[SeriesOrder]) -> ModelType:
  integration_orders = []
  for s in series_orders:
    integration_orders.append(s.order)

  all_stationary = True
  for order in integration_orders:
    if order != 0:
      all_stationary = False
      break

  if all_stationary:
    return ModelType.FULL_STATIONARY

  all_non_stationary = True
  for order in integration_orders:
    if order != 1:
      all_non_stationary = False
      break

  if all_non_stationary:
    return ModelType.FULL_NON_STATIONARY

  return ModelType.MIXED


def _prepare_data(
    series_list: list[np.ndarray],
    series_orders: list[SeriesOrder],
    model_type: ModelType
) -> PreparedData:
  prepared = PreparedData(
    original_series = series_list,
    series_orders = series_orders,
    model_type = model_type
  )

  all_breaks = []
  series_idx = 0
  for so in series_orders:
    if so.structural_break is not None:
      break_info = StructuralBreak(
        index = so.structural_break,
        series_index = series_idx
      )
      all_breaks.append(break_info)
    series_idx = series_idx + 1

  if len(all_breaks) == 0:
    return prepared

  log(f"found {len(all_breaks)} structural breaks:")
  for brk in all_breaks:
    log(f"  - series {brk.series_index}, index {brk.index}")

  unique_breakpoints = []
  for brk in all_breaks:
    idx = brk.index

    is_duplicate = False
    for existing_idx in unique_breakpoints:
      if abs(idx - existing_idx) < 5:
        is_duplicate = True
        break

    if not is_duplicate:
      unique_breakpoints.append(idx)

  unique_breakpoints.sort()

  log(f"unique breakpoints: {unique_breakpoints}")

  prepared.has_structural_break = True
  prepared.structural_breaks = all_breaks

  prepared.periods_data = _split_into_periods(series_list, unique_breakpoints)

  return prepared


def _split_into_periods(
    series_list: list[np.ndarray],
    breakpoints: list[int]
) -> list[PeriodData]:
  periods = []
  n_data = len(series_list[0])

  boundaries = [0]
  for bp in breakpoints:
    boundaries.append(bp)
  boundaries.append(n_data)

  period_num = 0
  i = 0
  while i < len(boundaries) - 1:
    start = boundaries[i]
    end = boundaries[i + 1]

    period_series = []
    for series in series_list:
      period_series.append(series[start:end])

    period = PeriodData(
      period_number = period_num,
      start_index = start,
      end_index = end,
      series_data = period_series,
      data_size = end - start
    )

    periods.append(period)
    log(f"period {period_num}: [{start}:{end}], size={end - start}")

    period_num = period_num + 1
    i = i + 1

  return periods


def _analyze_period(
    period_data: PeriodData,
    period_type: PeriodType
) -> PeriodAnalysis:
  log(f"analyzing period {period_data.period_number}")

  period_orders = _analyze_series_orders(period_data.series_data)
  period_model_type = _decide_model_type(period_orders)

  log(f"period {period_data.period_number} model type: {period_model_type.value}")

  return PeriodAnalysis(
    period_type = period_type,
    period_number = period_data.period_number,
    series_orders = period_orders,
    model_type = period_model_type,
    data_size = period_data.data_size
  )


def _build_model(prepared_data: PreparedData) -> Optional[ModelResults]:
  if prepared_data.has_structural_break:
    return _build_model_with_breaks(prepared_data)

  return _build_single_model(prepared_data)


def _build_model_with_breaks(prepared_data: PreparedData) -> ModelResults:
  num_periods = len(prepared_data.periods_data)
  log(f"building separate models for {num_periods} periods")

  period_results = []
  periods_info = []

  for period_data in prepared_data.periods_data:
    log(f"\n=== Period {period_data.period_number} ===")

    if num_periods == 2:
      if period_data.period_number == 0:
        period_type = PeriodType.BEFORE_BREAK
      else:
        period_type = PeriodType.AFTER_BREAK
    else:
      period_type = PeriodType.CUSTOM

    period_analysis = _analyze_period(period_data, period_type)

    period_prepared = PreparedData(
      original_series = period_data.series_data,
      series_orders = period_analysis.series_orders,
      model_type = period_analysis.model_type
    )
    period_model = _build_single_model(period_prepared)

    period_result = PeriodModelResult(
      period_type = period_analysis.period_type,
      period_number = period_data.period_number,
      start_index = period_data.start_index,
      end_index = period_data.end_index,
      model_type = period_analysis.model_type.value,
      data_size = period_data.data_size,
      series_orders = period_analysis.series_orders,
      cointegration = period_model.cointegration if period_model else None,
      regression = period_model.regression if period_model else None,
      error_message = period_model.error_message if period_model else None
    )

    period_results.append(period_result)

    period_info = PeriodInfo(
      period_number = period_data.period_number,
      start_index = period_data.start_index,
      end_index = period_data.end_index,
      data_size = period_data.data_size
    )
    periods_info.append(period_info)

  return ModelResults(
    has_structural_break = True,
    structural_breaks = prepared_data.structural_breaks,
    periods = periods_info,
    period_results = period_results
  )

def _build_single_model(prepared_data: PreparedData) -> Optional[ModelResults]:
  series_list = prepared_data.original_series
  series_orders = prepared_data.series_orders
  model_type = prepared_data.model_type

  if len(series_list) < 2:
    log("[ERROR] regression requires at least 2 variables (1 dependent + 1 independent)")
    return ModelResults(
      error_message = "Regression requires at least 2 variables (1 dependent + 1 independent)"
    )

  if model_type == ModelType.FULL_STATIONARY:
    log("full stationary: regression on levels")

    y = series_list[0]
    X_list = []

    i = 1
    while i < len(series_list):
      X_list.append(series_list[i])
      i += 1

    try:
      regression_result = ols_regression(
        y = y,
        X_list = X_list,
        add_constant = True,
        auto_select_lags = True,
        max_lags_search = 5
      )

      return ModelResults(regression = regression_result)
      
    except Exception as e:
      log(f"[ERROR] OLS regression failed: {e}")
      return ModelResults(
        error_message = f"OLS regression failed: {str(e)}"
      )

  elif model_type == ModelType.FULL_NON_STATIONARY:
    log("full non-stationary: testing cointegration")

    has_any_trend = False
    for so in series_orders:
      if so.has_trend:
        has_any_trend = True
        break

    if has_any_trend:
      log("trend detected in series: using 'ct' for cointegration")
      coint_regression = "ct"
    else:
      log("no trend: using 'c' for cointegration")
      coint_regression = "c"

    try:
      coint_result = _check_cointegration(series_list, coint_regression)
    except Exception as e:
      log(f"[ERROR] cointegration test failed: {e}")
      return ModelResults(
        error_message = f"Cointegration test failed: {str(e)}"
      )

    if coint_result.is_cointegrated:
      log("cointegration found: building ECM")
      
      try:
        regression_result = build_ecm_model(series_list, coint_regression)

        return ModelResults(
          cointegration = coint_result,
          regression = regression_result
        )
      except Exception as e:
        log(f"[ERROR] ECM model failed: {e}")
        return ModelResults(
          cointegration = coint_result,
          error_message = f"ECM model failed: {str(e)}"
        )
    else:
      log("no cointegration: building VAR on differences")
      
      try:
        regression_result = build_var_on_differences(series_list)

        return ModelResults(
          cointegration = coint_result,
          regression = regression_result
        )
      except Exception as e:
        log(f"[ERROR] VAR model failed: {e}")
        return ModelResults(
          cointegration = coint_result,
          error_message = f"VAR model failed: {str(e)}"
        )

  elif model_type == ModelType.MIXED:
    log("mixed integration orders: transforming series")

    transformed_series = []

    for i in range(len(series_list)):
      series = series_list[i]
      order = series_orders[i].order

      if order == 0:
        log(f"series {i}: I(0) → remains at levels")
        transformed_series.append(series)

      elif order == 1:
        log(f"series {i}: I(1) → first difference")
        diff1 = np.diff(series)
        transformed_series.append(diff1)

      elif order == 2:
        log(f"series {i}: I(2) → second difference")
        diff1 = np.diff(series)
        diff2 = np.diff(diff1)
        transformed_series.append(diff2)

      else:
        log(f"[WARNING] series {i}: unsupported order {order}, using levels")
        transformed_series.append(series)

    try:
      regression_result = build_mixed_regression(transformed_series, series_orders)

      return ModelResults(regression = regression_result)
      
    except Exception as e:
      log(f"[ERROR] Mixed regression failed: {e}")
      return ModelResults(
        error_message = f"Mixed regression failed: {str(e)}"
      )

def _check_cointegration(
    series_list: list[np.ndarray],
    regression: str
) -> CointegrationResult:
  n_series = len(series_list)

  if n_series == 2:
    aeg_result: AegTestResult = aeg_test(series_list, regression = regression)
    is_cointegrated = aeg_result.p_value < 0.05

    log(f"AEG: p = {aeg_result.p_value:.4f}, coint = {is_cointegrated}")

    return CointegrationResult(
      test_type = CointegrationTestType.AEG,
      n_series = n_series,
      is_cointegrated = is_cointegrated,
      aeg_result = aeg_result
    )
  else:
    johansen_result: JohansenTestResult = johansen_test(series_list, regression = regression)

    num_coint = 0
    for i in range(len(johansen_result.lr1)):
      if johansen_result.lr1[i] > johansen_result.cvt[i, 1]:
        num_coint = num_coint + 1

    is_cointegrated = num_coint > 0

    log(f"johansen test -> {num_coint} coint relations")

    return CointegrationResult(
      test_type = CointegrationTestType.JOHANSEN,
      n_series = n_series,
      is_cointegrated = is_cointegrated,
      johansen_eigenvalues = johansen_result.eig.tolist(),
      johansen_trace_stats = johansen_result.lr1.tolist(),
      n_cointegration_relations = num_coint
    )

def _create_transformation_info(
  series_orders: list[SeriesOrder],
  variable_names: list[str]
) -> list[TransformationInfo]:
  transformations = []
  
  for i in range(len(series_orders)):
    order = series_orders[i].order
    
    if order == 0:
      transformation = TransformationType.NONE
    elif order == 1:
      transformation = TransformationType.FIRST_DIFFERENCE
    elif order == 2:
      transformation = TransformationType.SECOND_DIFFERENCE
    else:
      transformation = TransformationType.NONE
    
    transformations.append(TransformationInfo(
      series_index = i,
      variable_name = variable_names[i],
      original_order = order,
      transformation = transformation
    ))
  
  return transformations 

def _clean_nans(obj):
  if isinstance(obj, float) and (math.isnan(obj) or math.isinf(obj)):
    return None
  if isinstance(obj, Enum):
    return obj.value
  if isinstance(obj, dict):
    return {k: _clean_nans(v) for k, v in obj.items()}
  if isinstance(obj, list):
    return [_clean_nans(v) for v in obj]
  return obj