package org.example.project_dw.test.fill_gaps

import kotlin.math.pow

object JarqueBeraTest {
    fun jarqueBeraTest(data: DoubleArray): Pair<Double, Boolean> {
        val n = data.size.toDouble()
        val mean = data.average()

        // Вычисляем моменты
        val m2 = data.map { (it - mean).pow(2) }.average()
        val m3 = data.map { (it - mean).pow(3) }.average()
        val m4 = data.map { (it - mean).pow(4) }.average()

        // Асимметрия и эксцесс
        val skewness = m3 / m2.pow(1.5)
        val kurtosis = m4 / m2.pow(2)

        // Статистика JB
        val jb = (n / 6.0) * (skewness.pow(2) + (kurtosis - 3).pow(2) / 4.0)

        // Критическое значение хи-квадрат с 2 степенями свободы при α=0.05
        val criticalValue = 5.991
        val isNormal = jb < criticalValue

        return Pair(jb, isNormal)
    }
}