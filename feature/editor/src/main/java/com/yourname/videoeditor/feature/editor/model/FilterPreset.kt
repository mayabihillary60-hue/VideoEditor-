package com.yourname.videoeditor.feature.editor.model

import com.yourname.videoeditor.library.rendering.filter.FilterType
import java.util.UUID

data class FilterPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filterType: FilterType,
    val parameters: Map<String, Float> = emptyMap(),
    val intensity: Float = 1.0f,
    val brightness: Float = 0.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val hue: Float = 0.0f,
    val sharpness: Float = 0.0f,
    val blurRadius: Float = 0.0f,
    val vignetteIntensity: Float = 0.0f,
    val thumbnailPath: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val category: PresetCategory = PresetCategory.CUSTOM
)

enum class PresetCategory {
    BASIC,
    COLOR,
    ARTISTIC,
    DISTORTION,
    INSTAGRAM,
    VINTAGE,
    CUSTOM
}

// Predefined presets
object DefaultPresets {
    val NONE = FilterPreset(
        name = "Original",
        filterType = FilterType.NONE,
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val GRAYSCALE = FilterPreset(
        name = "Grayscale",
        filterType = FilterType.GRAYSCALE,
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val SEPIA = FilterPreset(
        name = "Sepia",
        filterType = FilterType.SEPIA,
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val VINTAGE = FilterPreset(
        name = "Vintage",
        filterType = FilterType.VINTAGE,
        parameters = mapOf("warmth" to 0.7f, "fade" to 0.3f),
        isDefault = true,
        category = PresetCategory.VINTAGE
    )
    
    val COOL = FilterPreset(
        name = "Cool Blue",
        filterType = FilterType.COOL,
        isDefault = true,
        category = PresetCategory.COLOR
    )
    
    val WARM = FilterPreset(
        name = "Warm Sunset",
        filterType = FilterType.WARM,
        isDefault = true,
        category = PresetCategory.COLOR
    )
    
    val OIL_PAINTING = FilterPreset(
        name = "Oil Painting",
        filterType = FilterType.OIL_PAINTING,
        parameters = mapOf("Effect Strength" to 0.7f),
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val CARTOON = FilterPreset(
        name = "Cartoon",
        filterType = FilterType.CARTOON,
        parameters = mapOf("Effect Strength" to 0.8f),
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val SKETCH = FilterPreset(
        name = "Pencil Sketch",
        filterType = FilterType.SKETCH,
        parameters = mapOf("Effect Strength" to 0.6f),
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val VIGNETTE = FilterPreset(
        name = "Vignette",
        filterType = FilterType.VIGNETTE,
        parameters = mapOf("Intensity" to 1.2f),
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val BLOOM = FilterPreset(
        name = "Bloom",
        filterType = FilterType.BLOOM,
        intensity = 0.8f,
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val BULGE = FilterPreset(
        name = "Bulge",
        filterType = FilterType.BULGE,
        parameters = mapOf("Distortion" to 1.2f),
        isDefault = true,
        category = PresetCategory.DISTORTION
    )
    
    val PINCH = FilterPreset(
        name = "Pinch",
        filterType = FilterType.PINCH,
        parameters = mapOf("Distortion" to 1.0f),
        isDefault = true,
        category = PresetCategory.DISTORTION
    )
    
    val GAUSSIAN_BLUR = FilterPreset(
        name = "Soft Blur",
        filterType = FilterType.GAUSSIAN_BLUR,
        parameters = mapOf("Blur Radius" to 0.03f),
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val SHARPEN = FilterPreset(
        name = "Sharpen",
        filterType = FilterType.SHARPEN,
        parameters = mapOf("Sharpness" to 0.5f),
        isDefault = true,
        category = PresetCategory.BASIC
    )
    
    val EDGE_DETECT = FilterPreset(
        name = "Edge Detect",
        filterType = FilterType.EDGE_DETECT,
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val EMBOSS = FilterPreset(
        name = "Emboss",
        filterType = FilterType.EMBOSS,
        intensity = 0.7f,
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    val PIXELATE = FilterPreset(
        name = "Pixel Art",
        filterType = FilterType.PIXELATE,
        parameters = mapOf("Pixel Size" to 0.4f),
        isDefault = true,
        category = PresetCategory.ARTISTIC
    )
    
    // Instagram-like presets
    val VALENCIA = FilterPreset(
        name = "Valencia",
        filterType = FilterType.VALENCIA,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val AMARO = FilterPreset(
        name = "Amaro",
        filterType = FilterType.AMARO,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val RISE = FilterPreset(
        name = "Rise",
        filterType = FilterType.RISE,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val HUDSON = FilterPreset(
        name = "Hudson",
        filterType = FilterType.HUDSON,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val XPRO2 = FilterPreset(
        name = "X-Pro II",
        filterType = FilterType.XPRO2,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val SIERRA = FilterPreset(
        name = "Sierra",
        filterType = FilterType.SIERRA,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val LARK = FilterPreset(
        name = "Lark",
        filterType = FilterType.LARK,
        isDefault = true,
        category = PresetCategory.INSTAGRAM
    )
    
    val allPresets = listOf(
        NONE, GRAYSCALE, SEPIA, VINTAGE, COOL, WARM,
        OIL_PAINTING, CARTOON, SKETCH, VIGNETTE, BLOOM,
        BULGE, PINCH, GAUSSIAN_BLUR, SHARPEN, EDGE_DETECT,
        EMBOSS, PIXELATE, VALENCIA, AMARO, RISE, HUDSON,
        XPRO2, SIERRA, LARK
    )
}