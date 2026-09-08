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
    val financialDetailRepository by lazy {
        com.clarezafinanceira.app.data.repository.FinancialDetailRepository(monthlyAnalysisRepository)
    }
    val clock: Clock = Clock.systemDefaultZone()
    val financialEntryRepository by lazy {
        com.clarezafinanceira.app.data.repository.FinancialEntryRepository(database, clock)
    }
    val monthlyAnalysisViewModelFactory by lazy {
        MonthlyAnalysisViewModelFactory(monthlyAnalysisRepository, clock)
    }
}
