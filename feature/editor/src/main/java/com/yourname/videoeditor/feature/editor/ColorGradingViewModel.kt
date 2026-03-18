package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.library.rendering.color.ColorGradingParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ColorGradingViewModel : ViewModel() {

    private val _colorParams = MutableStateFlow(ColorGradingParams())
    val colorParams: StateFlow<ColorGradingParams> = _colorParams

    private val _isAdjusting = MutableStateFlow(false)
    val isAdjusting: StateFlow<Boolean> = _isAdjusting

    private val _selectedTool = MutableStateFlow(ColorTool.BASIC)
    val selectedTool: StateFlow<ColorTool> = _selectedTool

    private val _undoStack = mutableListOf<ColorGradingParams>()
    private val _redoStack = mutableListOf<ColorGradingParams>()

    enum class ColorTool {
        BASIC,
        TONE,
        COLOR,
        CURVES,
        HSL,
        RGB
    }

    // Basic adjustments
    fun setExposure(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(exposure = value.coerceIn(-2f, 2f))
    }

    fun setContrast(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(contrast = value.coerceIn(0f, 3f))
    }

    fun setSaturation(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(saturation = value.coerceIn(0f, 2f))
    }

    fun setVibrance(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(vibrance = value.coerceIn(0f, 2f))
    }

    fun setHueShift(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(hueShift = value.coerceIn(-180f, 180f))
    }

    // Tone adjustments
    fun setHighlights(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(highlights = value.coerceIn(-1f, 1f))
    }

    fun setShadows(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(shadows = value.coerceIn(-1f, 1f))
    }

    fun setWhites(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(whites = value.coerceIn(-1f, 1f))
    }

    fun setBlacks(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(blacks = value.coerceIn(-1f, 1f))
    }

    fun setTemperature(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(temperature = value.coerceIn(-1f, 1f))
    }

    fun setTint(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(tint = value.coerceIn(-1f, 1f))
    }

    // RGB adjustments
    fun setLift(r: Float, g: Float, b: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(lift = floatArrayOf(r, g, b))
    }

    fun setGain(r: Float, g: Float, b: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(gain = floatArrayOf(r, g, b))
    }

    fun setGammaCorrection(r: Float, g: Float, b: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(gammaCorrection = floatArrayOf(r, g, b))
    }

    fun setGamma(value: Float) {
        saveToUndo()
        _colorParams.value = _colorParams.value.copy(gamma = value.coerceIn(0.1f, 3f))
    }

    // Tool selection
    fun selectTool(tool: ColorTool) {
        _selectedTool.value = tool
    }

    // Reset all adjustments
    fun resetAll() {
        saveToUndo()
        _colorParams.value = ColorGradingParams()
    }

    // Reset current tool adjustments
    fun resetCurrentTool() {
        saveToUndo()
        val currentParams = _colorParams.value
        val defaultParams = ColorGradingParams()
        
        when (_selectedTool.value) {
            ColorTool.BASIC -> {
                _colorParams.value = currentParams.copy(
                    exposure = defaultParams.exposure,
                    contrast = defaultParams.contrast,
                    saturation = defaultParams.saturation,
                    vibrance = defaultParams.vibrance,
                    hueShift = defaultParams.hueShift
                )
            }
            ColorTool.TONE -> {
                _colorParams.value = currentParams.copy(
                    highlights = defaultParams.highlights,
                    shadows = defaultParams.shadows,
                    whites = defaultParams.whites,
                    blacks = defaultParams.blacks,
                    temperature = defaultParams.temperature,
                    tint = defaultParams.tint
                )
            }
            ColorTool.RGB -> {
                _colorParams.value = currentParams.copy(
                    lift = defaultParams.lift,
                    gain = defaultParams.gain,
                    gammaCorrection = defaultParams.gammaCorrection,
                    gamma = defaultParams.gamma
                )
            }
            else -> { /* No reset for other tools */ }
        }
    }

    // Undo/Redo
    private fun saveToUndo() {
        if (_isAdjusting.value) return
        _undoStack.add(_colorParams.value.copy())
        // Clear redo stack when new change is made
        _redoStack.clear()
    }

    fun undo() {
        if (_undoStack.isNotEmpty()) {
            _redoStack.add(_colorParams.value)
            _colorParams.value = _undoStack.removeAt(_undoStack.size - 1)
        }
    }

    fun redo() {
        if (_redoStack.isNotEmpty()) {
            _undoStack.add(_colorParams.value)
            _colorParams.value = _redoStack.removeAt(_redoStack.size - 1)
        }
    }

    fun canUndo() = _undoStack.isNotEmpty()
    fun canRedo() = _redoStack.isNotEmpty()

    // Save/Load presets
    fun saveAsPreset(name: String): ColorPreset {
        return ColorPreset(
            name = name,
            params = _colorParams.value.copy()
        )
    }

    fun loadPreset(preset: ColorPreset) {
        saveToUndo()
        _colorParams.value = preset.params.copy()
    }

    // Start/End adjustments (for performance)
    fun beginAdjustment() {
        _isAdjusting.value = true
    }

    fun endAdjustment() {
        _isAdjusting.value = false
        saveToUndo()
    }
}

data class ColorPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val params: ColorGradingParams,
    val thumbnail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)