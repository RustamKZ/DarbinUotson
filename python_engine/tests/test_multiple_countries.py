import pytest
import json
import pandas as pd
import numpy as np
from pathlib import Path
from api.analyzer import analyze_time_series

DATASET_PATH = Path(__file__).parent.parent / "datasets" / "global_climate_health_impact_tracker_2015_2025.csv"


class TestMultipleCountries:
  
  @pytest.fixture
  def all_data(self):
    """Load full dataset"""
    df = pd.read_csv(DATASET_PATH)
    print(f"\n📊 Loaded {len(df)} total records")
    return df
  
  def test_list_all_countries(self, all_data):
    """Show all unique countries in dataset"""
    
    countries = all_data.groupby('country_code').agg({
      'country_name': 'first',
      'record_id': 'count'
    }).reset_index()
    
    countries.columns = ['code', 'name', 'records']
    countries = countries.sort_values('records', ascending=False)
    
    print("\n" + "="*80)
    print("🌍 ALL COUNTRIES IN DATASET:")
    print("="*80)
    print(f"\n{'Code':<6} {'Name':<30} {'Records':>10}")
    print("-"*80)
    
    for _, row in countries.iterrows():
      print(f"{row['code']:<6} {row['name']:<30} {row['records']:>10}")
    
    print(f"\n📊 Total unique countries: {len(countries)}")
    print("="*80 + "\n")
    
    return countries
  
  def test_analyze_10_countries(self, all_data):
    """Analyze time series for 10 different countries"""
    
    # Получаем список стран
    countries = all_data.groupby('country_code').agg({
      'country_name': 'first',
      'record_id': 'count'
    }).reset_index()
    
    countries.columns = ['code', 'name', 'records']
    countries = countries.sort_values('records', ascending=False)
    
    # Берём топ-10 стран по количеству записей
    top_countries = countries.head(10)
    
    print("\n" + "="*80)
    print("🌍 ANALYZING TOP 10 COUNTRIES:")
    print("="*80)
    
    results_summary = []
    
    for idx, row in top_countries.iterrows():
      country_code = row['code']
      country_name = row['name']
      n_records = row['records']
      
      print(f"\n{'='*80}")
      print(f"🌍 {country_name} ({country_code}) - {n_records} records")
      print('='*80)
      
      # Фильтруем данные для страны
      country_data = all_data[all_data['country_code'] == country_code].copy()
      country_data = country_data.sort_values('date')
      
      # Извлекаем переменные
      temperature = country_data['temperature_celsius'].tolist()
      pm25 = country_data['pm25_ugm3'].tolist()
      respiratory = country_data['respiratory_disease_rate'].tolist()
      
      print(f"\n📊 Data preview (first 5):")
      print(f"  temperature: {[f'{x:.2f}' for x in temperature[:5]]}")
      print(f"  pm25: {[f'{x:.2f}' for x in pm25[:5]]}")
      print(f"  respiratory: {[f'{x:.2f}' for x in respiratory[:5]]}")
      
      # Проверяем минимальное количество данных
      if len(temperature) < 20:
        print(f"\n⚠️ SKIPPED: Not enough data ({len(temperature)} < 20)")
        results_summary.append({
          'country': country_name,
          'code': country_code,
          'status': 'SKIPPED',
          'reason': f'Only {len(temperature)} records'
        })
        continue
      
      # Формируем запрос
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
      
      try:
        # Запускаем анализ
        result_json = analyze_time_series(input_json)
        result = json.loads(result_json)
        
        # Проверяем на ошибки
        if 'error' in result:
          print(f"\n❌ ERROR: {result['error']}")
          print(f"   Message: {result['message']}")
          results_summary.append({
            'country': country_name,
            'code': country_code,
            'status': 'ERROR',
            'error': result['error']
          })
          continue
        
        # Выводим результаты
        print(f"\n✅ Analysis completed!")
        print(f"   Target: {result['target_variable']}")
        print(f"   Model: {result['model_type']}")
        print(f"   Structural break: {result['has_structural_break']}")
        
        # Series orders
        print(f"\n📊 Series Analysis:")
        for i, so in enumerate(result['series_orders']):
          name = result['variable_names'][i]
          print(f"   {name}: I({so['order']}), conflict={so['has_conflict']}, " +
                f"trend={so['has_trend']}, seasonality={so['has_seasonality']}")
        
        # Regression results
        if result['model_results'] and result['model_results'].get('regression'):
          reg = result['model_results']['regression']
          print(f"\n📈 Regression:")
          print(f"   R² = {reg['r_squared']:.4f}")
          print(f"   Adj R² = {reg['adj_r_squared']:.4f}")
          print(f"   DW = {reg['durbin_watson']['statistic']:.4f}")
          print(f"   Autocorrelation: {reg['durbin_watson']['has_autocorrelation']}")
          
          # Коэффициенты
          print(f"\n   Coefficients:")
          coef_names = ['Intercept'] + result['variable_names'][1:]  # Без Y
          for j, coef in enumerate(reg['coefficients']):
            sig = '***' if reg['p_values'][j] < 0.001 else \
                  '**' if reg['p_values'][j] < 0.01 else \
                  '*' if reg['p_values'][j] < 0.05 else ''
            print(f"     {coef_names[j]}: {coef:.6f} (p={reg['p_values'][j]:.6f}) {sig}")
          
          results_summary.append({
            'country': country_name,
            'code': country_code,
            'status': 'SUCCESS',
            'model_type': result['model_type'],
            'r_squared': reg['r_squared'],
            'has_break': result['has_structural_break']
          })
        else:
          results_summary.append({
            'country': country_name,
            'code': country_code,
            'status': 'NO_REGRESSION',
            'model_type': result['model_type']
          })
      
      except Exception as e:
        print(f"\n❌ EXCEPTION: {str(e)}")
        results_summary.append({
          'country': country_name,
          'code': country_code,
          'status': 'EXCEPTION',
          'error': str(e)
        })
    
    # Итоговая таблица
    print("\n" + "="*80)
    print("📊 SUMMARY:")
    print("="*80)
    print(f"\n{'Country':<30} {'Code':<6} {'Status':<12} {'Model':<20} {'R²':<8}")
    print("-"*80)
    
    for r in results_summary:
      r_squared = f"{r.get('r_squared', 0):.4f}" if r['status'] == 'SUCCESS' else '-'
      model = r.get('model_type', '-')
      print(f"{r['country']:<30} {r['code']:<6} {r['status']:<12} {model:<20} {r_squared:<8}")
    
    print("\n" + "="*80 + "\n")
    
    # Статистика
    success_count = sum(1 for r in results_summary if r['status'] == 'SUCCESS')
    print(f"✅ Successful: {success_count}/{len(results_summary)}")
    print(f"❌ Failed: {len(results_summary) - success_count}/{len(results_summary)}")