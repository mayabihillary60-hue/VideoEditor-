package com.yourname.videoeditor.library.rendering.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.random.Random

class GlitchEffectPlugin : EffectPlugin {

    private lateinit var context: Context
    private val parameters = mutableMapOf<String, Any>()
    private var intensity = 0.5f
    private var speed = 1f
    private var timeOffset = 0f

    override fun getInfo(): EffectInfo {
        return EffectInfo(
            id = "com.yourname.effects.glitch",
            name = "Digital Glitch",
            packageName = "com.yourname.effects.glitch",
            version = "1.0.0",
            author = "VideoEditor Team",
            description = "Retro digital glitch effect with RGB shift and noise",
            category = EffectCategory.DISTORTION,
            complexity = EffectComplexity.MEDIUM,
            parameters = listOf(
                EffectParameter(
                    id = "intensity",
                    name = "Glitch Intensity",
                    type = ParameterType.FLOAT,
                    defaultValue = 0.5f,
                    minValue = 0f,
                    maxValue = 2f
                ),
                EffectParameter(
                    id = "speed",
                    name = "Glitch Speed",
                    type = ParameterType.FLOAT,
                    defaultValue = 1f,
                    minValue = 0f,
                    maxValue = 5f
                )
            )
        )
    }

    override fun onCreate(context: Context) {
        this.context = context
    }

    override fun onDestroy() {}

    override fun getParameters(): List<EffectParameter> {
        return getInfo().parameters
    }

    override fun setParameter(id: String, value: Any) {
        parameters[id] = value
        when (id) {
            "intensity" -> intensity = (value as? Float) ?: 0.5f
            "speed" -> speed = (value as? Float) ?: 1f
        }
    }

    override fun getParameter(id: String): Any? {
        return parameters[id]
    }

    override fun processFrame(inputTexture: Int, width: Int, height: Int, timeMs: Long): Int {
        return inputTexture
    }

    override fun processAudio(samples: FloatArray, sampleRate: Int, channels: Int): FloatArray {
        return samples
    }

    override fun getThumbnail(): Bitmap? = null

    override fun renderPreview(canvas: Canvas, width: Int, height: Int, timeMs: Long) {
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#00FF00")
            textSize = 40f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        canvas.drawColor(android.graphics.Color.BLACK)
        
        // Simulate glitch with random blocks
        timeOffset += speed
        for (i in 0 until (intensity * 20).toInt()) {
            val x = Random.nextInt(width)
            val y = Random.nextInt(height)
            val w = Random.nextInt(100, 300)
            val h = Random.nextInt(10, 50)
            
            paint.color = android.graphics.Color.rgb(
                Random.nextInt(255),
                Random.nextInt(255),
                Random.nextInt(255)
            )
            
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
        }
        
        canvas.drawText("GLITCH", width / 2f, height / 2f, paint)
    }

    override fun getConfigurationUI(): android.view.View? = null

    override fun validateParameters(): Boolean = true

    override fun getPresets(): List<Pair<String, Map<String, Any>>> {
        return listOf(
            "Subtle Glitch" to mapOf("intensity" to 0.3f, "speed" to 0.5f),
            "Heavy Glitch" to mapOf("intensity" to 1.5f, "speed" to 2f),
            "Crazy Glitch" to mapOf("intensity" to 2f, "speed" to 5f)
        )
    }

    override fun applyPreset(presetName: String) {
        when (presetName) {
            "Subtle Glitch" -> {
                setParameter("intensity", 0.3f)
                setParameter("speed", 0.5f)
            }
            "Heavy Glitch" -> {
                setParameter("intensity", 1.5f)
                setParameter("speed", 2f)
            }
            "Crazy Glitch" -> {
                setParameter("intensity", 2f)
                setParameter("speed", 5f)
            }
        }
    }

    override fun getRequiredExtensions(): List<String> = emptyList()

    override fun getShaderCode(): String? {
        return """
            precision mediump float;
            varying vec2 texCoordinate;
            uniform sampler2D inputTexture;
            uniform float intensity;
            uniform float time;
            
            void main() {
                vec2 uv = texCoordinate;
                float glitch = sin(uv.y * 100.0 + time * 10.0) * intensity;
                uv.x += glitch * 0.05;
                
                vec4 colorR = texture2D(inputTexture, uv + vec2(glitch * 0.02, 0.0));
                vec4 colorG = texture2D(inputTexture, uv);
                vec4 colorB = texture2D(inputTexture, uv - vec2(glitch * 0.02, 0.0));
                
                gl_FragColor = vec4(colorR.r, colorG.g, colorB.b, 1.0);
            }
        """.trimIndent()
    }
}
