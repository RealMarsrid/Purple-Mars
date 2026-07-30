package io.purple.mars.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledBlack = Color(0xFF000000)
private val AmoledSurface = Color(0xFF0A0710)
private val AmoledSurfaceVariant = Color(0xFF160F20)

private val DarkColors = darkColorScheme(
    primary = PurpleMarsPrimary,
    onPrimary = PurpleMarsOnBackground,
    secondary = PurpleMarsSecondary,
    background = AmoledBlack,
    onBackground = PurpleMarsOnBackground,
    surface = AmoledSurface,
    onSurface = PurpleMarsOnBackground,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = PurpleMarsOnSurfaceMuted,
    tertiary = PurpleMarsAccent
)

private val LightColors = lightColorScheme(
    primary = PurpleMarsPrimaryDark,
    onPrimary = Color.White,
    secondary = PurpleMarsSecondary,
    background = Color.White,
    onBackground = Color(0xFF1C1626),
    surface = Color(0xFFF6F1FB),
    onSurface = Color(0xFF1C1626),
    surfaceVariant = Color(0xFFEDE3F7),
    onSurfaceVariant = Color(0xFF4A4056),
    tertiary = PurpleMarsAccent
)

@Composable
fun PurpleMarsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
