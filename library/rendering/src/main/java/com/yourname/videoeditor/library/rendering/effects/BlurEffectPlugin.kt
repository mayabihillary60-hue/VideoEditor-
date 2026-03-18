package com.yourname.videoeditor.library.rendering.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20

class BlurEffectPlugin : EffectPlugin {

    private lateinit var context: Context
    private val parameters = mutableMapOf<String, Any>()
    private var blurRadius = 10f

    override fun getInfo(): EffectInfo {
        return EffectInfo(
            id = "com.yourname.effects.blur",
            name = "Gaussian Blur",
            packageName = "com.yourname.effects.blur",
            version = "1.0.0",
            author = "VideoEditor Team",
            description = "Professional gaussian blur effect with adjustable radius",
            category = EffectCategory.VISUAL,
            complexity = EffectComplexity.LOW,
            parameters = listOf(
                EffectParameter(
                    id = "radius",
                    name = "Blur Radius",
                    type = ParameterType.FLOAT,
                    defaultValue = 10f,
                    minValue = 0f,
                    maxValue = 50f,
                    description = "Blur intensity (higher = more blur)"
                )
            )
        )
    }

    override fun onCreate(context: Context) {
        this.context = context
    }

    override fun onDestroy() {
        // Cleanup resources
    }

    override fun getParameters(): List<EffectParameter> {
        return getInfo().parameters
    }

    override fun setParameter(id: String, value: Any) {
        parameters[id] = value
        when (id) {
            "radius" -> blurRadius = (value as? Float) ?: 10f
        }
    }

    override fun getParameter(id: String): Any? {
        return parameters[id]
    }

    override fun processFrame(inputTexture: Int, width: Int, height: Int, timeMs: Long): Int {
        // This would implement actual blur using OpenGL shaders
        // For now, return input texture
        
        // In real implementation, you would:
        // 1. Create framebuffer
        // 2. Apply blur shader
        // 3. Return new texture ID
        
        return inputTexture
    }

    override fun processAudio(samples: FloatArray, sampleRate: Int, channels: Int): FloatArray {
        // Blur effect doesn't modify audio
        return samples
    }

    override fun getThumbnail(): Bitmap? {
        return null
    }

    override fun renderPreview(canvas: Canvas, width: Int, height: Int, timeMs: Long) {
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#FF4081")
            textSize = 40f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        canvas.drawColor(android.graphics.Color.BLACK)
        canvas.drawText("Blur Effect", width / 2f, height / 2f, paint)
        canvas.drawText("Radius: $blurRadius", width / 2f, height / 2f + 50f, paint)
    }

    override fun getConfigurationUI(): android.view.View? {
        return null
    }

    override fun validateParameters(): Boolean {
        return blurRadius in 0f..50f
    }

    override fun getPresets(): List<Pair<String, Map<String, Any>>> {
        return listOf(
            "Light Blur" to mapOf("radius" to 5f),
            "Medium Blur" to mapOf("radius" to 15f),
            "Heavy Blur" to mapOf("radius" to 30f),
            "Maximum Blur" to mapOf("radius" to 50f)
        )
    }

    override fun applyPreset(presetName: String) {
        when (presetName) {
            "Light Blur" -> setParameter("radius", 5f)
            "Medium Blur" -> setParameter("radius", 15f)
            "Heavy Blur" -> setParameter("radius", 30f)
            "Maximum Blur" -> setParameter("radius", 50f)
        }
    }

    override fun getRequiredExtensions(): List<String> {
        return emptyList()
    }

    override fun getShaderCode(): String? {
        return """
            precision mediump float;
            varying vec2 texCoordinate;
            uniform sampler2D inputTexture;
            uniform float radius;
            
            void main() {
                vec4 color = vec4(0.0);
                float total = 0.0;
                
                for (float x = -radius; x <= radius; x++) {
                    for (float y = -radius; y <= radius; y++) {
                        vec2 offset = vec2(x, y) / 1000.0;
                        float weight = 1.0 - (abs(x) + abs(y)) / (radius * 2.0);
                        color += texture2D(inputTexture, texCoordinate + offset) * weight;
                        total += weight;
                    }
                }
                
                gl_FragColor = color / total;
            }
        """.trimIndent()
    }
}
