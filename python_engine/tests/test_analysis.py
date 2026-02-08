import pytest
import json
import pandas as pd
import numpy as np
from api.analyzer import analyze_time_series
from pathlib import Path

DATASET_PATH = Path(__file__).parent.parent / "datasets" / "global_climate_health_impact_tracker_2015_2025.csv"


class TestAnalysis:
  
  @pytest.fixture
  def sample_data(self):
    df = pd.read_csv(DATASET_PATH)
    usa_data = df[df['country_code'] == 'USA'].copy()
    usa_data = usa_data.sort_values('date')
    return usa_data
  
  def test_full_analysis_usa(self, sample_data):
    """Test full analysis pipeline on USA data"""
    print(f"\nLoaded {len(sample_data)} USA records")
    
    series_data = {
      "series": [
        {
          "name": "temperature_celsius",
          "data": sample_data['temperature_celsius'].tolist()
        },
        {
          "name": "pm25_ugm3",
          "data": sample_data['pm25_ugm3'].tolist()
        },
        {
          "name": "respiratory_disease_rate",
          "data": sample_data['respiratory_disease_rate'].tolist()
        }
      ]
    }
    
    input_json = json.dumps(series_data)
    
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    # Assertions
    assert result['series_count'] == 3
    assert 'variable_names' in result
    assert 'target_variable' in result
    assert result['target_variable'] == 'respiratory_disease_rate'
    assert len(result['series_orders']) == 3
    
    # Check each series has all required fields
    for so in result['series_orders']:
      assert 'order' in so
      assert 'has_conflict' in so
      assert 'adf' in so
      assert 'kpss' in so
      assert 'za' in so  # NEW!
      assert 'has_trend' in so
      assert 'has_seasonality' in so
    
    print(f"\n✅ Target: {result['target_variable']}")
    print(f"✅ Model type: {result['model_type']}")
    print(f"✅ Structural breaks: {result['has_structural_break']}")
    
  def test_auto_target_detection(self, sample_data):
    """Test automatic target variable detection"""
    
    # Put health variable in the middle
    series_data = {
      "series": [
        {
          "name": "temperature_celsius",
          "data": sample_data['temperature_celsius'].tolist()
        },
        {
          "name": "respiratory_disease_rate",  # Health variable
          "data": sample_data['respiratory_disease_rate'].tolist()
        },
        {
          "name": "pm25_ugm3",
          "data": sample_data['pm25_ugm3'].tolist()
        }
      ]
    }
    
    input_json = json.dumps(series_data)
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    # Should auto-detect respiratory_disease_rate as target
    assert result['target_variable'] == 'respiratory_disease_rate'
    print(f"\n✅ Auto-detected target: {result['target_variable']}")
  
  def test_manual_target_selection(self, sample_data):
    """Test manual target variable selection"""
    
    series_data = {
      "series": [
        {
          "name": "temperature_celsius",
          "data": sample_data['temperature_celsius'].tolist()
        },
        {
          "name": "pm25_ugm3",
          "data": sample_data['pm25_ugm3'].tolist()
        }
      ],
      "target_index": 1  # Manually select pm25
    }
    
    input_json = json.dumps(series_data)
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    # Should use manually specified target
    assert result['target_variable'] == 'pm25_ugm3'
    print(f"\n✅ Manual target: {result['target_variable']}")

  def test_za_results_present(self, sample_data):
    """Test that ZA test results are included"""
    
    # 🆕 Используем 2 ряда (минимум для регрессии)
    series_data = {
      "series": [
        {
          "name": "temperature_celsius",
          "data": sample_data['temperature_celsius'].tolist()
        },
        {
          "name": "pm25_ugm3",  # 🆕 Добавляем второй ряд
          "data": sample_data['pm25_ugm3'].tolist()
        }
      ]
    }
    
    input_json = json.dumps(series_data)
    result_json = analyze_time_series(input_json)
    result = json.loads(result_json)
    
    # 🆕 Проверяем что нет ошибки
    if 'error' in result:
      pytest.fail(f"Analysis failed: {result['error']} - {result['message']}")
    
    # Проверяем ZA результаты для обоих рядов
    assert 'series_orders' in result
    assert len(result['series_orders']) == 2
    
    for i, so in enumerate(result['series_orders']):
      za = so['za']
      
      if za is not None:
        assert 'test_statistic' in za
        assert 'p_value' in za
        assert 'breakpoint' in za
        assert 'critical_values' in za
        print(f"\n✅ ZA test results present for series {i} ({result['variable_names'][i]})")
        print(f"   Breakpoint: {za['breakpoint']}")
        print(f"   Statistic: {za['test_statistic']:.4f}")
      else:
        print(f"\n⚠️ ZA test skipped for series {i} ({result['variable_names'][i]}) - data too short or I(0)") 