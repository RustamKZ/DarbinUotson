package org.example.project_dw.test

import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess
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
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// Этот data class для этапа 5
data class OutlierResult(
    val outlierIndices: List<Int>,
    val methodUsed: String,
    val cleanData: DoubleArray
)

class MainViewModel {
    var selectedColumns by mutableStateOf(setOf<Int>())
        private set
    // Числовая матрица
    // этап 3
    var jarqueBeraResults by mutableStateOf<Map<Int, JBResult>>(emptyMap())
        private set
    // этап 4
    var stlResults by mutableStateOf<Map<Int, STLResult>>(emptyMap())
    // этап 5
    var outlierResults by mutableStateOf<Map<Int, OutlierResult>>(emptyMap())
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

    data class STLResult(
        val trend: DoubleArray,
        val seasonal: DoubleArray,
        val residual: DoubleArray
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

    fun runSTLDecomposition(period: Int = 288) {
        val currentData = _csvData.value ?: return
        if (selectedColumns.isEmpty()) return

        val results = mutableMapOf<Int, STLResult>()

        selectedColumns.forEach { columnIndex ->
            val columnData = currentData.matrix.map { row -> row[columnIndex] }.toDoubleArray()
            val detectedPeriod = if (period == 0) {
                findPeriod(columnData, maxPeriod = 500)
            } else {
                period
            }
            try {
                val result = performSTLDecomposition(columnData, detectedPeriod)
                results[columnIndex] = result

                logger.d { "Column $columnIndex STL decomposed" }
                logger.d { "Trend mean: ${result.trend.average()}" }
                logger.d { "Seasonal mean: ${result.seasonal.average()}" }
                logger.d { "Residual mean: ${result.residual.average()}" }
            } catch (e: Exception) {
                logger.e(e) { "STL decomposition failed for column $columnIndex" }
            }
        }
        stlResults = results
        debugInfo = "STL декомпозиция выполнена для ${results.size} столбцов"
    }

    private fun performSTLDecomposition(data: DoubleArray, period: Int = 288): STLResult {
        val stl = SeasonalTrendLoess.Builder()
            .setPeriodLength(period)                        // Период сезонности
            .setSeasonalWidth(10 * period + 1)              // Ширина окна для сезонности
            .setTrendWidth((1.5 * period).toInt() + 1)      // Ширина окна для тренда
            /* Кароч это рекомендуемые статьи по STL (Cleveland et al., 1990):
                - **SeasonalWidth**: `10 * period + 1` - должно быть **нечётным** и больше периода
                - **TrendWidth**: `1.5 * period + 1` (округлённое до нечётного) - сглаживает тренд*/
            .setInnerIterations(2)                          // Внутренние итерации
            .setRobust()
            .buildSmoother(data)

        // Выполняем декомпозицию
        val result = stl.decompose()

        return STLResult(
            trend = result.trend,
            seasonal = result.seasonal,
            residual = result.residual
        )
    }

    fun findPeriod(data: DoubleArray, maxPeriod: Int = 500): Int {
        // Простой поиск пиков в автокорреляции
        val correlations = DoubleArray(maxPeriod)

        for (lag in 1 until maxPeriod) {
            var sum = 0.0
            var count = 0

            for (i in 0 until data.size - lag) {
                sum += data[i] * data[i + lag]
                count++
            }

            correlations[lag] = if (count > 0) sum / count else 0.0
        }

        // Находим максимум после lag=1
        var maxCorr = correlations[1]
        var maxLag = 1

        for (lag in 2 until maxPeriod) {
            if (correlations[lag] > maxCorr) {
                maxCorr = correlations[lag]
                maxLag = lag
            }
        }

        logger.d { "Detected period: $maxLag with correlation: $maxCorr" }
        return maxLag
    }

    // ЭТАП 5

    fun detectAndFixOutliers(strategy: String = "INTERPOLATE") {
        val currentData = _csvData.value ?: return
        val results = mutableMapOf<Int, OutlierResult>()

        selectedColumns.forEach { columnIndex ->
            val stl = stlResults[columnIndex] ?: return@forEach
            val isNormal = jarqueBeraResults[columnIndex]?.isNormal ?: false
            val residuals = stl.residual

            // 1. Поиск индексов выбросов
            val outlierIndices = if (isNormal) {
                findOutliersZScore(residuals)
            } else {
                findOutliersIQR(residuals)
            }

            // 2. Обработка (удаление / интерполяция)
            // Работаем с исходным рядом, заменяя значения по индексам выбросов
            val originalData = currentData.matrix.map { it[columnIndex] }.toDoubleArray()
            val fixedData = applyOutlierStrategy(originalData, outlierIndices, strategy)

            results[columnIndex] = OutlierResult(
                outlierIndices = outlierIndices,
                methodUsed = if (isNormal) "Z-Score" else "IQR",
                cleanData = fixedData
            )
        }
        outlierResults = results
    }

    private fun findOutliersZScore(data: DoubleArray, threshold: Double = 3.5): List<Int> {
        val mean = data.average()
        val stdDev = sqrt(data.map { (it - mean).pow(2) }.average())
        return data.indices.filter { abs(data[it] - mean) > threshold * stdDev }
    }

    private fun findOutliersIQR(data: DoubleArray): List<Int> {
        val sorted = data.sorted()
        val q1 = sorted[(sorted.size * 0.25).toInt()]
        val q3 = sorted[(sorted.size * 0.75).toInt()]
        val iqr = q3 - q1
        val lowerBound = q1 - 1.5 * iqr
        val upperBound = q3 + 1.5 * iqr
        return data.indices.filter { data[it] < lowerBound || data[it] > upperBound }
    }

    private fun applyOutlierStrategy(data: DoubleArray, indices: List<Int>, strategy: String): DoubleArray {
        val result = data.copyOf()
        val indexSet = indices.toSet()
        when (strategy) {
            "WINSORIZE" -> {
                // Замена на граничные значения (упрощенно: на медиану или ближайший предел)
                val median = data.sorted()[data.size / 2]
                indices.forEach { result[it] = median }
            }
            "INTERPOLATE" -> {
                // Замена на среднее между соседями
                indices.forEach { i ->
                    val prev = if (i > 0) result[i-1] else result.firstOrNull { !indexSet.contains(it.toInt()) } ?: 0.0
                    val next = if (i < result.size - 1) result[i+1] else prev
                    result[i] = (prev.toDouble() + next.toDouble()) / 2.0
                }
            }
        }
        return result
    }

}
