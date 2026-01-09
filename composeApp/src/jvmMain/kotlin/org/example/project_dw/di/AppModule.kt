package org.example.project_dw.di

import org.koin.dsl.module
import org.example.project_dw.shared.datasources.python.PythonBridge
import org.example.project_dw.shared.repositories.StatisticsRepositoryImpl
import org.example.project_dw.domain.repositories.IStatisticsRepository
import org.example.project_dw.domain.usecases.CheckStationarityUseCase
import org.example.project_dw.presentation.screens.main.MainViewModel

val appModule = module {
    single { PythonBridge() }

    single<IStatisticsRepository> {
        StatisticsRepositoryImpl(get())
    }

    factory { CheckStationarityUseCase(get()) }

    factory { MainViewModel(get()) }
}
