import numpy as np
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult, SeriesOrder


def build_mixed_regression(
  transformed_series: list[np.ndarray],
  series_orders: list[SeriesOrder]
) -> RegressionResult:
  from statsmodels.regression.linear_model import OLS
  from statsmodels.stats.stattools import durbin_watson

  log("building regression on mixed data")

  min_length = len(transformed_series[0])
  for series in transformed_series:
    if len(series) < min_length:
      min_length = len(series)

  log(f"aligned series length: {min_length}")

  aligned_series = []
  for series in transformed_series:
    aligned = series[-min_length:]
    aligned_series.append(aligned)

  y = aligned_series[0]

  X_columns = []
  i = 1
  while i < len(aligned_series):
    X_columns.append(aligned_series[i])
    i += 1

  X = np.column_stack(X_columns)

  result = _fit_mixed_simple(y, X)

  log(f"mixed regression (simple): R^2 = {result.rsquared:.4f}")

  dw_stat = durbin_watson(result.resid)
  has_autocorr = False
  if dw_stat < 1.5 or dw_stat > 2.5:
    has_autocorr = True

  log(f"Durbin-Watson: {dw_stat:.4f}")

  if has_autocorr:
    log("[WARNING] autocorrelation detected, selecting optimal lags via AIC")
    optimal_lags = _select_optimal_lags_for_mixed(y, X, max_lags=5)
    result = _fit_mixed_with_lags(y, X, optimal_lags)

    dw_stat_with_lags = durbin_watson(result.resid)
    has_autocorr_final = False
    if dw_stat_with_lags < 1.5 or dw_stat_with_lags > 2.5:
      has_autocorr_final = True

    log(f"Durbin-Watson (with lags): {dw_stat_with_lags:.4f}")

    if has_autocorr_final:
      log("[WARNING] autocorrelation still present, applying Newey-West")
      result = _fit_mixed_newey_west(y, X)
      dw_stat = durbin_watson(result.resid)
      has_autocorr = False
      if dw_stat < 1.5 or dw_stat > 2.5:
        has_autocorr = True
    else:
      dw_stat = dw_stat_with_lags
      has_autocorr = has_autocorr_final
  else:
    log("no autocorrelation detected")

  dw_result = DurbinWatsonResult(
    statistic = float(dw_stat),
    has_autocorrelation = has_autocorr
  )

  coefficients = []
  std_errors = []
  t_values = []
  p_values = []

  for val in result.params:
    coefficients.append(float(val))

  for val in result.bse:
    std_errors.append(float(val))

  for val in result.tvalues:
    t_values.append(float(val))

  for val in result.pvalues:
    p_values.append(float(val))

  has_lags = "has_lags" in dir(result) and result.has_lags
  uses_nw = "uses_newey_west" in dir(result) and result.uses_newey_west

  return RegressionResult(
    coefficients = coefficients,
    std_errors = std_errors,
    t_values = t_values,
    p_values = p_values,
    r_squared = float(result.rsquared),
    adj_r_squared = float(result.rsquared_adj),
    f_statistic = float(result.fvalue),
    f_pvalue = float(result.f_pvalue),
    durbin_watson = dw_result,
    n_obs = int(result.nobs),
    has_lags = has_lags,
    uses_newey_west = uses_nw
  )


def _fit_mixed_simple(y: np.ndarray, X: np.ndarray):
  from statsmodels.regression.linear_model import OLS
  import statsmodels.api as sm

  n = len(y)
  constant = np.ones(n)
  X_with_const = np.column_stack([constant, X])

  model = OLS(y, X_with_const)
  result = model.fit()

  result.has_lags = False
  result.uses_newey_west = False

  return result


def _select_optimal_lags_for_mixed(
  y: np.ndarray,
  X: np.ndarray,
  max_lags: int
) -> int:
  import statsmodels.api as sm
  from statsmodels.regression.linear_model import OLS

  best_aic = float('inf')
  best_lags = 1

  for lags in range(1, max_lags + 1):
    n = len(y)

    if n <= lags + 10:
      continue

    try:
      all_variables = []

      for col_idx in range(X.shape[1]):
        all_variables.append(X[:, col_idx])

      for lag in range(1, lags + 1):
        y_lag = np.roll(y, lag)
        all_variables.append(y_lag)

      for col_idx in range(X.shape[1]):
        for lag in range(1, lags + 1):
          X_lag = np.roll(X[:, col_idx], lag)
          all_variables.append(X_lag)

      y_clean = y[lags:]
      X_clean_list = []
      for var in all_variables:
        X_clean_list.append(var[lags:])

      X_combined = np.column_stack(X_clean_list)
      X_combined = sm.add_constant(X_combined)

      model = OLS(y_clean, X_combined)
      result = model.fit()

      n_obs = result.nobs
      k = len(result.params)
      rss = result.ssr
      aic = n_obs * np.log(rss / n_obs) + 2 * k

      if aic < best_aic:
        best_aic = aic
        best_lags = lags

    except Exception as e:
      continue

  log(f"optimal lags for mixed: {best_lags} (AIC={best_aic:.2f})")
  return best_lags


def _fit_mixed_with_lags(
  y: np.ndarray,
  X: np.ndarray,
  max_lags: int
):
  import statsmodels.api as sm
  from statsmodels.regression.linear_model import OLS

  n = len(y)

  if n <= max_lags + 10:
    log(f"[WARNING] insufficient data for {max_lags} lags, using simple OLS")
    return _fit_mixed_simple(y, X)

  all_variables = []

  for col_idx in range(X.shape[1]):
    all_variables.append(X[:, col_idx])

  for lag in range(1, max_lags + 1):
    y_lag = np.roll(y, lag)
    all_variables.append(y_lag)

  for col_idx in range(X.shape[1]):
    for lag in range(1, max_lags + 1):
      X_lag = np.roll(X[:, col_idx], lag)
      all_variables.append(X_lag)

  y_clean = y[max_lags:]
  X_clean_list = []
  for var in all_variables:
    X_clean_list.append(var[max_lags:])

  X_combined = np.column_stack(X_clean_list)
  X_combined = sm.add_constant(X_combined)

  model = OLS(y_clean, X_combined)
  result = model.fit()

  log(f"mixed with lags ({max_lags}): R^2 = {result.rsquared:.4f}")

  result.has_lags = True
  result.uses_newey_west = False

  return result


def _fit_mixed_newey_west(y: np.ndarray, X: np.ndarray):
  import statsmodels.api as sm
  from statsmodels.regression.linear_model import OLS

  n = len(y)
  constant = np.ones(n)
  X_with_const = np.column_stack([constant, X])

  model = OLS(y, X_with_const)
  result = model.fit(cov_type = "HAC", cov_kwds = {"maxlags": None})

  log(f"mixed Newey-West: R^2 = {result.rsquared:.4f}")

  result.has_lags = False
  result.uses_newey_west = True

  return result
