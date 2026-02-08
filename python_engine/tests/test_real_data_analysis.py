import pytest
import json
import pandas as pd
import numpy as np
from pathlib import Path
from api.analyzer import analyze_time_series

DATASET_PATH = Path(__file__).parent.parent / "datasets" / "global_climate_health_impact_tracker_2015_2025.csv"


class TestRealDataAnalysis:
  
  @pytest.fixture
  def usa_data(self):
    """Load USA data from CSV"""
    df = pd.read_csv(DATASET_PATH)
    usa_data = df[df['country_code'] == 'USA'].copy()
    usa_data = usa_data.sort_values('date')
    
    print(f"\n📊 Loaded {len(usa_data)} USA records")
    print(f"Date range: {usa_data['date'].min()} to {usa_data['date'].max()}")
    
    return usa_data
  
  @pytest.mark.parametrize("target_var,predictor_vars,description", [
    # Respiratory diseases
    ("respiratory_disease_rate", ["temperature_celsius", "pm25_ugm3"], 
     "Respiratory ~ Temperature + PM2.5"),
    ("respiratory_disease_rate", ["pm25_ugm3", "air_quality_index"], 
     "Respiratory ~ PM2.5 + AQI"),
    ("respiratory_disease_rate", ["temperature_celsius", "pm25_ugm3", "heat_wave_days"], 
     "Respiratory ~ Temperature + PM2.5 + Heat waves"),
    
    # Cardiovascular mortality
    ("cardio_mortality_rate", ["temperature_celsius", "pm25_ugm3"], 
     "Cardio ~ Temperature + PM2.5"),
    ("cardio_mortality_rate", ["temp_anomaly_celsius", "extreme_weather_events"], 
     "Cardio ~ Temp anomaly + Extreme weather"),
    ("cardio_mortality_rate", ["heat_wave_days", "pm25_ugm3"], 
     "Cardio ~ Heat waves + PM2.5"),
    
    # Heat-related admissions
    ("heat_related_admissions", ["temperature_celsius", "heat_wave_days"], 
     "Heat admissions ~ Temperature + Heat waves"),
    ("heat_related_admissions", ["temp_anomaly_celsius", "heat_wave_days"], 
     "Heat admissions ~ Temp anomaly + Heat waves"),
    
    # Vector-borne diseases
    ("vector_disease_risk_score", ["temperature_celsius", "precipitation_mm"], 
     "Vector diseases ~ Temperature + Precipitation"),
    ("vector_disease_risk_score", ["temperature_celsius", "precipitation_mm", "flood_indicator"], 
     "Vector diseases ~ Temperature + Precipitation + Floods"),
    
    # Waterborne diseases
    ("waterborne_disease_incidents", ["precipitation_mm", "flood_indicator"], 
     "Waterborne ~ Precipitation + Floods"),
    ("waterborne_disease_incidents", ["temperature_celsius", "precipitation_mm", "flood_indicator"], 
     "Waterborne ~ Temperature + Precipitation + Floods"),
    
    # Mental health
    ("mental_health_index", ["temperature_celsius", "extreme_weather_events"], 
     "Mental health ~ Temperature + Extreme weather"),
    ("mental_health_index", ["temp_anomaly_celsius", "drought_indicator", "flood_indicator"], 
     "Mental health ~ Temp anomaly + Drought + Floods"),
    
    # Complex multi-factor models
    ("respiratory_disease_rate", ["temperature_celsius", "pm25_ugm3", "air_quality_index", "heat_wave_days"], 
     "Respiratory ~ Full climate model"),
    ("cardio_mortality_rate", ["temperature_celsius", "pm25_ugm3", "heat_wave_days", "extreme_weather_events"], 
     "Cardio ~ Full climate model"),
  ])
  def test_different_health_outcomes(self, usa_data, target_var, predictor_vars, description):
    """Test analysis with different target variables and predictors"""
    
    print("\n" + "="*80)
    print(f"📊 TEST: {description}")
    print("="*80)
    
    # Prepare series data
    series_list = []
    
    # Add predictors first
    for var in predictor_vars:
      data = usa_data[var].tolist()
      series_list.append({
        "name": var,
        "data": data
      })
      print(f"  Predictor: {var} (first 5: {[f'{x:.2f}' for x in data[:5]]})")
    
    # Add target last
    target_data = usa_data[target_var].tolist()
    series_list.append({
      "name": target_var,
      "data": target_data
    })
    print(f"  Target: {target_var} (first 5: {[f'{x:.2f}' for x in target_data[:5]]})")
    
    series_data = {
      "series": series_list,
      "target_index": None  # Auto-detect
    }
    
    input_json = json.dumps(series_data)
    
    print(f"\n🚀 Running analysis...")
    
    # Run analysis
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    # Print results
    print("\n" + "-"*80)
    print("✅ RESULTS:")
    print("-"*80)
    
    assert result['target_variable'] == target_var, f"Expected target {target_var}, got {result['target_variable']}"
    
    print(f"Target: {result['target_variable']}")
    print(f"Model: {result['model_type']}")
    print(f"Structural break: {result['has_structural_break']}")
    
    # Series analysis summary
    print(f"\n📊 Series Orders:")
    for i, name in enumerate(result['variable_names']):
      so = result['series_orders'][i]
      marker = "🎯" if name == target_var else "📈"
      print(f"  {marker} {name}: I({so['order']}), trend={so['has_trend']}, seasonal={so['has_seasonality']}")
    
    # Model results
    if result['model_results'] is not None:
      model_results = result['model_results']
      
      # Cointegration
      if model_results.get('cointegration'):
        coint = model_results['cointegration']
        print(f"\n🔗 Cointegration: {coint['is_cointegrated']} ({coint['test_type']})")
      
      # Regression
      if model_results.get('regression'):
        reg = model_results['regression']
        print(f"\n📈 Regression:")
        print(f"  R² = {reg['r_squared']:.4f}, Adj R² = {reg['adj_r_squared']:.4f}")
        print(f"  DW = {reg['durbin_watson']['statistic']:.4f}, Autocorr = {reg['durbin_watson']['has_autocorrelation']}")
        print(f"  F = {reg['f_statistic']:.2f} (p={reg['f_pvalue']:.6f})")
        
        print(f"\n  Coefficients:")
        var_names = ['Intercept'] + result['variable_names'][:-1]  # Exclude target
        for i, (coef, pval) in enumerate(zip(reg['coefficients'], reg['p_values'])):
          sig = "***" if pval < 0.001 else "**" if pval < 0.01 else "*" if pval < 0.05 else ""
          name = var_names[i] if i < len(var_names) else f"Var{i}"
          print(f"    {name}: {coef:>10.6f} (p={pval:.6f}) {sig}")
    
    print("\n" + "="*80 + "\n")
  
  def test_full_analysis_with_real_data(self, usa_data):
    """Test complete analysis pipeline with real USA data (original test)"""
    
    print("\n" + "="*60)
    print("📊 DATA PREVIEW (first 10 values):")
    print("="*60)
    
    temperature = usa_data['temperature_celsius'].tolist()
    pm25 = usa_data['pm25_ugm3'].tolist()
    respiratory = usa_data['respiratory_disease_rate'].tolist()
    
    print("temperature_celsius:")
    print([f"{x:.2f}" for x in temperature[:10]])
    
    print("\npm25_ugm3:")
    print([f"{x:.2f}" for x in pm25[:10]])
    
    print("\nrespiratory_disease_rate:")
    print([f"{x:.2f}" for x in respiratory[:10]])
    
    print(f"\n📏 Total records: {len(temperature)}")
    print("="*60 + "\n")
    
    series_data = {
      "series": [
        {
          "name": "temperature_celsius",
          "data": temperature
        },
        {
          "name": "pm25_ugm3",
          "data": pm25
        },
        {
          "name": "respiratory_disease_rate",
          "data": respiratory
        }
      ],
      "target_index": None
    }
    
    input_json = json.dumps(series_data)
    
    print("🚀 Sending request to Python analyzer...")
    
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    print("\n" + "="*60)
    print("✅ ANALYSIS RESULTS:")
    print("="*60)
    
    assert 'series_count' in result
    assert result['series_count'] == 3
    
    print(f"Series count: {result['series_count']}")
    print(f"Target variable: {result['target_variable']}")
    print(f"Model type: {result['model_type']}")
    print(f"Has structural break: {result['has_structural_break']}")
    
    assert result['target_variable'] == 'respiratory_disease_rate'
    print("\n✅ Target variable correctly auto-detected!")
    
    print(f"\nVariables (after swap):")
    for i, name in enumerate(result['variable_names']):
      print(f"  {i}. {name}")
    
    print("\n" + "-"*60)
    print("SERIES ANALYSIS:")
    print("-"*60)
    
    for i, so in enumerate(result['series_orders']):
      name = result['variable_names'][i]
      print(f"\n{name}:")
      print(f"  Order: I({so['order']})")
      print(f"  Has conflict: {so['has_conflict']}")
      print(f"  Has trend: {so['has_trend']}")
      print(f"  Has seasonality: {so['has_seasonality']}")
      print(f"  Trend strength: {so['trend_strength']:.4f}")
      print(f"  Seasonal strength: {so['seasonal_strength']:.4f}")
      
      print(f"  ADF:")
      print(f"    Stationary: {so['adf']['is_stationary']}")
      print(f"    p-value: {so['adf']['p_value']:.6f}")
      print(f"    test_statistic: {so['adf']['test_statistic']:.4f}")
      
      print(f"  KPSS:")
      kpss_stationary = so['kpss']['p_value'] > 0.05
      print(f"    Stationary: {kpss_stationary}")
      print(f"    p-value: {so['kpss']['p_value']:.6f}")
      print(f"    kpss_stat: {so['kpss']['kpss_stat']:.4f}")
      
      if so['za'] is not None:
        print(f"  Zivot-Andrews:")
        print(f"    Breakpoint: {so['za']['breakpoint']}")
        print(f"    Stationary: {so['za']['is_stationary']}")
        print(f"    p-value: {so['za']['p_value']:.6f}")
    
    if result['model_results'] is not None:
      print("\n" + "-"*60)
      print("MODEL RESULTS:")
      print("-"*60)
      
      model_results = result['model_results']
      
      if model_results.get('cointegration'):
        coint = model_results['cointegration']
        print(f"\nCointegration Test ({coint['test_type']}):")
        print(f"  Is cointegrated: {coint['is_cointegrated']}")
      
      if model_results.get('regression'):
        reg = model_results['regression']
        print(f"\nRegression:")
        print(f"  R²: {reg['r_squared']:.4f}")
        print(f"  Adj R²: {reg['adj_r_squared']:.4f}")
        print(f"  F-statistic: {reg['f_statistic']:.4f}")
        print(f"  F p-value: {reg['f_pvalue']:.6f}")
        print(f"  DW statistic: {reg['durbin_watson']['statistic']:.4f}")
        print(f"  Has autocorrelation: {reg['durbin_watson']['has_autocorrelation']}")
        print(f"  N observations: {reg['n_obs']}")
        print(f"  Has lags: {reg['has_lags']}")
        print(f"  Uses Newey-West: {reg['uses_newey_west']}")
        
        print(f"\n  Coefficients:")
        for i, coef in enumerate(reg['coefficients']):
          print(f"    [{i}] {coef:.6f} (p={reg['p_values'][i]:.6f})")
    
    print("\n" + "="*60)
    print("✅ TEST PASSED")
    print("="*60 + "\n")
  
  def test_data_statistics(self, usa_data):
    """Show basic statistics of the data"""
    
    print("\n" + "="*60)
    print("📊 DATA STATISTICS:")
    print("="*60)
    
    variables = [
      'temperature_celsius',
      'pm25_ugm3',
      'respiratory_disease_rate',
      'cardio_mortality_rate',
      'vector_disease_risk_score',
      'waterborne_disease_incidents',
      'heat_related_admissions',
      'mental_health_index'
    ]
    
    for var in variables:
      print(f"\n{var}:")
      print(f"  Count: {usa_data[var].count()}")
      print(f"  Mean: {usa_data[var].mean():.4f}")
      print(f"  Std: {usa_data[var].std():.4f}")
      print(f"  Min: {usa_data[var].min():.4f}")
      print(f"  Max: {usa_data[var].max():.4f}")
      print(f"  Missing: {usa_data[var].isna().sum()}")
    
    print("\n" + "="*60 + "\n")