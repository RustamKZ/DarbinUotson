package org.example.project_dw.shared.datasources.python

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project_dw.shared.models.TimeSeriesInput
import org.example.project_dw.shared.models.FullAnalysisResult
import java.io.File

class PythonBridge {

    private val paths = PythonPathResolver.resolve()

    // time series analyzer
    suspend fun analyzeTimeSeries(
        y: DoubleArray,
        x: DoubleArray? = null
    ): Result<FullAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val input = TimeSeriesInput(
                y = y.toList(),
                x = x?.toList()
            )

            val inputJson = PythonSerializer.serialize(
                TimeSeriesInput.serializer(),
                input
            )

            val outputJson = executePython(inputJson)

            val result = PythonSerializer.deserialize(
                FullAnalysisResult.serializer(),
                outputJson
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
        // neccessary because need to split stderr and stdout
            .redirectErrorStream(false)
            .start()

        val output = process.inputStream
            .bufferedReader()
            .readText()

        val errorOutput = process.errorStream
            .bufferedReader()
            .readText()

        val exitCode = process.waitFor()

        // TODO: debug delete when ready
        println("=== PYTHON STDOUT ===")
        println(output)
        println("=== PYTHON STDERR ===")
        println(errorOutput)
        println("=== EXIT CODE: $exitCode ===")

        if (exitCode != 0) {
            throw PythonExecutionException("Python failed: $output")
        }

        return output
    }

    private fun validatePythonExists() {
        val file = File(paths.executable)
        if (!file.exists()) {
            val message = if (paths.isProd) {
                "Production binary not found. Build python runtime first."
            } else {
                "Python venv not found. Setup python venv first."
            }
            throw PythonNotFoundException("$message\nPath: ${paths.executable}")
        }
    }

    private fun buildProcessArgs(inputJson: String): List<String> {
        return if (paths.isProd) {
            listOf(paths.executable, inputJson)
        } else {
            listOf(paths.executable, paths.script, inputJson)
        }
    }
}

class PythonNotFoundException(message: String) : Exception(message)
class PythonExecutionException(message: String) : Exception(message)
