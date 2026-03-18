package com.yourname.videoeditor.library.rendering.overlay

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri

enum class OverlayType {
    TEXT,
    STICKER,
    EMOJI,
    SHAPE
}

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

enum class TextStyle {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC
}

enum class AnimationType {
    NONE,
    FADE_IN,
    FADE_OUT,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    BOUNCE,
    PULSE,
    ROTATE,
    ZOOM_IN,
    ZOOM_OUT,
    TYPEWRITER,
    BLINK,
    SHAKE,
    CUSTOM
}

data class TextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "",
    var fontFamily: String? = null,
    var fontSize: Float = 32f,
    var textColor: Int = Color.WHITE,
    var backgroundColor: Int = Color.TRANSPARENT,
    var alignment: TextAlignment = TextAlignment.CENTER,
    var style: TextStyle = TextStyle.NORMAL,
    var underline: Boolean = false,
    var strikethrough: Boolean = false,
    var letterSpacing: Float = 0f,
    var lineSpacing: Float = 0f,
    var shadowRadius: Float = 0f,
    var shadowColor: Int = Color.BLACK,
    var shadowOffsetX: Float = 2f,
    var shadowOffsetY: Float = 2f
)

data class StickerOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    var uri: Uri? = null,
    var stickerType: String = "emoji", // emoji, sticker, shape
    var emojiCode: String? = null,      // Unicode emoji
    var tintColor: Int? = null           // For tinting stickers
)

data class OverlayPosition(
    var x: Float = 0.5f,      // 0-1 relative to video width
    var y: Float = 0.5f,      // 0-1 relative to video height
    var rotation: Float = 0f,  // degrees
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var opacity: Float = 1f    // 0-1
)

data class OverlayTiming(
    var startTimeMs: Long = 0,
    var endTimeMs: Long = 5000,  // 5 seconds default
    var animationIn: AnimationType = AnimationType.NONE,
    var animationOut: AnimationType = AnimationType.NONE,
    var animationDurationMs: Long = 500
)

data class OverlayElement(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OverlayType,
    val textOverlay: TextOverlay? = null,
    val stickerOverlay: StickerOverlay? = null,
    var position: OverlayPosition = OverlayPosition(),
    var timing: OverlayTiming = OverlayTiming(),
    var layer: Int = 0,  // Z-order (higher = on top)
    var isSelected: Boolean = false
)
