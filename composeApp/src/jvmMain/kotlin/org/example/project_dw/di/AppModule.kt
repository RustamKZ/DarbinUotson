package org.example.project_dw.di

import org.koin.dsl.module
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.presentation.screens.main.MainViewModel

val appModule = module {
    single { PythonBridge() }
    factory { MainViewModel(get()) }
}
