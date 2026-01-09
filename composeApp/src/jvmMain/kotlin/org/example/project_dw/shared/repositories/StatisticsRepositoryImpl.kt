package org.example.project_dw.shared.repositories

import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.domain.models.StationarityResult
import org.example.project_dw.domain.repositories.IStatisticsRepository

class StatisticsRepositoryImpl(
    private val pythonBridge: PythonBridge
) : IStatisticsRepository {

    override suspend fun checkStationarity(
        data: DoubleArray
    ): StationarityResult {
        val result = pythonBridge.runAdfTest(data)

        return result.fold(
            onSuccess = { adf ->
                StationarityResult.Success(
                    isStationary = adf.isStationary,
                    pValue = adf.pValue,
                    testStatistic = adf.testStatistic
                )
            },
            onFailure = { exception ->
                StationarityResult.Error(
                    exception.message ?: "unknown error"
                )
            }
        )
    }
}
