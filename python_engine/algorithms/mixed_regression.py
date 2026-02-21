import numpy as np
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult, CoefficientInfo, SeriesOrder


def build_mixed_regression(
  transformed_series: list[np.ndarray],
  series_orders: list[SeriesOrder],
  variable_names: list[str] = None
) -> RegressionResult:
  from statsmodels.regression.linear_model import OLS
  from statsmodels.stats.stattools import durbin_watson

  log("building regression on mixed data")

  if variable_names is None:
    target_name = "Y"
    predictor_names = [f"X{i}" for i in range(1, len(transformed_series))]
  else:
    target_name = variable_names[0]
    predictor_names = variable_names[1:]

  # build display names with Δ prefix for differenced variables
  display_names = []
  target_order = series_orders[0].order
  if target_order == 1:
    target_display = f"Δ{target_name}"
  elif target_order == 2:
    target_display = f"Δ²{target_name}"
  else:
    target_display = target_name

  for i in range(1, len(series_orders)):
    base = predictor_names[i - 1] if i - 1 < len(predictor_names) else f"X{i}"
    order = series_orders[i].order
    if order == 1:
      display_names.append(f"Δ{base}")
    elif order == 2:
      display_names.append(f"Δ²{base}")
    else:
      display_names.append(base)

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

  has_lags = False
  uses_nw = False
  optimal_lags = 0

  log(f"Durbin-Watson: {dw_stat:.4f}")

  if has_autocorr:
    log("[WARNING] autocorrelation detected, selecting optimal lags via AIC")
    optimal_lags = _select_optimal_lags_for_mixed(y, X, max_lags=5)
    result = _fit_mixed_with_lags(y, X, optimal_lags)
    has_lags = True

    dw_stat = durbin_watson(result.resid)
    has_autocorr = False
    if dw_stat < 1.5 or dw_stat > 2.5:
      has_autocorr = True

    log(f"Durbin-Watson (with lags): {dw_stat:.4f}")

    if has_autocorr:
      log("[WARNING] autocorrelation still present, applying Newey-West")
      result = _fit_mixed_newey_west(y, X)
      has_lags = False
      uses_nw = True
      dw_stat = durbin_watson(result.resid)
      has_autocorr = False
      if dw_stat < 1.5 or dw_stat > 2.5:
        has_autocorr = True
  else:
    log("no autocorrelation detected")

  # build names
  if has_lags:
    names = _build_names_with_lags(target_display, display_names, optimal_lags)
  else:
    names = ["const"] + display_names

  coeffs = []
  for i in range(len(result.params)):
    name = names[i] if i < len(names) else f"coef_{i}"
    coeffs.append(CoefficientInfo(
      name = name,
      value = float(result.params[i]),
      std_error = float(result.bse[i]),
      t_value = float(result.tvalues[i]),
      p_value = float(result.pvalues[i]),
      is_significant = float(result.pvalues[i]) < 0.05
    ))

  return RegressionResult(
    coefficients = coeffs,
    r_squared = float(result.rsquared),
    adj_r_squared = float(result.rsquared_adj),
    f_statistic = float(result.fvalue),
    f_pvalue = float(result.f_pvalue),
    durbin_watson = DurbinWatsonResult(
      statistic = float(dw_stat),
      has_autocorrelation = has_autocorr
    ),
    n_obs = int(result.nobs),
    has_lags = has_lags,
    uses_newey_west = uses_nw
  )


def _build_names_with_lags(target_name, predictor_names, max_lags):
  names = ["const"]
  for name in predictor_names:
    names.append(name)
  for lag in range(1, max_lags + 1):
    names.append(f"lag{lag}_{target_name}")
  for name in predictor_names:
    for lag in range(1, max_lags + 1):
      names.append(f"lag{lag}_{name}")
  return names


def _fit_mixed_simple(y, X):
  from statsmodels.regression.linear_model import OLS
  n = len(y)
  constant = np.ones(n)
  X_with_const = np.column_stack([constant, X])
  model = OLS(y, X_with_const)
  return model.fit()


def _select_optimal_lags_for_mixed(y, X, max_lags):
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
        all_variables.append(np.roll(y, lag))
      for col_idx in range(X.shape[1]):
        for lag in range(1, lags + 1):
          all_variables.append(np.roll(X[:, col_idx], lag))

      y_clean = y[lags:]
      X_clean_list = [var[lags:] for var in all_variables]
      X_combined = sm.add_constant(np.column_stack(X_clean_list))

      result = OLS(y_clean, X_combined).fit()
      n_obs = result.nobs
      k = len(result.params)
      aic = n_obs * np.log(result.ssr / n_obs) + 2 * k

      if aic < best_aic:
        best_aic = aic
        best_lags = lags
    except Exception:
      continue

  log(f"optimal lags for mixed: {best_lags} (AIC={best_aic:.2f})")
  return best_lags


def _fit_mixed_with_lags(y, X, max_lags):
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
    all_variables.append(np.roll(y, lag))
  for col_idx in range(X.shape[1]):
    for lag in range(1, max_lags + 1):
      all_variables.append(np.roll(X[:, col_idx], lag))

  y_clean = y[max_lags:]
  X_clean_list = [var[max_lags:] for var in all_variables]
  X_combined = sm.add_constant(np.column_stack(X_clean_list))

  result = OLS(y_clean, X_combined).fit()
  log(f"mixed with lags ({max_lags}): R^2 = {result.rsquared:.4f}")
  return result


def _fit_mixed_newey_west(y, X):
  import statsmodels.api as sm
  from statsmodels.regression.linear_model import OLS

  n = len(y)
  X_with_const = np.column_stack([np.ones(n), X])
  result = OLS(y, X_with_const).fit(cov_type="HAC", cov_kwds={"maxlags": None})
  log(f"mixed Newey-West: R^2 = {result.rsquared:.4f}")
  return result