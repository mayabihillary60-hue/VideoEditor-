package com.yourname.videoeditor.library.rendering.transition

enum class TransitionType {
    // Basic Transitions
    CUT,                // No transition (instant cut)
    FADE_IN_OUT,        // Fade out first clip, fade in second
    CROSSFADE,          // Smooth blend between clips
    
    // Slide Transitions
    SLIDE_LEFT,         // Second clip slides from left
    SLIDE_RIGHT,        // Second clip slides from right
    SLIDE_UP,           // Second clip slides from top
    SLIDE_DOWN,         // Second clip slides from bottom
    
    // Push Transitions
    PUSH_LEFT,          // First clip pushed left by second
    PUSH_RIGHT,         // First clip pushed right by second
    PUSH_UP,            // First clip pushed up by second
    PUSH_DOWN,          // First clip pushed down by second
    
    // Wipe Transitions
    WIPE_LEFT,          // Wipe from left to right
    WIPE_RIGHT,         // Wipe from right to left
    WIPE_UP,            // Wipe from bottom to top
    WIPE_DOWN,          // Wipe from top to bottom
    WIPE_CIRCLE,        // Circle wipe expanding from center
    WIPE_DIAMOND,       // Diamond wipe expanding from center
    WIPE_STAR,          // Star wipe
    
    // Zoom Transitions
    ZOOM_IN,            // Second clip zooms in
    ZOOM_OUT,           // First clip zooms out
    ZOOM_ROTATE,        // Zoom with rotation
    
    // 3D Transitions
    CUBE_ROTATE,        // Cube rotation effect
    FLIP_HORIZONTAL,    // Horizontal flip
    FLIP_VERTICAL,      // Vertical flip
    PAGE_CURL,          // Page curl effect
    
    // Creative Transitions
    HEART,              // Heart shape wipe
    STAR,               // Star shape wipe
    DIAMOND,            // Diamond shape wipe
    CIRCLE,             // Circle shape wipe
    BLINDS,             // Venetian blinds effect
    CHECKERBOARD,       // Checkerboard pattern
    DISSOLVE,           // Dissolve with noise
    GLITCH,             // Digital glitch effect
    BURN,               // Film burn effect
    LIGHT_LEAK,         // Light leak transition
    
    // Custom
    CUSTOM
}

enum class TransitionEasing {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE,
    ELASTIC
}

data class TransitionConfig(
    val type: TransitionType = TransitionType.CROSSFADE,
    val durationMs: Long = 1000,           // 1 second default
    val easing: TransitionEasing = TransitionEasing.EASE_IN_OUT,
    val direction: Int = 0,                 // Direction parameter for transitions that support it
    val intensity: Float = 1.0f,            // Effect intensity
    val customShader: String? = null         // Custom GLSL shader for custom transitions
)
