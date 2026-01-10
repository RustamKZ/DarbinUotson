package org.example.project_dw.shared.datasources.python

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.example.project_dw.shared.models.DataInput
import org.example.project_dw.shared.models.AdfTestResult
import java.io.File

class PythonBridge {
    private val json = Json { ignoreUnknownKeys = true }
    private val pythonExe: String
    private val pythonScript: String
    private val isProd: Boolean

    init {
        val os = System.getProperty("os.name").lowercase()
        val projectRoot = findProjectRoot()

        println("PythonBridge: projectRoot = $projectRoot")

        val prodBinary = when {
            os.contains("win") ->
                "$projectRoot/python_runtime/windows/stats_engine.exe"
            else ->
                "$projectRoot/python_runtime/linux/stats_engine"
        }

        isProd = File(prodBinary).exists()

        if (isProd) {
            pythonExe = prodBinary
            pythonScript = ""
            println("PythonBridge: PROD mode - $pythonExe")
        } else {
            pythonExe = when {
                os.contains("win") ->
                    "$projectRoot/python_engine/venv/Scripts/python.exe"
                else ->
                    "$projectRoot/python_engine/venv/bin/python"
            }
            pythonScript = "$projectRoot/python_engine/main.py"
            println("PythonBridge: DEV mode - $pythonExe")
        }
    }

    private fun findProjectRoot(): String {
        // for AppImage: executable in lib/app/
        val userDir = System.getProperty("user.dir")

        val candidates = listOf(
            File(userDir),                           // DEV: DarbinUotson/
            File(userDir).parentFile,                // DEV: composeApp/
            File(userDir).parentFile?.parentFile,    // AppImage: lib/
            File(userDir, ".."),                     // relative path
        )

        for (dir in candidates) {
            if (dir != null && dir.exists()) {
                if (File(dir, "python_runtime").exists() ||
                    File(dir, "python_engine").exists()) {
                    println("Found project root: ${dir.absolutePath}")
                    return dir.absolutePath
                }
            }
        }

        // fallback
        return userDir
    }

    suspend fun runAdfTest(
        data: DoubleArray
    ): Result<AdfTestResult> = withContext(Dispatchers.IO) {
        try {
            val input = json.encodeToString(
                DataInput.serializer(),
                DataInput(data.toList())
            )
            val output = execute(PythonCommands.ADF_TEST, input)
            val result = json.decodeFromString(
                AdfTestResult.serializer(),
                output
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun execute(cmd: String, input: String): String {
        val file = File(pythonExe)
        if (!file.exists()) {
            throw Exception(
                "Python not found: $pythonExe\n" +
                if (isProd) {
                    "Build python runtime first"
                } else {
                    "Setup python venv first"
                }
            )
        }

        val processArgs = if (isProd) {
            listOf(pythonExe, cmd, input)
        } else {
            listOf(pythonExe, pythonScript, cmd, input)
        }

        val process = ProcessBuilder(processArgs)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream
            .bufferedReader()
            .readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw Exception("Python failed: $output")
        }

        return output
    }
}
