package org.example.project_dw.shared.datasources.python

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project_dw.shared.models.MultiSeriesInput
import org.example.project_dw.shared.models.MultiSeriesAnalysisResult
import java.io.File

class PythonBridge {
  private val paths = PythonPathResolver.resolve()

  suspend fun analyzeTimeSeries(
    series: List<DoubleArray>
  ): Result<MultiSeriesAnalysisResult> = withContext(Dispatchers.IO) {
    try {
      val input = MultiSeriesInput(
        series = series.map { it.toList() }
      )

      val inputJson = PythonSerializer.serialize(
        MultiSeriesInput.serializer(),
        input
      )

      val output = executePython(inputJson)

      val result = PythonSerializer.deserialize(
        MultiSeriesAnalysisResult.serializer(),
        output
      )

      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun executePython(inputJson: String): String {
    val file = File(paths.executable)
    if (!file.exists()) {
      throw PythonNotFoundException("Python not found: ${paths.executable}")
    }

    val processArgs = if (paths.isProd) {
      listOf(paths.executable, inputJson)
    } else {
      listOf(paths.executable, paths.script, inputJson)
    }

    val process = ProcessBuilder(processArgs)
      .redirectErrorStream(false)
      .start()

    val output = process.inputStream
      .bufferedReader()
      .readText()

    val exitCode = process.waitFor()

    if (exitCode != 0) {
      throw PythonExecutionException("Python failed: $output")
    }

    return output
  }
}

class PythonNotFoundException(message: String) : Exception(message)
class PythonExecutionException(message: String) : Exception(message)
