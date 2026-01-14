package org.example.project_dw

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter

fun main() {
    Logger.setLogWriters(platformLogWriter())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "DarbinUotson",
        ) {
            App()
        }
    }
}