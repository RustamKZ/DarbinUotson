import numpy as np
from scipy import stats
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

  # R² for the first equation (target)
  ss_res = np.sum(y_resid ** 2)
  y_diff = diff_data[0][optimal_lag:]
  ss_tot = np.sum((y_diff - np.mean(y_diff)) ** 2)

  if ss_tot > 0:
    r_squared = 1.0 - ss_res / ss_tot
  else:
    r_squared = 0.0

  n = len(y_resid)
  k = len(coefficients)

  if n - k - 1 > 0:
    adj_r_squared = 1.0 - (1.0 - r_squared) * (n - 1) / (n - k - 1)
  else:
    adj_r_squared = r_squared

  if k > 0 and (1.0 - r_squared) > 0 and (n - k - 1) > 0:
    f_stat = (r_squared / k) / ((1.0 - r_squared) / (n - k - 1))
  else:
    f_stat = 0.0

  if f_stat > 0 and k > 0 and (n - k - 1) > 0:
    f_pvalue = 1.0 - stats.f.cdf(f_stat, k, n - k - 1)
  else:
    f_pvalue = 1.0

  return RegressionResult(
    coefficients = coefficients,
    std_errors = std_errors,
    t_values = t_values,
    p_values = p_values,
    r_squared = float(r_squared),
    adj_r_squared = float(adj_r_squared),
    f_statistic = float(f_stat),
    f_pvalue = float(f_pvalue),
    durbin_watson = dw_result,
    n_obs = n,
    has_lags = True
  )