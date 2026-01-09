package org.example.project_dw.domain.usecases

import org.example.project_dw.domain.models.StationarityResult
import org.example.project_dw.domain.repositories.IStatisticsRepository

class CheckStationarityUseCase(
    private val repository: IStatisticsRepository
) {
    suspend operator fun invoke(
        data: DoubleArray
    ): StationarityResult {
        return repository.checkStationarity(data)
    }
}