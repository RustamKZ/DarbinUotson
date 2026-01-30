from __future__ import annotations
from dataclasses import dataclass
from typing import Optional
import numpy as np
from models.responses import SeriesOrder, ModelType, PeriodType, StructuralBreak

@dataclass
class PeriodData:
  period_number: int
  start_index: int
  end_index: int
  series_data: list[np.ndarray]
  data_size: int

@dataclass
class PreparedData:
  original_series: list[np.ndarray]
  series_orders: list[SeriesOrder]
  model_type: ModelType
  has_structural_break: bool = False
  structural_breaks: Optional[list[StructuralBreak]] = None
  periods_data: Optional[list[PeriodData]] = None

@dataclass
class PeriodAnalysis:
  period_type: PeriodType
  period_number: int
  series_orders: list[SeriesOrder]
  model_type: ModelType
  data_size: int
