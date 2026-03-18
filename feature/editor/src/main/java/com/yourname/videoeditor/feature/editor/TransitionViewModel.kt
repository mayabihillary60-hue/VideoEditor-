package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.library.rendering.transition.TransitionConfig
import com.yourname.videoeditor.library.rendering.transition.TransitionType
import com.yourname.videoeditor.library.rendering.transition.TransitionEasing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TransitionCategory(
    val name: String,
    val transitions: List<TransitionType>
)

class TransitionViewModel : ViewModel() {

    private val _transitionConfig = MutableStateFlow(TransitionConfig())
    val transitionConfig: StateFlow<TransitionConfig> = _transitionConfig

    private val _selectedTransition = MutableStateFlow<TransitionType?>(null)
    val selectedTransition: StateFlow<TransitionType?> = _selectedTransition

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _durationMs = MutableStateFlow(1000L)
    val durationMs: StateFlow<Long> = _durationMs

    // Categorized transitions for UI
    val transitionCategories = listOf(
        TransitionCategory(
            "Basic",
            listOf(
                TransitionType.CUT,
                TransitionType.FADE_IN_OUT,
                TransitionType.CROSSFADE
            )
        ),
        TransitionCategory(
            "Slide",
            listOf(
                TransitionType.SLIDE_LEFT,
                TransitionType.SLIDE_RIGHT,
                TransitionType.SLIDE_UP,
                TransitionType.SLIDE_DOWN,
                TransitionType.PUSH_LEFT,
                TransitionType.PUSH_RIGHT,
                TransitionType.PUSH_UP,
                TransitionType.PUSH_DOWN
            )
        ),
        TransitionCategory(
            "Wipe",
            listOf(
                TransitionType.WIPE_LEFT,
                TransitionType.WIPE_RIGHT,
                TransitionType.WIPE_UP,
                TransitionType.WIPE_DOWN,
                TransitionType.WIPE_CIRCLE,
                TransitionType.WIPE_DIAMOND,
                TransitionType.WIPE_STAR
            )
        ),
        TransitionCategory(
            "Zoom",
            listOf(
                TransitionType.ZOOM_IN,
                TransitionType.ZOOM_OUT,
                TransitionType.ZOOM_ROTATE
            )
        ),
        TransitionCategory(
            "3D",
            listOf(
                TransitionType.CUBE_ROTATE,
                TransitionType.FLIP_HORIZONTAL,
                TransitionType.FLIP_VERTICAL,
                TransitionType.PAGE_CURL
            )
        ),
        TransitionCategory(
            "Creative",
            listOf(
                TransitionType.HEART,
                TransitionType.STAR,
                TransitionType.DIAMOND,
                TransitionType.CIRCLE,
                TransitionType.BLINDS,
                TransitionType.CHECKERBOARD,
                TransitionType.DISSOLVE,
                TransitionType.GLITCH,
                TransitionType.BURN,
                TransitionType.LIGHT_LEAK
            )
        )
    )

    // Easing options
    val easingOptions = listOf(
        TransitionEasing.LINEAR to "Linear",
        TransitionEasing.EASE_IN to "Ease In",
        TransitionEasing.EASE_OUT to "Ease Out",
        TransitionEasing.EASE_IN_OUT to "Ease In/Out",
        TransitionEasing.BOUNCE to "Bounce",
        TransitionEasing.ELASTIC to "Elastic"
    )

    fun selectTransition(type: TransitionType) {
        _selectedTransition.value = type
        _transitionConfig.value = _transitionConfig.value.copy(type = type)
        resetPreview()
    }

    fun setDuration(durationMs: Long) {
        _durationMs.value = durationMs.coerceIn(100L, 5000L)
        _transitionConfig.value = _transitionConfig.value.copy(durationMs = _durationMs.value)
    }

    fun setEasing(easing: TransitionEasing) {
        _transitionConfig.value = _transitionConfig.value.copy(easing = easing)
    }

    fun setIntensity(intensity: Float) {
        _transitionConfig.value = _transitionConfig.value.copy(intensity = intensity.coerceIn(0f, 2f))
    }

    fun setDirection(direction: Int) {
        _transitionConfig.value = _transitionConfig.value.copy(direction = direction)
    }

    fun playPreview() {
        _isPlaying.value = true
        viewModelScope.launch {
            // Animate progress from 0 to 1 over duration
            val startTime = System.currentTimeMillis()
            val duration = _durationMs.value
            
            while (_isPlaying.value && _previewProgress.value < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                _previewProgress.value = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                
                if (_previewProgress.value >= 1f) {
                    _isPlaying.value = false
                }
                
                kotlinx.coroutines.delay(16) // ~60fps
            }
        }
    }

    fun pausePreview() {
        _isPlaying.value = false
    }

    fun resetPreview() {
        _isPlaying.value = false
        _previewProgress.value = 0f
    }

    fun seekPreview(progress: Float) {
        _previewProgress.value = progress.coerceIn(0f, 1f)
    }

    fun getTransitionIcon(type: TransitionType): Int {
        return when (type) {
            TransitionType.CUT -> android.R.drawable.ic_menu_slideshow
            TransitionType.FADE_IN_OUT -> android.R.drawable.ic_menu_gallery
            TransitionType.CROSSFADE -> android.R.drawable.ic_menu_edit
            TransitionType.SLIDE_LEFT -> android.R.drawable.ic_media_previous
            TransitionType.SLIDE_RIGHT -> android.R.drawable.ic_media_next
            TransitionType.ZOOM_IN -> android.R.drawable.ic_menu_zoom
            TransitionType.ZOOM_OUT -> android.R.drawable.ic_menu_revert
            else -> android.R.drawable.ic_menu_gallery
        }
    }

    fun getTransitionName(type: TransitionType): String {
        return when (type) {
            TransitionType.CUT -> "Cut"
            TransitionType.FADE_IN_OUT -> "Fade"
            TransitionType.CROSSFADE -> "Crossfade"
            TransitionType.SLIDE_LEFT -> "Slide Left"
            TransitionType.SLIDE_RIGHT -> "Slide Right"
            TransitionType.SLIDE_UP -> "Slide Up"
            TransitionType.SLIDE_DOWN -> "Slide Down"
            TransitionType.PUSH_LEFT -> "Push Left"
            TransitionType.PUSH_RIGHT -> "Push Right"
            TransitionType.PUSH_UP -> "Push Up"
            TransitionType.PUSH_DOWN -> "Push Down"
            TransitionType.WIPE_LEFT -> "Wipe Left"
            TransitionType.WIPE_RIGHT -> "Wipe Right"
            TransitionType.WIPE_UP -> "Wipe Up"
            TransitionType.WIPE_DOWN -> "Wipe Down"
            TransitionType.WIPE_CIRCLE -> "Circle Wipe"
            TransitionType.WIPE_DIAMOND -> "Diamond Wipe"
            TransitionType.WIPE_STAR -> "Star Wipe"
            TransitionType.ZOOM_IN -> "Zoom In"
            TransitionType.ZOOM_OUT -> "Zoom Out"
            TransitionType.ZOOM_ROTATE -> "Zoom & Rotate"
            TransitionType.CUBE_ROTATE -> "Cube"
            TransitionType.FLIP_HORIZONTAL -> "Flip Horizontal"
            TransitionType.FLIP_VERTICAL -> "Flip Vertical"
            TransitionType.PAGE_CURL -> "Page Curl"
            TransitionType.HEART -> "Heart"
            TransitionType.STAR -> "Star"
            TransitionType.DIAMOND -> "Diamond"
            TransitionType.CIRCLE -> "Circle"
            TransitionType.BLINDS -> "Blinds"
            TransitionType.CHECKERBOARD -> "Checkerboard"
            TransitionType.DISSOLVE -> "Dissolve"
            TransitionType.GLITCH -> "Glitch"
            TransitionType.BURN -> "Burn"
            TransitionType.LIGHT_LEAK -> "Light Leak"
            TransitionType.CUSTOM -> "Custom"
        }
    }
}
