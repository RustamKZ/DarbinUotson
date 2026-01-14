import sys
import numpy as np
from algorithms.stationarity_tests import adf_test, kpss_test, zivot_andrews_test
from models.responses import IntegrationOrderResult

MIN_SAMPLE_ZA = 20

def log(msg):
  print(msg, file=sys.stderr)

def determine_integration_order(
  data: np.ndarray,
  max_order: int = 2,
  kpss_regression: str = "c",
  za_regression: str = "c"
) -> IntegrationOrderResult:
  current_data = data.copy()

  for i in range(max_order + 1):
    log(f"----> I({i})")
    log(f"data length: {len(current_data)}")

    adf = adf_test(current_data)
    kpss = kpss_test(current_data)
    adf_stationary = adf.is_stationary
    kpss_stationary = kpss.p_value > 0.05

    if adf_stationary and kpss_stationary:
      log(f"case 1: both stationary I({i})")
      return IntegrationOrderResult(
        order = i,
        adf_result = adf,
        kpss_result = kpss,
        has_conflict = False
      )

    elif not adf_stationary and not kpss_stationary:
      log(f"case 2: both non-stationary")
      if i < max_order:
        log(f"take diff I({i}) -> I({i + 1})")
        current_data = np.diff(current_data)
        continue
      else:
        log(f"reached max I({max_order})")
        return IntegrationOrderResult(
          order = max_order,
          adf_result = adf,
          kpss_result = kpss,
          has_conflict = False
        )

    elif not adf_stationary and kpss_stationary:
      log(f"case 3: adf non-stat, kpss stat")
      if len(current_data) < MIN_SAMPLE_ZA:
        log(f"ds < {MIN_SAMPLE_ZA}, skip ZA")
        if i < max_order:
          log(f"take diff")
          current_data = np.diff(current_data)
          continue
        else:
          return IntegrationOrderResult(
            order = max_order,
            adf_result = adf,
            kpss_result = kpss,
            has_conflict = True
          )
      za = zivot_andrews_test(current_data, trend=za_regression)
      if za.is_stationary:
        log(f"ZA: stat with break at {za.breakpoint}")
        return IntegrationOrderResult(
          order = i,
          adf_result = adf,
          kpss_result = kpss,
          has_conflict = False,
          structural_break = za.breakpoint if za.breakpoint else None
        )
      else:
        if i < max_order:
          log(f"ZA: non-stat, take diff")
          current_data = np.diff(current_data)
          continue
        else:
          return IntegrationOrderResult(
            order = max_order,
            adf_result = adf,
            kpss_result = kpss,
            has_conflict = True,
            structural_break = za.breakpoint if za.breakpoint else None
          )

    else:
      log(f"case 4: adf stat, kpss non-stat")
      if len(current_data) < MIN_SAMPLE_ZA:
        log(f"ds < {MIN_SAMPLE_ZA}, skip ZA")
        return IntegrationOrderResult(
          order = i,
          adf_result = adf,
          kpss_result = kpss,
          has_conflict = True
        )
      za = zivot_andrews_test(current_data, trend=za_regression)
      log(f"ZA: break at {za.breakpoint}")
      return IntegrationOrderResult(
        order = i,
        adf_result = adf,
        kpss_result = kpss,
        has_conflict = not za.is_stationary
      )

  return IntegrationOrderResult(
    order = max_order,
    adf_result = adf,
    kpss_result = kpss,
    has_conflict = False
  )
