package com.yourname.videoeditor.library.rendering.effects

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject

enum class EffectCategory {
    VISUAL,
    AUDIO,
    TRANSITION,
    FILTER,
    DISTORTION,
    GENERATOR,
    CUSTOM
}

enum  class EffectComplexity {
    LOW,      // Simple effects that run on all devices
    MEDIUM,   // Moderate effects that need decent GPU
    HIGH,     // Complex effects that need high-end devices
    ULTRA     // Professional effects that need flagship devices
}

data class EffectParameter(
    val id: String,
    val name: String,
    val type: ParameterType,
    val defaultValue: Float,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float = 0.01f,
    val options: List<String>? = null,  // For enum/select parameters
    val description: String = ""
)

enum class ParameterType {
    FLOAT,
    INTEGER,
    BOOLEAN,
    COLOR,
    POSITION,
    ANGLE,
    TEXTURE,
    ENUM,
    FILE_PATH
}

data class EffectInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val version: String,
    val author: String,
    val description: String,
    val category: EffectCategory,
    val complexity: EffectComplexity,
    val thumbnail: Bitmap? = null,
    val previewVideo: String? = null,
    val parameters: List<EffectParameter> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val minApiLevel: Int = 21,
    val isPremium: Boolean = false,
    val price: Double = 0.0,
    val downloadSize: Long = 0,
    val installTime: Long = System.currentTimeMillis()
)

data class EffectInstance(
    val effectId: String,
    var isEnabled: Boolean = true,
    var parameters: MutableMap<String, Any> = mutableMapOf(),
    var startTimeMs: Long = 0,
    var endTimeMs: Long? = null,
    var intensity: Float = 1.0f,
    var blendMode: String = "NORMAL",
    var layer: Int = 0
)

interface EffectPlugin {
    // Basic info
    fun getInfo(): EffectInfo
    
    // Lifecycle
    fun onCreate(context: Context)
    fun onDestroy()
    
    // Parameter handling
    fun getParameters(): List<EffectParameter>
    fun setParameter(id: String, value: Any)
    fun getParameter(id: String): Any?
    
    // Processing
    fun processFrame(inputTexture: Int, width: Int, height: Int, timeMs: Long): Int
    fun processAudio(samples: FloatArray, sampleRate: Int, channels: Int): FloatArray
    
    // Preview
    fun getThumbnail(): Bitmap?
    fun renderPreview(canvas: android.graphics.Canvas, width: Int, height: Int, timeMs: Long)
    
    // Configuration
    fun getConfigurationUI(): android.view.View?
    fun validateParameters(): Boolean
    
    // Presets
    fun getPresets(): List<Pair<String, Map<String, Any>>>
    fun applyPreset(presetName: String)
    
    // Metadata
    fun getRequiredExtensions(): List<String>
    fun getShaderCode(): String?
}
