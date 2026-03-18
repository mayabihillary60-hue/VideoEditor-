package com.yourname.videoeditor.library.rendering.overlay

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OverlayRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoordinate;
        varying vec2 texCoordinate;
        uniform mat4 uMVPMatrix;
        
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            texCoordinate = vTexCoordinate;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 texCoordinate;
        uniform sampler2D inputTexture;
        uniform sampler2D overlayTexture;
        uniform float opacity;
        
        void main() {
            vec4 videoColor = texture2D(inputTexture, texCoordinate);
            vec4 overlayColor = texture2D(overlayTexture, texCoordinate);
            gl_FragColor = mix(videoColor, overlayColor, overlayColor.a * opacity);
        }
    """

    private val overlayElements = mutableListOf<OverlayElement>()
    private var currentTimeMs: Long = 0
    private var program = 0
    private var overlayTextureId = 0

    fun addElement(element: OverlayElement) {
        overlayElements.add(element)
        overlayElements.sortByDescending { it.layer } // Higher layer first
    }

    fun removeElement(elementId: String) {
        overlayElements.removeAll { it.id == elementId }
    }

    fun updateElement(element: OverlayElement) {
        val index = overlayElements.indexOfFirst { it.id == element.id }
        if (index != -1) {
            overlayElements[index] = element
        }
    }

    fun clearElements() {
        overlayElements.clear()
    }

    fun setCurrentTime(timeMs: Long) {
        this.currentTimeMs = timeMs
    }

    fun getElementsAtTime(timeMs: Long): List<OverlayElement> {
        return overlayElements.filter {
            timeMs in it.timing.startTimeMs..it.timing.endTimeMs
        }
    }

    fun renderOverlay(canvas: Canvas, videoWidth: Int, videoHeight: Int) {
        val visibleElements = getElementsAtTime(currentTimeMs)
        
        for (element in visibleElements) {
            val progress = calculateAnimationProgress(element)
            
            canvas.save()
            
            // Apply transformations
            canvas.translate(
                element.position.x * videoWidth,
                element.position.y * videoHeight
            )
            canvas.rotate(element.position.rotation)
            canvas.scale(
                element.position.scaleX * progress.scale,
                element.position.scaleY * progress.scale
            )
            
            // Set alpha
            val paint = Paint().apply {
                alpha = (element.position.opacity * progress.alpha * 255).toInt()
            }
            
            when (element.type) {
                OverlayType.TEXT -> {
                    element.textOverlay?.let { text ->
                        renderText(canvas, text, paint)
                    }
                }
                OverlayType.STICKER -> {
                    element.stickerOverlay?.let { sticker ->
                        renderSticker(canvas, sticker, paint)
                    }
                }
                OverlayType.EMOJI -> {
                    element.stickerOverlay?.let { emoji ->
                        renderEmoji(canvas, emoji, paint)
                    }
                }
                OverlayType.SHAPE -> {
                    renderShape(canvas, element, paint)
                }
            }
            
            canvas.restore()
        }
    }

    private fun renderText(canvas: Canvas, text: TextOverlay, paint: Paint) {
        paint.apply {
            color = text.textColor
            textSize = text.fontSize
            typeface = createTypeface(text)
            isUnderlineText = text.underline
            isStrikeThruText = text.strikethrough
            letterSpacing = text.letterSpacing
            setShadowLayer(
                text.shadowRadius,
                text.shadowOffsetX,
                text.shadowOffsetY,
                text.shadowColor
            )
        }

        // Handle alignment
        val x = when (text.alignment) {
            TextAlignment.LEFT -> 0f
            TextAlignment.CENTER -> -paint.measureText(text.text) / 2
            TextAlignment.RIGHT -> -paint.measureText(text.text)
            TextAlignment.JUSTIFY -> 0f // Simplified
        }

        // Draw background if needed
        if (text.backgroundColor != Color.TRANSPARENT) {
            val bounds = Rect()
            paint.getTextBounds(text.text, 0, text.text.length, bounds)
            paint.color = text.backgroundColor
            canvas.drawRect(
                x - 10,
                -bounds.height().toFloat() - 10,
                x + paint.measureText(text.text) + 10,
                10f,
                paint
            )
            paint.color = text.textColor
        }

        canvas.drawText(text.text, x, 0f, paint)
    }

    private fun renderSticker(canvas: Canvas, sticker: StickerOverlay, paint: Paint) {
        sticker.uri?.let { uri ->
            // Load sticker image (you'll need to implement this)
            // For now, draw a placeholder
            paint.color = Color.parseColor("#FF4081")
            canvas.drawRect(-50f, -50f, 50f, 50f, paint)
        }
    }

    private fun renderEmoji(canvas: Canvas, emoji: StickerOverlay, paint: Paint) {
        emoji.emojiCode?.let { code ->
            // Render emoji using TextView or custom bitmap
            paint.color = Color.WHITE
            paint.textSize = 48f
            canvas.drawText(code, -24f, 24f, paint)
        }
    }

    private fun renderShape(canvas: Canvas, element: OverlayElement, paint: Paint) {
        // Render basic shapes (rectangle, circle, etc.)
        paint.color = Color.parseColor("#FF4081")
        canvas.drawRect(-50f, -50f, 50f, 50f, paint)
    }

    private fun createTypeface(text: TextOverlay): Typeface {
        return when (text.style) {
            TextStyle.NORMAL -> Typeface.create(text.fontFamily, Typeface.NORMAL)
            TextStyle.BOLD -> Typeface.create(text.fontFamily, Typeface.BOLD)
            TextStyle.ITALIC -> Typeface.create(text.fontFamily, Typeface.ITALIC)
            TextStyle.BOLD_ITALIC -> Typeface.create(text.fontFamily, Typeface.BOLD_ITALIC)
        }
    }

    private fun calculateAnimationProgress(element: OverlayElement): AnimationProgress {
        val time = currentTimeMs
        val timing = element.timing
        
        return when {
            time < timing.startTimeMs -> AnimationProgress(0f, 0f)
            time in timing.startTimeMs..(timing.startTimeMs + timing.animationDurationMs) -> {
                // Animation in
                val progress = (time - timing.startTimeMs).toFloat() / timing.animationDurationMs
                calculateAnimationValues(timing.animationIn, progress)
            }
            time in (timing.endTimeMs - timing.animationDurationMs)..timing.endTimeMs -> {
                // Animation out
                val progress = 1f - ((time - (timing.endTimeMs - timing.animationDurationMs)).toFloat() / timing.animationDurationMs)
                calculateAnimationValues(timing.animationOut, progress)
            }
            time in timing.startTimeMs..timing.endTimeMs -> AnimationProgress(1f, 1f)
            else -> AnimationProgress(0f, 0f)
        }
    }

    private fun calculateAnimationValues(type: AnimationType, progress: Float): AnimationProgress {
        return when (type) {
            AnimationType.NONE -> AnimationProgress(1f, 1f)
            AnimationType.FADE_IN -> AnimationProgress(progress, 1f)
            AnimationType.FADE_OUT -> AnimationProgress(1f - progress, 1f)
            AnimationType.SLIDE_LEFT -> AnimationProgress(1f, progress, -progress * 100)
            AnimationType.SLIDE_RIGHT -> AnimationProgress(1f, progress, progress * 100)
            AnimationType.SLIDE_UP -> AnimationProgress(1f, progress, 0f, -progress * 100)
            AnimationType.SLIDE_DOWN -> AnimationProgress(1f, progress, 0f, progress * 100)
            AnimationType.BOUNCE -> {
                val bounce = (Math.sin(progress * Math.PI * 3) * 0.2f).toFloat()
                AnimationProgress(1f, progress, 0f, bounce * 50)
            }
            AnimationType.PULSE -> {
                val scale = 1f + (Math.sin(progress * Math.PI * 4) * 0.2f).toFloat()
                AnimationProgress(scale, 1f)
            }
            AnimationType.ROTATE -> AnimationProgress(1f, 1f, rotation = progress * 360)
            AnimationType.ZOOM_IN -> AnimationProgress(progress, 1f)
            AnimationType.ZOOM_OUT -> AnimationProgress(1f + (1f - progress), 1f)
            else -> AnimationProgress(1f, 1f)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        createProgram()
        setupTextures()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // OpenGL rendering would go here
        // For simplicity, we're using Canvas for overlays
    }

    private fun createProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    private fun setupTextures() {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        overlayTextureId = textureIds[0]
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}

data class AnimationProgress(
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val rotation: Float = 0f
)
