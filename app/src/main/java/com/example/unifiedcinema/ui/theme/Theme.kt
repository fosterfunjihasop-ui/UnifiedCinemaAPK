package com.example.unifiedcinema.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC5),
    secondaryContainer = Color(0xFF018786),
    tertiary = Color(0xFFFF0187),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF79747E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF018786),
    secondaryContainer = Color(0xFFBB86FC),
    tertiary = Color(0xFFFF0187),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F5),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    outline = Color(0xFF79747E)
)

@Composable
fun UnifiedCinemaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

object Typography {
    val displayLarge = androidx.compose.material3.Typography.default.displayLarge
    val displayMedium = androidx.compose.material3.Typography.default.displayMedium
    val displaySmall = androidx.compose.material3.Typography.default.displaySmall
    val headlineLarge = androidx.compose.material3.Typography.default.headlineLarge
    val headlineMedium = androidx.compose.material3.Typography.default.headlineMedium
    val headlineSmall = androidx.compose.material3.Typography.default.headlineSmall
    val titleLarge = androidx.compose.material3.Typography.default.titleLarge
    val titleMedium = androidx.compose.material3.Typography.default.titleMedium
    val titleSmall = androidx.compose.material3.Typography.default.titleSmall
    val bodyLarge = androidx.compose.material3.Typography.default.bodyLarge
    val bodyMedium = androidx.compose.material3.Typography.default.bodyMedium
    val bodySmall = androidx.compose.material3.Typography.default.bodySmall
    val labelLarge = androidx.compose.material3.Typography.default.labelLarge
    val labelMedium = androidx.compose.material3.Typography.default.labelMedium
    val labelSmall = androidx.compose.material3.Typography.default.labelSmall
}