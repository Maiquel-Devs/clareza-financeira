package com.clarezafinanceira.app

import android.content.Context
import com.clarezafinanceira.app.data.local.FinancialDatabase
import com.clarezafinanceira.app.data.repository.MonthlyAnalysisRepository
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModelFactory
import java.time.Clock

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database by lazy { FinancialDatabase.getInstance(applicationContext) }
    val monthlyAnalysisRepository by lazy { MonthlyAnalysisRepository(database) }
    val clock: Clock = Clock.systemDefaultZone()
    val monthlyAnalysisViewModelFactory by lazy {
        MonthlyAnalysisViewModelFactory(monthlyAnalysisRepository, clock)
    }
}
