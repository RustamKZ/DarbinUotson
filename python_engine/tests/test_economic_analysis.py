import pytest
import json
import pandas as pd
import numpy as np
from pathlib import Path
from api.analyzer import analyze_time_series

DATASET_PATH = Path(__file__).parent.parent / "datasets" / "global_climate_health_impact_tracker_2015_2025.csv"


class TestEconomicAnalysis:
  
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
    # Non-stationary economic indicators
    ("gdp_per_capita_usd", ["temperature_celsius", "extreme_weather_events"], 
     "GDP ~ Temperature + Extreme weather"),
    ("gdp_per_capita_usd", ["pm25_ugm3", "air_quality_index"], 
     "GDP ~ PM2.5 + Air quality"),
    
    # Food security (potentially I(1))
    ("food_security_index", ["temperature_celsius", "precipitation_mm"], 
     "Food security ~ Temperature + Precipitation"),
    ("food_security_index", ["drought_indicator", "flood_indicator"], 
     "Food security ~ Drought + Floods"),
    
    # Healthcare access (slowly changing)
    ("healthcare_access_index", ["gdp_per_capita_usd"], 
     "Healthcare access ~ GDP"),
    ("healthcare_access_index", ["pm25_ugm3", "air_quality_index"], 
     "Healthcare access ~ Air pollution"),
    
    # Population (definitely I(1) - has trend)
    ("population_millions", ["gdp_per_capita_usd"], 
     "Population ~ GDP"),
    
    # Complex relationships with potential cointegration
    ("mental_health_index", ["gdp_per_capita_usd", "pm25_ugm3"], 
     "Mental health ~ GDP + PM2.5"),
    ("respiratory_disease_rate", ["gdp_per_capita_usd", "healthcare_access_index"], 
     "Respiratory ~ GDP + Healthcare access"),
    
    # Testing if cumulative pollution affects long-term health
    ("cardio_mortality_rate", ["gdp_per_capita_usd", "food_security_index"], 
     "Cardio ~ GDP + Food security"),
  ])
  def test_economic_and_longterm_variables(self, usa_data, target_var, predictor_vars, description):
    """Test analysis with economic and potentially I(1) variables"""
    
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
      "target_index": len(series_list) - 1
      # "target_index": None  # Auto-detect
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
    
    # Series analysis summary with integration orders
    print(f"\n📊 Integration Orders:")
    has_i1 = False
    has_i2 = False
    for i, name in enumerate(result['variable_names']):
      so = result['series_orders'][i]
      marker = "🎯" if name == target_var else "📈"
      order_symbol = "I(0)" if so['order'] == 0 else f"I({so['order']})"
      if so['order'] == 1:
        has_i1 = True
        order_symbol = "🔴 I(1)"
      elif so['order'] == 2:
        has_i2 = True
        order_symbol = "🔴🔴 I(2)"
      
      conflict_mark = " ⚠️ CONFLICT" if so['has_conflict'] else ""
      print(f"  {marker} {name}: {order_symbol}, trend={so['has_trend']}, seasonal={so['has_seasonality']}{conflict_mark}")
    
    if has_i1 or has_i2:
      print(f"\n🎉 Found non-stationary series!")
    
    # Transformation info for mixed models
    if result.get('transformations'):
      print(f"\n🔄 Transformations:")
      for trans in result['transformations']:
        print(f"  {trans['variable_name']}: {trans['transformation']}")
    
    # Cointegration info
    if result['model_results'] is not None:
      model_results = result['model_results']
      
      if model_results.get('cointegration'):
        coint = model_results['cointegration']
        coint_mark = "✅" if coint['is_cointegrated'] else "❌"
        print(f"\n🔗 Cointegration: {coint_mark} {coint['is_cointegrated']} ({coint['test_type']})")
        
        if coint.get('aeg_result'):
          print(f"   AEG p-value: {coint['aeg_result']['p_value']:.6f}")
        if coint.get('n_cointegration_relations') is not None:
          print(f"   Johansen relations: {coint['n_cointegration_relations']}")
      
      # Regression
      if model_results.get('regression'):
        reg = model_results['regression']
        print(f"\n📈 Regression:")
        print(f"  R² = {reg['r_squared']:.4f}, Adj R² = {reg['adj_r_squared']:.4f}")
        print(f"  DW = {reg['durbin_watson']['statistic']:.4f}, Autocorr = {reg['durbin_watson']['has_autocorrelation']}")
        print(f"  F = {reg['f_statistic']:.2f} (p={reg['f_pvalue']:.6f})")
        
        if reg.get('uses_newey_west'):
          print(f"  ⚠️ Uses Newey-West robust SE")
        
        print(f"\n  Coefficients:")
        var_names = ['Intercept'] + result['variable_names'][:-1]  # Exclude target
        for i, (coef, pval) in enumerate(zip(reg['coefficients'], reg['p_values'])):
          sig = "***" if pval < 0.001 else "**" if pval < 0.01 else "*" if pval < 0.05 else ""
          name = var_names[i] if i < len(var_names) else f"Var{i}"
          print(f"    {name}: {coef:>10.6f} (p={pval:.6f}) {sig}")
    
    print("\n" + "="*80 + "\n")
  
  def test_cumulative_series_for_cointegration(self, usa_data):
    """Create cumulative series to force I(1) behavior and test ECM/cointegration"""
    
    print("\n" + "="*80)
    print(f"📊 TEST: Cumulative series (forcing I(1))")
    print("="*80)
    
    # Create cumulative respiratory cases
    respiratory = usa_data['respiratory_disease_rate'].values
    cumulative_respiratory = np.cumsum(respiratory)
    
    # Create cumulative PM2.5 exposure
    pm25 = usa_data['pm25_ugm3'].values
    cumulative_pm25 = np.cumsum(pm25)
    
    # Create cumulative temperature (anomaly accumulation)
    temp_anomaly = usa_data['temp_anomaly_celsius'].values
    cumulative_temp = np.cumsum(temp_anomaly)
    
    print(f"  Cumulative PM2.5 (first 5): {cumulative_pm25[:5]}")
    print(f"  Cumulative temperature anomaly (first 5): {cumulative_temp[:5]}")
    print(f"  Cumulative respiratory (first 5): {cumulative_respiratory[:5]}")
    
    series_data = {
      "series": [
        {
          "name": "cumulative_pm25_exposure",
          "data": cumulative_pm25.tolist()
        },
        {
          "name": "cumulative_temp_anomaly",
          "data": cumulative_temp.tolist()
        },
        {
          "name": "cumulative_respiratory_burden",
          "data": cumulative_respiratory.tolist()
        }
      ],
      "target_index": 2  # Explicitly set cumulative respiratory as target
    }
    
    input_json = json.dumps(series_data)
    
    print(f"\n🚀 Running analysis on cumulative series...")
    
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    print("\n" + "-"*80)
    print("✅ RESULTS:")
    print("-"*80)
    
    print(f"Target: {result['target_variable']}")
    print(f"Model: {result['model_type']}")
    
    # Check integration orders
    print(f"\n📊 Integration Orders (expecting I(1) or I(2)):")
    for i, name in enumerate(result['variable_names']):
      so = result['series_orders'][i]
      marker = "🎯" if name == result['target_variable'] else "📈"
      
      if so['order'] >= 1:
        order_color = "🔴" * so['order']
      else:
        order_color = "✅"
      
      print(f"  {marker} {name}: {order_color} I({so['order']})")
      print(f"     ADF: stat={so['adf']['test_statistic']:.4f}, p={so['adf']['p_value']:.6f}, stationary={so['adf']['is_stationary']}")
      print(f"     KPSS: stat={so['kpss']['kpss_stat']:.4f}, p={so['kpss']['p_value']:.6f}")
      if so.get('za'):
        print(f"     ZA: breakpoint={so['za']['breakpoint']}, p={so['za']['p_value']:.6f}")
    
    # Expect non-stationary model
    if result['model_type'] in ['full_non_stationary', 'mixed']:
      print(f"\n🎉 SUCCESS: Got {result['model_type']} model as expected!")
    else:
      print(f"\n⚠️ Got {result['model_type']} model")
    
    # Check for cointegration
    if result['model_results'] and result['model_results'].get('cointegration'):
      coint = result['model_results']['cointegration']
      print(f"\n🔗 Cointegration Test:")
      print(f"   Type: {coint['test_type']}")
      print(f"   Cointegrated: {'✅ YES' if coint['is_cointegrated'] else '❌ NO'}")
      
      if coint.get('aeg_result'):
        aeg = coint['aeg_result']
        print(f"   AEG statistic: {aeg['test_statistic']:.4f}")
        print(f"   AEG p-value: {aeg['p_value']:.6f}")
      
      if coint.get('johansen_trace_stats'):
        print(f"   Johansen trace stats: {coint['johansen_trace_stats'][:3]}")
        print(f"   Cointegration relations: {coint.get('n_cointegration_relations', 0)}")
    
    # Regression results
    if result['model_results'] and result['model_results'].get('regression'):
      reg = result['model_results']['regression']
      print(f"\n📈 Regression/ECM Results:")
      print(f"   R² = {reg['r_squared']:.4f}")
      print(f"   DW = {reg['durbin_watson']['statistic']:.4f}")
      print(f"   N obs = {reg['n_obs']}")
      print(f"   Has lags = {reg.get('has_lags', False)}")
      
      if len(reg['coefficients']) <= 10:  # Don't print too many
        print(f"\n   Coefficients:")
        for i, (coef, pval) in enumerate(zip(reg['coefficients'], reg['p_values'])):
          sig = "***" if pval < 0.001 else "**" if pval < 0.01 else "*" if pval < 0.05 else ""
          print(f"     [{i}] {coef:>10.6f} (p={pval:.6f}) {sig}")
    
    print("\n" + "="*80 + "\n")
  
  def test_gdp_trend_analysis(self, usa_data):
    """Test GDP with clear trend - should be I(1)"""
    
    print("\n" + "="*80)
    print(f"📊 TEST: GDP trend analysis")
    print("="*80)
    
    gdp = usa_data['gdp_per_capita_usd'].values
    population = usa_data['population_millions'].values
    
    print(f"  GDP range: {gdp.min():.2f} to {gdp.max():.2f}")
    print(f"  Population range: {population.min():.2f} to {population.max():.2f}")
    print(f"  GDP first 5: {gdp[:5]}")
    print(f"  GDP last 5: {gdp[-5:]}")
    
    series_data = {
      "series": [
        {
          "name": "population_millions",
          "data": population.tolist()
        },
        {
          "name": "gdp_per_capita_usd",
          "data": gdp.tolist()
        }
      ],
      "target_index": 1
    }
    
    input_json = json.dumps(series_data)
    
    print(f"\n🚀 Running analysis...")
    
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    print("\n" + "-"*80)
    print("✅ RESULTS:")
    print("-"*80)
    
    print(f"Model: {result['model_type']}")
    
    print(f"\n📊 Integration Orders:")
    for i, name in enumerate(result['variable_names']):
      so = result['series_orders'][i]
      marker = "🎯" if i == 1 else "📈"
      
      order_display = f"I({so['order']})"
      if so['order'] == 1:
        order_display = "🔴 I(1)"
      elif so['order'] == 2:
        order_display = "🔴🔴 I(2)"
      
      print(f"  {marker} {name}: {order_display}")
      print(f"     Trend: {so['has_trend']}, Seasonal: {so['has_seasonality']}")
      print(f"     Trend strength: {so['trend_strength']:.4f}")
      print(f"     ADF p-value: {so['adf']['p_value']:.6f}")
      print(f"     KPSS p-value: {so['kpss']['p_value']:.6f}")
    
    print("\n" + "="*80 + "\n")