package com.qingban.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun from(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Small preference store; everything stays on the device. */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "qingban_settings",
        Context.MODE_PRIVATE,
    )

    private val _themeMode = MutableStateFlow(
        ThemeMode.from(prefs.getString(KEY_THEME_MODE, null)),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
