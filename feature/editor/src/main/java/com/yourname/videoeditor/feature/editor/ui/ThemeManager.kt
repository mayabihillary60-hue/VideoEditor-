package com.yourname.videoeditor.feature.editor.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.yourname.videoeditor.feature.editor.R

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
    AMOLED,
    HIGH_CONTRAST
}

enum class AccentColor {
    PINK,
    BLUE,
    PURPLE,
    GREEN,
    ORANGE,
    RED,
    TEAL,
    YELLOW
}

data class ThemeConfig(
    var theme: AppTheme = AppTheme.SYSTEM,
    var accentColor: AccentColor = AccentColor.PINK,
    var useDynamicColors: Boolean = true,
    var reduceMotion: Boolean = false,
    var highContrastText: Boolean = false,
    var fontSizeScale: Float = 1.0f
)

class ThemeManager(private val context: Context) {

    companion object {
        const val PREFS_NAME = "theme_prefs"
        const val KEY_THEME = "theme"
        const val KEY_ACCENT = "accent"
        const val KEY_DYNAMIC = "dynamic"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_FONT_SCALE = "font_scale"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val listeners = mutableListOf<ThemeChangeListener>()

    var config = ThemeConfig(
        theme = AppTheme.values()[prefs.getInt(KEY_THEME, AppTheme.SYSTEM.ordinal)],
        accentColor = AccentColor.values()[prefs.getInt(KEY_ACCENT, AccentColor.PINK.ordinal)],
        useDynamicColors = prefs.getBoolean(KEY_DYNAMIC, true),
        reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
        highContrastText = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
        fontSizeScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
    )
        set(value) {
            field = value
            saveConfig()
            notifyThemeChanged()
        }

    interface ThemeChangeListener {
        fun onThemeChanged(config: ThemeConfig)
        fun onAccentColorChanged(color: Int)
        fun onMotionPreferenceChanged(reduceMotion: Boolean)
    }

    fun applyTheme() {
        when (config.theme) {
            AppTheme.LIGHT -> setLightTheme()
            AppTheme.DARK -> setDarkTheme()
            AppTheme.SYSTEM -> setSystemTheme()
            AppTheme.AMOLED -> setAmoledTheme()
            AppTheme.HIGH_CONTRAST -> setHighContrastTheme()
        }
    }

    private fun setLightTheme() {
        // Apply light theme
        val activity = context as? android.app.Activity
        activity?.let {
            it.setTheme(R.style.Theme_VideoEditor_Light)
        }
    }

    private fun setDarkTheme() {
        val activity = context as? android.app.Activity
        activity?.let {
            it.setTheme(R.style.Theme_VideoEditor_Dark)
        }
    }

    private fun setSystemTheme() {
        val activity = context as? android.app.Activity
        activity?.let {
            val nightMode = context.resources.configuration.uiMode and 
                Configuration.UI_MODE_NIGHT_MASK
            when (nightMode) {
                Configuration.UI_MODE_NIGHT_YES -> it.setTheme(R.style.Theme_VideoEditor_Dark)
                Configuration.UI_MODE_NIGHT_NO -> it.setTheme(R.style.Theme_VideoEditor_Light)
            }
        }
    }

    private fun setAmoledTheme() {
        val activity = context as? android.app.Activity
        activity?.let {
            it.setTheme(R.style.Theme_VideoEditor_AMOLED)
        }
    }

    private fun setHighContrastTheme() {
        val activity = context as? android.app.Activity
        activity?.let {
            it.setTheme(R.style.Theme_VideoEditor_HighContrast)
        }
    }

    fun getAccentColor(): Int {
        return when (config.accentColor) {
            AccentColor.PINK -> ContextCompat.getColor(context, R.color.pink_500)
            AccentColor.BLUE -> ContextCompat.getColor(context, R.color.blue_500)
            AccentColor.PURPLE -> ContextCompat.getColor(context, R.color.purple_500)
            AccentColor.GREEN -> ContextCompat.getColor(context, R.color.green_500)
            AccentColor.ORANGE -> ContextCompat.getColor(context, R.color.orange_500)
            AccentColor.RED -> ContextCompat.getColor(context, R.color.red_500)
            AccentColor.TEAL -> ContextCompat.getColor(context, R.color.teal_500)
            AccentColor.YELLOW -> ContextCompat.getColor(context, R.color.yellow_500)
        }
    }

    fun isDarkMode(): Boolean {
        return when (config.theme) {
            AppTheme.DARK, AppTheme.AMOLED -> true
            AppTheme.LIGHT, AppTheme.HIGH_CONTRAST -> false
            AppTheme.SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and 
                    Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getAnimationDuration(factor: Float = 1f): Long {
        return if (config.reduceMotion) {
            (100 * factor).toLong() // Faster animations
        } else {
            (300 * factor).toLong() // Normal animations
        }
    }

    fun getFontScale(): Float {
        return config.fontSizeScale
    }

    fun addListener(listener: ThemeChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }

    private fun saveConfig() {
        prefs.edit().apply {
            putInt(KEY_THEME, config.theme.ordinal)
            putInt(KEY_ACCENT, config.accentColor.ordinal)
            putBoolean(KEY_DYNAMIC, config.useDynamicColors)
            putBoolean(KEY_REDUCE_MOTION, config.reduceMotion)
            putBoolean(KEY_HIGH_CONTRAST, config.highContrastText)
            putFloat(KEY_FONT_SCALE, config.fontSizeScale)
            apply()
        }
    }

    private fun notifyThemeChanged() {
        listeners.forEach { it.onThemeChanged(config) }
        listeners.forEach { it.onAccentColorChanged(getAccentColor()) }
        listeners.forEach { it.onMotionPreferenceChanged(config.reduceMotion) }
    }
}
