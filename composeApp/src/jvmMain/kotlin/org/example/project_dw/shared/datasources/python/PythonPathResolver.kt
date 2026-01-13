package org.example.project_dw.shared.datasources.python

import java.io.File

object PythonPathResolver {

    data class PythonPaths(
        val executable: String,
        val script: String,
        val isProd: Boolean
    )

    fun resolve(): PythonPaths {
        val os = System.getProperty("os.name").lowercase()
        val projectRoot = findProjectRoot()

        println("PythonPathResolver: projectRoot = $projectRoot")

        val prodBinary = when {
            os.contains("win") -> "$projectRoot/python_runtime/windows/stats_engine.exe"
            else -> "$projectRoot/python_runtime/linux/stats_engine"
        }

        val isProd = File(prodBinary).exists()

        return if (isProd) {
            println("PythonPathResolver: PROD mode")
            PythonPaths(
                executable = prodBinary,
                script = "",
                isProd = true
            )
        } else {
            val pythonExe = when {
                os.contains("win") -> "$projectRoot/python_engine/venv/Scripts/python.exe"
                else -> "$projectRoot/python_engine/venv/bin/python"
            }
            println("PythonPathResolver: DEV mode")
            PythonPaths(
                executable = pythonExe,
                script = "$projectRoot/python_engine/main.py",
                isProd = false
            )
        }
    }

    private fun findProjectRoot(): String {
        val userDir = System.getProperty("user.dir")
        val candidates = listOf(
            File(userDir),
            File(userDir).parentFile,
            File(userDir).parentFile?.parentFile,
            File(userDir, "..")
        )

        for (dir in candidates) {
            if (dir != null && dir.exists()) {
                if (File(dir, "python_runtime").exists() ||
                    File(dir, "python_engine").exists()) {
                    return dir.absolutePath
                }
            }
        }

        return userDir
    }
}
