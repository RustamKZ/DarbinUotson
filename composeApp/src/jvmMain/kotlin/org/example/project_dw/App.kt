package org.example.project_dw.presentation

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.example.project_dw.test.MainScreen

@Composable
fun App() {
    MaterialTheme {
       Navigator(MainScreen()) { navigator ->
           SlideTransition(navigator)
       }
    }
}
