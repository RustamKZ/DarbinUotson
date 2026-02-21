package org.example.project_dw.test

import TimeSeriesPreview
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.example.project_dw.shared.datasources.python.PythonApiException
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.shared.models.ModelResults
import org.example.project_dw.shared.models.SeriesOrder
import org.example.project_dw.shared.models.TimeSeriesAnalysisResult
import org.example.project_dw.shared.models.TimeSeriesRequest

class FinalScreen(
    private val viewModel: MainViewModel,
    private val pythonBridge: PythonBridge,
    private val data: TimeSeriesRequest
) : Screen {

    @Composable
    override fun Content() {
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FinalScreenContent()
            }
        }
    }

    @Composable
    private fun FinalScreenContent() {
        val navigator = LocalNavigator.currentOrThrow
        var result by remember { mutableStateOf<Result<TimeSeriesAnalysisResult>?>(null) }

        LaunchedEffect(Unit) {
            result = pythonBridge.analyzeTimeSeries(data)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Верхняя панель навигации
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { navigator.pop() }) {
                        Text(
                            text = "Назад",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Итоговый анализ временных рядов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Основной контент
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp) // отступ под верхнюю панель
            ) {
                // Левая часть — превью рядов
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    SectionCard(
                        title = "Временные ряды, переданные на анализ"
                    ) {
                        TimeSeriesPreview(req = data, maxRows = 80)
                    }
                }

                // Вертикальный разделитель
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // Правая часть — результаты анализа
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (val r = result) {
                        null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Выполняется анализ…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        else -> r.fold(
                            onSuccess = { analysis ->
                                AnalysisResultView(analysis)
                            },
                            onFailure = { error ->
                                ErrorView(error)
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------
// Общие вспомогательные блоки
// -----------------------------

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // заголовок секции с лёгкой подложкой
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Divider(color = outlineColor.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
private fun SubsectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(4.dp))
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))
}

// -----------------------------
// Основные представления анализа
// -----------------------------

@Composable
fun AnalysisResultView(analysis: TimeSeriesAnalysisResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Блок 1 — Общая сводка
        StepCard(
            stepLabel = "1",
            title = "Общая сводка",
            description = "Краткая характеристика данных и выбранной модели."
        ) {
            Text("Целевая переменная (Y): ${analysis.targetVariable}")
            Text("Число рядов: ${analysis.seriesCount}")
            Text("Тип модели: ${analysis.modelType}")
            Spacer(Modifier.height(8.dp))

            StructuralBreakNote(analysis)
        }

        Spacer(Modifier.height(16.dp))

        // Блок 2 — стационарность
        StepCard(
            stepLabel = "2",
            title = "Стационарность и свойства рядов",
            description = "Результаты тестов ADF, KPSS и Zivot–Andrews."
        ) {
            SeriesOrdersView(
                variableNames = analysis.variableNames,
                seriesOrders = analysis.seriesOrders
            )
        }

        Spacer(Modifier.height(16.dp))

        // Блок 3 — результаты модели
        StepCard(
            stepLabel = "3",
            title = "Результаты регрессионного моделирования",
            description = "Коинтеграция, глобальная регрессия и модели по периодам."
        ) {
            ModelResultsView(
                variableNames = analysis.variableNames,
                targetVariable = analysis.targetVariable,
                modelResults = analysis.modelResults
            )
        }
    }
}

@Composable
fun StructuralBreakNote(analysis: TimeSeriesAnalysisResult) {
    val hasBreak = analysis.hasStructuralBreak ||
            (analysis.modelResults?.hasStructuralBreak == true)

    if (!hasBreak) return

    val breakIndexFromTop = analysis.structuralBreaks?.firstOrNull()?.index
        ?: analysis.modelResults?.structuralBreaks?.firstOrNull()?.index
        ?: analysis.seriesOrders.firstOrNull { it.za?.breakpoint != null }?.za?.breakpoint

    val periods = analysis.modelResults?.periods
    val nPeriods = periods?.size ?: 2

    val text = if (breakIndexFromTop != null) {
        "Примечание: тест Zivot–Andrews обнаружил структурный разрыв " +
                "в точке №$breakIndexFromTop. Данные были разделены на $nPeriods период(а), " +
                "для каждого из которых выполнен полный цикл тестирования стационарности " +
                "(ADF, KPSS, STL) и построена отдельная регрессионная модель. " +
                "Это позволяет учесть изменение динамики и получить более робастные оценки параметров."
    } else {
        "Примечание: в данных обнаружены структурные сдвиги, " +
                "для разных периодов построены отдельные регрессионные модели."
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SeriesOrdersView(
    variableNames: List<String>,
    seriesOrders: List<SeriesOrder>
) {
    Column {
        seriesOrders.forEachIndexed { idx, order ->
            val name = variableNames.getOrNull(idx) ?: "Ряд $idx"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))

                    val orderStr = when (order.order) {
                        0 -> "I(0) — стационарный"
                        1 -> "I(1) — нестационарный, стационарна первая разность"
                        2 -> "I(2) — стационарна вторая разность"
                        else -> "I(${order.order})"
                    }
                    Text("Порядок интегрирования: $orderStr")

                    Text(
                        "ADF: p = ${"%.4f".format(order.adf.pValue)}, " +
                                "стационарен = ${order.adf.isStationary}"
                    )
                    Text(
                        "KPSS: p = ${"%.4f".format(order.kpss.pValue)}, " +
                                "стационарен = ${order.kpss.isStationary}"
                    )

                    order.za?.let { za ->
                        Text(
                            "Zivot–Andrews: p = ${"%.4f".format(za.pValue)}, " +
                                    "сдвиг в точке ${za.breakpoint}, " +
                                    "стационарен = ${za.isStationary}"
                        )
                    }

                    if (order.hasConflict) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Конфликт ADF и KPSS разрешён с учётом структурного сдвига.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Наличие тренда: ${order.hasTrend}, " +
                                "сила тренда = ${"%.3f".format(order.trendStrength)}"
                    )
                    Text(
                        "Наличие сезонности: ${order.hasSeasonality}, " +
                                "сила сезонности = ${"%.3f".format(order.seasonalStrength)}"
                    )
                }
            }
        }
    }
}

@Composable
fun ModelResultsView(
    variableNames: List<String>, // пока не используем, но оставляем для расширения
    targetVariable: String,      // тоже можно будет использовать в подписи
    modelResults: ModelResults?
) {
    if (modelResults == null) {
        Text(
            "Результаты модели отсутствуют.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    modelResults.errorMessage?.let {
        Text(
            "Ошибка модели: $it",
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
    }

    // Глобальная регрессионная модель
    modelResults.regression?.let { reg ->
        SubsectionHeader("Глобальная регрессионная модель")

        Text("R² = ${"%.4f".format(reg.rSquared)}")
        Text("Adj. R² = ${"%.4f".format(reg.adjRSquared)}")
        Text("F-статистика = ${"%.3f".format(reg.fStatistic)}, p = ${"%.4f".format(reg.fPvalue)}")
        Text(
            "Durbin–Watson = ${"%.3f".format(reg.durbinWatson.statistic)}, " +
                    "автокорреляция = ${reg.durbinWatson.hasAutocorrelation}"
        )

        Spacer(Modifier.height(12.dp))
    }

    // Коинтеграция
    modelResults.cointegration?.let { coint ->
        SubsectionHeader("Коинтеграция")

        Text("Число рядов: ${coint.nSeries}")
        Text("Наличие коинтеграции: ${coint.isCointegrated}")
        coint.aegResult?.let { aeg ->
            Text("AEG: coint_t = ${"%.3f".format(aeg.cointT)}, p = ${"%.4f".format(aeg.pValue)}")
        }

        Spacer(Modifier.height(12.dp))
    }

    // Структурные сдвиги и периоды
    if (modelResults.hasStructuralBreak) {
        SubsectionHeader("Структурные сдвиги и периодизация")

        modelResults.structuralBreaks?.forEach { br ->
            Text("Разрыв в точке ${br.index} (ряд #${br.seriesIndex})")
        }

        modelResults.periods?.let { periods ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Периоды выборки:",
                fontWeight = FontWeight.Medium
            )
            periods.forEach { p ->
                Text(
                    "Период ${p.periodNumber}: индексы [${p.startIndex}–${p.endIndex}], n = ${p.dataSize}"
                )
            }
        }

        modelResults.periodResults?.let { prList ->
            Spacer(Modifier.height(12.dp))
            Text(
                "Модели по периодам:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            prList.forEach { pr ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Период ${pr.periodNumber} (${pr.periodType}), " +
                                    "индексы [${pr.startIndex}–${pr.endIndex}], n = ${pr.dataSize}",
                            fontWeight = FontWeight.Medium
                        )
                        Text("Тип модели: ${pr.modelType}")

                        pr.regression?.let { reg ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "R² = ${"%.4f".format(reg.rSquared)}, " +
                                        "Adj. R² = ${"%.4f".format(reg.adjRSquared)}"
                            )
                            Text(
                                "F-статистика = ${"%.3f".format(reg.fStatistic)}, " +
                                        "p = ${"%.4f".format(reg.fPvalue)}"
                            )
                        }

                        if (pr.errorMessage != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ошибка: ${pr.errorMessage}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorView(error: Throwable) {
    SectionCard(
        title = "Анализ завершился с ошибкой",
        subtitle = "Подробная информация об ошибке"
    ) {
        Text(
            text = when (error) {
                is PythonApiException -> "Код ошибки: ${error.errorCode}"
                else -> "Тип ошибки: ${error::class.simpleName}"
            },
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))

        val message = when (error) {
            is PythonApiException -> error.message
            else -> error.message
        } ?: "Неизвестная ошибка"

        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
    }
}