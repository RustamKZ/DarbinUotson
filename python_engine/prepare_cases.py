"""
Подготовка CSV файлов для 4 кейсов из статьи.
Запуск: python prepare_cases.py

Создаёт в datasets/:
  case1_IDN_OLS.csv       — Индонезия, OLS на уровнях
  case2a_NGA_ECM.csv      — Нигерия, ECM (кумулятивные)
  case2b_IDN_VAR.csv      — Индонезия, VAR (кумулятивные, нет коинтеграции)
  case3_NGA_Mixed.csv     — Нигерия, Mixed (уровни, разные порядки)
"""

import pandas as pd
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATASETS_DIR = os.path.join(SCRIPT_DIR, "datasets")
SOURCE = os.path.join(DATASETS_DIR, "global_climate_health_impact_tracker_2015_2025.csv")

META_COLS = [
    "record_id", "country_code", "country_name", "region", "income_level",
    "date", "year", "month", "week", "latitude", "longitude"
]

df = pd.read_csv(SOURCE)
print(f"Loaded: {len(df)} rows, {df['country_code'].nunique()} countries")


# Case 1: IDN — OLS (все стационарные, уровни)
idn = df[df["country_code"] == "IDN"].copy().reset_index(drop=True)
case1 = idn[META_COLS + ["vector_disease_risk_score", "temperature_celsius", "precipitation_mm"]]
case1.to_csv(os.path.join(DATASETS_DIR, "case1_IDN_OLS.csv"), index=False)
print(f"Case 1 (IDN OLS):   {len(case1)} rows")


# Case 2a: NGA — ECM (кумулятивные, коинтеграция есть)
nga = df[df["country_code"] == "NGA"].copy().reset_index(drop=True)
case2a = nga[META_COLS].copy()
case2a["heat_related_admissions_cumsum"] = nga["heat_related_admissions"].cumsum()
case2a["temperature_celsius_cumsum"] = nga["temperature_celsius"].cumsum()
case2a["heat_wave_days_cumsum"] = nga["heat_wave_days"].cumsum()
case2a.to_csv(os.path.join(DATASETS_DIR, "case2a_NGA_ECM.csv"), index=False)
print(f"Case 2a (NGA ECM):  {len(case2a)} rows")


# Case 2b: IDN — VAR (кумулятивные, коинтеграции нет)
case2b = idn[META_COLS].copy()
case2b["gdp_per_capita_usd_cumsum"] = idn["gdp_per_capita_usd"].cumsum()
case2b["food_security_index_cumsum"] = idn["food_security_index"].cumsum()
case2b["mental_health_index_cumsum"] = idn["mental_health_index"].cumsum()
case2b.to_csv(os.path.join(DATASETS_DIR, "case2b_IDN_VAR.csv"), index=False)
print(f"Case 2b (IDN VAR):  {len(case2b)} rows")


# Case 3: NGA — Mixed (уровни, разные порядки интеграции)
case3 = nga[META_COLS + ["vector_disease_risk_score", "precipitation_mm", "gdp_per_capita_usd"]]
case3.to_csv(os.path.join(DATASETS_DIR, "case3_NGA_Mixed.csv"), index=False)
print(f"Case 3 (NGA Mixed): {len(case3)} rows")


print("\nDone! Files in datasets/")
