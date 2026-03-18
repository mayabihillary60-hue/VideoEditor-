package com.yourname.videoeditor.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.videoeditor.library.rendering.overlay.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.graphics.Color
import android.net.Uri

class OverlayViewModel : ViewModel() {

    private val _overlayElements = MutableStateFlow<List<OverlayElement>>(emptyList())
    val overlayElements: StateFlow<List<OverlayElement>> = _overlayElements

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    // Available fonts
    val availableFonts = listOf(
        "Default",
        "Arial",
        "Helvetica",
        "Times New Roman",
        "Courier",
        "Verdana",
        "Georgia",
        "Comic Sans MS",
        "Impact"
    )

    // Available colors
    val availableColors = listOf(
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.parseColor("#FF4081"), // Pink
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#4CAF50")  // Green
    )

    // Animation presets
    val animationPresets = listOf(
        AnimationType.NONE to "None",
        AnimationType.FADE_IN to "Fade In",
        AnimationType.FADE_OUT to "Fade Out",
        AnimationType.SLIDE_LEFT to "Slide Left",
        AnimationType.SLIDE_RIGHT to "Slide Right",
        AnimationType.SLIDE_UP to "Slide Up",
        AnimationType.SLIDE_DOWN to "Slide Down",
        AnimationType.BOUNCE to "Bounce",
        AnimationType.PULSE to "Pulse",
        AnimationType.ROTATE to "Rotate",
        AnimationType.ZOOM_IN to "Zoom In",
        AnimationType.ZOOM_OUT to "Zoom Out",
        AnimationType.TYPEWRITER to "Typewriter",
        AnimationType.BLINK to "Blink",
        AnimationType.SHAKE to "Shake"
    )

    // Sticker categories (you would load these from assets)
    val stickerCategories = mapOf(
        "Emoji" to listOf("😀", "😂", "😍", "👍", "🎉", "❤️", "⭐", "🔥"),
        "Animals" to listOf("🐶", "🐱", "🐼", "🦊", "🐸", "🐧", "🦁", "🐨"),
        "Food" to listOf("🍕", "🍔", "🌮", "🍣", "🍦", "🍩", "🍪", "🍫"),
        "Objects" to listOf("📱", "💻", "📷", "🎮", "🎸", "⚽", "🏀", "🎯"),
        "Nature" to listOf("🌞", "🌙", "⭐", "🌈", "🌺", "🌲", "🌊", "⛰️")
    )

    fun addTextOverlay() {
        val newElement = OverlayElement(
            type = OverlayType.TEXT,
            textOverlay = TextOverlay(
                text = "Double tap to edit",
                fontSize = 48f,
                textColor = Color.WHITE,
                backgroundColor = Color.parseColor("#80000000")
            ),
            timing = OverlayTiming(
                startTimeMs = _currentTimeMs.value,
                endTimeMs = _currentTimeMs.value + 5000
            )
        )
        _overlayElements.value = _overlayElements.value + newElement
        selectElement(newElement.id)
    }

    fun addStickerOverlay(emojiCode: String) {
        val newElement = OverlayElement(
            type = OverlayType.EMOJI,
            stickerOverlay = StickerOverlay(
                emojiCode = emojiCode,
                stickerType = "emoji"
            ),
            timing = OverlayTiming(
                startTimeMs = _currentTimeMs.value,
                endTimeMs = _currentTimeMs.value + 5000
            )
        )
        _overlayElements.value = _overlayElements.value + newElement
        selectElement(newElement.id)
    }

    fun addShapeOverlay() {
        val newElement = OverlayElement(
            type = OverlayType.SHAPE,
            position = OverlayPosition(scaleX = 0.3f, scaleY = 0.3f)
        )
        _overlayElements.value = _overlayElements.value + newElement
        selectElement(newElement.id)
    }

    fun removeElement(elementId: String) {
        _overlayElements.value = _overlayElements.value.filter { it.id != elementId }
        if (_selectedElementId.value == elementId) {
            _selectedElementId.value = null
        }
    }

    fun selectElement(elementId: String?) {
        _selectedElementId.value = elementId
        _isEditing.value = elementId != null
    }

    fun updateElementText(elementId: String, text: String) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    textOverlay = element.textOverlay?.copy(text = text)
                )
            } else element
        }
    }

    fun updateElementPosition(elementId: String, x: Float, y: Float) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    position = element.position.copy(x = x, y = y)
                )
            } else element
        }
    }

    fun updateElementScale(elementId: String, scale: Float) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    position = element.position.copy(scaleX = scale, scaleY = scale)
                )
            } else element
        }
    }

    fun updateElementRotation(elementId: String, rotation: Float) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    position = element.position.copy(rotation = rotation)
                )
            } else element
        }
    }

    fun updateElementOpacity(elementId: String, opacity: Float) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    position = element.position.copy(opacity = opacity)
                )
            } else element
        }
    }

    fun updateElementTiming(elementId: String, startMs: Long, endMs: Long) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    timing = element.timing.copy(startTimeMs = startMs, endTimeMs = endMs)
                )
            } else element
        }
    }

    fun updateElementAnimation(elementId: String, inAnim: AnimationType, outAnim: AnimationType) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    timing = element.timing.copy(
                        animationIn = inAnim,
                        animationOut = outAnim
                    )
                )
            } else element
        }
    }

    fun updateElementTextStyle(elementId: String, style: TextStyle) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    textOverlay = element.textOverlay?.copy(style = style)
                )
            } else element
        }
    }

    fun updateElementTextColor(elementId: String, color: Int) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    textOverlay = element.textOverlay?.copy(textColor = color)
                )
            } else element
        }
    }

    fun updateElementFontSize(elementId: String, size: Float) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(
                    textOverlay = element.textOverlay?.copy(fontSize = size)
                )
            } else element
        }
    }

    fun bringToFront(elementId: String) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(layer = (_overlayElements.value.maxOfOrNull { it.layer } ?: 0) + 1)
            } else element
        }
    }

    fun sendToBack(elementId: String) {
        _overlayElements.value = _overlayElements.value.map { element ->
            if (element.id == elementId) {
                element.copy(layer = 0)
            } else element.copy(layer = element.layer + 1)
        }
    }

    fun duplicateElement(elementId: String) {
        val element = _overlayElements.value.find { it.id == elementId }
        element?.let {
            val duplicate = it.copy(
                id = java.util.UUID.randomUUID().toString(),
                position = it.position.copy(x = it.position.x + 0.1f, y = it.position.y + 0.1f)
            )
            _overlayElements.value = _overlayElements.value + duplicate
        }
    }

    fun setCurrentTime(timeMs: Long) {
        _currentTimeMs.value = timeMs
    }

    fun getSelectedElement(): OverlayElement? {
        return _overlayElements.value.find { it.id == _selectedElementId.value }
    }
}
