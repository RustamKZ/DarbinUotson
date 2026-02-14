package org.example.project_dw.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project_dw.test.plot.openPlotWindow

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { MainViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        val csvData by viewModel.csvData.collectAsState()
        var step1 by remember { mutableStateOf(false) }
        var step2 by remember { mutableStateOf(false) }
        var step3 by remember { mutableStateOf(false) }

        // для выбора страны
        var showCountryDialog by remember { mutableStateOf(false) }

        // для выбора целевой переменной
        var showTargetRowDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Выбор набора данных", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        chooseCsvFile()?.let { viewModel.loadCsv(it) }
                    }) {
                        Text("Выберите набор данных")
                    }
                    Button(
                        onClick = {
                            navigator.push(JarqueBeraScreen(viewModel))
                        },
                    ) {
                        Text("Далее (TEST)")
                    }
                    Button(
                        onClick = { showCountryDialog = true },
                        enabled = csvData != null
                    ) {
                        Text(viewModel.selectedCountry?.let { "Страна: $it" } ?: "Выбрать страну")
                    }
                    Button(
                        onClick = {
                            val req = viewModel.buildTimeSeriesRequest()
                            viewModel.debugInfo = req?.let { "Request series=${it.series.size}, targetIndex=${it.targetIndex}" }
                                ?: viewModel.error
                        },
                        enabled = csvData != null && viewModel.selectedColumns.isNotEmpty()
                    ) { Text("Сформировать TimeSeriesRequest") }

                }
                Spacer(Modifier.height(16.dp))

                viewModel.error?.let {
                    Text(text = it, color = Color.Red)
                }

                viewModel.debugInfo?.let {
                    Text(text = it, color = Color.Blue)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    csvData?.let { data ->
                        MatrixPreview(
                            data = data,
                            selectedCols = viewModel.selectedColumns,
                            onColumnClick = { index -> viewModel.toggleColumnSelection(index) }
                        )
                    }
                    VerticalDivider(color = Color.Black, thickness = 1.dp)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Шаг 1", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    viewModel.applyInterpolation()
                                    step1 = true
                                },
                                enabled = viewModel.selectedColumns.isNotEmpty()
                            ) {
                                Text("Заполнить пропуски в выбранных (${viewModel.selectedColumns.size})")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Шаг 2", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    viewModel.runJarqueBeraTest()
                                    step2 = true
                                },
                                enabled = viewModel.selectedColumns.isNotEmpty() and step1,
                            ) {
                                Text("Применить тест Жака-Бера")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        viewModel.jarqueBeraResults.forEach { (columnIndex, result) ->
                            Text("Column $columnIndex: JB = ${result.statistic}, Normal = ${result.isNormal}")
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Шаг 3", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    viewModel.runSTLDecomposition(period = 52)
                                    step3 = true
                                }, // 24 часа × 12 записей/час = 288
                                enabled = viewModel.selectedColumns.isNotEmpty() and step2,
                            ) {
                                Text("STL декомпозиция")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        viewModel.stlResults.forEach { (columnIndex, result) ->
                            Text("Column $columnIndex:")
                            Text("  Trend mean: ${result.trend.average()}")
                            Text("  Seasonal mean: ${result.seasonal.average()}")
                            Text("  Residual mean: ${result.residual.average()}")
                            Button(
                                onClick = { openPlotWindow(viewModel, columnIndex) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Посмотреть графики")
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Шаг 4", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    viewModel.detectAndFixOutliers(strategy = "INTERPOLATE")
                                },
                                enabled = viewModel.selectedColumns.isNotEmpty() && step3
                            ) {
                                Text("Обработать выбросы")
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        viewModel.outlierResults.forEach { (columnIndex, result) ->
                            Text("Column $columnIndex: Найдено ${result.outlierIndices.size} выбросов (${result.methodUsed})")
                        }
                        // Вот сюда надо добавить выбор целевой переменной - ряда
                        Spacer(Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Выбор целевой переменной", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                                Button(
                                    onClick = {
                                        showTargetRowDialog = true
                                    },
                                    enabled = step3 && viewModel.selectedColumns.isNotEmpty()
                                ) {
                                    Text(
                                        viewModel.targetColumn?.let { "Y: ${viewModel.columnName(it)}" }
                                            ?: "Выбрать целевую (Y)"
                                    )
                                }
                                Button(
                                    onClick = { viewModel.runVifAndDropLeastRelatedToY(threshold = 10.0) },
                                    enabled = step3 && viewModel.targetColumn != null && viewModel.selectedColumns.size > 2
                                ) {
                                    Text("Проверить VIF и удалить X")
                                }
                            }
                        Spacer(Modifier.height(16.dp))

                        viewModel.vifInfo?.let { Text(it) }

                    }
                }
                if (showCountryDialog) {
                    val countries = viewModel.availableCountries()

                    CountryPickerDialog(
                        countries = countries,
                        onPick = { chosen ->
                            viewModel.selectCountry(chosen)
                            showCountryDialog = false
                        },
                        onDismiss = { showCountryDialog = false }
                    )
                }
                if (showTargetRowDialog) {
                    TargetPickerDialog(
                        columns = viewModel.selectedColumns.toList().sorted(),
                        getName = { idx -> viewModel.columnName(idx) },
                        onPick = { y ->
                            viewModel.setTargetColumn(y)
                            showTargetRowDialog = false
                        },
                        onDismiss = { showTargetRowDialog = false }
                    )
                }

            }
        }

    }
}