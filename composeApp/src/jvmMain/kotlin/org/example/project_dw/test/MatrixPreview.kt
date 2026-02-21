package org.example.project_dw.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MatrixPreview(
    data: CsvData,
    selectedCols: Set<Int>,
    onColumnClick: (Int) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    val baseWidth = 120.dp
    val extendedWidth = 200.dp
    val autoScrollStepPx = 30f
    val autoScrollDelayMs = 30L

    var hoverLeft by remember { mutableStateOf(false) }
    var hoverRight by remember { mutableStateOf(false) }

    // Эффект автоскролла, когда курсор у края
    LaunchedEffect(hoverLeft, hoverRight) {
        while (isActive && (hoverLeft || hoverRight)) {
            val delta = when {
                hoverLeft && !hoverRight -> -autoScrollStepPx
                hoverRight && !hoverLeft -> autoScrollStepPx
                else -> 0f
            }

            if (delta != 0f) {
                horizontalScrollState.scrollBy(delta)
            }
            delay(autoScrollDelayMs)
        }
    }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Подпись над таблицей
        Text(
            text = "Нажмите на заголовок числовой колонки, чтобы выбрать её для обработки",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .border(1.dp, outlineColor, MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(4.dp)
        ) {
            // Содержимое таблицы с горизонтальным скроллом
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn {

                    // ===== HEADER =====
                    item {
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )
                        ) {
                            data.headers.forEachIndexed { index, header ->

                                // измеряем ширину текста
                                val pxWidth = textMeasurer.measure(
                                    text = header,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 14.sp
                                    )
                                ).size.width

                                // переводим px в dp
                                val requiredWidthDp = with(LocalDensity.current) { pxWidth.toDp() }

                                // выбираем ширину
                                val columnWidth =
                                    if (requiredWidthDp > baseWidth) extendedWidth else baseWidth

                                val isSelected = selectedCols.contains(index)
                                val isNumeric = data.isNumericColumn(index)

                                val textColor = when {
                                    !isNumeric -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                val stripeColor = when {
                                    !isNumeric ->
                                        MaterialTheme.colorScheme.surfaceVariant
                                    isSelected ->
                                        MaterialTheme.colorScheme.primary
                                    else ->
                                        MaterialTheme.colorScheme.outlineVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .clickable(enabled = isNumeric) { onColumnClick(index) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = header,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        // цветная полоска под заголовком
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .background(stripeColor)
                                        )
                                    }
                                }
                            }
                        }
                        Divider(
                            color = outlineColor
                        )
                    }

                    // ===== ROWS =====
                    itemsIndexed(data.matrix) { rowIndex, row ->
                        val isEvenRow = rowIndex % 2 == 0

                        Row {
                            row.forEachIndexed { index, value ->

                                // вычисляем ширину столбца снова (нужно в данных!)
                                val header = data.headers[index]

                                val pxWidth = textMeasurer.measure(
                                    text = header,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp)
                                ).size.width

                                val requiredWidthDp = with(LocalDensity.current) { pxWidth.toDp() }

                                val columnWidth =
                                    if (requiredWidthDp > baseWidth) extendedWidth else baseWidth

                                val isSelected = selectedCols.contains(index)

                                val baseBg = if (isEvenRow)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.surface

                                val bgColor =
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else
                                        baseBg

                                val text = value.asDisplayText()
                                val isBad = value.isBadNumberOrText()

                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .background(bgColor)
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBad)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Тонкий разделитель между строками
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            // Левая hover-зона автопрокрутки влево
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .pointerMoveFilter(
                        onEnter = {
                            hoverLeft = true
                            false
                        },
                        onExit = {
                            hoverLeft = false
                            false
                        }
                    )
            ) {

            }

            // Правая hover-зона автопрокрутки вправо
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .pointerMoveFilter(
                        onEnter = {
                            hoverRight = true
                            false
                        },
                        onExit = {
                            hoverRight = false
                            false
                        }
                    )
            ) {

            }
        }
    }
}