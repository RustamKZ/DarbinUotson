package org.example.project_dw.domain.models

sealed class StationarityResult {
    data class Success(
        val isStationary: Boolean,
        val pValue: Double,
        val testStatistic: Double
    ) : StationarityResult()

    data class Error(val message: String) : StationarityResult()
}
