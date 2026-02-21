import numpy as np
from scipy import stats
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult, CoefficientInfo


def build_var_on_differences(
  series_list: list[np.ndarray],
  maxlags: int = 15,
  variable_names: list[str] = None
) -> RegressionResult:
  from statsmodels.tsa.api import VAR
  from statsmodels.stats.stattools import durbin_watson

  log("building VAR model on differences")

  if variable_names is None:
    all_names = [f"var{i}" for i in range(len(series_list))]
  else:
    all_names = variable_names

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

  # names: const, lag1_Δvar0, lag1_Δvar1, ..., lag2_Δvar0, ...
  names = ["const"]
  for lag in range(1, optimal_lag + 1):
    for vname in all_names:
      names.append(f"lag{lag}_Δ{vname}")

  coeffs = []
  for i in range(len(y_equation)):
    name = names[i] if i < len(names) else f"coef_{i}"
    coeffs.append(CoefficientInfo(
      name = name,
      value = float(y_equation[i]),
      std_error = float(y_stderr[i]),
      t_value = float(y_tvalues[i]),
      p_value = float(y_pvalues[i]),
      is_significant = float(y_pvalues[i]) < 0.05
    ))

  # R² for target equation
  ss_res = np.sum(y_resid ** 2)
  y_diff = diff_data[0][optimal_lag:]
  ss_tot = np.sum((y_diff - np.mean(y_diff)) ** 2)

  if ss_tot > 0:
    r_squared = 1.0 - ss_res / ss_tot
  else:
    r_squared = 0.0

  n = len(y_resid)
  k = len(coeffs)

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
    coefficients = coeffs,
    r_squared = float(r_squared),
    adj_r_squared = float(adj_r_squared),
    f_statistic = float(f_stat),
    f_pvalue = float(f_pvalue),
    durbin_watson = DurbinWatsonResult(
      statistic = float(dw_stat),
      has_autocorrelation = has_autocorr
    ),
    n_obs = n,
    has_lags = True
  )