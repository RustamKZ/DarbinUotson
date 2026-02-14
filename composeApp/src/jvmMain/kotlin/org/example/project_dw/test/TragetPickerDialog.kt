package org.example.project_dw.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

@Composable
fun TargetPickerDialog(
    columns: List<Int>,
    getName: (Int) -> String,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите целевую переменную (Y)") },
        text = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .fillMaxWidth()
                ) {
                    items(columns) { idx ->
                        Text(
                            text = "${getName(idx)} (col=$idx)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(idx) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                        )
                        Divider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}
