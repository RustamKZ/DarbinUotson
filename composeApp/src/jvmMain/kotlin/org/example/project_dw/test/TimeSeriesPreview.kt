import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project_dw.shared.models.TimeSeriesRequest
import kotlin.math.min

@Composable
fun TimeSeriesPreview(
    req: TimeSeriesRequest,
    maxRows: Int = 60
) {
    val horizontalScrollState = rememberScrollState()
    val columnWidth = 200.dp

    val series = req.series
    if (series.isEmpty()) {
        Text(
            text = "Нет рядов для отображения",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val n = series.minOf { it.data.size }
    val rowsToShow = min(n, maxRows)
    val targetIdx = req.targetIndex

    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Строка с общей информацией — в стиле заголовка блока
        Text(
            text = buildString {
                append("Итоговые ряды: ${series.size}, наблюдений: $n")
                targetIdx?.let { append(", Целевая переменная Y = ${series[it].name}") }
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, outlineColor, MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn {

                    // ===== Header =====
                    item {
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )
                        ) {
                            // колонка индекса строки
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            series.forEachIndexed { idx, s ->
                                val isTarget = idx == targetIdx

                                val bg = if (isTarget) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                }

                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .background(bg)
                                        .padding(vertical = 8.dp, horizontal = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = s.name + if (isTarget) " (Y)" else "",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isTarget)
                                                FontWeight.Bold
                                            else
                                                FontWeight.SemiBold,
                                            color = if (isTarget)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )

                                        // тонкая полоска под заголовком, как в MatrixPreview
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .background(
                                                    if (isTarget)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        Divider(
                            color = outlineColor
                        )
                    }

                    // ===== Rows =====
                    val rowIndices = (0 until rowsToShow).toList()
                    items(rowIndices) { r ->
                        val isEvenRow = r % 2 == 0

                        Row {
                            // индекс строки
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = (r + 1).toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            series.forEachIndexed { idx, s ->
                                val v = s.data[r]
                                val isTarget = idx == targetIdx

                                val baseBg = if (isEvenRow)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.surface

                                val bg = if (isTarget) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    baseBg
                                }

                                val textValue = if (v.isNaN()) "NaN" else "%.4f".format(v)

                                Box(
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .background(bg)
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = textValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (v.isNaN())
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

                    if (rowsToShow < n) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Показано $rowsToShow из $n строк",
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}