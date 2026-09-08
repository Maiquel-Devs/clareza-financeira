package com.clarezafinanceira.app.presentation.entry

import android.net.Uri
import androidx.lifecycle.createSavedStateHandle
import com.clarezafinanceira.app.presentation.history.*
import com.clarezafinanceira.app.data.repository.HistoryRepository
import java.time.YearMonth
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.clarezafinanceira.app.AppContainer
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.dashboard.DashboardRoute

@Composable
fun EntryNavigation(container: AppContainer, analysis: MonthlyAnalysisViewModel) {
    val navigation = rememberNavController()
    NavHost(navigation, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardRoute(analysis,
                onHistory = { navigation.navigate("history") { launchSingleTop = true } },
                onAdd = { navigation.navigate("movement/new") { launchSingleTop = true } },
                onEdit = { navigation.navigate("movement/edit/${Uri.encode(it)}") { launchSingleTop = true } },
                onEditRecurrence = { id, period -> navigation.navigate("recurrence/${Uri.encode(id)}/$period") { launchSingleTop = true } })
        }
        composable("history") {
            val history: HistoryViewModel = viewModel(factory = viewModelFactory {
                initializer { HistoryViewModel(HistoryRepository(container.monthlyAnalysisRepository, container.clock),
                    container.clock, createSavedStateHandle()) }
            })
            HistoryRoute(history, onBack = { navigation.popBackStack() }, onPeriod = { period ->
                if (period <= YearMonth.now(container.clock)) navigation.navigate("dashboard/$period")
            })
        }
        composable("dashboard/{period}", arguments = listOf(navArgument("period") { type = NavType.StringType })) { entry ->
            val historical: MonthlyAnalysisViewModel = viewModel(factory = viewModelFactory {
                initializer { MonthlyAnalysisViewModel(container.monthlyAnalysisRepository, container.clock).apply {
                    selectPeriod(YearMonth.parse(requireNotNull(entry.arguments?.getString("period"))))
                } }
            })
            DashboardRoute(historical, onBack = { navigation.popBackStack() },
                onEdit = { navigation.navigate("movement/edit/${Uri.encode(it)}") },
                onEditRecurrence = { id, period -> navigation.navigate("recurrence/${Uri.encode(id)}/$period") })
        }
        composable("movement/new") {
            val form: EntryViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryViewModel(container.financialEntryRepository, container.clock) }
            })
            EntryRoute(form) { navigation.popBackStack() }
        }
        composable("recurrence/{id}/{period}", arguments = listOf(
            navArgument("id") { type = NavType.StringType }, navArgument("period") { type = NavType.StringType })) { entry ->
            val form: RecurrenceViewModel = viewModel(factory = viewModelFactory {
                initializer { RecurrenceViewModel(container.financialEntryRepository,
                    requireNotNull(entry.arguments?.getString("id")),
                    java.time.YearMonth.parse(requireNotNull(entry.arguments?.getString("period")))) }
            })
            RecurrenceRoute(form) { navigation.popBackStack() }
        }
        composable("movement/edit/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val form: EntryViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryViewModel(container.financialEntryRepository, container.clock,
                    requireNotNull(entry.arguments?.getString("id"))) }
            })
            EntryRoute(form) { navigation.popBackStack() }
        }
    }
}
