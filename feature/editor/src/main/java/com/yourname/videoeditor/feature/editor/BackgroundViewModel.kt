package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.library.rendering.background.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.graphics.Color

class BackgroundViewModel : ViewModel() {

    private val _backgroundConfig = MutableStateFlow(BackgroundConfig())
    val backgroundConfig: StateFlow<BackgroundConfig> = _backgroundConfig

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    // Available colors
    val availableColors = listOf(
        Color.BLACK,
        Color.WHITE,
        Color.parseColor("#FF4081"), // Pink
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#FFC107"), // Amber
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#795548"), // Brown
        Color.parseColor("#607D8B")  // Blue Grey
    )

    // Gradient presets
    val gradientPresets = listOf(
        Triple("Sunset", Color.parseColor("#FF512F"), Color.parseColor("#DD2476")),
        Triple("Ocean", Color.parseColor("#2193b0"), Color.parseColor("#6dd5ed")),
        Triple("Forest", Color.parseColor("#134E5E"), Color.parseColor("#71B280")),
        Triple("Purple", Color.parseColor("#8E2DE2"), Color.parseColor("#4A00E0")),
        Triple("Fire", Color.parseColor("#f12711"), Color.parseColor("#f5af19")),
        Triple("Mint", Color.parseColor("#1FA2FF"), Color.parseColor("#12D8FA"), Color.parseColor("#A6FFCB"))
    )

    // Pattern presets
    val patternPresets = listOf(
        "Dots" to PatternType.DOTS,
        "Lines" to PatternType.LINES,
        "Grid" to PatternType.GRID,
        "Chessboard" to PatternType.CHESSBOARD,
        "Stripes" to PatternType.STRIPES
    )

    fun setBackgroundType(type: BackgroundType) {
        _backgroundConfig.value = _backgroundConfig.value.copy(type = type, isEnabled = true)
    }

    fun setSolidColor(color: Int) {
        _backgroundConfig.value = _backgroundConfig.value.copy(
            solidColor = SolidColorBackground(color = color)
        )
    }

    fun setGradient(
        type: GradientType,
        startColor: Int,
        endColor: Int,
        colors: IntArray? = null
    ) {
        _backgroundConfig.value = _backgroundConfig.value.copy(
            gradient = GradientBackground(
                type = type,
                startColor = startColor,
                endColor = endColor,
                colors = colors
            )
        )
    }

    fun setPattern(type: PatternType, primaryColor: Int, secondaryColor: Int) {
        _backgroundConfig.value = _backgroundConfig.value.copy(
            pattern = PatternBackground(
                type = type,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
        )
    }

    fun setPatternDensity(density: Float) {
        _backgroundConfig.value = _backgroundConfig.value.copy(
            pattern = _backgroundConfig.value.pattern?.copy(density = density)
        )
    }

    fun setPatternRotation(rotation: Float) {
        _backgroundConfig.value = _backgroundConfig.value.copy(
            pattern = _backgroundConfig.value.pattern?.copy(rotation = rotation)
        )
    }

    fun setOpacity(opacity: Float) {
        _backgroundConfig.value = _backgroundConfig.value.copy(opacity = opacity)
    }

    fun disableBackground() {
        _backgroundConfig.value = _backgroundConfig.value.copy(isEnabled = false)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "Sunset" -> setGradient(GradientType.LINEAR, gradientPresets[0].second, gradientPresets[0].third)
            "Ocean" -> setGradient(GradientType.LINEAR, gradientPresets[1].second, gradientPresets[1].third)
            "Forest" -> setGradient(GradientType.LINEAR, gradientPresets[2].second, gradientPresets[2].third)
            "Purple" -> setGradient(GradientType.LINEAR, gradientPresets[3].second, gradientPresets[3].third)
            "Fire" -> setGradient(GradientType.LINEAR, gradientPresets[4].second, gradientPresets[4].third)
            "Mint" -> setGradient(GradientType.LINEAR, gradientPresets[5].second, gradientPresets[5].third, 
                intArrayOf(gradientPresets[5].second, gradientPresets[5].third, Color.parseColor("#A6FFCB")))
        }
    }
}
