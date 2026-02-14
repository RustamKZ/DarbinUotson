package org.example.project_dw.test

import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project_dw.shared.models.SeriesData
import org.example.project_dw.shared.models.TimeSeriesRequest
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
    // этап VIF
    var targetColumn by mutableStateOf<Int?>(null)
        private set

    var vifInfo by mutableStateOf<String?>(null)
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

    fun runSTLDecomposition(period: Int = 52) {
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

    // VIF

    fun setTargetColumn(col : Int) {
        if (!selectedColumns.contains(col)) return
        targetColumn = col
        vifInfo = "Целевая переменная: ${columnName(col)} (col=$col)"
    }

    fun columnName(col: Int): String =
        _csvData.value?.headers?.getOrNull(col) ?: "col_$col"

    private fun pearson(x: DoubleArray, y: DoubleArray): Double {
        // предполагаем одинаковую длину
        val n = x.size
        val mx = x.average()
        val my = y.average()

        var num = 0.0
        var dx = 0.0
        var dy = 0.0
        for (i in 0 until n) {
            val vx = x[i] - mx
            val vy = y[i] - my
            num += vx * vy
            dx += vx * vx
            dy += vy * vy
        }
        val den = kotlin.math.sqrt(dx) * kotlin.math.sqrt(dy)
        return if (den == 0.0) 0.0 else num / den
    }

    private fun r2ByOls(y: DoubleArray, X: Array<DoubleArray>): Double {
        // X: n x p
        val n = y.size
        val p = X[0].size

        // Добавим intercept: X1 = [1, X...]
        val A = Array(p + 1) { DoubleArray(p + 1) }
        val b = DoubleArray(p + 1)

        for (i in 0 until n) {
            val row = DoubleArray(p + 1)
            row[0] = 1.0
            for (j in 0 until p) row[j + 1] = X[i][j]

            // A += row^T row ; b += row^T y
            for (r in 0..p) {
                b[r] += row[r] * y[i]
                for (c in 0..p) A[r][c] += row[r] * row[c]
            }
        }

        val beta = solveLinearSystem(A, b) ?: return 0.0

        val yMean = y.average()
        var ssTot = 0.0
        var ssRes = 0.0

        for (i in 0 until n) {
            var yHat = beta[0]
            for (j in 0 until p) yHat += beta[j + 1] * X[i][j]

            val e = y[i] - yHat
            ssRes += e * e

            val d = y[i] - yMean
            ssTot += d * d
        }

        return if (ssTot == 0.0) 0.0 else (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
    }

    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        // Гаусс с частичным выбором главного элемента
        val n = b.size
        val M = Array(n) { i -> A[i].clone() }
        val x = b.clone()

        for (k in 0 until n) {
            var pivot = k
            var max = kotlin.math.abs(M[k][k])
            for (i in k + 1 until n) {
                val v = kotlin.math.abs(M[i][k])
                if (v > max) { max = v; pivot = i }
            }
            if (max == 0.0) return null

            if (pivot != k) {
                val tmpRow = M[k]; M[k] = M[pivot]; M[pivot] = tmpRow
                val tmp = x[k]; x[k] = x[pivot]; x[pivot] = tmp
            }

            val diag = M[k][k]
            for (j in k until n) M[k][j] /= diag
            x[k] /= diag

            for (i in 0 until n) {
                if (i == k) continue
                val factor = M[i][k]
                if (factor == 0.0) continue
                for (j in k until n) M[i][j] -= factor * M[k][j]
                x[i] -= factor * x[k]
            }
        }
        return x
    }

    private fun computeVif(predictors: List<DoubleArray>): DoubleArray {
        // predictors: list of X columns, each length n
        val p = predictors.size
        val n = predictors[0].size

        // соберём матрицу n x p
        val Xfull = Array(n) { i -> DoubleArray(p) { j -> predictors[j][i] } }

        val vifs = DoubleArray(p)
        for (j in 0 until p) {
            // y = X_j, X = остальные
            val y = DoubleArray(n) { i -> Xfull[i][j] }
            val othersIdx = (0 until p).filter { it != j }
            if (othersIdx.isEmpty()) {
                vifs[j] = 1.0
                continue
            }
            val X = Array(n) { i -> DoubleArray(othersIdx.size) { k -> Xfull[i][othersIdx[k]] } }
            val r2 = r2ByOls(y, X)
            vifs[j] = if (r2 >= 0.999999) Double.POSITIVE_INFINITY else 1.0 / (1.0 - r2)
        }
        return vifs
    }

    fun runVifAndDropLeastRelatedToY(threshold: Double = 10.0) {
        val data = _csvData.value ?: return
        val yCol = targetColumn ?: run {
            vifInfo = "Выберите целевую переменную (Y)"
            return
        }

        // X = все выбранные кроме Y
        var xCols = selectedColumns.filter { it != yCol }.toMutableList()
        if (xCols.size < 2) {
            vifInfo = "Для VIF нужно минимум 2 X (сейчас ${xCols.size})"
            return
        }

        // здесь предполагаем, что NaN уже убраны (интерполяция сделана)
        val y = data.matrix.map { it[yCol].asDoubleOrNaN() }.toDoubleArray()
        if (y.any { it.isNaN() || it.isInfinite() }) {
            vifInfo = "Y содержит NaN/Inf — сначала заполните пропуски"
            return
        }

        fun colAsDouble(c: Int): DoubleArray =
            data.matrix.map { it[c].asDoubleOrNaN() }.toDoubleArray()

        // Проверяем и удаляем, пока VIF плохой
        var iteration = 0
        while (xCols.size >= 2) {
            iteration++

            val predictors = xCols.map { colAsDouble(it) }
            if (predictors.any { col -> col.any { it.isNaN() || it.isInfinite() } }) {
                vifInfo = "X содержит NaN/Inf — сначала заполните пропуски"
                return
            }

            val vifs = computeVif(predictors)
            val maxVif = vifs.maxOrNull() ?: 1.0

            // соберём лог
            val lines = buildString {
                appendLine("VIF iter=$iteration (threshold=$threshold), Y=${columnName(yCol)}")
                xCols.forEachIndexed { i, c ->
                    appendLine("  X=${columnName(c)} (col=$c) VIF=${vifs[i]}")
                }
                appendLine("  maxVIF=$maxVif")
            }
            vifInfo = lines

            if (maxVif <= threshold) break

            // Удаляем X с минимальной |corr(X, Y)|
            val corrs = predictors.map { x -> kotlin.math.abs(pearson(x, y)) }
            val minIdx = corrs.indices.minBy { corrs[it] }  // индекс в predictors/xCols
            val removedCol = xCols[minIdx]
            xCols.removeAt(minIdx)

            // И удаляем из выбранных колонок тоже
            selectedColumns = selectedColumns - removedCol

            vifInfo = lines + "\nУдалён X: ${columnName(removedCol)} (col=$removedCol), |corr|=${corrs[minIdx]}"
        }
    }

    fun buildTimeSeriesRequest(): TimeSeriesRequest? {
        val data = _csvData.value ?: run {
            error = "Нет данных"
            return null
        }

        val cols = selectedColumns.toList().sorted()
        if (cols.isEmpty()) {
            error = "Не выбраны ряды"
            return null
        }

        val seriesList = cols.map { col ->
            val name = data.headers.getOrNull(col) ?: "col_$col"
            val values = data.matrix.map { row -> row[col].asDoubleOrNaN() }

            if (values.any { it.isNaN() || it.isInfinite() }) {
                error = "В ряде '$name' есть NaN/Inf — сначала заполните пропуски"
                return null
            }

            SeriesData(
                name = name,
                data = values
            )
        }

        val targetIdx = targetColumn?.let { yCol ->
            val idx = cols.indexOf(yCol)
            if (idx < 0) {
                error = "Целевая переменная не входит в выбранные ряды"
                return null
            }
            idx
        }

        val request = TimeSeriesRequest(
            series = seriesList,
            targetIndex = targetIdx
        )

        // ====== DEBUG LOG ======
        logger.d {
            buildString {
                appendLine("========== TimeSeriesRequest ==========")
                appendLine("Series count: ${request.series.size}")
                appendLine("Rows per series: ${request.series.firstOrNull()?.data?.size ?: 0}")
                appendLine("Target index: ${request.targetIndex}")

                request.series.forEachIndexed { index, s ->
                    val isTarget = index == request.targetIndex
                    appendLine(
                        "  [$index] ${s.name} | n=${s.data.size}" +
                                if (isTarget) "  <-- TARGET (Y)" else ""
                    )
                }

                appendLine("=======================================")
            }
        }

        return request
    }


    // если нужен json

    fun buildTimeSeriesRequestJson(pretty: Boolean = true): String? {
        val req = buildTimeSeriesRequest() ?: return null
        val json = Json {
            prettyPrint = pretty
            encodeDefaults = true
        }
        return json.encodeToString(req)
    }

}
