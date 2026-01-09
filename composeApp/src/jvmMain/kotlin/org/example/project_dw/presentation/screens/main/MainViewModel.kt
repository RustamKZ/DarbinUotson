package org.example.project_dw.presentation.screens.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project_dw.domain.models.StationarityResult
import org.example.project_dw.domain.usecases.CheckStationarityUseCase

class MainViewModel(
    private val checkStationarity: CheckStationarityUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun runAdfTest() {
        scope.launch {
            _state.value = UiState.Loading

            val testData = doubleArrayOf(
                1.2, 1.5, 1.3, 1.8, 1.6,
                2.1, 1.9, 2.3, 2.0, 2.4
            )

            val result = checkStationarity(testData)

            _state.value = when (result) {
                is StationarityResult.Success ->
                    UiState.Success(result)
                is StationarityResult.Error ->
                    UiState.Error(result.message)
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val result: StationarityResult.Success
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
}
