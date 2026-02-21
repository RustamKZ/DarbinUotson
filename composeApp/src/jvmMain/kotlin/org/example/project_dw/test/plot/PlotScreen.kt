package org.example.project_dw.test.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project_dw.test.MainViewModel
import java.awt.Dimension
import javax.swing.WindowConstants
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.example.project_dw.test.SUBackground
import org.example.project_dw.test.SUPrimary
import org.example.project_dw.test.SUPrimaryLight
import org.example.project_dw.test.SUTitle

@Composable
fun PlotScreenContent(viewModel: MainViewModel, columnIndex: Int) {
    val stlResult = viewModel.stlResults[columnIndex]
    val outlierResult = viewModel.outlierResults[columnIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок экрана
        Text(
            text = "STL-декомпозиция для колонки $columnIndex",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Визуализация тренда, сезонности и остаточной компоненты для выбранного временного ряда.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (stlResult != null) {
            // Тренд
            StlChartCard(
                title = "Тренд",
                data = stlResult.trend,
                lineColor = MaterialTheme.colorScheme.primary
            )

            // Сезонность
            StlChartCard(
                title = "Сезонность",
                data = stlResult.seasonal,
                lineColor = MaterialTheme.colorScheme.secondary
            )

            // Остаток с выбросами
            StlChartCard(
                title = "Остаток",
                data = stlResult.residual,
                lineColor = MaterialTheme.colorScheme.error,
                dashed = true,
                outlierIndices = outlierResult?.outlierIndices
            )

            // Очищенный ряд
            if (outlierResult != null) {
                StlChartCard(
                    title = "Очищенный ряд (интерполяция выбросов)",
                    data = outlierResult.cleanData,
                    lineColor = MaterialTheme.colorScheme.tertiary
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "Данные STL не найдены для выбранного столбца.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StlChartCard(
    title: String,
    data: DoubleArray,
    lineColor: Color,
    dashed: Boolean = false,
    maxPoints: Int = 14100,
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

    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Шапка карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scale = (scale * 1.5f).coerceAtMost(10f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            "+",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { scale = (scale / 1.5f).coerceAtLeast(1f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            "−",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            "⟲",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (data.size > maxPoints) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Показано ${displayData.size} из ${data.size} точек (zoom: ${String.format("%.1f", scale)}×)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            val tertiaryCircle = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
            val gridColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            val outlierInner = MaterialTheme.colorScheme.error
            val yLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 10f)

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

                // === Сетка и линия графика внутри clipRect ===
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
                            color = gridColor,
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

                    // Выбросы — только для остатка
                    if (title.startsWith("Остаток") && outlierIndices != null) {
                        val step = if (data.size > maxPoints) data.size / maxPoints else 1

                        outlierIndices.forEach { index ->
                            val displayIndex = index / step
                            if (displayIndex in displayData.indices) {
                                val x = paddingLeft + offsetX + (stepX * displayIndex)
                                val y = yToPx(data[index])

                                drawCircle(
                                    color = tertiaryCircle,
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = outlierInner,
                                    radius = 4f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                // === Оси ===
                drawLine(
                    color = outlineColor,
                    start = Offset(paddingLeft, paddingTop),
                    end = Offset(paddingLeft, size.height - paddingBottom),
                    strokeWidth = 2f
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(paddingLeft, size.height - paddingBottom),
                    end = Offset(size.width - paddingRight, size.height - paddingBottom),
                    strokeWidth = 2f
                )

                // === Подписи по оси Y ===
                val labelStyle = TextStyle(
                    color = yLabelColor,
                    fontSize = 10.sp
                )

                val gridLines = 4
                repeat(gridLines + 1) { i ->
                    val value = minY + rangeY * (gridLines - i) / gridLines
                    val text = String.format("%.1f", value)

                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = labelStyle,
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
        title = "STL-декомпозиция — столбец $columnIndex"
        size = Dimension(1200, 900)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        setContent {
            // Здесь важно, чтобы уже был подключён твой SUAppTheme
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = SUPrimary,
                    onPrimary = Color.White,
                    secondary = SUPrimaryLight,
                    surface = SUBackground,
                    onSurface = SUTitle,
                    error = Color(0xFFB00020),
                    primaryContainer = SUPrimaryLight,
                    onPrimaryContainer = SUTitle,
                    background = SUBackground,
                    onBackground = SUTitle,
                    surfaceVariant = SUPrimaryLight.copy(alpha = 0.25f),
                    outlineVariant = SUPrimary.copy(alpha = 0.4f)
                )
            ) {
                PlotScreenContent(viewModel, columnIndex)
            }
        }

        isVisible = true
    }
}