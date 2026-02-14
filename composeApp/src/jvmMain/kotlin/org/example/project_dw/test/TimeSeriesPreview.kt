import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val columnWidth = 160.dp

    val series = req.series
    if (series.isEmpty()) {
        Text("Нет рядов для отображения")
        return
    }
    val n = series.minOf { it.data.size }
    val rowsToShow = min(n, maxRows)
    val targetIdx = req.targetIndex

    Column(Modifier.padding(16.dp)) {
        Text(
            "Итоговые ряды: ${series.size}, точек: $n" +
                    (targetIdx?.let { ", Y = ${series[it].name}" } ?: ""),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        Box(Modifier.horizontalScroll(horizontalScrollState)) {
            LazyColumn {
                // ===== Header =====
                item {
                    Row {
                        // колонка индекса строки
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(8.dp)
                        ) {
                            Text("#", fontWeight = FontWeight.Bold)
                        }

                        series.forEachIndexed { idx, s ->
                            val isTarget = idx == targetIdx
                            val bg = if (isTarget) Color(0xFFFFF3E0) else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .background(bg)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    s.name + if (isTarget) " (Y)" else "",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Divider()
                }

                // ===== Rows =====
                val rowIndices = (0 until rowsToShow).toList()
                items(rowIndices) { r ->
                    Row {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(8.dp)
                        ) {
                            Text((r + 1).toString())
                        }

                        series.forEachIndexed { idx, s ->
                            val v = s.data[r]
                            val isTarget = idx == targetIdx

                            val bg = if (isTarget) Color(0xFFFFF8E1) else Color.Transparent
                            val text = if (v.isNaN()) "NaN" else "%.4f".format(v)

                            Text(
                                text = text,
                                modifier = Modifier
                                    .width(columnWidth)
                                    .background(bg)
                                    .padding(8.dp),
                                color = if (v.isNaN()) Color.Red else Color.Black
                            )
                        }
                    }
                }

                if (rowsToShow < n) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Показано $rowsToShow из $n строк",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
