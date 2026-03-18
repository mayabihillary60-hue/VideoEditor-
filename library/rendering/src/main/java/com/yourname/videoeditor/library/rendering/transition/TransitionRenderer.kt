package com.yourname.videoeditor.library.rendering.transition

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TransitionRenderer : GLSurfaceView.Renderer {

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
        uniform sampler2D frame1;
        uniform sampler2D frame2;
        uniform float progress;
        uniform int transitionType;
        uniform float intensity;
        uniform vec2 resolution;
        
        // Helper functions for transitions
        
        // Linear interpolation
        float easeLinear(float t) {
            return t;
        }
        
        // Ease in
        float easeIn(float t) {
            return t * t;
        }
        
        // Ease out
        float easeOut(float t) {
            return t * (2.0 - t);
        }
        
        // Ease in-out
        float easeInOut(float t) {
            return t < 0.5 ? 2.0 * t * t : -1.0 + (4.0 - 2.0 * t) * t;
        }
        
        // Bounce ease
        float easeBounce(float t) {
            if (t < 0.3636) {
                return 7.5625 * t * t;
            } else if (t < 0.7273) {
                float s = t - 0.5455;
                return 7.5625 * s * s + 0.75;
            } else if (t < 0.9091) {
                float s = t - 0.8182;
                return 7.5625 * s * s + 0.9375;
            } else {
                float s = t - 0.9545;
                return 7.5625 * s * s + 0.984375;
            }
        }
        
        // Elastic ease
        float easeElastic(float t) {
            if (t == 0.0 || t == 1.0) return t;
            float p = 0.3;
            return pow(2.0, -10.0 * t) * sin((t - p / 4.0) * (2.0 * 3.14159) / p) + 1.0;
        }
        
        // Fade transition
        vec4 fade(vec4 color1, vec4 color2, float t) {
            return mix(color1, color2, t);
        }
        
        // Slide left
        vec4 slideLeft(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 offset = vec2(t - 1.0, 0.0);
            vec2 uv1 = uv + offset;
            vec2 uv2 = uv;
            
            vec4 c1 = texture2D(frame1, uv1);
            vec4 c2 = texture2D(frame2, uv2);
            
            return mix(c1, c2, step(1.0, uv1.x));
        }
        
        // Slide right
        vec4 slideRight(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 offset = vec2(1.0 - t, 0.0);
            vec2 uv1 = uv;
            vec2 uv2 = uv + offset;
            
            vec4 c1 = texture2D(frame1, uv1);
            vec4 c2 = texture2D(frame2, uv2);
            
            return mix(c1, c2, step(1.0, uv2.x));
        }
        
        // Wipe left
        vec4 wipeLeft(vec4 color1, vec4 color2, vec2 uv, float t) {
            float edge = uv.x;
            return mix(color2, color1, step(edge, t));
        }
        
        // Wipe right
        vec4 wipeRight(vec4 color1, vec4 color2, vec2 uv, float t) {
            float edge = 1.0 - uv.x;
            return mix(color2, color1, step(edge, t));
        }
        
        // Circle wipe
        vec4 circleWipe(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 center = vec2(0.5, 0.5);
            float dist = distance(uv, center) * 2.0;
            return mix(color2, color1, step(dist, t));
        }
        
        // Diamond wipe
        vec4 diamondWipe(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 center = vec2(0.5, 0.5);
            float dist = abs(uv.x - center.x) + abs(uv.y - center.y);
            return mix(color2, color1, step(dist, t));
        }
        
        // Heart wipe
        vec4 heartWipe(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 center = vec2(0.5, 0.4);
            uv -= center;
            uv *= 2.0;
            
            float a = atan(uv.y, uv.x);
            float r = length(uv);
            
            // Heart equation
            float heart = r * (1.0 + 0.5 * sin(a + 3.14159)) - 0.8;
            return mix(color2, color1, step(heart, t - 0.5));
        }
        
        // Star wipe
        vec4 starWipe(vec4 color1, vec4 color2, vec2 uv, float t) {
            vec2 center = vec2(0.5, 0.5);
            uv -= center;
            
            float angle = atan(uv.y, uv.x);
            float radius = length(uv) * 2.0;
            
            float spikes = 5.0;
            float star = radius * (1.0 + 0.3 * sin(angle * spikes));
            
            return mix(color2, color1, step(star, t));
        }
        
        // Zoom in
        vec4 zoomIn(vec4 color1, vec4 color2, vec2 uv, float t) {
            float scale = 1.0 - t;
            vec2 uv1 = uv;
            vec2 uv2 = (uv - 0.5) * scale + 0.5;
            
            if (uv2.x < 0.0 || uv2.x > 1.0 || uv2.y < 0.0 || uv2.y > 1.0) {
                return color1;
            }
            
            vec4 c1 = texture2D(frame1, uv1);
            vec4 c2 = texture2D(frame2, uv2);
            
            return mix(c1, c2, step(0.5, t));
        }
        
        // Cube rotate (3D effect)
        vec4 cubeRotate(vec4 color1, vec4 color2, vec2 uv, float t) {
            float angle = t * 3.14159;
            
            // Simulate cube rotation by skewing
            vec2 uv1 = uv + vec2(t, 0.0);
            vec2 uv2 = uv - vec2(1.0 - t, 0.0);
            
            uv1.x = fract(uv1.x);
            uv2.x = fract(uv2.x);
            
            vec4 c1 = texture2D(frame1, uv1);
            vec4 c2 = texture2D(frame2, uv2);
            
            return mix(c1, c2, t);
        }
        
        // Page curl
        vec4 pageCurl(vec4 color1, vec4 color2, vec2 uv, float t) {
            // Simple curl simulation
            float curl = uv.x - t;
            float curve = sin(curl * 3.14159);
            
            vec2 uv1 = uv;
            vec2 uv2 = uv + vec2(curve * 0.2, 0.0);
            
            if (curl < 0.0) {
                return texture2D(frame2, uv2);
            } else {
                return texture2D(frame1, uv1);
            }
        }
        
        // Glitch effect
        vec4 glitch(vec4 color1, vec4 color2, vec2 uv, float t) {
            float glitchAmount = intensity * 0.2;
            
            // Random glitch lines
            float line = floor(uv.y * 100.0) / 100.0;
            float randomShift = fract(sin(line * 100.0) * 43758.5453);
            
            if (randomShift < t * 2.0) {
                uv.x += glitchAmount;
            }
            
            vec4 c1 = texture2D(frame1, uv);
            vec4 c2 = texture2D(frame2, uv);
            
            return mix(c1, c2, t);
        }
        
        // Light leak
        vec4 lightLeak(vec4 color1, vec4 color2, vec2 uv, float t) {
            // Create moving light effect
            vec2 lightPos = vec2(sin(t * 6.283) * 0.3 + 0.5, cos(t * 3.141) * 0.3 + 0.5);
            float dist = distance(uv, lightPos);
            float light = (1.0 - dist) * t;
            
            vec4 c1 = color1 + vec4(light * 0.5, light * 0.3, light * 0.1, 0.0);
            vec4 c2 = color2;
            
            return mix(c1, c2, t);
        }
        
        void main() {
            vec2 uv = texCoordinate;
            uv.y = 1.0 - uv.y; // Flip Y coordinate
            
            vec4 color1 = texture2D(frame1, uv);
            vec4 color2 = texture2D(frame2, uv);
            
            // Apply easing based on transition type
            float t = progress;
            
            vec4 result;
            
            if (transitionType == ${TransitionType.CUT.ordinal}) {
                result = t < 0.5 ? color1 : color2;
            } else if (transitionType == ${TransitionType.FADE_IN_OUT.ordinal}) {
                result = fade(color1, color2, t);
            } else if (transitionType == ${TransitionType.CROSSFADE.ordinal}) {
                result = fade(color1, color2, t);
            } else if (transitionType == ${TransitionType.SLIDE_LEFT.ordinal}) {
                result = slideLeft(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.SLIDE_RIGHT.ordinal}) {
                result = slideRight(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.WIPE_LEFT.ordinal}) {
                result = wipeLeft(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.WIPE_RIGHT.ordinal}) {
                result = wipeRight(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.WIPE_CIRCLE.ordinal}) {
                result = circleWipe(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.WIPE_DIAMOND.ordinal}) {
                result = diamondWipe(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.HEART.ordinal}) {
                result = heartWipe(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.STAR.ordinal}) {
                result = starWipe(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.ZOOM_IN.ordinal}) {
                result = zoomIn(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.CUBE_ROTATE.ordinal}) {
                result = cubeRotate(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.PAGE_CURL.ordinal}) {
                result = pageCurl(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.GLITCH.ordinal}) {
                result = glitch(color1, color2, uv, t);
            } else if (transitionType == ${TransitionType.LIGHT_LEAK.ordinal}) {
                result = lightLeak(color1, color2, uv, t);
            } else {
                result = fade(color1, color2, t);
            }
            
            gl_FragColor = result;
        }
    """

    private var program = 0
    private var transitionType = TransitionType.CROSSFADE
    private var progress = 0f
    private var intensity = 1f
    
    // Uniform handles
    private lateinit var frame1Handle: Int
    private lateinit var frame2Handle: Int
    private lateinit var progressHandle: Int
    private lateinit var transitionTypeHandle: Int
    private lateinit var intensityHandle: Int
    private lateinit var resolutionHandle: Int

    fun setTransition(type: TransitionType) {
        this.transitionType = type
    }

    fun setProgress(value: Float) {
        this.progress = value.coerceIn(0f, 1f)
    }

    fun setIntensity(value: Float) {
        this.intensity = value.coerceIn(0f, 2f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        createProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(program)
        
        // Update uniforms
        GLES20.glUniform1f(progressHandle, progress)
        GLES20.glUniform1i(transitionTypeHandle, transitionType.ordinal)
        GLES20.glUniform1f(intensityHandle, intensity)
        
        // Draw quad
        // This would bind textures and draw the full-screen quad
    }

    private fun createProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // Get uniform locations
        frame1Handle = GLES20.glGetUniformLocation(program, "frame1")
        frame2Handle = GLES20.glGetUniformLocation(program, "frame2")
        progressHandle = GLES20.glGetUniformLocation(program, "progress")
        transitionTypeHandle = GLES20.glGetUniformLocation(program, "transitionType")
        intensityHandle = GLES20.glGetUniformLocation(program, "intensity")
        resolutionHandle = GLES20.glGetUniformLocation(program, "resolution")
        
        // Set texture units
        GLES20.glUniform1i(frame1Handle, 0)
        GLES20.glUniform1i(frame2Handle, 1)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
