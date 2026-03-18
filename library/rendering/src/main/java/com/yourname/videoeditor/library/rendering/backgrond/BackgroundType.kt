package com.yourname.videoeditor.library.rendering.background

import android.graphics.Color

enum class BackgroundType {
    SOLID_COLOR,
    GRADIENT,
    IMAGE,
    VIDEO,
    BLUR,
    PATTERN,
    ANIMATED
}

enum class GradientType {
    LINEAR,
    RADIAL,
    SWEEP
}

enum class PatternType {
    DOTS,
    LINES,
    GRID,
    CHESSBOARD,
    STRIPES,
    CUSTOM
}

data class SolidColorBackground(
    var color: Int = Color.BLACK
)

data class GradientBackground(
    var type: GradientType = GradientType.LINEAR,
    var startColor: Int = Color.parseColor("#FF4081"),
    var endColor: Int = Color.parseColor("#3F51B5"),
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float = 1f,
    var endY: Float = 1f,
    var positions: FloatArray? = null,
    var colors: IntArray? = null // For multi-color gradients
)

data class ImageBackground(
    var imagePath: String? = null,
    var scaleType: String = "COVER", // COVER, CONTAIN, STRETCH, TILE
    var blurAmount: Float = 0f
)

data class VideoBackground(
    var videoPath: String? = null,
    var loop: Boolean = true,
    var volume: Float = 0f, // Muted by default
    var startTimeMs: Long = 0,
    var playRate: Float = 1f
)

data class BlurBackground(
    var blurRadius: Float = 25f,
    var sourceType: String = "VIDEO", // VIDEO, IMAGE, COLOR
    var sourceValue: String? = null,
    var brightness: Float = 1f
)

data class PatternBackground(
    var type: PatternType = PatternType.DOTS,
    var primaryColor: Int = Color.WHITE,
    var secondaryColor: Int = Color.GRAY,
    var density: Float = 0.5f,
    var rotation: Float = 0f
)

data class BackgroundConfig(
    var type: BackgroundType = BackgroundType.SOLID_COLOR,
    var solidColor: SolidColorBackground = SolidColorBackground(),
    var gradient: GradientBackground? = null,
    var image: ImageBackground? = null,
    var video: VideoBackground? = null,
    var blur: BlurBackground? = null,
    var pattern: PatternBackground? = null,
    var isEnabled: Boolean = false,
    var opacity: Float = 1f,
    var blendMode: String = "NORMAL"
)
