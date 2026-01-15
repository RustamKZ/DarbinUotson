package org.example.project_dw.test

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project_dw.test.fill_gaps.LinearInterpolation
import java.io.File
import kotlin.collections.forEach
import kotlin.math.pow
import kotlin.math.sqrt

class MainViewModel {
    var selectedColumns by mutableStateOf(setOf<Int>())
        private set
    // Числовая матрица

    var jarqueBeraResults by mutableStateOf<Map<Int, JBResult>>(emptyMap())
        private set

    private val _csvData = MutableStateFlow<CsvData?>(null)
    val csvData: StateFlow<CsvData?> = _csvData.asStateFlow()

    private val logger = Logger.withTag("CSV")

    // Сырые данные
    var rawCsv by mutableStateOf<RawCsv?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var debugInfo by mutableStateOf<String?>(null)
        private set

    data class JBResult(
        val statistic: Double,
        val isNormal: Boolean
    )

    fun toggleColumnSelection(index: Int) {
        selectedColumns = if (selectedColumns.contains(index)) {
            selectedColumns - index
        } else {
            selectedColumns + index
        }
    }

    fun applyInterpolation() {
        val currentData = _csvData.value ?: return
        if (selectedColumns.isEmpty()) return

        // Интерполируем только выбранные
        _csvData.value = LinearInterpolation.interpolateSpecificColumns(
            currentData,
            selectedColumns.toList()
        )

        // Очищаем выбор после обработки (опционально)
        debugInfo = "Интерполяция применена к выбранным столбцам"
    }

    // Загружаем CSV
    fun loadCsv(file: File) {
        try {
            val raw = CsvLoader.loadRaw(file)
            rawCsv = raw
            debugInfo = "CSV loaded: ${raw.rows.size} rows"

            // Автоматически выбираем все колонки после загрузки
            val allIndices = raw.headers.indices.toList()
            selectColumns(allIndices)

            error = null
        } catch (e: Exception) {
            error = "Failed to load CSV: ${e.message}"
        }
    }

    // Создаём числовую матрицу из выбранных колонок
    fun selectColumns(selectedColumns: List<Int>) {
        try {
            val data = rawCsv?.let { CsvLoader.extractNumericMatrix(it, selectedColumns) }
            _csvData.value = data
            debugInfo = "Matrix: ${data?.matrix?.size ?: 0} rows, ${data?.matrix?.get(0)?.size ?: 0} columns"
            logger.i { debugInfo ?: "" }
            error = null
        } catch (e: Exception) {
            error = "Failed to extract numeric matrix: ${e.message}"
            logger.e(e) { error ?: "" }
        }
    }

    fun runJarqueBeraTest() {
        val currentData = _csvData.value ?: return
        if (selectedColumns.isEmpty()) return

        val results = mutableMapOf<Int, JBResult>()

        selectedColumns.forEach { columnIndex ->
            val columnData = currentData.matrix.map { row -> row[columnIndex] }.toDoubleArray()
            val result = calculateJarqueBera(columnData)
            results[columnIndex] = result
        }

        jarqueBeraResults = results
    }

    private fun calculateJarqueBera(data: DoubleArray): JBResult {
        val n = data.size.toDouble()
        val mean = data.average()

        // Стандартное отклонение s
        val s = sqrt(data.sumOf { (it - mean).pow(2) } / n)

        logger.d { "Sample size: $n" }
        logger.d { "Mean: $mean, StdDev: $s" }

        // Skewness (S)
        val skewness = (data.sumOf { (it - mean).pow(3) } / n) / s.pow(3)

        // Kurtosis (K)
        val kurtosis = (data.sumOf { (it - mean).pow(4) } / n) / s.pow(4)

        logger.d { "Skewness: $skewness, Kurtosis: $kurtosis" }
        val jb = (n / 6.0) * (skewness.pow(2) + (kurtosis - 3).pow(2) / 4.0)
        logger.d { "JB statistic: $jb" }

        val criticalValue = 5.991
        val isNormal = jb < criticalValue

        return JBResult(jb, isNormal)

        /*
        Уровень значимости (α) Критическое значение
        α = 0.10 (90%)         4.605
        α = 0.05 (95%)         5.991
        α = 0.01 (99%)         9.210
        α = 0.001 (99.9%)      13.816
         */
    }
}
