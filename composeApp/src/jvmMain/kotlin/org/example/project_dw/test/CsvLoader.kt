package org.example.project_dw.test

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// Для "сырого" чтения CSV
data class RawCsv(
    val headers: List<String>,
    val rows: List<List<String>>
)

// Для матрицы чисел после выбора колонок
data class CsvData(
    val headers: List<String>,
    val matrix: List<DoubleArray>
)

object CsvLoader {

    // Шаг 1: загрузка как строки
    fun loadRaw(
        file: File,
        delimiter: Char = ',',
        hasHeader: Boolean = true
    ): RawCsv {
        val headers = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()

        file.useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (line.isBlank()) return@forEachIndexed

                val parts = line.split(delimiter).map { it.trim() }

                if (index == 0 && hasHeader) {
                    headers.addAll(parts)
                } else {
                    rows.add(parts)
                }
            }
        }

        return RawCsv(headers, rows)
    }

    // Шаг 2: преобразование выбранных колонок в числовую матрицу
    fun extractNumericMatrix(raw: RawCsv, selectedColumns: List<Int>): CsvData {
        val matrix = raw.rows.map { row ->
            DoubleArray(selectedColumns.size) { i ->
                row[selectedColumns[i]].toDoubleOrNaN()
            }
        }.toList()

        val headers = selectedColumns.map { raw.headers[it] }
        return CsvData(headers, matrix)
    }

    private fun String.toDoubleOrNaN(): Double = this.toDoubleOrNull() ?: Double.NaN
}

fun chooseCsvFile(): File? {
    val dialog = FileDialog(null as Frame?, "Choose CSV", FileDialog.LOAD)
    dialog.isVisible = true
    return dialog.file?.let { File(dialog.directory, it) }
}