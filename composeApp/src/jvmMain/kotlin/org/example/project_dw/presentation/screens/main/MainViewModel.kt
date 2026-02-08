package org.example.project_dw.presentation.screens.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.shared.datasources.python.PythonApiException
import org.example.project_dw.shared.models.TimeSeriesRequest
import org.example.project_dw.shared.models.SeriesData
import org.example.project_dw.shared.models.TimeSeriesAnalysisResult
import java.io.File

class MainViewModel(
  private val pythonBridge: PythonBridge
) {
  private val scope = CoroutineScope(Dispatchers.Main)
  private val _state = MutableStateFlow<UiState>(UiState.Idle)
  val state: StateFlow<UiState> = _state

  fun runAnalysis() {
    scope.launch {
      _state.value = UiState.Loading

      try {
        val csvPath = findCsvPath()
        val data = loadUsaDataFromCsv(csvPath)

        // 🆕 Логируем первые 10 значений каждого ряда
        println("\n========================================")
        println("📊 DATA PREVIEW (first 10 values):")
        println("========================================")
        println("temperature_celsius:")
        println(data.temperature.take(10).joinToString(", ") { "%.2f".format(it) })
        println("\npm25_ugm3:")
        println(data.pm25.take(10).joinToString(", ") { "%.2f".format(it) })
        println("\nrespiratory_disease_rate:")
        println(data.respiratory.take(10).joinToString(", ") { "%.2f".format(it) })
        println("\n📏 Total records: ${data.temperature.size}")
        println("========================================\n")

        val request = TimeSeriesRequest(
          series = listOf(
            SeriesData(
              name = "temperature_celsius",
              data = data.temperature
            ),
            SeriesData(
              name = "pm25_ugm3",
              data = data.pm25
            ),
            SeriesData(
              name = "respiratory_disease_rate",
              data = data.respiratory
            )
          ),
          targetIndex = null
        )

        println("🚀 Sending request to Python...")

        val result = pythonBridge.analyzeTimeSeries(request)

        _state.value = result.fold(
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
      } catch (e: Exception) {
        println("❌ Exception occurred!")
        println("   ${e.message}")
        e.printStackTrace()
        _state.value = UiState.Error("Exception: ${e.message}\n${e.stackTraceToString()}")
      }
    }
  }

  private fun findCsvPath(): String {
    val userDir = System.getProperty("user.dir")
    
    val candidates = listOf(
      "$userDir/python_engine/datasets/global_climate_health_impact_tracker_2015_2025.csv",
      "$userDir/../python_engine/datasets/global_climate_health_impact_tracker_2015_2025.csv",
      "${File(userDir).parent}/python_engine/datasets/global_climate_health_impact_tracker_2015_2025.csv"
    )
    
    for (path in candidates) {
      if (File(path).exists()) {
        println("📂 Found CSV at: $path")
        return path
      }
    }
    
    throw Exception("CSV file not found! Checked:\n${candidates.joinToString("\n")}")
  }

  private fun loadUsaDataFromCsv(csvPath: String): UsaData {
    val file = File(csvPath)
    val lines = file.readLines()
    
    println("📖 Reading CSV: ${lines.size} total lines")
    
    val temperature = mutableListOf<Double>()
    val pm25 = mutableListOf<Double>()
    val respiratory = mutableListOf<Double>()
    
    var usaCount = 0
    
    lines.drop(1).forEach { line ->
      val parts = line.split(",")
      
      if (parts.size >= 23 && parts[1] == "USA") {
        usaCount++
        try {
          temperature.add(parts[12].toDouble())
          pm25.add(parts[19].toDouble())
          respiratory.add(parts[21].toDouble())
        } catch (e: Exception) {
          println("⚠️ Parsing error on line $usaCount: ${e.message}")
        }
      }
    }
    
    println("✅ Loaded USA data: $usaCount records")
    println("   temperature: ${temperature.size} values")
    println("   pm25: ${pm25.size} values")
    println("   respiratory: ${respiratory.size} values")
    
    if (temperature.isEmpty()) {
      throw Exception("No USA data found in CSV!")
    }
    
    return UsaData(
      temperature = temperature,
      pm25 = pm25,
      respiratory = respiratory
    )
  }

  data class UsaData(
    val temperature: List<Double>,
    val pm25: List<Double>,
    val respiratory: List<Double>
  )

  sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(
      val result: TimeSeriesAnalysisResult
    ) : UiState()
    data class Error(val message: String) : UiState()
  }
}