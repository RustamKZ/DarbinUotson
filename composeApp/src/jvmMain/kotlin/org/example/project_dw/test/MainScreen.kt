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
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { MainViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        val csvData by viewModel.csvData.collectAsState()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        chooseCsvFile()?.let { viewModel.loadCsv(it) }
                    }) {
                        Text("Выберите набор данных")
                    }
                    Button(
                        onClick = { viewModel.applyInterpolation() },
                        enabled = viewModel.selectedForInterpolation.isNotEmpty()
                    ) {
                        Text("Заполнить пропуски в выбранных (${viewModel.selectedForInterpolation.size})")
                    }
                    Button(
                        onClick = {
                            navigator.push(JarqueBeraScreen(viewModel))
                        },
                    ) {
                        Text("Далее")
                    }
                }
                Spacer(Modifier.height(16.dp))

                viewModel.error?.let {
                    Text(text = it, color = Color.Red)
                }

                viewModel.debugInfo?.let {
                    Text(text = it, color = Color.Blue)
                }

                csvData?.let { data ->
                    MatrixPreview(
                        data = data,
                        selectedCols = viewModel.selectedForInterpolation,
                        onColumnClick = { index -> viewModel.toggleColumnSelection(index) }
                    )
                }
            }
        }

    }
}