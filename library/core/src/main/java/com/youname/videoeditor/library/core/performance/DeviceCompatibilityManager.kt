package com.yourname.videoeditor.library.core.compatibility

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.camera2.CameraManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val product: String = Build.PRODUCT,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val release: String = Build.VERSION.RELEASE
)

data class HardwareCapabilities(
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val ramMB: Long = getTotalRam(),
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val screenDpi: Int = 0,
    val hasNeon: Boolean = checkNeon(),
    val hasOpenGLES30: Boolean = false,
    val hasOpenGLES31: Boolean = false,
    val hasOpenGLES32: Boolean = false,
    val hasVulkan: Boolean = false,
    val gpuVendor: String = "",
    val gpuRenderer: String = ""
)

data class MediaCapabilities(
    val maxVideoWidth: Int = 0,
    val maxVideoHeight: Int = 0,
    val maxVideoBitrate: Int = 0,
    val maxVideoFramerate: Int = 0,
    val supportedVideoCodecs: List<String> = emptyList(),
    val supportedAudioCodecs: List<String> = emptyList(),
    val hasHardwareDecoder: Boolean = false,
    val hasHardwareEncoder: Boolean = false,
    val maxConcurrentDecode: Int = 0
)

class DeviceCompatibilityManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceCompatibility"
    }

    private val deviceInfo = DeviceInfo()
    private lateinit var hardwareCapabilities: HardwareCapabilities
    private lateinit var mediaCapabilities: MediaCapabilities

    interface CompatibilityListener {
        fun onFeatureUnavailable(feature: String, reason: String)
        fun onPerformanceProfile(profile: PerformanceProfile)
        fun onRecommendedSettings(settings: Map<String, Any>)
    }

    enum class PerformanceProfile {
        LOW_END,      // Budget devices, limited capabilities
        MID_RANGE,    // Mainstream devices, good performance
        HIGH_END,     // Flagship devices, all features
        ULTRA         // Latest devices, maximum quality
    }

    private val listeners = mutableListOf<CompatibilityListener>()

    fun analyzeDevice() {
        hardwareCapabilities = analyzeHardware()
        mediaCapabilities = analyzeMediaCapabilities()
        
        val profile = determinePerformanceProfile()
        notifyPerformanceProfile(profile)
        
        val recommendations = generateRecommendations(profile)
        notifyRecommendedSettings(recommendations)
    }

    private fun analyzeHardware(): HardwareCapabilities {
        val displayMetrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getMetrics(displayMetrics)

        val glVersion = checkOpenGLVersion()
        val hasVulkan = checkVulkanSupport()
        val gpuInfo = getGPUInfo()

        return HardwareCapabilities(
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            screenDpi = displayMetrics.densityDpi,
            hasOpenGLES30 = glVersion >= 3.0,
            hasOpenGLES31 = glVersion >= 3.1,
            hasOpenGLES32 = glVersion >= 3.2,
            hasVulkan = hasVulkan,
            gpuVendor = gpuInfo.first,
            gpuRenderer = gpuInfo.second
        )
    }

    private fun analyzeMediaCapabilities(): MediaCapabilities {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val videoCodecs = mutableListOf<String>()
        val audioCodecs = mutableListOf<String>()

        codecList.codecInfos.forEach { codecInfo ->
            val isEncoder = codecInfo.isEncoder
            codecInfo.supportedTypes.forEach { mimeType ->
                if (mimeType.startsWith("video/")) {
                    if (!videoCodecs.contains(mimeType)) {
                        videoCodecs.add(mimeType)
                    }
                } else if (mimeType.startsWith("audio/")) {
                    if (!audioCodecs.contains(mimeType)) {
                        audioCodecs.add(mimeType)
                    }
                }
            }
        }

        // Check hardware acceleration
        val hasHardwareCodec = codecList.codecInfos.any { 
            !it.isEncoder && it.name.contains("OMX.google", ignoreCase = true) 
        }

        return MediaCapabilities(
            maxVideoWidth = 3840, // 4K max
            maxVideoHeight = 2160,
            maxVideoBitrate = 50000000, // 50 Mbps
            maxVideoFramerate = 60,
            supportedVideoCodecs = videoCodecs,
            supportedAudioCodecs = audioCodecs,
            hasHardwareDecoder = !hasHardwareCodec, // If not Google software decoder
            hasHardwareEncoder = true,
            maxConcurrentDecode = 2
        )
    }

    private fun checkOpenGLVersion(): Float {
        return try {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

            val eglContext = EGL14.eglCreateContext(
                eglDisplay,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )

            val eglSurface = EGL14.eglCreatePbufferSurface(
                eglDisplay,
                configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                0
            )

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            
            val versionString = GLES20.glGetString(GLES20.GL_VERSION)
            val versionNum = versionString?.let { parseGLVersion(it) } ?: 2.0f

            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)

            versionNum
        } catch (e: Exception) {
            Log.e(TAG, "Error checking OpenGL version", e)
            2.0f
        }
    }

    private fun parseGLVersion(versionString: String): Float {
        val regex = "OpenGL ES (\\d+)\\.(\\d+)".toRegex()
        val matchResult = regex.find(versionString)
        return matchResult?.let {
            val major = it.groupValues[1].toInt()
            val minor = it.groupValues[2].toInt()
            major + minor / 10f
        } ?: 2.0f
    }

    private fun checkVulkanSupport(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        } else {
            false
        }
    }

    private fun getGPUInfo(): Pair<String, String> {
        return try {
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            Pair(vendor, renderer)
        } catch (e: Exception) {
            Pair("Unknown", "Unknown")
        }
    }

    private fun determinePerformanceProfile(): PerformanceProfile {
        return when {
            // Ultra: Latest flagships with 8+ cores, 8GB+ RAM, Vulkan
            hardwareCapabilities.cpuCores >= 8 && 
            hardwareCapabilities.ramMB >= 8192 &&
            hardwareCapabilities.hasVulkan &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> 
                PerformanceProfile.ULTRA

            // High-end: 6+ cores, 4GB+ RAM, OpenGL ES 3.1+
            hardwareCapabilities.cpuCores >= 6 &&
            hardwareCapabilities.ramMB >= 4096 &&
            hardwareCapabilities.hasOpenGLES31 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> 
                PerformanceProfile.HIGH_END

            // Mid-range: 4 cores, 2GB+ RAM, OpenGL ES 3.0
            hardwareCapabilities.cpuCores >= 4 &&
            hardwareCapabilities.ramMB >= 2048 &&
            hardwareCapabilities.hasOpenGLES30 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> 
                PerformanceProfile.MID_RANGE

            // Low-end: Everything else
            else -> PerformanceProfile.LOW_END
        }
    }

    private fun generateRecommendations(profile: PerformanceProfile): Map<String, Any> {
        return when (profile) {
            PerformanceProfile.ULTRA -> mapOf(
                "renderQuality" to "ULTRA",
                "textureQuality" to 1.0f,
                "maxVideoBitrate" to 50000000,
                "maxVideoResolution" to "3840x2160",
                "maxFPS" to 60,
                "enableHDR" to true,
                "enableHardwareEncoding" to true,
                "enableMultipleTracks" to 4,
                "enableRealTimeEffects" to true
            )
            PerformanceProfile.HIGH_END -> mapOf(
                "renderQuality" to "HIGH",
                "textureQuality" to 0.9f,
                "maxVideoBitrate" to 20000000,
                "maxVideoResolution" to "1920x1080",
                "maxFPS" to 30,
                "enableHDR" to false,
                "enableHardwareEncoding" to true,
                "enableMultipleTracks" to 2,
                "enableRealTimeEffects" to true
            )
            PerformanceProfile.MID_RANGE -> mapOf(
                "renderQuality" to "MEDIUM",
                "textureQuality" to 0.75f,
                "maxVideoBitrate" to 10000000,
                "maxVideoResolution" to "1280x720",
                "maxFPS" to 30,
                "enableHDR" to false,
                "enableHardwareEncoding" to true,
                "enableMultipleTracks" to 1,
                "enableRealTimeEffects" to false
            )
            PerformanceProfile.LOW_END -> mapOf(
                "renderQuality" to "LOW",
                "textureQuality" to 0.5f,
                "maxVideoBitrate" to 5000000,
                "maxVideoResolution" to "854x480",
                "maxFPS" to 24,
                "enableHDR" to false,
                "enableHardwareEncoding" to true,
                "enableMultipleTracks" to 0,
                "enableRealTimeEffects" to false,
                "useSoftwareDecoder" to true
            )
        }
    }

    fun isFeatureSupported(feature: String): Boolean {
        return when (feature) {
            "4K_EXPORT" -> hardwareCapabilities.screenWidth >= 3840 && hardwareCapabilities.hasOpenGLES31
            "HDR" -> hardwareCapabilities.hasOpenGLES32 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            "MULTITRACK" -> hardwareCapabilities.cpuCores >= 4 && hardwareCapabilities.ramMB >= 4096
            "REAL_TIME_EFFECTS" -> hardwareCapabilities.hasOpenGLES31 && hardwareCapabilities.ramMB >= 4096
            "GPU_ACCELERATION" -> hardwareCapabilities.hasOpenGLES30
            "HARDWARE_ENCODE" -> mediaCapabilities.hasHardwareEncoder
            else -> false
        }
    }

    fun getRecommendedExportSettings(): ExportSettings {
        val profile = determinePerformanceProfile()
        return when (profile) {
            PerformanceProfile.ULTRA -> ExportSettings(
                resolutions = listOf("2160p", "1080p", "720p", "480p"),
                bitrates = listOf(50000, 20000, 10000, 5000),
                codec = "h265",
                format = "mp4"
            )
            PerformanceProfile.HIGH_END -> ExportSettings(
                resolutions = listOf("1080p", "720p", "480p"),
                bitrates = listOf(20000, 10000, 5000),
                codec = "h264",
                format = "mp4"
            )
            PerformanceProfile.MID_RANGE -> ExportSettings(
                resolutions = listOf("720p", "480p"),
                bitrates = listOf(10000, 5000),
                codec = "h264",
                format = "mp4"
            )
            PerformanceProfile.LOW_END -> ExportSettings(
                resolutions = listOf("480p"),
                bitrates = listOf(5000),
                codec = "h264",
                format = "mp4"
            )
        }
    }

    fun addListener(listener: CompatibilityListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CompatibilityListener) {
        listeners.remove(listener)
    }

    private fun notifyPerformanceProfile(profile: PerformanceProfile) {
        listeners.forEach { it.onPerformanceProfile(profile) }
    }

    private fun notifyRecommendedSettings(settings: Map<String, Any>) {
        listeners.forEach { it.onRecommendedSettings(settings) }
    }
}

data class ExportSettings(
    val resolutions: List<String>,
    val bitrates: List<Int>,
    val codec: String,
    val format: String
)

private fun getTotalRam(): Long {
    return try {
        val reader = java.io.BufferedReader(java.io.FileReader("/proc/meminfo"))
        val line = reader.readLine()
        reader.close()
        val parts = line.split("\\s+".toRegex())
        parts[1].toLong() / 1024
    } catch (e: Exception) {
        0
    }
}

private fun checkNeon(): Boolean {
    return try {
        val abi = Build.CPU_ABI
        abi.contains("armeabi-v7a") || abi.contains("arm64-v8a")
    } catch (e: Exception) {
        false
    }
}
