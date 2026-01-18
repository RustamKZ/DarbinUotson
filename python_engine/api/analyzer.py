import json
import sys
import numpy as np
from dataclasses import asdict
from typing import Optional
from algorithms.integration import determine_integration_order, log
from algorithms.cointegration_tests import aeg_test, johansen_test
from models.responses import (
  SeriesOrder,
  AnalysisResult,
  ModelType,
  CointegrationResult,
  CointegrationTestType,
  ModelResults,
  IntegrationOrderResult
)

def analyze_time_series(input_json: str) -> str:
  # input_json: JSON string {"series": [[1,2,3], [4,5,6], ...]}
  # it's a List<DoubleArray> that we get from the UI
  input_data = json.loads(input_json)

  series_list = []
  for s in input_data["series"]:
    series_list.append(np.array(s))

  # get all info from stationarity tests
  series_orders: list[SeriesOrder] = _analyze_series_orders(series_list)

  # get one of three possible model types: all with I(0), all with I(1) or mixed
  model_type: ModelType = _decide_model_type(series_orders)
  log(f"model type: {model_type.value}")

  # build model based on [model_type]
  model_results = _build_model(series_list, series_orders, model_type)

  result = AnalysisResult(
    series_count = len(series_list),
    series_orders = series_orders,
    model_type = model_type.value,
    model_results = asdict(model_results) if model_results else None
  )

  return json.dumps(asdict(result), default = str)



# get integration order
def _analyze_series_orders(series_list: list[np.ndarray]) -> list[SeriesOrder]:
  series_orders = []
  i = 0

  for series in series_list:
    log(f"\nseries {i + 1}")
    i += 1

    # TODO: implement STL decomposition and create statement tree.
    # need to know if there's a trend.
    # if has trend: kpss_regression = "ct" and za_regression = "ct"
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

  # fallback
  return ModelType.MIXED

def _check_cointegration(
    series_list: list[np.ndarray],
    regression: str
) -> CointegrationResult:
  n_series = len(series_list)

  if n_series == 2:
    aeg_result = aeg_test(series_list, regression = regression)
    is_cointegrated = aeg_result.p_value < 0.05

    log(f"AEG: p = {aeg_result.p_value:.4f}, coint = {is_cointegrated}")

    return CointegrationResult(
      test_type = CointegrationTestType.AEG,
      n_series = n_series,
      is_cointegrated = is_cointegrated,
      aeg_result = aeg_result
    )

  else:
    johansen_result = johansen_test(series_list, regression = regression)

    num_coint = 0
    for i in range(len(johansen_result.lr1)):
      # compare with 5% accuracy
      if johansen_result.lr1[i] > johansen_result.cvt[i, 1]:
        num_coint += 1

    is_cointegrated = num_coint > 0

    log(f"johansen test -> {num_coint} coint relations")

    return CointegrationResult(
      test_type = CointegrationTestType.JOHANSEN,
      n_series = n_series,
      is_cointegrated = is_cointegrated,
      johansen_eigenvalues = johansen_result.eig.tolist(),
      johansen_trace_stats = johansen_result.lr1.tolist(),
      n_cointegration_relations = n_coint
    )

def _build_model(
    series_list: list[np.ndarray],
    series_orders: list[SeriesOrder],
    model_type: ModelType
) -> Optional[ModelResults]:
  if model_type == ModelType.FULL_STATIONARY:
    # TODO: implement regression on levels
    return None
  elif model_type == ModelType.FULL_NON_STATIONARY:
    coint_regression = "c"
    coint_result = _check_cointegration(series_list, coint_regression)
    return ModelResults(cointegration = coint_result)

  elif model_type == ModelType.MIXED:
    # TODO: implement logic for mixed integration orders case
    return None
