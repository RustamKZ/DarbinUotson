import json
import sys
import numpy as np
from dataclasses import asdict
from typing import Optional
from statsmodels.tsa.vector_ar.vecm import JohansenTestResult
from algorithms.integration import determine_integration_order, log
from algorithms.cointegration_tests import aeg_test, johansen_test
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
  AegTestResult
)
from models.domain import (
  PreparedData,
  PeriodAnalysis,
  PeriodData
)


def analyze_time_series(input_json: str) -> str:
  # input_json: JSON string {"series": [[1,2,3], [4,5,6], ...]}
  input_data = json.loads(input_json)

  series_list = []
  for s in input_data["series"]:
    series_list.append(np.array(s))

  # get all info from stationarity tests
  series_orders = _analyze_series_orders(series_list)

  # get one of three possible model types
  model_type = _decide_model_type(series_orders)
  log(f"model type: {model_type.value}")

  # checking structural breaks existence
  prepared_data = _prepare_data(series_list, series_orders, model_type)

  # build model based on prepared data
  model_results = _build_model(prepared_data)

  result = AnalysisResult(
    series_count = len(series_list),
    series_orders = series_orders,
    model_type = model_type.value,
    model_results = asdict(model_results) if model_results else None,
    has_structural_break = prepared_data.has_structural_break,
    structural_breaks = prepared_data.structural_breaks
  )

  return json.dumps(asdict(result), default = str)


def _analyze_series_orders(series_list: list[np.ndarray]) -> list[SeriesOrder]:
  series_orders = []
  i = 0

  for series in series_list:
    log(f"\nseries {i + 1}")
    i = i + 1

    # TODO: implement STL decomposition and create statement tree.
    # need to know if there's a trend.
    # if has trend: kpss_regression = "ct" and za_regression = "ct"
    kpss_regression = "c"
    za_regression = "c"

    order_result = determine_integration_order(
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
        structural_break = order_result.structural_break
      )
    )

  return series_orders


def _decide_model_type(series_orders: list[SeriesOrder]) -> ModelType:
  integration_orders = []
  for s in series_orders:
    integration_orders.append(s.order)

  # check if everything is I(0)
  all_stationary = True
  for order in integration_orders:
    if order != 0:
      all_stationary = False
      break

  if all_stationary:
    return ModelType.FULL_STATIONARY

  # check if everything is I(1)
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
  model_type = prepared_data.model_type

  if model_type == ModelType.FULL_STATIONARY:
    log("full stationary: regression on levels")
    # TODO: implement regression on levels
    return None
  elif model_type == ModelType.FULL_NON_STATIONARY:
    log("full non-stationary: testing cointegration")
    # TODO: depends on STL decomposition result
    coint_regression = "c"
    coint_result = _check_cointegration(series_list, coint_regression)

    if coint_result.is_cointegrated:
      log("cointegration found: ECM")
      # TODO: implement ECM model
    else:
      log("no cointegration: VAR on differences")
      # TODO: implement VAR model

    return ModelResults(cointegration = coint_result)
  elif model_type == ModelType.MIXED:
    log("mixed integration orders")
    # TODO: implement mixed case logic
    return None


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
