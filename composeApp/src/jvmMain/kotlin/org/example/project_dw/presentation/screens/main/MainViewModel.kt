package org.example.project_dw.presentation.screens.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.shared.models.SeriesAnalysisResult

class MainViewModel(
    private val pythonBridge: PythonBridge
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun runAnalysis() {
        scope.launch {
            _state.value = UiState.Loading

            val testData = doubleArrayOf(
                1.2, 1.5, 1.3, 1.8, 1.6,
                2.1, 1.9, 2.3, 2.0, 2.4,
                1.7, 1.8, 2.2, 1.9, 2.1
            )

            val result = pythonBridge.analyzeTimeSeries(y = testData)

            _state.value = result.fold(
                onSuccess = { analysis ->
                    UiState.Success(analysis.y)
                },
                onFailure = { error ->
                    UiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val result: SeriesAnalysisResult
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
}
