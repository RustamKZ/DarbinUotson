package org.example.project_dw.presentation.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.koinInject
import org.example.project_dw.shared.models.TimeSeriesAnalysisResult
import org.example.project_dw.shared.models.SeriesOrder

class MainScreen : Screen {
    @Composable
    override fun Content() {
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
                text = "DarbinUotson - Time Series Analysis",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { viewModel.runAnalysis() }) {
                Text("Run Test Analysis")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val s = state) {
                is MainViewModel.UiState.Idle -> IdleView()
                is MainViewModel.UiState.Loading -> LoadingView()
                is MainViewModel.UiState.Success -> SuccessView(s.result)
                is MainViewModel.UiState.Error -> ErrorView(s.message)
            }
        }
    }
}

@Composable
private fun IdleView() {
    Text("Press button to analyze test data")
}

@Composable
private fun LoadingView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Analyzing...")
    }
}

@Composable
private fun SuccessView(result: TimeSeriesAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✅ Analysis Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Основная информация
            InfoRow("Series Count", result.seriesCount.toString())
            InfoRow("Target Variable", result.targetVariable)
            InfoRow("Model Type", result.modelType)
            InfoRow("Structural Break", if (result.hasStructuralBreak) "Yes" else "No")

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Variables:", style = MaterialTheme.typography.titleMedium)
            result.variableNames.forEachIndexed { index, name ->
                Text(
                    "  ${index + 1}. $name",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Series analysis
            Text("Series Analysis:", style = MaterialTheme.typography.titleLarge)
            result.seriesOrders.forEachIndexed { index, series ->
                Spacer(modifier = Modifier.height(12.dp))
                SeriesCard(name = result.variableNames[index], series = series)
            }

            // Regression results
            result.modelResults?.regression?.let { regression ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Regression Results:", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("R²", "%.4f".format(regression.rSquared))
                InfoRow("Adj R²", "%.4f".format(regression.adjRSquared))
                InfoRow("F-statistic", "%.4f".format(regression.fStatistic))
                InfoRow("DW statistic", "%.4f".format(regression.durbinWatson.statistic))
                InfoRow("Autocorrelation", if (regression.durbinWatson.hasAutocorrelation) "Yes ⚠️" else "No ✅")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SeriesCard(name: String, series: SeriesOrder) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("Order", "I(${series.order})")
            InfoRow("Conflict", if (series.hasConflict) "Yes ⚠️" else "No ✅")
            InfoRow("Has Trend", if (series.hasTrend) "Yes" else "No")
            InfoRow("Has Seasonality", if (series.hasSeasonality) "Yes" else "No")
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("ADF Test:", style = MaterialTheme.typography.labelLarge)
            InfoRow("  Stationary", if (series.adf.isStationary) "Yes ✅" else "No")
            InfoRow("  p-value", "%.4f".format(series.adf.pValue))
            
            series.za?.let { za ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("Zivot-Andrews:", style = MaterialTheme.typography.labelLarge)
                InfoRow("  Breakpoint", za.breakpoint.toString())
                InfoRow("  Stationary", if (za.isStationary) "Yes ✅" else "No")
            }
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "❌ Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}