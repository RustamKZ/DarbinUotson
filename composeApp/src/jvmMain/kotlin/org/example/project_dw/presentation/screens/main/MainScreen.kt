package org.example.project_dw.presentation.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Time Series Analysis",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { viewModel.runAnalysis() }) {
            Text("Run Integration Order Analysis")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val s = state) {
            is MainViewModel.UiState.Idle -> {
                Text("Press button to analyze test data")
            }

            is MainViewModel.UiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzing...")
            }

            is MainViewModel.UiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Analysis Results",
                            style = MaterialTheme.typography.h6
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Integration Order
                        Row {
                            Text("Integration Order: ", style = MaterialTheme.typography.body1)
                            Text(
                                "I(${s.result.order})",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.primary
                            )
                        }

                        if (s.result.hasConflict) {
                            Text(
                                "conflict detected (structural break possible)",
                                color = MaterialTheme.colors.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // ADF Results
                        Text("ADF Test:", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stationary: ${s.result.adf.isStationary}")
                        Text("Test Statistic: ${"%.4f".format(s.result.adf.testStatistic)}")
                        Text("P-Value: ${"%.4f".format(s.result.adf.pValue)}")
                        Text("Used Lag: ${s.result.adf.usedLag}")
                        Text("N Observations: ${s.result.adf.nObs}")

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // KPSS Results
                        Text("KPSS Test:", style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stationary: ${s.result.kpss.pValue > 0.05}")
                        Text("Test Statistic: ${"%.4f".format(s.result.kpss.kpssStat)}")
                        Text("P-Value: ${"%.4f".format(s.result.kpss.pValue)}")
                        Text("Lags: ${s.result.kpss.lags}")
                    }
                }
            }

            is MainViewModel.UiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.message,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}
