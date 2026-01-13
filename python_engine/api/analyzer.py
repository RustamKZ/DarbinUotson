import json
import sys
import numpy as np
from dataclasses import asdict
from algorithms.integration import determine_integration_order, log
from models.responses import (
  SeriesOrder,
  AnalysisResult,
  ModelType,
  IntegrationOrderResult
)

def analyze_time_series(input_json: str) -> str:
  # input_json: JSON string {"series": [[1,2,3], [4,5,6], ...]}
  # it's a List<DoubleArray> that we get from the UI
  input_data = json.loads(input_json)

  series_list = []
  for s in input_data["series"]:
    series_list.append(np.array(s))

  series_orders = []
  i = 0
  for series in series_list:
    log(f"\nseries {i+1}")
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

    # struct that will be sent to the server in JSON,
    # consists of data that we want to show in the app UI
    series_orders.append(
      SeriesOrder(
        order = order_result.order,
        has_conflict = order_result.has_conflict,
        adf = order_result.adf_result,
        kpss = order_result.kpss_result,
        structural_break = order_result.structural_break
      )
    )

  # decision point
  model_type = decide_model_type(series_orders)
  log(f"\nmodel type: {model_type.value}")

  # TODO: build model based on [model_type]
  model_results = None
  if model_type == ModelType.FULL_STATIONARY:
    model_results = None
  elif model_type == ModelType.FULL_NON_STATIONARY:
    model_results = None
  elif model_type == ModelType.MIXED:
    model_results = None


  result = AnalysisResult(
    series_count = len(series_list),
    series_orders = series_orders,
    model_type = model_type.value,
    model_results = model_results
  )

  return json.dumps(asdict(result), default=str)


def decide_model_type(series_orders: list[SeriesOrder]) -> ModelType:
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
