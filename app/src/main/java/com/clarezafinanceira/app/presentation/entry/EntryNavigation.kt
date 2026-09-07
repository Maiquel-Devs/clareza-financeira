package com.clarezafinanceira.app.presentation.entry

import android.net.Uri
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
                onAdd = { navigation.navigate("movement/new") { launchSingleTop = true } },
                onEdit = { navigation.navigate("movement/edit/${Uri.encode(it)}") { launchSingleTop = true } })
        }
        composable("movement/new") {
            val form: EntryViewModel = viewModel(factory = viewModelFactory {
                initializer { EntryViewModel(container.financialEntryRepository, container.clock) }
            })
            EntryRoute(form) { navigation.popBackStack() }
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
