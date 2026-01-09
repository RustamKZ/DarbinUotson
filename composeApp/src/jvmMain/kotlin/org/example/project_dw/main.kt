package org.example.project_dw

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DarbinUotson",
    ) {
        App()
    }
}