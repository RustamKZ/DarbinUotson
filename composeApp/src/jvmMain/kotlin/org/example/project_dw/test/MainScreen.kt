package org.example.project_dw.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.lightColorScheme
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.test.plot.openPlotWindow

val SUPrimary = Color(0xFF005DAB)       // основной синий
val SUPrimaryLight = Color(0xFFB2D1FF)  // мягкий голубой
val SUBackground = Color(0xFFFFFFFF)    // белый
val SUTitle = Color(0xFF0F1B33)         // тёмный текст

class MainScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = remember { MainViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        val csvData by viewModel.csvData.collectAsState()

        var step1 by remember { mutableStateOf(false) }
        var step2 by remember { mutableStateOf(false) }
        var step3 by remember { mutableStateOf(false) }

        val pythonBridge = remember { PythonBridge() }

        // диалог выбора страны
        var showCountryDialog by remember { mutableStateOf(false) }

        // диалог выбора целевой переменной
        var showTargetRowDialog by remember { mutableStateOf(false) }

        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = SUPrimary,
                onPrimary = Color.White,
                secondary = SUPrimaryLight,
                surface = SUBackground,
                onSurface = SUTitle,
                error = Color(0xFFB00020)
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Верхняя панель: заголовок + действия
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Выбор набора данных",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        chooseCsvFile()?.let { viewModel.loadCsv(it) }
                                    }
                                ) {
                                    Text("Выберите набор данных")
                                }

                                Button(
                                    onClick = { showCountryDialog = true },
                                    enabled = csvData != null
                                ) {
                                    Text(
                                        viewModel.selectedCountry?.let { "Страна: $it" }
                                            ?: "Выбрать страну"
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.buildTimeSeriesRequest()?.let { req ->
                                            navigator.push(
                                                FinalScreen(
                                                    viewModel = viewModel,
                                                    pythonBridge = pythonBridge,
                                                    data = req
                                                )
                                            )
                                        }
                                    },
                                    enabled = csvData != null && viewModel.selectedColumns.isNotEmpty()
                                ) {
                                    Text("Сформировать TimeSeriesRequest")
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Ошибки и отладочная информация
                        viewModel.error?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        viewModel.debugInfo?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Основная область: слева таблица, справа шаги
                        Row(
                            modifier = Modifier.fillMaxSize()
                                .weight(1f)
                        ) {
                            // Левая часть — предпросмотр матрицы
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                csvData?.let { data ->
                                    MatrixPreview(
                                        data = data,
                                        selectedCols = viewModel.selectedColumns,
                                        onColumnClick = { index ->
                                            viewModel.toggleColumnSelection(index)
                                        }
                                    )
                                } ?: run {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Файл не выбран",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Вертикальный разделитель
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Правая часть — шаги обработки
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Spacer(Modifier.height(24.dp))
                                // Шаг 1 — интерполяция пропусков
                                StepCard(
                                    stepLabel = "Шаг 1",
                                    title = "Обработка пропусков",
                                    description = "Интерполяция пропущенных значений в выбранных столбцах."
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Выбрано столбцов: ${viewModel.selectedColumns.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.applyInterpolation()
                                                step1 = true
                                            },
                                            enabled = viewModel.selectedColumns.isNotEmpty()
                                        ) {
                                            Text("Заполнить пропуски")
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Шаг 2 — тест Жарке–Бера
                                StepCard(
                                    stepLabel = "Шаг 2",
                                    title = "Тест Жарке–Бера",
                                    description = "Проверка нормальности распределения остатков."
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Тест применим к выбранным столбцам.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.runJarqueBeraTest()
                                                step2 = true
                                            },
                                            enabled = viewModel.selectedColumns.isNotEmpty() && step1
                                        ) {
                                            Text("Выполнить тест")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    if (viewModel.jarqueBeraResults.isNotEmpty()) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            viewModel.jarqueBeraResults.forEach { (columnIndex, result) ->
                                                Text(
                                                    "Столбец ${viewModel.columnName(columnIndex)}: " +
                                                            "JB = ${"%.3f".format(result.statistic)}, " +
                                                            "Normal = ${result.isNormal}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Шаг 3 — STL декомпозиция
                                StepCard(
                                    stepLabel = "Шаг 3",
                                    title = "STL-декомпозиция",
                                    description = "Выделение тренда, сезонности и остатка."
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Период сезонности: 52 (настроено вручную).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.runSTLDecomposition(period = 52)
                                                step3 = true
                                            },
                                            enabled = viewModel.selectedColumns.isNotEmpty() && step2
                                        ) {
                                            Text("Выполнить STL")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    viewModel.stlResults.forEach { (columnIndex, result) ->
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Столбец ${viewModel.columnName(columnIndex)}:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "  Средний тренд: ${"%.4f".format(result.trend.average())}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "  Средняя сезонная компонента: ${"%.4f".format(result.seasonal.average())}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "  Средний остаток: ${"%.4f".format(result.residual.average())}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(
                                            onClick = { openPlotWindow(viewModel, columnIndex) },
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            Text("Посмотреть графики")
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Шаг 4 — обработка выбросов
                                StepCard(
                                    stepLabel = "Шаг 4",
                                    title = "Обработка выбросов",
                                    description = "Поиск аномальных значений и их обработка."
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Стратегия: интерполяция выбросов.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.detectAndFixOutliers(strategy = "INTERPOLATE")
                                            },
                                            enabled = viewModel.selectedColumns.isNotEmpty() && step3
                                        ) {
                                            Text("Обработать выбросы")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    if (viewModel.outlierResults.isNotEmpty()) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            viewModel.outlierResults.forEach { (columnIndex, result) ->
                                                Text(
                                                    "Столбец ${viewModel.columnName(columnIndex)}: " +
                                                            "найдено ${result.outlierIndices.size} выбросов " +
                                                            "(${result.methodUsed})",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Блок выбора целевой переменной + VIF
                                StepCard(
                                    stepLabel = "Y",
                                    title = "Выбор целевой переменной и мультиколлинеарность",
                                    description = "Определение Y и проверка VIF для регрессоров."
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showTargetRowDialog = true },
                                            enabled = step3 && viewModel.selectedColumns.isNotEmpty()
                                        ) {
                                            Text(
                                                viewModel.targetColumn?.let {
                                                    "Шаг 5: ${viewModel.columnName(it)}"
                                                } ?: "Выбрать целевую (Y)"
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.runVifAndDropLeastRelatedToY(threshold = 10.0)
                                            },
                                            enabled = step3 &&
                                                    viewModel.targetColumn != null &&
                                                    viewModel.selectedColumns.size > 2
                                        ) {
                                            Text("Проверить VIF и удалить X")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    viewModel.vifInfo?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Диалог выбора страны
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

                        // Диалог выбора целевой переменной
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
    }
}


@Composable
fun StepCard(
    stepLabel: String,
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, outlineColor, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Верхняя зона: бейдж шага + заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // маленький “бейдж” шага
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stepLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (description != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // Внутреннее содержимое шага (кнопки, результаты тестов и т.п.)
            content()
        }
    }
}