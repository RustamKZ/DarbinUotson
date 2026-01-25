package org.example.project_dw.test.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project_dw.test.MainViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import java.awt.Dimension
import javax.swing.WindowConstants

@Composable
fun PlotScreenContent(viewModel: MainViewModel, columnIndex: Int) {
    val stlResult = viewModel.stlResults[columnIndex]
    val outlierResult = viewModel.outlierResults[columnIndex]

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "STL Декомпозиция для колонки $columnIndex",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (stlResult != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StlChartCard(
                        title = "Тренд", data = stlResult.trend, lineColor = Color(0xFF2979FF)
                    )

                    StlChartCard(
                        title = "Сезонность", data = stlResult.seasonal, lineColor = Color(0xFF2E7D32)
                    )

                    StlChartCard(
                        title = "Остаток",
                        data = stlResult.residual,
                        lineColor = Color(0xFFC62828),
                        dashed = true,
                        outlierIndices = outlierResult?.outlierIndices
                    )
                    if (outlierResult != null) {
                        StlChartCard(
                            title = "Очищенный ряд (Интерполяция)",
                            data = outlierResult.cleanData,
                            lineColor = Color.Black
                        )
                    }
                }
            } else {
                Text("Данные STL не найдены")
            }
        }
    }



@Composable
fun StlChartCard(
    title: String,
    data: DoubleArray,
    lineColor: Color,
    dashed: Boolean = false,
    maxPoints: Int = 2000,
    outlierIndices: List<Int>? = null
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }

    val displayData = remember(data, maxPoints) {
        if (data.size <= maxPoints) {
            data
        } else {
            val step = data.size / maxPoints
            DoubleArray(maxPoints) { i -> data[i * step] }
        }
    }

    val minY = remember(displayData) { displayData.minOrNull() ?: 0.0 }
    val maxY = remember(displayData) { displayData.maxOrNull() ?: 1.0 }
    val rangeY = (maxY - minY).takeIf { it != 0.0 } ?: 1.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale * 1.5f).coerceAtMost(10f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { scale = (scale / 1.5f).coerceAtLeast(1f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("−", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("⟲", fontSize = 16.sp)
                    }
                }
            }

            if (data.size > maxPoints) {
                Text(
                    "Showing ${displayData.size} of ${data.size} points (zoom: ${String.format("%.1f", scale)}x)",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 10f)

                            // Ограничиваем перемещение
                            val maxOffset = size.width * (scale - 1f)
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset, 0f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val maxOffset = size.width * (scale - 1f)
                            offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffset, 0f)
                        }
                    }
            ) {
                val paddingLeft = 60f
                val paddingBottom = 32f
                val paddingTop = 16f
                val paddingRight = 16f

                val chartWidth = (size.width - paddingLeft - paddingRight) * scale
                val chartHeight = size.height - paddingTop - paddingBottom

                val stepX = chartWidth / (displayData.size - 1).coerceAtLeast(1)

                fun yToPx(value: Double): Float =
                    paddingTop + chartHeight * (1f - ((value - minY) / rangeY).toFloat())

                clipRect(
                    left = paddingLeft,
                    top = paddingTop,
                    right = size.width - paddingRight,
                    bottom = size.height - paddingBottom
                ) {
                    val gridLines = 4
                    repeat(gridLines + 1) { i ->
                        val y = paddingTop + chartHeight * i / gridLines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(paddingLeft + offsetX, y),
                            end = Offset(paddingLeft + offsetX + chartWidth, y),
                            strokeWidth = 1f
                        )
                    }
                    val path = Path()
                    displayData.forEachIndexed { index, value ->
                        val x = paddingLeft + offsetX + stepX * index
                        val y = yToPx(value)
                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = if (dashed) {
                                PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                            } else null
                        )
                    )
                    if (title == "Остаток" && outlierIndices != null) {
                        outlierIndices.forEach { index ->
                            // Важно: если вы используете maxPoints (даунсэмплинг),
                            // нужно сопоставить индекс оригинала с индексом на экране.
                            val step = if (data.size > maxPoints) data.size / maxPoints else 1
                            val displayIndex = index / step

                            // Рисуем только если точка попала в выборку или мы отображаем всё
                            if (index % step == 0 || data.size <= maxPoints) {
                                val x = paddingLeft + offsetX + (stepX * displayIndex)
                                val y = yToPx(data[index])

                                drawCircle(
                                    color = Color.Yellow, // Желтый ободок для видимости
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.Red,
                                    radius = 4f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                    /* Это как второй вариант надо протестировать будет
                    * if (title == "Остаток" && outlierIndices != null) {
                        outlierIndices.forEach { originalIndex ->
                            // Считаем X на основе позиции в ОРИГИНАЛЬНОМ массиве (data.size)
                            // Это гарантирует, что точка будет на своем временном отрезке
                            val xFraction = originalIndex.toFloat() / (data.size - 1).coerceAtLeast(1)
                            val x = paddingLeft + offsetX + (xFraction * chartWidth)

                            // Y берем из ОРИГИНАЛЬНЫХ данных
                            val y = yToPx(data[originalIndex])

                            // Рисуем, только если точка входит в видимую область (из-за offsetX и scale)
                            if (x in paddingLeft..(size.width - paddingRight)) {
                                drawCircle(
                                    color = Color.Yellow,
                                    radius = 5f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.Red,
                                    radius = 3f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                     } */

                    /* Еще один вариант
                    * if (title == "Остаток" && outlierIndices != null) {
                        // Группируем выбросы по экранным пикселям, чтобы не рисовать 1.5 млн кругов
                        val step = if (data.size > maxPoints) data.size / maxPoints else 1

                        val groupedOutliers = outlierIndices
                            .groupBy { it / step } // Группируем по displayIndex
                            .mapValues { entry -> entry.value.maxBy { abs(data[it]) } } // Берем самый экстремальный выброс в этом шаге

                        groupedOutliers.forEach { (displayIndex, originalIndex) ->
                            val x = paddingLeft + offsetX + (stepX * displayIndex)
                            val y = yToPx(data[originalIndex])

                            drawCircle(color = Color.Yellow, radius = 5f, center = Offset(x, y))
                            drawCircle(color = Color.Red, radius = 3f, center = Offset(x, y))
                        }
                    }
                    * */
                }

                // === AXES (рисуем поверх всего) ===
                drawLine(
                    color = Color.Gray,
                    start = Offset(paddingLeft, paddingTop),
                    end = Offset(paddingLeft, size.height - paddingBottom),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(paddingLeft, size.height - paddingBottom),
                    end = Offset(size.width - paddingRight, size.height - paddingBottom),
                    strokeWidth = 2f
                )

                // === Y LABELS ===
                val textStyle = TextStyle(
                    color = Color.DarkGray,
                    fontSize = 10.sp
                )

                val gridLines = 4
                repeat(gridLines + 1) { i ->
                    val value = minY + rangeY * (gridLines - i) / gridLines
                    val text = String.format("%.1f", value)

                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = textStyle,
                        topLeft = Offset(
                            5f,
                            paddingTop + chartHeight * i / gridLines - 8f
                        ),
                        size = Size(paddingLeft - 10f, 20f)
                    )
                }
            }
        }
    }
}

fun openPlotWindow(viewModel: MainViewModel, columnIndex: Int) {
    ComposeWindow().apply {
        title = "STL Декомпозиция - Столбец $columnIndex"
        size = Dimension(1200, 900)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        setContent {
            MaterialTheme {
                PlotScreenContent(viewModel, columnIndex)
            }
        }

        isVisible = true
    }
}


