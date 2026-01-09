package org.example.project_dw.domain.repositories

import org.example.project_dw.domain.models.StationarityResult

interface IStatisticsRepository {
    suspend fun checkStationarity(data: DoubleArray): StationarityResult
}
