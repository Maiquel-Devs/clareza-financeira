package com.clarezafinanceira.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.dashboard.DashboardRoute
import com.clarezafinanceira.app.presentation.dashboard.DashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { false },
        )
        val container = (application as ClarezaFinanceiraApplication).container
        val viewModel = ViewModelProvider.create(this, container.monthlyAnalysisViewModelFactory)[
            MonthlyAnalysisViewModel::class.java
        ]
        setContent {
            DashboardTheme {
                DashboardRoute(viewModel)
            }
        }
    }
}
