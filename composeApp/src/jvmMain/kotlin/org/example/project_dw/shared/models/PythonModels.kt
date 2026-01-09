package org.example.project_dw.shared.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class DataInput(val values: List<Double>)

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
