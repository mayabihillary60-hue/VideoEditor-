package com.yourname.videoeditor.library.rendering.background

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

class BackgroundRenderer(private val context: Context) : GLSurfaceView.Renderer {

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
        precision highp float;
        varying vec2 texCoordinate;
        uniform sampler2D inputTexture;
        uniform int backgroundType;
        uniform vec4 solidColor;
        uniform vec4 gradientStartColor;
        uniform vec4 gradientEndColor;
        uniform int gradientType;
        uniform float time;
        uniform vec2 resolution;
        
        // Solid color background
        vec4 solidColorBackground() {
            return solidColor;
        }
        
        // Linear gradient
        vec4 linearGradient(vec2 uv) {
            return mix(gradientStartColor, gradientEndColor, uv.x);
        }
        
        // Radial gradient
        vec4 radialGradient(vec2 uv) {
            vec2 center = vec2(0.5, 0.5);
            float dist = distance(uv, center);
            return mix(gradientStartColor, gradientEndColor, dist * 2.0);
        }
        
        // Sweep gradient
        vec4 sweepGradient(vec2 uv) {
            vec2 center = vec2(0.5, 0.5);
            vec2 dir = uv - center;
            float angle = atan(dir.y, dir.x);
            float t = (angle / 6.28318) + 0.5;
            return mix(gradientStartColor, gradientEndColor, t);
        }
        
        // Pattern: Dots
        vec4 dotsPattern(vec2 uv, float time) {
            float scale = 20.0;
            vec2 grid = floor(uv * scale);
            vec2 pos = fract(uv * scale);
            
            float dot = distance(pos, vec2(0.5));
            float alpha = smoothstep(0.4, 0.5, dot);
            
            vec4 color1 = vec4(1.0, 1.0, 1.0, 1.0);
            vec4 color2 = vec4(0.5, 0.5, 0.5, 1.0);
            
            return mix(color2, color1, alpha);
        }
        
        // Pattern: Grid
        vec4 gridPattern(vec2 uv) {
            float thickness = 0.02;
            vec2 grid = fract(uv * 20.0);
            
            float lineX = step(grid.x, thickness) + step(1.0 - thickness, grid.x);
            float lineY = step(grid.y, thickness) + step(1.0 - thickness, grid.y);
            float line = max(lineX, lineY);
            
            vec4 color1 = vec4(1.0);
            vec4 color2 = vec4(0.3);
            
            return mix(color2, color1, line);
        }
        
        // Pattern: Stripes
        vec4 stripesPattern(vec2 uv, float rotation) {
            float angle = rotation * 3.14159 / 180.0;
            float s = sin(angle);
            float c = cos(angle);
            
            vec2 rotatedUv = vec2(
                uv.x * c - uv.y * s,
                uv.x * s + uv.y * c
            );
            
            float stripe = fract(rotatedUv.x * 10.0);
            float alpha = step(0.5, stripe);
            
            return mix(vec4(0.3), vec4(1.0), alpha);
        }
        
        void main() {
            vec2 uv = texCoordinate;
            uv.y = 1.0 - uv.y;
            
            vec4 result;
            
            if (backgroundType == ${BackgroundType.SOLID_COLOR.ordinal}) {
                result = solidColorBackground();
            } else if (backgroundType == ${BackgroundType.GRADIENT.ordinal}) {
                if (gradientType == ${GradientType.LINEAR.ordinal}) {
                    result = linearGradient(uv);
                } else if (gradientType == ${GradientType.RADIAL.ordinal}) {
                    result = radialGradient(uv);
                } else {
                    result = sweepGradient(uv);
                }
            } else if (backgroundType == ${BackgroundType.IMAGE.ordinal}) {
                result = texture2D(inputTexture, uv);
            } else if (backgroundType == ${BackgroundType.VIDEO.ordinal}) {
                result = texture2D(inputTexture, uv);
            } else if (backgroundType == ${BackgroundType.BLUR.ordinal}) {
                // Simple blur approximation
                vec4 color = vec4(0.0);
                float blurSize = 0.01;
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        vec2 offset = vec2(float(x), float(y)) * blurSize;
                        color += texture2D(inputTexture, uv + offset);
                    }
                }
                result = color / 25.0;
            } else if (backgroundType == ${BackgroundType.PATTERN.ordinal}) {
                result = dotsPattern(uv, time);
            } else {
                result = solidColorBackground();
            }
            
            gl_FragColor = result;
        }
    """

    private var program = 0
    private var backgroundConfig = BackgroundConfig()
    
    fun setBackground(config: BackgroundConfig) {
        this.backgroundConfig = config
    }

    fun renderBackground(canvas: Canvas, width: Int, height: Int) {
        when (backgroundConfig.type) {
            BackgroundType.SOLID_COLOR -> {
                canvas.drawColor(backgroundConfig.solidColor.color)
            }
            BackgroundType.GRADIENT -> {
                backgroundConfig.gradient?.let { gradient ->
                    val paint = Paint().apply {
                        shader = when (gradient.type) {
                            GradientType.LINEAR -> LinearGradient(
                                gradient.startX * width,
                                gradient.startY * height,
                                gradient.endX * width,
                                gradient.endY * height,
                                gradient.startColor,
                                gradient.endColor,
                                Shader.TileMode.CLAMP
                            )
                            GradientType.RADIAL -> RadialGradient(
                                width / 2f,
                                height / 2f,
                                maxOf(width, height) / 2f,
                                gradient.startColor,
                                gradient.endColor,
                                Shader.TileMode.CLAMP
                            )
                            GradientType.SWEEP -> SweepGradient(
                                width / 2f,
                                height / 2f,
                                gradient.startColor,
                                gradient.endColor
                            )
                        }
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                }
            }
            BackgroundType.PATTERN -> {
                backgroundConfig.pattern?.let { pattern ->
                    renderPattern(canvas, pattern, width, height)
                }
            }
            else -> {
                // Other background types handled by OpenGL
            }
        }
    }

    private fun renderPattern(canvas: Canvas, pattern: PatternBackground, width: Int, height: Int) {
        val paint = Paint()
        paint.color = pattern.primaryColor
        
        when (pattern.type) {
            PatternType.DOTS -> {
                val dotSpacing = 50 * pattern.density
                for (x in 0..width step dotSpacing.toInt()) {
                    for (y in 0..height step dotSpacing.toInt()) {
                        canvas.drawCircle(x.toFloat(), y.toFloat(), 5f, paint)
                    }
                }
            }
            PatternType.LINES -> {
                val lineSpacing = 30 * pattern.density
                for (y in 0..height step lineSpacing.toInt()) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            PatternType.GRID -> {
                val gridSize = 50 * pattern.density
                paint.strokeWidth = 2f
                for (x in 0..width step gridSize.toInt()) {
                    canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
                }
                for (y in 0..height step gridSize.toInt()) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                }
            }
            PatternType.CHESSBOARD -> {
                val squareSize = 40 * pattern.density
                for (x in 0 until (width / squareSize).toInt()) {
                    for (y in 0 until (height / squareSize).toInt()) {
                        if ((x + y) % 2 == 0) {
                            paint.color = pattern.primaryColor
                        } else {
                            paint.color = pattern.secondaryColor
                        }
                        canvas.drawRect(
                            x * squareSize,
                            y * squareSize,
                            (x + 1) * squareSize,
                            (y + 1) * squareSize,
                            paint
                        )
                    }
                }
            }
            PatternType.STRIPES -> {
                val stripeWidth = 30 * pattern.density
                canvas.save()
                canvas.rotate(pattern.rotation, width / 2f, height / 2f)
                for (x in -width..width * 2 step stripeWidth.toInt()) {
                    if ((x / stripeWidth).toInt() % 2 == 0) {
                        paint.color = pattern.primaryColor
                    } else {
                        paint.color = pattern.secondaryColor
                    }
                    canvas.drawRect(x.toFloat(), -height.toFloat(), x + stripeWidth, height * 2f, paint)
                }
                canvas.restore()
            }
            else -> {}
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        createProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // OpenGL rendering would go here
    }

    private fun createProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
