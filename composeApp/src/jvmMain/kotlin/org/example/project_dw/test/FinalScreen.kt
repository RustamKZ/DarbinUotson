package org.example.project_dw.test

import TimeSeriesPreview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project_dw.presentation.screens.main.MainViewModel.UiState
import org.example.project_dw.shared.datasources.python.PythonApiException
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.shared.models.TimeSeriesAnalysisResult
import org.example.project_dw.shared.models.TimeSeriesRequest

class FinalScreen(
    private val viewModel: MainViewModel,
    val pythonBridge: PythonBridge,
    val data: TimeSeriesRequest
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var result by remember { mutableStateOf<Result<TimeSeriesAnalysisResult>?>(null) }
        LaunchedEffect(Unit) {
            result = pythonBridge.analyzeTimeSeries(data)
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(0.5f),
                    verticalArrangement = Arrangement.Top
                ) {
                    TimeSeriesPreview(req = data, maxRows = 80)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { navigator.pop() }) {
                            Text("Назад")
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize(0.5f),
                    verticalArrangement = Arrangement.Top
                ) {
                    result?.fold(
                        onSuccess = { analysis ->
                            println("✅ Analysis completed successfully!")
                            println("   Target: ${analysis.targetVariable}")
                            println("   Model: ${analysis.modelType}")
                            println("   R²: ${analysis.modelResults?.regression?.rSquared}")
                            UiState.Success(analysis)
                        },
                        onFailure = { error ->
                            println("❌ Analysis failed!")
                            when (error) {
                                is PythonApiException -> {
                                    println("   Error code: ${error.errorCode}")
                                    println("   Message: ${error.message}")
                                    UiState.Error("API Error [${error.errorCode}]: ${error.message}")
                                }

                                else -> {
                                    println("   Error: ${error.message}")
                                    UiState.Error(error.message ?: "Unknown error")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}