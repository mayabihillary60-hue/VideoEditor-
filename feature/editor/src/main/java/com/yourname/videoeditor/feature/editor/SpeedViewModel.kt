package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.library.rendering.speed.SpeedConfig
import com.yourname.videoeditor.library.rendering.speed.SpeedController
import com.yourname.videoeditor.library.rendering.speed.SpeedPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeedViewModel : ViewModel() {

    private val speedController = SpeedController()
    
    private val _speedConfig = MutableStateFlow(SpeedConfig())
    val speedConfig: StateFlow<SpeedConfig> = _speedConfig
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val _presetOptions = MutableStateFlow(
        listOf(
            SpeedPreset.SLOWEST to "0.25x",
            SpeedPreset.SLOWER to "0.5x",
            SpeedPreset.SLOW to "0.75x",
            SpeedPreset.NORMAL to "1.0x",
            SpeedPreset.FAST to "1.5x",
            SpeedPreset.FASTER to "2.0x",
            SpeedPreset.FASTEST to "4.0x"
        )
    )
    val presetOptions: StateFlow<List<Pair<SpeedPreset, String>>> = _presetOptions
    
    fun setPreset(preset: SpeedPreset) {
        _speedConfig.value = _speedConfig.value.copy(preset = preset)
    }
    
    fun setCustomSpeed(speed: Float) {
        _speedConfig.value = _speedConfig.value.copy(
            preset = SpeedPreset.CUSTOM,
            speedFactor = speed.coerceIn(0.25f, 4.0f)
        )
    }
    
    fun setPreservePitch(preserve: Boolean) {
        _speedConfig.value = _speedConfig.value.copy(preservePitch = preserve)
    }
    
    fun setFrameBlending(blend: Boolean) {
        _speedConfig.value = _speedConfig.value.copy(frameBlending = blend)
    }
    
    fun applySpeedChange(
        inputPath: String,
        outputPath: String,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0f
            
            speedController.changeSpeed(
                inputPath = inputPath,
                outputPath = outputPath,
                config = _speedConfig.value,
                callback = object : SpeedController.SpeedCallback {
                    override fun onProgress(progress: Float) {
                        _progress.value = progress
                    }
                    
                    override fun onComplete(outputPath: String) {
                        _isProcessing.value = false
                        onComplete(outputPath)
                    }
                    
                    override fun onError(error: String) {
                        _isProcessing.value = false
                        // Handle error
                    }
                }
            )
        }
    }
    
    fun getCurrentSpeedLabel(): String {
        return when (_speedConfig.value.preset) {
            SpeedPreset.SLOWEST -> "0.25x"
            SpeedPreset.SLOWER -> "0.5x"
            SpeedPreset.SLOW -> "0.75x"
            SpeedPreset.NORMAL -> "1.0x"
            SpeedPreset.FAST -> "1.5x"
            SpeedPreset.FASTER -> "2.0x"
            SpeedPreset.FASTEST -> "4.0x"
            SpeedPreset.CUSTOM -> String.format("%.2fx", _speedConfig.value.speedFactor)
        }
    }
}
