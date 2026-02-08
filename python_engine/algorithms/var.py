import numpy as np
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult


def build_var_on_differences(
  series_list: list[np.ndarray],
  maxlags: int = 15
) -> RegressionResult:
  from statsmodels.tsa.api import VAR
  from statsmodels.stats.stattools import durbin_watson

  log("building VAR model on differences")

  diff_data = []
  for series in series_list:
    diff_series = np.diff(series)
    diff_data.append(diff_series)

  data_matrix = np.column_stack(diff_data)

  model = VAR(data_matrix)

  optimal_lag = 1
  try:
    lag_order = model.select_order(maxlags=maxlags)
    optimal_lag = lag_order.aic
    log(f"VAR: selected lag = {optimal_lag} (AIC)")
  except Exception as e:
    log(f"VAR: lag selection failed ({e}), using lag=1")

  result = model.fit(maxlags=optimal_lag)

  log(f"VAR fitted: {len(series_list)} equations, lag={optimal_lag}")

  y_equation = result.params[0, :]
  y_stderr = result.stderr[0, :]
  y_tvalues = result.tvalues[0, :]
  y_pvalues = result.pvalues[0, :]

  y_resid = result.resid[:, 0]
  dw_stat = durbin_watson(y_resid)

  has_autocorr = False
  if dw_stat < 1.5 or dw_stat > 2.5:
    has_autocorr = True

  dw_result = DurbinWatsonResult(
    statistic = float(dw_stat),
    has_autocorrelation = has_autocorr
  )

  coefficients = []
  std_errors = []
  t_values = []
  p_values = []

  for val in y_equation:
    coefficients.append(float(val))

  for val in y_stderr:
    std_errors.append(float(val))

  for val in y_tvalues:
    t_values.append(float(val))

  for val in y_pvalues:
    p_values.append(float(val))

  return RegressionResult(
    coefficients = coefficients,
    std_errors = std_errors,
    t_values = t_values,
    p_values = p_values,
    r_squared = 0.0,
    adj_r_squared = 0.0,
    f_statistic = 0.0,
    f_pvalue = 1.0,
    durbin_watson = dw_result,
    n_obs = len(y_resid),
    has_lags = True
  )
