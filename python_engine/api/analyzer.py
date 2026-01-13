"""
Args:
  input_json: {"y": [...], "x": [...]}

Returns:
  JSON string with results
"""

import json
import sys
import numpy as np
from dataclasses import asdict
from algorithms.integration import determine_integration_order

def analyze_time_series(input_json: str) -> str:
  input_data = json.loads(input_json)

  y = np.array(input_data['y'])
  x = np.array(input_data.get('x')) if 'x' in input_data else None

  y_order = determine_integration_order(y)

  result = {
    'y': {
      'order': y_order.order,
      'has_conflict': y_order.has_conflict,
      'adf': asdict(y_order.adf_result),
      'kpss': asdict(y_order.kpss_result),
    }
  }

  if x is not None:
    x_order = determine_integration_order(x)
    result['x'] = {
      'order': x_order.order,
      'has_conflict': x_order.has_conflict,
      'adf': asdict(x_order.adf_result),
      'kpss': asdict(x_order.kpss_result),
    }

  return json.dumps(result, default=str)
