package com.yourname.videoeditor.library.rendering.filter

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

enum class FilterType {
    NONE,
    // Color Filters
    GRAYSCALE,
    SEPIA,
    VINTAGE,
    COOL,
    WARM,
    // Artistic Filters
    OIL_PAINTING,
    CARTOON,
    SKETCH,
    // Lighting Effects
    VIGNETTE,
    BLOOM,
    // Distortion
    BULGE,
    PINCH,
    // Advanced
    GAUSSIAN_BLUR,
    SHARPEN,
    EDGE_DETECT,
    EMBOSS,
    PIXELATE,
    // Instagram-like
    VALENCIA,
    AMARO,
    RISE,
    HUDSON,
    XPRO2,
    SIERRA,
    LARK,
    // Custom
    CUSTOM
}

data class FilterParameter(
    val name: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float
)

class AdvancedFilterRenderer(private val context: Context) : GLSurfaceView.Renderer {

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

    private var currentFilter = FilterType.NONE
    private var filterIntensity = 1.0f
    private var filterParams = mutableMapOf<String, Float>()
    
    // Filter parameters
    private var brightness = 0.0f
    private var contrast = 1.0f
    private var saturation = 1.0f
    private var hue = 0.0f
    private var sharpness = 0.0f
    private var blurRadius = 0.0f
    private var vignetteIntensity = 0.0f
    
    private var program = 0
    private var vPositionHandle = 0
    private var vTexCoordinateHandle = 0
    private var uMVPMatrixHandle = 0
    private var inputTextureHandle = 0
    private var externalTextureHandle = 0
    
    // Advanced filter shaders
    private val fragmentShaderAdvanced = """
        precision highp float;
        varying vec2 texCoordinate;
        uniform sampler2D inputTexture;
        uniform samplerExternalOES externalTexture;
        
        uniform int filterType;
        uniform float intensity;
        uniform float brightness;
        uniform float contrast;
        uniform float saturation;
        uniform float hue;
        uniform float sharpness;
        uniform float blurRadius;
        uniform float vignetteIntensity;
        uniform vec2 resolution;
        uniform float time;
        
        const int FILTER_NONE = 0;
        const int FILTER_GRAYSCALE = 1;
        const int FILTER_SEPIA = 2;
        const int FILTER_VINTAGE = 3;
        const int FILTER_COOL = 4;
        const int FILTER_WARM = 5;
        const int FILTER_OIL_PAINTING = 6;
        const int FILTER_CARTOON = 7;
        const int FILTER_SKETCH = 8;
        const int FILTER_VIGNETTE = 9;
        const int FILTER_BLOOM = 10;
        const int FILTER_BULGE = 11;
        const int FILTER_PINCH = 12;
        const int FILTER_GAUSSIAN_BLUR = 13;
        const int FILTER_SHARPEN = 14;
        const int FILTER_EDGE_DETECT = 15;
        const int FILTER_EMBOSS = 16;
        const int FILTER_PIXELATE = 17;
        const int FILTER_VALENCIA = 18;
        const int FILTER_AMARO = 19;
        const int FILTER_RISE = 20;
        const int FILTER_HUDSON = 21;
        const int FILTER_XPRO2 = 22;
        const int FILTER_SIERRA = 23;
        const int FILTER_LARK = 24;
        
        // Color conversion utilities
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
        
        // Advanced filters
        
        // 1. Oil Painting Effect (Artistic)
        vec4 oilPainting(vec2 uv) {
            float radius = intensity * 5.0;
            float quantize = intensity * 8.0;
            
            vec3 color = vec3(0.0);
            float total = 0.0;
            
            for (float i = -radius; i <= radius; i++) {
                for (float j = -radius; j <= radius; j++) {
                    vec2 offset = vec2(i, j) / resolution;
                    vec4 sample = texture2D(inputTexture, uv + offset);
                    
                    // Quantize the color for oil painting effect
                    vec3 quantized = floor(sample.rgb * quantize) / quantize;
                    color += quantized * sample.a;
                    total += sample.a;
                }
            }
            
            return vec4(color / total, 1.0);
        }
        
        // 2. Cartoon Effect (Edge detection + color quantization)
        vec4 cartoon(vec2 uv) {
            vec4 color = texture2D(inputTexture, uv);
            
            // Sobel edge detection
            float dx = 0.0, dy = 0.0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    vec2 offset = vec2(float(i), float(j)) / resolution;
                    vec4 sample = texture2D(inputTexture, uv + offset);
                    float gray = dot(sample.rgb, vec3(0.299, 0.587, 0.114));
                    
                    if (i == 1 && j == 1) continue;
                    if (i == 0 && j == 0) continue;
                    
                    dx += float(i) * gray;
                    dy += float(j) * gray;
                }
            }
            
            float edge = length(vec2(dx, dy));
            float edgeFactor = smoothstep(0.1, 0.3, edge);
            
            // Color quantization
            vec3 quantized = floor(color.rgb * 8.0) / 8.0;
            
            // Combine quantized colors with edge lines
            vec3 final = mix(quantized, vec3(0.0), edgeFactor);
            
            return vec4(final, color.a);
        }
        
        // 3. Sketch Effect
        vec4 sketch(vec2 uv) {
            vec4 color = texture2D(inputTexture, uv);
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            
            // Generate random noise
            float noise = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
            
            // Create sketch lines
            float lines = 0.0;
            for (float i = -1.0; i <= 1.0; i++) {
                for (float j = -1.0; j <= 1.0; j++) {
                    vec2 offset = vec2(i, j) / resolution * 2.0;
                    vec4 neighbor = texture2D(inputTexture, uv + offset);
                    float neighborGray = dot(neighbor.rgb, vec3(0.299, 0.587, 0.114));
                    lines += abs(gray - neighborGray);
                }
            }
            
            lines = lines / 9.0;
            
            // Combine with noise for pencil texture
            float sketch = lines * (0.8 + 0.2 * noise);
            
            return vec4(vec3(sketch), 1.0);
        }
        
        // 4. Bloom Effect
        vec4 bloom(vec2 uv) {
            vec4 color = texture2D(inputTexture, uv);
            
            // Extract bright areas
            float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            vec3 bright = max(vec3(0.0), color.rgb - 0.5) * 2.0;
            
            // Blur the bright areas (simple Gaussian approximation)
            vec3 blur = vec3(0.0);
            float kernel[5] = float[](0.06136, 0.24477, 0.38774, 0.24477, 0.06136);
            
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    vec2 offset = vec2(float(i), float(j)) / resolution * 2.0;
                    vec4 sample = texture2D(inputTexture, uv + offset);
                    float weight = kernel[i+2] * kernel[j+2];
                    blur += sample.rgb * weight;
                }
            }
            
            // Combine original with blurred bright areas
            vec3 result = color.rgb + blur * bright * intensity;
            
            return vec4(result, color.a);
        }
        
        // 5. Bulge Effect (Distortion)
        vec4 bulge(vec2 uv) {
            vec2 center = vec2(0.5, 0.5);
            float radius = 0.3;
            float scale = intensity * 0.5;
            
            vec2 dir = uv - center;
            float dist = length(dir);
            
            if (dist < radius) {
                float bulge = (dist - radius) * scale;
                vec2 offset = normalize(dir) * bulge;
                uv = uv - offset;
            }
            
            return texture2D(inputTexture, uv);
        }
        
        // 6. Instagram Valencia Filter
        vec4 valencia(vec2 uv) {
            vec4 color = texture2D(inputTexture, uv);
            
            // Warm highlights, cool shadows with purple tint
            float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            
            // Shadows: add blue/purple
            vec3 shadows = mix(vec3(0.2, 0.1, 0.3), vec3(0.0), luma);
            
            // Highlights: add yellow/orange
            vec3 highlights = mix(vec3(0.0), vec3(0.3, 0.2, 0.0), luma);
            
            // Increase saturation slightly
            vec3 hsv = rgb2hsv(color.rgb);
            hsv.y *= 1.1;
            vec3 adjusted = hsv2rgb(hsv);
            
            vec3 result = adjusted + shadows + highlights;
            
            return vec4(result, color.a);
        }
        
        // 7. Gaussian Blur
        vec4 gaussianBlur(vec2 uv) {
            float sigma = blurRadius * 5.0;
            float twoSigma2 = 2.0 * sigma * sigma;
            float radius = sigma * 2.0;
            
            vec4 color = vec4(0.0);
            float total = 0.0;
            
            for (float i = -radius; i <= radius; i++) {
                for (float j = -radius; j <= radius; j++) {
                    vec2 offset = vec2(i, j) / resolution;
                    float dist = (i*i + j*j) / (radius*radius);
                    float weight = exp(-dist * dist / twoSigma2);
                    
                    color += texture2D(inputTexture, uv + offset) * weight;
                    total += weight;
                }
            }
            
            return color / total;
        }
        
        // 8. Edge Detection
        vec4 edgeDetect(vec2 uv) {
            // Sobel operator
            float gx = 0.0, gy = 0.0;
            
            mat3 sobelX = mat3(-1.0, 0.0, 1.0, -2.0, 0.0, 2.0, -1.0, 0.0, 1.0);
            mat3 sobelY = mat3(-1.0, -2.0, -1.0, 0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
            
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    vec2 offset = vec2(float(i), float(j)) / resolution;
                    vec4 sample = texture2D(inputTexture, uv + offset);
                    float gray = dot(sample.rgb, vec3(0.299, 0.587, 0.114));
                    
                    gx += gray * sobelX[i+1][j+1];
                    gy += gray * sobelY[i+1][j+1];
                }
            }
            
            float edge = sqrt(gx*gx + gy*gy);
            
            return vec4(vec3(edge), 1.0);
        }
        
        // 9. Emboss Effect
        vec4 emboss(vec2 uv) {
            float offset = 1.0 / resolution.x;
            
            vec4 color = texture2D(inputTexture, uv);
            
            float c = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            float tl = dot(texture2D(inputTexture, uv + vec2(-offset, -offset)).rgb, vec3(0.299, 0.587, 0.114));
            float br = dot(texture2D(inputTexture, uv + vec2(offset, offset)).rgb, vec3(0.299, 0.587, 0.114));
            
            float diff = (c - tl) + (c - br);
            float embossed = diff * intensity + 0.5;
            
            return vec4(vec3(embossed), color.a);
        }
        
        // 10. Pixelate Effect
        vec4 pixelate(vec2 uv) {
            float pixelSize = max(2.0, intensity * 20.0);
            vec2 pixelCoord = floor(uv * resolution / pixelSize);
            vec2 pixelUV = (pixelCoord * pixelSize + pixelSize * 0.5) / resolution;
            
            return texture2D(inputTexture, pixelUV);
        }
        
        void main() {
            vec2 uv = texCoordinate;
            vec4 result;
            
            if (filterType == FILTER_NONE) {
                result = texture2D(inputTexture, uv);
            } else if (filterType == FILTER_GRAYSCALE) {
                vec4 color = texture2D(inputTexture, uv);
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                result = vec4(gray, gray, gray, color.a);
            } else if (filterType == FILTER_SEPIA) {
                vec4 color = texture2D(inputTexture, uv);
                float r = color.r;
                float g = color.g;
                float b = color.b;
                
                float outR = (r * 0.393) + (g * 0.769) + (b * 0.189);
                float outG = (r * 0.349) + (g * 0.686) + (b * 0.168);
                float outB = (r * 0.272) + (g * 0.534) + (b * 0.131);
                
                result = vec4(outR, outG, outB, color.a);
            } else if (filterType == FILTER_VINTAGE) {
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x += 0.1; // Shift hue
                hsv.y *= 1.2; // Increase saturation
                result = vec4(hsv2rgb(hsv), color.a);
            } else if (filterType == FILTER_COOL) {
                vec4 color = texture2D(inputTexture, uv);
                result = vec4(color.r * 0.9, color.g * 1.1, color.b * 1.3, color.a);
            } else if (filterType == FILTER_WARM) {
                vec4 color = texture2D(inputTexture, uv);
                result = vec4(color.r * 1.3, color.g * 1.1, color.b * 0.9, color.a);
            } else if (filterType == FILTER_OIL_PAINTING) {
                result = oilPainting(uv);
            } else if (filterType == FILTER_CARTOON) {
                result = cartoon(uv);
            } else if (filterType == FILTER_SKETCH) {
                result = sketch(uv);
            } else if (filterType == FILTER_VIGNETTE) {
                vec4 color = texture2D(inputTexture, uv);
                float dist = distance(uv, vec2(0.5, 0.5));
                float vignette = 1.0 - (dist * vignetteIntensity);
                result = vec4(color.rgb * vignette, color.a);
            } else if (filterType == FILTER_BLOOM) {
                result = bloom(uv);
            } else if (filterType == FILTER_BULGE) {
                result = bulge(uv);
            } else if (filterType == FILTER_PINCH) {
                vec2 center = vec2(0.5, 0.5);
                float radius = 0.3;
                float scale = intensity * 0.3;
                
                vec2 dir = uv - center;
                float dist = length(dir);
                
                if (dist < radius) {
                    float pinch = (radius - dist) * scale;
                    vec2 offset = normalize(dir) * pinch;
                    uv = uv + offset;
                }
                
                result = texture2D(inputTexture, uv);
            } else if (filterType == FILTER_GAUSSIAN_BLUR) {
                result = gaussianBlur(uv);
            } else if (filterType == FILTER_SHARPEN) {
                vec4 color = texture2D(inputTexture, uv);
                float offset = 1.0 / resolution.x;
                
                vec4 laplacian = -texture2D(inputTexture, uv + vec2(-offset, 0.0))
                                 -texture2D(inputTexture, uv + vec2(offset, 0.0))
                                 -texture2D(inputTexture, uv + vec2(0.0, -offset))
                                 -texture2D(inputTexture, uv + vec2(0.0, offset))
                                 + texture2D(inputTexture, uv) * 4.0;
                
                result = color + laplacian * sharpness;
            } else if (filterType == FILTER_EDGE_DETECT) {
                result = edgeDetect(uv);
            } else if (filterType == FILTER_EMBOSS) {
                result = emboss(uv);
            } else if (filterType == FILTER_PIXELATE) {
                result = pixelate(uv);
            } else if (filterType == FILTER_VALENCIA) {
                result = valencia(uv);
            } else if (filterType == FILTER_AMARO) {
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x += 0.05;
                hsv.y *= 1.3;
                hsv.z *= 1.1;
                result = vec4(hsv2rgb(hsv), color.a);
            } else if (filterType == FILTER_RISE) {
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x += 0.02;
                hsv.y *= 1.2;
                hsv.z *= 1.2;
                result = vec4(hsv2rgb(hsv), color.a);
            } else if (filterType == FILTER_HUDSON) {
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x -= 0.05;
                hsv.y *= 1.4;
                hsv.z *= 1.1;
                result = vec4(hsv2rgb(hsv), color.a);
            } else if (filterType == FILTER_XPRO2) {
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x += 0.1;
                hsv.y *= 1.5;
                hsv.z *= 0.9;
                result = vec4(hsv2rgb(hsv), color.a);
            } else {
                // Apply basic color adjustments
                vec4 color = texture2D(inputTexture, uv);
                vec3 hsv = rgb2hsv(color.rgb);
                hsv.x += hue;
                hsv.y *= saturation;
                hsv.z = (hsv.z - 0.5) * contrast + 0.5 + brightness;
                result = vec4(hsv2rgb(hsv), color.a);
            }
            
            gl_FragColor = result;
        }
    """

    fun setFilter(filter: FilterType) {
        currentFilter = filter
    }

    fun setIntensity(intensity: Float) {
        this.filterIntensity = intensity.coerceIn(0f, 1f)
    }

    fun setBrightness(value: Float) {
        this.brightness = value.coerceIn(-0.5f, 0.5f)
    }

    fun setContrast(value: Float) {
        this.contrast = value.coerceIn(0.5f, 2.0f)
    }

    fun setSaturation(value: Float) {
        this.saturation = value.coerceIn(0f, 2.0f)
    }

    fun setHue(value: Float) {
        this.hue = value.coerceIn(-0.5f, 0.5f)
    }

    fun setSharpness(value: Float) {
        this.sharpness = value.coerceIn(0f, 1f)
    }

    fun setBlurRadius(value: Float) {
        this.blurRadius = value.coerceIn(0f, 0.1f)
    }

    fun setVignette(value: Float) {
        this.vignetteIntensity = value.coerceIn(0f, 2f)
    }

    fun getFilterParameters(filter: FilterType): List<FilterParameter> {
        return when (filter) {
            FilterType.GAUSSIAN_BLUR -> listOf(
                FilterParameter("Blur Radius", 0f, 0.1f, 0.02f)
            )
            FilterType.SHARPEN -> listOf(
                FilterParameter("Sharpness", 0f, 1f, 0.3f)
            )
            FilterType.VIGNETTE -> listOf(
                FilterParameter("Intensity", 0f, 2f, 1f)
 