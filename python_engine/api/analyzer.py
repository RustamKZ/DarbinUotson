import json
import sys
import numpy as np
from dataclasses import asdict
from algorithms.integration import determine_integration_order

def analyze_time_series(input_json: str) -> str:
  # input_json: JSON string {"series": [[1,2,3], [4,5,6], ...]}
  input_data = json.loads(input_json)
  series_list = [np.array(s) for s in input_data['series']]

  orders = []
  for i, series in enumerate(series_list):
    print(f"\nseries {i+1}", file=sys.stderr)
    order_result = determine_integration_order(series)
    orders.append({
      'order': order_result.order,
      'has_conflict': order_result.has_conflict,
      'adf': asdict(order_result.adf_result),
      'kpss': asdict(order_result.kpss_result),
    })

  # TODO: cointegration matrix and regression results

  result = {
    'series_count': len(series_list),
    'series_orders': orders,
  }

  return json.dumps(result, default=str)
