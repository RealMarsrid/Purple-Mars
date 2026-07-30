package io.purple.mars.ui.theme

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePreference {
    private const val PREFS = "purple_mars_prefs"
    private const val KEY_THEME = "theme_mode"

    fun get(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_THEME, "SYSTEM")) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}
