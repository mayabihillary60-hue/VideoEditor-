package com.yourname.videoeditor.library.rendering.color

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class ColorGradingParams(
    var exposure: Float = 0.0f,        // -1.0 to 1.0
    var contrast: Float = 1.0f,         // 0.5 to 2.0
    var highlights: Float = 0.0f,       // -1.0 to 1.0
    var shadows: Float = 0.0f,          // -1.0 to 1.0
    var whites: Float = 0.0f,           // -1.0 to 1.0
    var blacks: Float = 0.0f,           // -1.0 to 1.0
    var temperature: Float = 0.0f,      // -1.0 to 1.0 (blue to yellow)
    var tint: Float = 0.0f,             // -1.0 to 1.0 (green to magenta)
    var vibrance: Float = 1.0f,          // 0.0 to 2.0
    var saturation: Float = 1.0f,        // 0.0 to 2.0
    var hueShift: Float = 0.0f,          // -180 to 180 (degrees)
    var gamma: Float = 1.0f,             // 0.1 to 3.0
    var lift: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f), // RGB lift
    var gain: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f),  // RGB gain
    var gammaCorrection: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f) // RGB gamma
)

class ColorGradingRenderer : GLSurfaceView.Renderer {

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
        
        uniform float exposure;
        uniform float contrast;
        uniform float highlights;
        uniform float shadows;
        uniform float whites;
        uniform float blacks;
        uniform float temperature;
        uniform float tint;
        uniform float vibrance;
        uniform float saturation;
        uniform float hueShift;
        uniform float gamma;
        uniform vec3 lift;
        uniform vec3 gain;
        uniform vec3 gammaCorrection;
        
        // Color space conversion utilities
        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }
        
        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }
        
        // Exposure adjustment
        vec3 applyExposure(vec3 color, float ev) {
            return color * pow(2.0, ev);
        }
        
        // Contrast with luminance preservation
        vec3 applyContrast(vec3 color, float amount) {
            float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
            return mix(vec3(luminance), color, amount);
        }
        
        // Highlights and shadows
        vec3 applyHighlightsShadows(vec3 color, float hl, float sh) {
            float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
            
            // Highlights adjustment (affects bright areas)
            float hlFactor = smoothstep(0.5, 1.0, luminance);
            color += color * hl * hlFactor;
            
            // Shadows adjustment (affects dark areas)
            float shFactor = 1.0 - smoothstep(0.0, 0.5, luminance);
            color += color * sh * shFactor;
            
            return color;
        }
        
        // Whites and blacks (clipping points)
        vec3 applyWhitesBlacks(vec3 color, float whites, float blacks) {
            // Whites - push bright areas toward white
            float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
            float whiteFactor = smoothstep(0.8, 1.0, luminance);
            color = mix(color, vec3(1.0), whiteFactor * whites);
            
            // Blacks - push dark areas toward black
            float blackFactor = 1.0 - smoothstep(0.0, 0.2, luminance);
            color = mix(color, vec3(0.0), blackFactor * blacks);
            
            return color;
        }
        
        // Color temperature (blue to yellow)
        vec3 applyTemperature(vec3 color, float temp) {
            // RGB to LMS conversion (approximation)
            float l = 0.3897 * color.r + 0.6890 * color.g - 0.0787 * color.b;
            float m = -0.2298 * color.r + 1.1834 * color.g + 0.0464 * color.b;
            float s = color.b;
            
            // Apply temperature adjustment
            l *= (1.0 + temp * 0.1);
            s *= (1.0 - temp * 0.1);
            
            // Convert back to RGB
            vec3 result;
            result.r = 1.9102 * l - 1.1121 * m + 0.2019 * s;
            result.g = -0.5846 * l + 1.3339 * m - 0.1308 * s;
            result.b = 0.0 * l + 0.0 * m + 1.0 * s;
            
            return result;
        }
        
        // Tint (green to magenta)
        vec3 applyTint(vec3 color, float tint) {
            vec3 hsv = rgb2hsv(color);
            hsv.x += tint * 0.1; // Shift hue
            return hsv2rgb(hsv);
        }
        
        // Vibrance (protects skin tones)
        vec3 applyVibrance(vec3 color, float amount) {
            float maxChannel = max(max(color.r, color.g), color.b);
            float minChannel = min(min(color.r, color.g), color.b);
            float saturation = maxChannel - minChannel;
            
            vec3 hsv = rgb2hsv(color);
            hsv.y *= (1.0 + (amount - 1.0) * (1.0 - saturation));
            return hsv2rgb(hsv);
        }
        
        // Hue shift
        vec3 applyHueShift(vec3 color, float shift) {
            vec3 hsv = rgb2hsv(color);
            hsv.x += shift / 360.0;
            return hsv2rgb(hsv);
        }
        
        // Lift, Gamma, Gain (Log color correction)
        vec3 applyLiftGammaGain(vec3 color, vec3 lift, vec3 gamma, vec3 gain) {
            // Lift - raises shadows
            color = color + lift;
            
            // Gamma - midtones adjustment
            color = pow(color, 1.0 / (gamma + 0.001));
            
            // Gain - highlights adjustment
            color = color * gain;
            
            return color;
        }
        
        // RGB curves (simplified)
        vec3 applyRGBCurves(vec3 color) {
            // This would use lookup tables for precise color curves
            // For now, we'll use a simplified spline interpolation
            return color;
        }
        
        void main() {
            vec4 color = texture2D(inputTexture, texCoordinate);
            vec3 result = color.rgb;
            
            // Apply in proper order
            result = applyExposure(result, exposure);
            result = applyTemperature(result, temperature);
            result = applyTint(result, tint);
            result = applyHighlightsShadows(result, highlights, shadows);
            result = applyWhitesBlacks(result, whites, blacks);
            result = applyContrast(result, contrast);
            
            // Vibrance before saturation
            result = applyVibrance(result, vibrance);
            
            // HSV adjustments
            result = applyHueShift(result, hueShift);
            
            // Saturation
            vec3 hsv = rgb2hsv(result);
            hsv.y *= saturation;
            result = hsv2rgb(hsv);
            
            // Lift, Gamma, Gain
            result = applyLiftGammaGain(result, lift, gammaCorrection, gain);
            
            // Gamma correction
            result = pow(result, vec3(1.0 / gamma));
            
            gl_FragColor = vec4(result, color.a);
        }
    """

    private var program = 0
    private var params = ColorGradingParams()
    
    // Uniform handles
    private lateinit var exposureHandle: Int
    private lateinit var contrastHandle: Int
    private lateinit var highlightsHandle: Int
    private lateinit var shadowsHandle: Int
    private lateinit var whitesHandle: Int
    private lateinit var blacksHandle: Int
    private lateinit var temperatureHandle: Int
    private lateinit var tintHandle: Int
    private lateinit var vibranceHandle: Int
    private lateinit var saturationHandle: Int
    private lateinit var hueShiftHandle: Int
    private lateinit var gammaHandle: Int
    private lateinit var liftHandle: Int
    private lateinit var gainHandle: Int
    private lateinit var gammaCorrectionHandle: Int

    fun updateParams(newParams: ColorGradingParams) {
        this.params = newParams
    }

    fun setExposure(value: Float) {
        params.exposure = value.coerceIn(-2.0f, 2.0f)
    }

    fun setContrast(value: Float) {
        params.contrast = value.coerceIn(0.0f, 3.0f)
    }

    fun setHighlights(value: Float) {
        params.highlights = value.coerceIn(-1.0f, 1.0f)
    }

    fun setShadows(value: Float) {
        params.shadows = value.coerceIn(-1.0f, 1.0f)
    }

    fun setWhites(value: Float) {
        params.whites = value.coerceIn(-1.0f, 1.0f)
    }

    fun setBlacks(value: Float) {
        params.blacks = value.coerceIn(-1.0f, 1.0f)
    }

    fun setTemperature(value: Float) {
        params.temperature = value.coerceIn(-1.0f, 1.0f)
    }

    fun setTint(value: Float) {
        params.tint = value.coerceIn(-1.0f, 1.0f)
    }

    fun setVibrance(value: Float) {
        params.vibrance = value.coerceIn(0.0f, 2.0f)
    }

    fun setSaturation(value: Float) {
        params.saturation = value.coerceIn(0.0f, 2.0f)
    }

    fun setHueShift(value: Float) {
        params.hueShift = value.coerceIn(-180.0f, 180.0f)
    }

    fun setGamma(value: Float) {
        params.gamma = value.coerceIn(0.1f, 3.0f)
    }

    fun setLift(r: Float, g: Float, b: Float) {
        params.lift = floatArrayOf(r, g, b)
    }

    fun setGain(r: Float, g: Float, b: Float) {
        params.gain = floatArrayOf(r, g, b)
    }

    fun setGammaCorrection(r: Float, g: Float, b: Float) {
        params.gammaCorrection = floatArrayOf(r, g, b)
    }

    fun resetToDefault() {
        params = ColorGradingParams()
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
        
        // Apply color grading parameters
        GLES20.glUseProgram(program)
        
        // Set uniform values
        GLES20.glUniform1f(exposureHandle, params.exposure)
        GLES20.glUniform1f(contrastHandle, params.contrast)
        GLES20.glUniform1f(highlightsHandle, params.highlights)
        GLES20.glUniform1f(shadowsHandle, params.shadows)
        GLES20.glUniform1f(whitesHandle, params.whites)
        GLES20.glUniform1f(blacksHandle, params.blacks)
        GLES20.glUniform1f(temperatureHandle, params.temperature)
        GLES20.glUniform1f(tintHandle, params.tint)
        GLES20.glUniform1f(vibranceHandle, params.vibrance)
        GLES20.glUniform1f(saturationHandle, params.saturation)
        GLES20.glUniform1f(hueShiftHandle, params.hueShift)
        GLES20.glUniform1f(gammaHandle, params.gamma)
        GLES20.glUniform3f(liftHandle, params.lift[0], params.lift[1], params.lift[2])
        GLES20.glUniform3f(gainHandle, params.gain[0], params.gain[1], params.gain[2])
        GLES20.glUniform3f(gammaCorrectionHandle, params.gammaCorrection[0], params.gammaCorrection[1], params.gammaCorrection[2])
    }

    private fun createProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // Get uniform locations
        exposureHandle = GLES20.glGetUniformLocation(program, "exposure")
        contrastHandle = GLES20.glGetUniformLocation(program, "contrast")
        highlightsHandle = GLES20.glGetUniformLocation(program, "highlights")
        shadowsHandle = GLES20.glGetUniformLocation(program, "shadows")
        whitesHandle = GLES20.glGetUniformLocation(program, "whites")
        blacksHandle = GLES20.glGetUniformLocation(program, "blacks")
        temperatureHandle = GLES20.glGetUniformLocation(program, "temperature")
        tintHandle = GLES20.glGetUniformLocation(program, "tint")
        vibranceHandle = GLES20.glGetUniformLocation(program, "vibrance")
        saturationHandle = GLES20.glGetUniformLocation(program, "saturation")
        hueShiftHandle = GLES20.glGetUniformLocation(program, "hueShift")
        gammaHandle = GLES20.glGetUniformLocation(program, "gamma")
        liftHandle = GLES20.glGetUniformLocation(program, "lift")
        gainHandle = GLES20.glGetUniformLocation(program, "gain")
        gammaCorrectionHandle = GLES20.glGetUniformLocation(program, "gammaCorrection")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}