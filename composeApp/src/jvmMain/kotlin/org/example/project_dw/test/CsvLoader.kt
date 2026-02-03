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
    val matrix: List<List<CsvValue>>
)

sealed class CsvValue {
    data class Num(val value: Double) : CsvValue()
    data class Text(val value: String) : CsvValue()
}

fun CsvValue.asDoubleOrNaN(): Double = when (this) {
    is CsvValue.Num -> value
    is CsvValue.Text -> value.toDoubleOrNull() ?: Double.NaN
}

fun CsvValue.asDisplayText(): String = when (this) {
    is CsvValue.Num -> if (value.isNaN()) "NaN" else "%.4f".format(value)
    is CsvValue.Text -> value
}

fun CsvValue.isBadNumberOrText(): Boolean = when (this) {
    is CsvValue.Num -> value.isNaN()
    is CsvValue.Text -> true // строка — не число
}

fun CsvData.isNumericColumn(col: Int): Boolean {
    if (matrix.isEmpty()) return false
    if (col !in matrix.first().indices) return false
    return matrix.all { row ->
        when (val v = row[col]) {
            is CsvValue.Num -> true
            is CsvValue.Text -> v.value.toDoubleOrNull() != null
        }
    }
}

fun CsvValue.asString(): String = when (this) {
    is CsvValue.Num -> value.toString()
    is CsvValue.Text -> value
}

fun CsvData.columnIndexOrNull(name: String): Int? =
    headers.indexOfFirst { it.equals(name, ignoreCase = true) }
        .takeIf { it >= 0 }


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
            selectedColumns.map { colIndex ->
                val cell = row[colIndex]
                cell.toDoubleOrNull()?.let { CsvValue.Num(it) }
                    ?: CsvValue.Text(cell)
            }
        }

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