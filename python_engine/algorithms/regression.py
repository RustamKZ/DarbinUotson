import sys
import numpy as np
import statsmodels.api as sm
from statsmodels.stats.stattools import durbin_watson
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult, CoefficientInfo
from statsmodels.regression.linear_model import RegressionResultsWrapper


def ols_regression(
    y: np.ndarray,
    X_list: list[np.ndarray],
    add_constant: bool = True,
    auto_select_lags: bool = True,
    max_lags_search: int = 5,
    variable_names: list[str] = None
) -> RegressionResult:

  if variable_names is None:
    target_name = "Y"
    predictor_names = [f"X{i+1}" for i in range(len(X_list))]
  else:
    target_name = variable_names[0]
    predictor_names = variable_names[1:]

  result = _fit_ols(y, X_list, add_constant, predictor_names)

  if result.durbin_watson.has_autocorrelation == False:
    return result

  if auto_select_lags:
    optimal_lags = _select_optimal_lags(y, X_list, add_constant, max_lags_search)
  else:
    optimal_lags = 2
    log(f"using default {optimal_lags} lags")

  result_with_lags = _fit_ols_with_lags(y, X_list, add_constant, optimal_lags, target_name, predictor_names)

  if result_with_lags.durbin_watson.has_autocorrelation == False:
    return result_with_lags

  result_nw = _fit_ols_newey_west(y, X_list, add_constant, predictor_names)

  return result_nw


# AIC autolag
def _select_optimal_lags(
  y: np.ndarray,
  X_list: list[np.ndarray],
  add_constant: bool,
  max_lags: int
) -> int:

  best_aic = float('inf')
  best_lags = 1

  for lags in range(1, max_lags + 1):
    if len(y) <= lags + 10:
      continue

    try:
      all_variables = []

      for X in X_list:
        all_variables.append(X)

      for lag in range(1, lags + 1):
        y_lag = np.roll(y, lag)
        all_variables.append(y_lag)

      for X in X_list:
        for lag in range(1, lags + 1):
          X_lag = np.roll(X, lag)
          all_variables.append(X_lag)

      y_clean = y[lags:]
      X_clean_list = []
      for var in all_variables:
        X_clean_list.append(var[lags:])

      X_combined = _prepare_X_matrix(X_clean_list)

      if add_constant:
        X_combined = sm.add_constant(X_combined)

      model = sm.OLS(y_clean, X_combined)
      results = model.fit()

      n = results.nobs
      k = len(results.params)
      rss = results.ssr
      aic = n * np.log(rss / n) + 2 * k

      if aic < best_aic:
        best_aic = aic
        best_lags = lags

    except Exception as e:
      continue

  log(f"optimal lags: {best_lags} (AIC={best_aic:.2f})")
  return best_lags


def _fit_ols(
    y: np.ndarray,
    X_list: list[np.ndarray],
    add_constant: bool,
    predictor_names: list[str]
) -> RegressionResult:

  X = _prepare_X_matrix(X_list)

  if add_constant:
    X = sm.add_constant(X)

  names = _build_names_simple(add_constant, predictor_names)

  model = sm.OLS(y, X)
  results = model.fit()

  dw_stat = durbin_watson(results.resid)
  has_autocorr = _check_autocorrelation(dw_stat)

  log(f"ols: r_squared = {results.rsquared:.4f}, dw = {dw_stat:.4f}")

  return _create_regression_result(
    results = results,
    names = names,
    dw_stat = dw_stat,
    has_autocorr = has_autocorr,
    has_lags = False,
    uses_newey_west = False
  )


# ols with lagged Y and X vars
def _fit_ols_with_lags(
    y: np.ndarray,
    X_list: list[np.ndarray],
    add_constant: bool,
    max_lags: int,
    target_name: str,
    predictor_names: list[str]
) -> RegressionResult:
  n = len(y)

  if n <= max_lags + 10:
    log(f"warning: insufficient data for {max_lags} lags (n={n}), using simple OLS")
    return _fit_ols(y, X_list, add_constant, predictor_names)

  all_variables = []

  for X in X_list:
    all_variables.append(X)

  for lag in range(1, max_lags + 1):
    y_lag = _create_lag(y, lag)
    all_variables.append(y_lag)

  for X in X_list:
    for lag in range(1, max_lags + 1):
      X_lag = _create_lag(X, lag)
      all_variables.append(X_lag)

  y_clean = y[max_lags:]
  X_clean_list = []
  for var in all_variables:
    X_clean_list.append(var[max_lags:])

  X_combined = _prepare_X_matrix(X_clean_list)

  if add_constant:
    X_combined = sm.add_constant(X_combined)

  names = _build_names_with_lags(add_constant, target_name, predictor_names, max_lags)

  model = sm.OLS(y_clean, X_combined)
  results = model.fit()

  dw_stat = durbin_watson(results.resid)
  has_autocorr = _check_autocorrelation(dw_stat)

  log(f"ols with lags: r_squared = {results.rsquared:.4f}, dw = {dw_stat:.4f}")

  return _create_regression_result(
    results = results,
    names = names,
    dw_stat = dw_stat,
    has_autocorr = has_autocorr,
    has_lags = True,
    uses_newey_west = False
  )


def _fit_ols_newey_west(
    y: np.ndarray,
    X_list: list[np.ndarray],
    add_constant: bool,
    predictor_names: list[str]
) -> RegressionResult:
  X = _prepare_X_matrix(X_list)

  if add_constant:
    X = sm.add_constant(X)

  names = _build_names_simple(add_constant, predictor_names)

  model = sm.OLS(y, X)
  results = model.fit(cov_type = "HAC", cov_kwds = {"maxlags": None})

  dw_stat = durbin_watson(results.resid)
  has_autocorr = _check_autocorrelation(dw_stat)

  log(f"ols newey-west: r_squared = {results.rsquared:.4f}, dw = {dw_stat:.4f}")

  return _create_regression_result(
    results = results,
    names = names,
    dw_stat = dw_stat,
    has_autocorr = has_autocorr,
    has_lags = False,
    uses_newey_west = True
  )


# ===== HELPER METHODS =====

def _prepare_X_matrix(X_list: list[np.ndarray]) -> np.ndarray:
  if len(X_list) == 1:
    return X_list[0].reshape(-1, 1)
  else:
    return np.column_stack(X_list)


def _create_lag(data: np.ndarray, lag: int) -> np.ndarray:
  return np.roll(data, lag)


def _check_autocorrelation(dw_stat: float) -> bool:
  if dw_stat < 1.5:
    return True
  if dw_stat > 2.5:
    return True
  return False


def _build_names_simple(add_constant: bool, predictor_names: list[str]) -> list[str]:
  names = []
  if add_constant:
    names.append("const")
  for name in predictor_names:
    names.append(name)
  return names


def _build_names_with_lags(
    add_constant: bool,
    target_name: str,
    predictor_names: list[str],
    max_lags: int
) -> list[str]:
  names = []
  if add_constant:
    names.append("const")

  for name in predictor_names:
    names.append(name)

  for lag in range(1, max_lags + 1):
    names.append(f"lag{lag}_{target_name}")

  for name in predictor_names:
    for lag in range(1, max_lags + 1):
      names.append(f"lag{lag}_{name}")

  return names


def _create_regression_result(
    results: RegressionResultsWrapper,
    names: list[str],
    dw_stat: float,
    has_autocorr: bool,
    has_lags: bool,
    uses_newey_west: bool
) -> RegressionResult:

  coeffs = []
  for i in range(len(results.params)):
    name = names[i] if i < len(names) else f"coef_{i}"
    coeffs.append(CoefficientInfo(
      name = name,
      value = float(results.params[i]),
      std_error = float(results.bse[i]),
      t_value = float(results.tvalues[i]),
      p_value = float(results.pvalues[i]),
      is_significant = float(results.pvalues[i]) < 0.05
    ))

  return RegressionResult(
    coefficients = coeffs,
    r_squared = float(results.rsquared),
    adj_r_squared = float(results.rsquared_adj),
    f_statistic = float(results.fvalue),
    f_pvalue = float(results.f_pvalue),
    durbin_watson = DurbinWatsonResult(
      statistic = float(dw_stat),
      has_autocorrelation = has_autocorr
    ),
    n_obs = int(results.nobs),
    has_lags = has_lags,
    uses_newey_west = uses_newey_west
  )