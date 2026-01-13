package org.example.project_dw.shared.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MultiSeriesInput(
  val series: List<List<Double>>
)

@Serializable
data class CriticalValues(
  @SerialName("one_percent")
  val onePercent: Double,
  @SerialName("five_percent")
  val fivePercent: Double,
  @SerialName("ten_percent")
  val tenPercent: Double
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
  val criticalValues: CriticalValues,
  @SerialName("is_stationary")
  val isStationary: Boolean
)

@Serializable
data class KpssTestResult(
  @SerialName("kpss_stat")
  val kpssStat: Double,
  @SerialName("p_value")
  val pValue: Double,
  val lags: Int,
  val crit: KpssCriticalValues
)

@Serializable
data class SeriesAnalysisResult(
  val order: Int,
  @SerialName("has_conflict")
  val hasConflict: Boolean,
  val adf: AdfTestResult,
  val kpss: KpssTestResult
)

@Serializable
data class MultiSeriesAnalysisResult(
  @SerialName("series_count")
  val seriesCount: Int,
  @SerialName("series_orders")
  val seriesOrders: List<SeriesAnalysisResult>
)
