package com.yourname.videoeditor.library.rendering.filter

enum class FilterType {
    // Basic Filters
    NONE("Original"),
    GRAYSCALE("Black & White"),
    SEPIA("Sepia"),
    INVERT("Invert"),
    
    // Color Filters
    VINTAGE("Vintage"),
    COOL("Cool"),
    WARM("Warm"),
    
    // Artistic Filters
    OIL_PAINTING("Oil Painting"),
    CARTOON("Cartoon"),
    SKETCH("Sketch"),
    
    // Lighting Effects
    VIGNETTE("Vignette"),
    BLOOM("Bloom"),
    
    // Distortion
    BULGE("Bulge"),
    PINCH("Pinch"),
    
    // Advanced
    GAUSSIAN_BLUR("Blur"),
    SHARPEN("Sharpen"),
    EDGE_DETECT("Edge Detect"),
    EMBOSS("Emboss"),
    PIXELATE("Pixelate"),
    
    // Instagram-like
    VALENCIA("Valencia"),
    AMARO("Amaro"),
    RISE("Rise"),
    HUDSON("Hudson"),
    XPRO2("X-Pro II"),
    SIERRA("Sierra"),
    LARK("Lark");
    
    val displayName: String
    
    constructor(displayName: String) {
        this.displayName = displayName
    }
    
    companion object {
        fun fromDisplayName(displayName: String): FilterType {
            return values().find { it.displayName == displayName } ?: NONE
        }
    }
}