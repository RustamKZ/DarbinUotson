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
import org.example.project_dw.shared.models.MultiSeriesAnalysisResult
import org.example.project_dw.shared.models.SeriesAnalysisResult
import org.example.project_dw.shared.models.AdfTestResult
import org.example.project_dw.shared.models.KpssTestResult

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
      Text("Run Analysis")
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
private fun SuccessView(result: MultiSeriesAnalysisResult) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = 4.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Analysis Results",
        style = MaterialTheme.typography.h6
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text("Series Count: ${result.seriesCount}")

      result.seriesOrders.forEachIndexed { index, series ->
        Spacer(modifier = Modifier.height(24.dp))
        SeriesCard(index = index + 1, series = series)
      }
    }
  }
}

@Composable
private fun SeriesCard(index: Int, series: SeriesAnalysisResult) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = 2.dp,
    backgroundColor = MaterialTheme.colors.surface
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = "Series $index",
        style = MaterialTheme.typography.h6
      )

      Spacer(modifier = Modifier.height(8.dp))

      IntegrationOrderRow(order = series.order, hasConflict = series.hasConflict)

      Spacer(modifier = Modifier.height(12.dp))
      Divider()
      Spacer(modifier = Modifier.height(12.dp))

      AdfTestSection(adf = series.adf)

      Spacer(modifier = Modifier.height(12.dp))
      Divider()
      Spacer(modifier = Modifier.height(12.dp))

      KpssTestSection(kpss = series.kpss)
    }
  }
}

@Composable
private fun IntegrationOrderRow(order: Int, hasConflict: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Integration Order: ", style = MaterialTheme.typography.body1)
    Text(
      "I($order)",
      style = MaterialTheme.typography.body1,
      color = MaterialTheme.colors.primary
    )
  }

  if (hasConflict) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      "⚠ conflict detected",
      color = MaterialTheme.colors.error,
      style = MaterialTheme.typography.caption
    )
  }
}

@Composable
private fun AdfTestSection(adf: AdfTestResult) {
  Text("ADF Test:", style = MaterialTheme.typography.subtitle1)
  Spacer(modifier = Modifier.height(4.dp))
  TestResultRow("Stationary", adf.isStationary.toString())
  TestResultRow("Test Statistic", "%.4f".format(adf.testStatistic))
  TestResultRow("P-Value", "%.4f".format(adf.pValue))
  TestResultRow("Used Lag", adf.usedLag.toString())
  TestResultRow("N Observations", adf.nObs.toString())
}

@Composable
private fun KpssTestSection(kpss: KpssTestResult) {
  Text("KPSS Test:", style = MaterialTheme.typography.subtitle1)
  Spacer(modifier = Modifier.height(4.dp))
  TestResultRow("Stationary", (kpss.pValue > 0.05).toString())
  TestResultRow("Test Statistic", "%.4f".format(kpss.kpssStat))
  TestResultRow("P-Value", "%.4f".format(kpss.pValue))
  TestResultRow("Lags", kpss.lags.toString())
}

@Composable
private fun TestResultRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.body2)
    Text(value, style = MaterialTheme.typography.body2)
  }
}

@Composable
private fun ErrorView(message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        "Error",
        style = MaterialTheme.typography.h6,
        color = MaterialTheme.colors.error
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        message,
        color = MaterialTheme.colors.error
      )
    }
  }
}
