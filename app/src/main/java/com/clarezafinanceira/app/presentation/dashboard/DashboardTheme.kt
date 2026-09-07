package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val IncomeSurface = Color(0xFFE9F7EF)
internal val IncomeInk = Color(0xFF176344)
internal val RegisteredSurface = Color(0xFFFFF6D9)
internal val ForecastSurface = Color(0xFFF1EBFF)

private val DashboardColors = lightColorScheme(
    primary = Color(0xFF2164CE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F0FF),
    onPrimaryContainer = Color(0xFF173D78),
    secondary = IncomeInk,
    secondaryContainer = IncomeSurface,
    onSecondaryContainer = IncomeInk,
    background = Color(0xFFF2F7FD),
    onBackground = Color(0xFF172E4D),
    surface = Color.White,
    onSurface = Color(0xFF172E4D),
    onSurfaceVariant = Color(0xFF4D6078),
    outlineVariant = Color(0xFFD2E1F2),
    error = Color(0xFFA13632),
    errorContainer = Color(0xFFFFEDE9),
    onErrorContainer = Color(0xFF782824),
)

@Composable
fun DashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DashboardColors, content = content)
}
