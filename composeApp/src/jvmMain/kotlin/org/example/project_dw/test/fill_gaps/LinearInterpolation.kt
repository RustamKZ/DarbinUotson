package org.example.project_dw.test.fill_gaps

import org.example.project_dw.test.CsvData
import org.example.project_dw.test.CsvValue
import org.example.project_dw.test.asDoubleOrNaN
import kotlin.math.pow

object LinearInterpolation {

    // Линейная интерполяция
    fun interpolateSpecificColumns(data: CsvData, targetIndices: List<Int>): CsvData {
        val newMatrix: MutableList<MutableList<CsvValue>> =
            data.matrix.map { it.toMutableList() }.toMutableList()

        val rowCount = newMatrix.size
        val colCount = newMatrix.firstOrNull()?.size ?: 0
        if (rowCount == 0 || colCount == 0) return data

        for (colIdx in targetIndices) {
            if (colIdx !in 0 until colCount) continue
            val isTextColumn = newMatrix.any { row ->
                val v = row[colIdx]
                when (v) {
                    is CsvValue.Num -> false
                    is CsvValue.Text -> v.value.toDoubleOrNull() == null
                }
            }
            if (isTextColumn) continue

            val columnData = DoubleArray(rowCount) { r ->
                newMatrix[r][colIdx].asDoubleOrNaN()
            }
            fillMissingValuesLinear(columnData)
            for (r in 0 until rowCount) {
                newMatrix[r][colIdx] = CsvValue.Num(columnData[r])
            }
        }

        return data.copy(matrix = newMatrix.map { it.toList() })
    }

    fun fillMissingValuesLinear(data: DoubleArray) {
        var i = 0
        while (i < data.size) {
            if (data[i].isNaN()) {
                val startIdx = i - 1
                var endIdx = i
                // Ищем следующее не-NaN значение
                while (endIdx < data.size && data[endIdx].isNaN()) {
                    endIdx++
                }
                if (startIdx >= 0 && endIdx < data.size) {
                    // Интерполируем
                    val startVal = data[startIdx]
                    val endVal = data[endIdx]
                    val step = (endVal - startVal) / (endIdx - startIdx)

                    for (j in (startIdx + 1) until endIdx) {
                        data[j] = startVal + step * (j - startIdx)
                    }
                } else if (startIdx >= 0) {
                    for (j in i until data.size) data[j] = data[startIdx]
                } else if (endIdx < data.size) {
                    for (j in 0 until endIdx) data[j] = data[endIdx]
                }
                i = endIdx
            } else {
                i++
            }
        }
    }

}
