package org.example.project_dw.shared.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ===============================
// main response

@Serializable
data class TimeSeriesAnalysisResult(
  @SerialName("series_count")
  val seriesCount: Int,
  
  @SerialName("variable_names")
  val variableNames: List<String>,
  
  @SerialName("target_variable")
  val targetVariable: String,
  
  @SerialName("model_type")
  val modelType: String,  // "full_stationary" | "full_non_stationary" | "mixed"
  
  @SerialName("has_structural_break")
  val hasStructuralBreak: Boolean,
  
  @SerialName("structural_breaks")
  val structuralBreaks: List<StructuralBreak>? = null,
  
  @SerialName("series_orders")
  val seriesOrders: List<SeriesOrder>,
  
  val transformations: List<TransformationInfo>? = null,
  
  @SerialName("model_results")
  val modelResults: ModelResults? = null
)

// ===============================
// structural breaks

@Serializable
data class StructuralBreak(
  val index: Int,
  @SerialName("series_index")
  val seriesIndex: Int
)

// ===============================
// series analysis

@Serializable
data class SeriesOrder(
  val order: Int,  // 0=I(0), 1=I(1), 2=I(2)
  
  @SerialName("has_conflict")
  val hasConflict: Boolean,
  
  val adf: AdfTestResult,
  val kpss: KpssTestResult,
  val za: ZivotAndrewsResult? = null,
  
  @SerialName("structural_break")
  val structuralBreak: Int? = null,
  
  @SerialName("has_trend")
  val hasTrend: Boolean,
  
  @SerialName("has_seasonality")
  val hasSeasonality: Boolean,
  
  @SerialName("trend_strength")
  val trendStrength: Double,
  
  @SerialName("seasonal_strength")
  val seasonalStrength: Double
)

// ===============================
// stationarity test 

@Serializable
data class AdfTestResult(
  @SerialName("test_statistic")
  val testStatistic: Double,
  
  @SerialName("p_value")
  val pValue: Double,
  
  @SerialName("used_lag")
  val usedLag: Int,
  
  @SerialName("n_obs")
  val nObs: Int,
  
  @SerialName("critical_values")
  val criticalValues: AdfCriticalValues,
  
  @SerialName("is_stationary")
  val isStationary: Boolean
)

@Serializable
data class AdfCriticalValues(
  @SerialName("one_percent")
  val onePercent: Double,
  
  @SerialName("five_percent")
  val fivePercent: Double,
  
  @SerialName("ten_percent")
  val tenPercent: Double
)

@Serializable
data class KpssTestResult(
  @SerialName("kpss_stat")
  val kpssStat: Double,
  
  @SerialName("p_value")
  val pValue: Double,
  
  val lags: Int,
  val crit: KpssCriticalValues,

  @SerialName("is_stationary")
  val isStationary: Boolean
)

@Serializable
data class KpssCriticalValues(
  @SerialName("one_percent")
  val onePercent: Double,
  
  @SerialName("two_and_half_percent")
  val twoAndHalfPercent: Double,
  
  @SerialName("five_percent")
  val fivePercent: Double,
  
  @SerialName("ten_percent")
  val tenPercent: Double
)

@Serializable
data class ZivotAndrewsResult(
  @SerialName("test_statistic")
  val testStatistic: Double,
  
  @SerialName("p_value")
  val pValue: Double,
  
  @SerialName("used_lag")
  val usedLag: Int,
  
  val breakpoint: Int,
  
  @SerialName("critical_values")
  val criticalValues: AdfCriticalValues,
  
  @SerialName("is_stationary")
  val isStationary: Boolean
)

// ===============================
// series transforms (for MIXED case)

@Serializable
data class TransformationInfo(
  @SerialName("series_index")
  val seriesIndex: Int,
  
  @SerialName("variable_name")
  val variableName: String,
  
  @SerialName("original_order")
  val originalOrder: Int,
  
  val transformation: String  // "none" | "first_difference" | "second_difference"
)

// ===============================
// model's results

@Serializable
data class ModelResults(
  val cointegration: CointegrationResult? = null,
  val regression: RegressionResult? = null,
  
  @SerialName("error_message")
  val errorMessage: String? = null,
  
  @SerialName("has_structural_break")
  val hasStructuralBreak: Boolean = false,
  
  @SerialName("structural_breaks")
  val structuralBreaks: List<StructuralBreak>? = null,
  
  val periods: List<PeriodInfo>? = null,
  
  @SerialName("period_results")
  val periodResults: List<PeriodModelResult>? = null
)

// ===============================
// cointegration

@Serializable
data class CointegrationResult(
  @SerialName("test_type")
  val testType: String,  // "aeg" | "johansen"
  
  @SerialName("n_series")
  val nSeries: Int,
  
  @SerialName("is_cointegrated")
  val isCointegrated: Boolean,
  
  @SerialName("aeg_result")
  val aegResult: AegTestResult? = null,
  
  @SerialName("johansen_eigenvalues")
  val johannsenEigenvalues: List<Double>? = null,
  
  @SerialName("johansen_trace_stats")
  val johannsenTraceStats: List<Double>? = null,
  
  @SerialName("n_cointegration_relations")
  val nCointegrationRelations: Int? = null
)

@Serializable
data class AegTestResult(
  @SerialName("coint_t")
  val cointT: Double,
  
  @SerialName("p_value")
  val pValue: Double,
  
  @SerialName("crit_values")
  val critValues: AegCritValues
)

@Serializable
data class AegCritValues(
  @SerialName("one_percent")
  val onePercent: Double,
  
  @SerialName("five_percent")
  val fivePercent: Double,
  
  @SerialName("ten_percent")
  val tenPercent: Double
)

// ===============================
// regression

@Serializable
data class RegressionResult(
  val coefficients: List<Double>,
  
  @SerialName("std_errors")
  val stdErrors: List<Double>,
  
  @SerialName("t_values")
  val tValues: List<Double>,
  
  @SerialName("p_values")
  val pValues: List<Double>,
  
  @SerialName("r_squared")
  val rSquared: Double,
  
  @SerialName("adj_r_squared")
  val adjRSquared: Double,
  
  @SerialName("f_statistic")
  val fStatistic: Double,
  
  @SerialName("f_pvalue")
  val fPvalue: Double,
  
  @SerialName("durbin_watson")
  val durbinWatson: DurbinWatsonResult,
  
  @SerialName("n_obs")
  val nObs: Int,
  
  @SerialName("has_lags")
  val hasLags: Boolean = false,
  
  @SerialName("uses_newey_west")
  val usesNeweyWest: Boolean = false
)

@Serializable
data class DurbinWatsonResult(
  val statistic: Double,
  
  @SerialName("has_autocorrelation")
  val hasAutocorrelation: Boolean
)

// ===============================
// periods (for structural breaks)

@Serializable
data class PeriodInfo(
  @SerialName("period_number")
  val periodNumber: Int,
  
  @SerialName("start_index")
  val startIndex: Int,
  
  @SerialName("end_index")
  val endIndex: Int,
  
  @SerialName("data_size")
  val dataSize: Int
)

@Serializable
data class PeriodModelResult(
  @SerialName("period_type")
  val periodType: String,  // "before_break" | "after_break" | "custom"
  
  @SerialName("period_number")
  val periodNumber: Int,
  
  @SerialName("start_index")
  val startIndex: Int,
  
  @SerialName("end_index")
  val endIndex: Int,
  
  @SerialName("model_type")
  val modelType: String,
  
  @SerialName("data_size")
  val dataSize: Int,
  
  @SerialName("series_orders")
  val seriesOrders: List<SeriesOrder>,
  
  val cointegration: CointegrationResult? = null,
  val regression: RegressionResult? = null,
  
  @SerialName("error_message")
  val errorMessage: String? = null
)