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

    // для выбора страны
    var selectedCountry by mutableStateOf<String?>(null)
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
    // для выбора страны
    fun availableCountries(): List<String> {
        val data = _csvData.value ?: return emptyList()
        val countryCol = data.columnIndexOrNull("country_name") ?: return emptyList()

        return data.matrix
            .mapNotNull { row -> row.getOrNull(countryCol)?.asString()?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun selectCountry(country: String) {
        val current = _csvData.value ?: return
        val countryCol = current.columnIndexOrNull("country_name")
            ?: run {
                error = "Column 'country_name' not found"
                return
            }

        val filteredMatrix = current.matrix.filter { row ->
            row.getOrNull(countryCol)?.asString()?.trim() == country
        }

        _csvData.value = current.copy(matrix = filteredMatrix)
        selectedCountry = country
        selectedColumns = emptySet()
        jarqueBeraResults = emptyMap()
        stlResults = emptyMap()
        outlierResults = emptyMap()

        debugInfo = "Фильтр применён: $country (rows=${filteredMatrix.size})"
    }


    fun toggleColumnSelection(index: Int) {
        val data = csvData.value ?: return
        if (!data.isNumericColumn(index)) return

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
            val columnData = currentData.matrix.map { row -> row[columnIndex].asDoubleOrNaN() }.toDoubleArray()
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
            val columnData = currentData.matrix.map { row -> row[columnIndex].asDoubleOrNaN() }.toDoubleArray()
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
                logger.e(e) {
                    "STL decomposition failed for column $columnIndex; " +
                            "n=${columnData.size}, period=$detectedPeriod, " +
                            "nanCount=${columnData.count { it.isNaN() }}, " +
                            "infCount=${columnData.count { !it.isFinite() }}"
                }
                error = "STL failed: col=$columnIndex (${e.message})"
            }
        }
        stlResults = results
        debugInfo = "STL декомпозиция выполнена для ${results.size} столбцов"
    }

    private fun oddAtMost(x: Int, max: Int): Int {
        var v = minOf(x, max)
        if (v % 2 == 0) v -= 1
        return v.coerceAtLeast(3) // минимально разумное нечётное
    }


    private fun performSTLDecomposition(data: DoubleArray, period: Int = 288): STLResult {
        val n = data.size

        // период должен быть хотя бы 2 и меньше n
        val safePeriod = period.coerceIn(2, n - 1)

        // Максимально допустимая ширина окна (нечётная и <= n-1)
        val maxWidth = if ((n - 1) % 2 == 1) (n - 1) else (n - 2)

        // Было: 10*period+1 (слишком много). Делаем безопасно:
        val seasonalTarget = 10 * safePeriod + 1
        val seasonalWidth = oddAtMost(seasonalTarget, maxWidth)

        // Тренд тоже клампим
        val trendTarget = (1.5 * safePeriod).toInt() + 1
        val trendWidth = oddAtMost(trendTarget, maxWidth)

        logger.d {
            "STL params: n=$n period=$safePeriod seasonalWidth=$seasonalWidth trendWidth=$trendWidth"
        }

        val stl = SeasonalTrendLoess.Builder()
            .setPeriodLength(safePeriod)
            .setSeasonalWidth(seasonalWidth)
            .setTrendWidth(trendWidth)
            .setInnerIterations(2)
            .setRobust()
            .buildSmoother(data)

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
            val originalData = currentData.matrix.map { it[columnIndex].asDoubleOrNaN() }.toDoubleArray()
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
                val median = data.sorted()[data.size / 2]
                indices.forEach { result[it] = median }
            }
            "INTERPOLATE" -> {
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
