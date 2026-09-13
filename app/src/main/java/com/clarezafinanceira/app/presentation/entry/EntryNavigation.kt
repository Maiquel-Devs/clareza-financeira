package com.clarezafinanceira.app.presentation.entry

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.detail.*
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
import com.clarezafinanceira.app.presentation.about.AboutRoute

@Composable
fun EntryNavigation(container: AppContainer, analysis: MonthlyAnalysisViewModel) {
    val navigation = rememberNavController()
    val openItem: (FinancialItemReference) -> Unit = { reference ->
        navigation.navigate("detail/${reference.origin}/${Uri.encode(reference.sourceId)}/${reference.period}") { launchSingleTop = true }
    }
    val openCategory: (ExpenseCategory, YearMonth) -> Unit = { category, period ->
        navigation.navigate("category/$category/$period") { launchSingleTop = true }
    }
    NavHost(navigation, startDestination = "dashboard",
        enterTransition = { fadeIn(tween(100)) }, exitTransition = { fadeOut(tween(70)) },
        popEnterTransition = { fadeIn(tween(100)) }, popExitTransition = { fadeOut(tween(70)) }) {
        composable("dashboard") {
            DashboardRoute(analysis,
                onAbout = { navigation.navigate("about") { launchSingleTop = true } },
                onHistory = { navigation.navigate("history") { launchSingleTop = true } },
                onAdd = { navigation.navigate("movement/new") { launchSingleTop = true } },
                onCategory = openCategory, onItem = openItem)
        }
        composable("about") {
            AboutRoute(onBack = { navigation.popBackStack() })
        }
        composable("history") {
            val history: HistoryViewModel = viewModel(factory = viewModelFactory {
                initializer { HistoryViewModel(HistoryRepository(container.monthlyAnalysisRepository, container.clock,
                    container.currentPeriod), container.clock, createSavedStateHandle(), container.currentPeriod) }
            })
            HistoryRoute(history, onBack = { navigation.popBackStack() }, onPeriod = { period ->
                if (period <= YearMonth.now(container.clock)) navigation.navigate("dashboard/$period")
            })
        }
        composable("dashboard/{period}", arguments = listOf(navArgument("period") { type = NavType.StringType })) { entry ->
            val historical: MonthlyAnalysisViewModel = viewModel(factory = viewModelFactory {
                initializer { MonthlyAnalysisViewModel(container.monthlyAnalysisRepository, container.clock, container.currentPeriod).apply {
                    selectPeriod(YearMonth.parse(requireNotNull(entry.arguments?.getString("period"))))
                } }
            })
            DashboardRoute(historical, onBack = { navigation.popBackStack() },
                onCategory = openCategory, onItem = openItem)
        }
        composable("category/{category}/{period}", arguments = listOf(
            navArgument("category") { type = NavType.StringType }, navArgument("period") { type = NavType.StringType })) { entry ->
            val category: CategoryViewModel = viewModel(factory = viewModelFactory {
                initializer { CategoryViewModel(container.financialDetailRepository,
                    YearMonth.parse(requireNotNull(entry.arguments?.getString("period"))),
                    ExpenseCategory.valueOf(requireNotNull(entry.arguments?.getString("category")))) }
            })
            CategoryRoute(category, onBack = { navigation.popBackStack() }, onItem = openItem)
        }
        composable("detail/{origin}/{id}/{period}", arguments = listOf(
            navArgument("origin") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType },
            navArgument("period") { type = NavType.StringType })) { entry ->
            val detail: FinancialDetailViewModel = viewModel(factory = viewModelFactory {
                initializer { FinancialDetailViewModel(container.financialDetailRepository, container.financialEntryRepository,
                    FinancialItemReference(ItemOrigin.valueOf(requireNotNull(entry.arguments?.getString("origin"))),
                        requireNotNull(entry.arguments?.getString("id")),
                        YearMonth.parse(requireNotNull(entry.arguments?.getString("period"))))) }
            })
            FinancialDetailRoute(detail, onBack = { navigation.popBackStack() }, onEdit = { reference ->
                val route = if (reference.origin == ItemOrigin.MOVEMENT) "movement/edit/${Uri.encode(reference.sourceId)}"
                    else "recurrence/${Uri.encode(reference.sourceId)}/${reference.period}"
                navigation.navigate(route) { launchSingleTop = true }
            })
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
