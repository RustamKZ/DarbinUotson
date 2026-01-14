package org.example.project_dw.test

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MatrixPreview(
    data: CsvData,
    selectedCols: Set<Int>,
    onColumnClick: (Int) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val columnWidth = 120.dp

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Нажмите на заголовок, чтобы выбрать колонку для интерполяции",
            style = MaterialTheme.typography.bodySmall)

        Box(Modifier.horizontalScroll(horizontalScrollState)) {
            LazyColumn {
                item {
                    Row {
                        data.headers.forEachIndexed { index, header ->
                            val isSelected = selectedCols.contains(index)
                            Box(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .background(if (isSelected) Color(0xFFBBDEFB) else Color.Transparent)
                                    .clickable { onColumnClick(index) }
                                    .padding(8.dp)
                            ) {
                                Text(header, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Divider()
                }

                items(data.matrix) { row ->
                    Row {
                        row.forEachIndexed { index, value ->
                            val isSelected = selectedCols.contains(index)
                            Text(
                                text = if (value.isNaN()) "NaN" else "%.4f".format(value),
                                modifier = Modifier
                                    .width(columnWidth)
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
                                    .padding(8.dp),
                                color = if (value.isNaN()) Color.Red else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
