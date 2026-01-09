package org.example.project_dw.presentation.screens.main

import androidx.compose.foundation.layout.*
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { viewModel.runAdfTest() }) {
            Text("Run ADF Test")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is MainViewModel.UiState.Idle -> {
                Text("Press button to test")
            }
            is MainViewModel.UiState.Loading -> {
                CircularProgressIndicator()
            }
            is MainViewModel.UiState.Success -> {
                Column {
                    Text("Stationary: ${s.result.isStationary}")
                    Text("p-value: ${s.result.pValue}")
                    Text(
                        "test statistic: ${s.result.testStatistic}"
                    )
                }
            }
            is MainViewModel.UiState.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colors.error)
            }
        }
    }
}
