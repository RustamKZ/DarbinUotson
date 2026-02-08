import numpy as np
from algorithms.integration import log
from models.responses import RegressionResult, DurbinWatsonResult
from statsmodels.regression.linear_model import OLS
from statsmodels.stats.stattools import durbin_watson

def build_ecm_model(
    series_list: list[np.ndarray],
    regression: str = "c"
) -> RegressionResult:
  log("building ecm model")

  y = series_list[0]
  n = len(y)

  X_columns = []
  i = 1
  while i < len(series_list):
    X_columns.append(series_list[i])
    i += 1

  X = np.column_stack(X_columns)

  if regression == "ct":
    trend = np.arange(n)
    constant = np.ones(n)
    X_long = np.column_stack([constant, trend, X])
  else:
    constant = np.ones(n)
    X_long = np.column_stack([constant, X])

  model_long = OLS(y, X_long)
  result_long = model_long.fit()

  log(f"long-run equation: R^2 = {result_long.rsquared:.4f}")

  ect = result_long.resid

  dy = np.diff(y)
  dX = np.diff(X, axis = 0)
  ect_lagged = ect[:-1]

  n_short = len(dy)
  constant_short = np.ones(n_short)
  X_short = np.column_stack([constant_short, ect_lagged, dX])

  model_short = OLS(dy, X_short)
  result_short = model_short.fit()

  ect_coef = result_short.params[1]
  ect_pvalue = result_short.pvalues[1]

  log(f"ECT coefficient: {ect_coef:.4f} (p={ect_pvalue:.4f})")

  if ect_coef >= 0:
    log("[WARNING] ect coefficient is positive (should be negative!)")

  dw_stat = durbin_watson(result_short.resid)
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

  for val in result_short.params:
    coefficients.append(float(val))

  for val in result_short.bse:
    std_errors.append(float(val))

  for val in result_short.tvalues:
    t_values.append(float(val))

  for val in result_short.pvalues:
    p_values.append(float(val))

  return RegressionResult(
    coefficients = coefficients,
    std_errors = std_errors,
    t_values = t_values,
    p_values = p_values,
    r_squared = float(result_short.rsquared),
    adj_r_squared = float(result_short.rsquared_adj),
    f_statistic = float(result_short.fvalue),
    f_pvalue = float(result_short.f_pvalue),
    durbin_watson = dw_result,
    n_obs = len(dy)
  )
