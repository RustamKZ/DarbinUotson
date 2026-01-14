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

class MainViewModel {
    var selectedForInterpolation by mutableStateOf(setOf<Int>())
        private set

    private val logger = Logger.withTag("CSV")

    // Сырые данные
    var rawCsv by mutableStateOf<RawCsv?>(null)
        private set

    // Числовая матрица
    private val _csvData = MutableStateFlow<CsvData?>(null)
    val csvData: StateFlow<CsvData?> = _csvData.asStateFlow()

    var error by mutableStateOf<String?>(null)
        private set

    var debugInfo by mutableStateOf<String?>(null)
        private set

    fun toggleColumnSelection(index: Int) {
        selectedForInterpolation = if (selectedForInterpolation.contains(index)) {
            selectedForInterpolation - index
        } else {
            selectedForInterpolation + index
        }
    }

    fun applyInterpolation() {
        val currentData = _csvData.value ?: return
        if (selectedForInterpolation.isEmpty()) return

        // Интерполируем только выбранные
        _csvData.value = LinearInterpolation.interpolateSpecificColumns(
            currentData,
            selectedForInterpolation.toList()
        )

        // Очищаем выбор после обработки (опционально)
        selectedForInterpolation = emptySet()
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
}
