package org.example.project_dw.test.fill_gaps

import org.example.project_dw.test.CsvData
import kotlin.math.pow

object LinearInterpolation {

    // Линейная интерполяция
    fun interpolateSpecificColumns(data: CsvData, targetIndices: List<Int>): CsvData {
        val newMatrix: List<DoubleArray> = data.matrix.map { it.copyOf() }.toList()

        // Проходим только по тем индексам, которые выбрал пользователь
        for (colIdx in targetIndices) {
            if (colIdx in 0 until (newMatrix.getOrNull(0)?.size ?: 0)) {
                val columnData = DoubleArray(newMatrix.size) { row -> newMatrix[row][colIdx] }
                fillMissingValuesLinear(columnData)

                for (row in newMatrix.indices) {
                    newMatrix[row][colIdx] = columnData[row]
                }
            }
        }
        return data.copy(matrix = newMatrix)
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
