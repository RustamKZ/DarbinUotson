package org.example.project_dw

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import org.example.project_dw.di.appModule
import org.example.project_dw.presentation.App

fun main() = application {
    startKoin {
        modules(appModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Medical Data Analyzer"
    ) {
        App()
    }
}
