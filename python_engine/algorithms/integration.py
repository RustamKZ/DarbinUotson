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
  za_result = None

  for i in range(max_order + 1):
    log(f"----> I({i})")
    log(f"data length: {len(current_data)}")

    adf = adf_test(current_data)
    kpss = kpss_test(current_data, regression = kpss_regression)

    adf_stationary = adf.is_stationary
    kpss_stationary = kpss.is_stationary

    if adf_stationary and kpss_stationary:
      log(f"case 1: both stationary I({i})")
      return IntegrationOrderResult(
        order = i,
        adf_result = adf,
        kpss_result = kpss,
        has_conflict = False,
        za_result = za_result
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
          has_conflict = False,
          za_result = za_result
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
            has_conflict = True,
            za_result = za_result
          )

      za = zivot_andrews_test(current_data, trend=za_regression)
      za_result = za

      if za.is_stationary:
        log(f"ZA: stat with break at {za.breakpoint}")
        return IntegrationOrderResult(
          order = i,
          adf_result = adf,
          kpss_result = kpss,
          has_conflict = False,
          structural_break = za.breakpoint if za.breakpoint else None,
          za_result = za_result
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
            structural_break = za.breakpoint if za.breakpoint else None,
            za_result = za_result
          )

    else:
      log(f"case 4: adf stat, kpss non-stat")

      if len(current_data) < MIN_SAMPLE_ZA:
        log(f"ds < {MIN_SAMPLE_ZA}, skip ZA")
        return IntegrationOrderResult(
          order = i,
          adf_result = adf,
          kpss_result = kpss,
          has_conflict = True,
          za_result = za_result
        )

      za = zivot_andrews_test(current_data, trend=za_regression)
      za_result = za

      log(f"ZA: break at {za.breakpoint}")
      return IntegrationOrderResult(
        order = i,
        adf_result = adf,
        kpss_result = kpss,
        has_conflict = not za.is_stationary,
        structural_break = za.breakpoint if za.breakpoint else None,
        za_result = za_result
      )

  return IntegrationOrderResult(
    order = max_order,
    adf_result = adf,
    kpss_result = kpss,
    has_conflict = False,
    za_result = za_result
  )
