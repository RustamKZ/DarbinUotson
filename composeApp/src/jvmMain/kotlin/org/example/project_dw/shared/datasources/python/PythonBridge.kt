package org.example.project_dw.shared.datasources.python

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project_dw.shared.models.TimeSeriesRequest
import org.example.project_dw.shared.models.TimeSeriesAnalysisResult
import org.example.project_dw.shared.models.ApiError
import java.io.File

class PythonBridge {
  private val paths = PythonPathResolver.resolve()

  suspend fun analyzeTimeSeries(
    request: TimeSeriesRequest
  ): Result<TimeSeriesAnalysisResult> = withContext(Dispatchers.IO) {
    try {
      val inputJson = PythonSerializer.serialize(
        TimeSeriesRequest.serializer(),
        request
      )
        println("===== JSON SENT TO PYTHON =====")
        println(inputJson)
        println("===== END JSON =====")

      val output = executePython(inputJson)

      if (output.contains("\"error\"")) {
        val error = PythonSerializer.deserialize(
          ApiError.serializer(),
          output
        )
        return@withContext Result.failure(
          PythonApiException(error.error, error.message)
        )
      }

      val result = PythonSerializer.deserialize(
        TimeSeriesAnalysisResult.serializer(),
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
    // MUST BE false because we need to separate sys.stdout and sys.stderr
      .redirectErrorStream(false)
      .start()

    val output = process.inputStream
      .bufferedReader()
      .readText()

    val errors = process.errorStream
      .bufferedReader()
      .readText()

    val exitCode = process.waitFor()

    if (exitCode != 0) {
      throw PythonExecutionException(
        "Python failed with exit code $exitCode\nStdout: $output\nStderr: $errors"
      )
    }

    return output
  }
}

class PythonNotFoundException(message: String) : Exception(message)
class PythonExecutionException(message: String) : Exception(message)
class PythonApiException(val errorCode: String, message: String) : Exception(message)
