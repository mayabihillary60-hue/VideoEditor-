package com.yourname.videoeditor.library.rendering.export

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Size
import com.yourname.videoeditor.library.core.compatibility.DeviceCompatibilityManager
import com.yourname.videoeditor.library.core.compatibility.ExportSettings

enum class ExportPreset {
    SOCIAL_MEDIA,    // Optimized for Instagram, TikTok, etc.
    YOUTUBE,         // High quality for YouTube
    ARCHIVE,         // Maximum quality for storage
    CUSTOM
}

enum class VideoCodec {
    H264,            // Most compatible
    H265,            // Better compression (HEVC)
    VP9,             // Google's codec
    AV1              // Latest codec
}

enum class AudioCodec {
    AAC,             // Most compatible
    MP3,             // Legacy
    OPUS,            // Better quality at lower bitrate
    FLAC             // Lossless
}

enum class ContainerFormat {
    MP4,             // Most compatible
    MKV,             // Matroska
    WEBM,            // WebM
    MOV              // QuickTime
}

data class ExportQualityConfig(
    var preset: ExportPreset = ExportPreset.SOCIAL_MEDIA,
    var videoCodec: VideoCodec = VideoCodec.H264,
    var audioCodec: AudioCodec = AudioCodec.AAC,
    var format: ContainerFormat = ContainerFormat.MP4,
    var resolution: Size = Size(1920, 1080),
    var videoBitrate: Int = 20_000_000, // 20 Mbps
    var audioBitrate: Int = 192_000,    // 192 kbps
    var frameRate: Int = 30,
    var keyFrameInterval: Int = 2,       // seconds
    var profile: String = "HIGH",
    var level: String = "4.1",
    var enableHardwareEncoding: Boolean = true,
    var enableTwoPass: Boolean = false,
    var enableParallelEncoding: Boolean = false,
    var qualityPreset: String = "BALANCED", // FAST, BALANCED, QUALITY
    var tune: String? = null,               // FILM, ANIMATION, STILLIMAGE, etc.
    var colorRange: String = "FULL",         // FULL, LIMITED
    var colorStandard: String = "BT709"      // BT709, BT2020, etc.
)

class ExportQualityManager(private val deviceManager: DeviceCompatibilityManager) {

    companion object {
        // Platform-specific recommendations
        val SOCIAL_PLATFORMS = mapOf(
            "instagram" to ExportSettings(
                resolutions = listOf("1080p", "720p"),
                bitrates = listOf(15_000, 8_000),
                codec = "h264",
                format = "mp4"
            ),
            "tiktok" to ExportSettings(
                resolutions = listOf("1080p", "720p"),
                bitrates = listOf(10_000, 6_000),
                codec = "h264",
                format = "mp4"
            ),
            "youtube" to ExportSettings(
                resolutions = listOf("2160p", "1080p", "720p"),
                bitrates = listOf(50_000, 20_000, 10_000),
                codec = "h264",
                format = "mp4"
            ),
            "twitter" to ExportSettings(
                resolutions = listOf("720p", "480p"),
                bitrates = listOf(8_000, 4_000),
                codec = "h264",
                format = "mp4"
            ),
            "facebook" to ExportSettings(
                resolutions = listOf("1080p", "720p", "480p"),
                bitrates = listOf(12_000, 8_000, 4_000),
                codec = "h264",
                format = "mp4"
            )
        )
    }

    private val exportConfig = ExportQualityConfig()

    interface ExportQualityListener {
        fun onQualityPresetApplied(preset: ExportPreset)
        fun onEncodingOptimized(settings: Map<String, Any>)
        fun onFileSizeEstimated(sizeMb: Long, durationSec: Int)
    }

    private val listeners = mutableListOf<ExportQualityListener>()

    fun optimizeForPlatform(platform: String): ExportQualityConfig {
        val settings = SOCIAL_PLATFORMS[platform] ?: SOCIAL_PLATFORMS["instagram"]!!
        
        exportConfig.apply {
            preset = ExportPreset.SOCIAL_MEDIA
            resolution = when (settings.resolutions.first()) {
                "2160p" -> Size(3840, 2160)
                "1080p" -> Size(1920, 1080)
                "720p" -> Size(1280, 720)
                "480p" -> Size(854, 480)
                else -> Size(1920, 1080)
            }
            videoBitrate = settings.bitrates.first() * 1000
            videoCodec = if (settings.codec == "h264") VideoCodec.H264 else VideoCodec.H265
            format = if (settings.format == "mp4") ContainerFormat.MP4 else ContainerFormat.MOV
        }

        notifyQualityPresetApplied(ExportPreset.SOCIAL_MEDIA)
        return exportConfig
    }

    fun getRecommendedSettings(sourceResolution: Size, sourceBitrate: Int, durationSec: Int): ExportQualityConfig {
        val deviceProfile = deviceManager.getRecommendedExportSettings()
        val resolution = calculateOptimalResolution(sourceResolution)
        val bitrate = calculateOptimalBitrate(sourceBitrate, resolution)
        val frameRate = calculateOptimalFrameRate(sourceResolution)

        exportConfig.apply {
            this.resolution = resolution
            this.videoBitrate = bitrate
            this.frameRate = frameRate
            this.videoCodec = getBestCodec()
            this.audioBitrate = calculateOptimalAudioBitrate()
            this.enableHardwareEncoding = deviceManager.isFeatureSupported("HARDWARE_ENCODE")
            this.enableTwoPass = durationSec > 60 // Use two-pass for longer videos
            this.enableParallelEncoding = Runtime.getRuntime().availableProcessors() >= 4
        }

        notifyEncodingOptimized(mapOf(
            "resolution" to "${resolution.width}x${resolution.height}",
            "bitrate" to bitrate,
            "codec" to exportConfig.videoCodec.name
        ))

        estimateFileSize(durationSec)
        return exportConfig
    }

    private fun calculateOptimalResolution(source: Size): Size {
        val maxResolution = when (deviceManager.determinePerformanceProfile()) {
            DeviceCompatibilityManager.PerformanceProfile.ULTRA -> Size(7680, 4320) // 8K
            DeviceCompatibilityManager.PerformanceProfile.HIGH_END -> Size(3840, 2160) // 4K
            DeviceCompatibilityManager.PerformanceProfile.MID_RANGE -> Size(1920, 1080) // 1080p
            DeviceCompatibilityManager.PerformanceProfile.LOW_END -> Size(1280, 720) // 720p
        }

        // Don't upscale beyond source
        return if (source.width <= maxResolution.width && source.height <= maxResolution.height) {
            source
        } else {
            maxResolution
        }
    }

    private fun calculateOptimalBitrate(sourceBitrate: Int, targetResolution: Size): Int {
        val sourcePixels = targetResolution.width * targetResolution.height
        val bitrateFactor = when (deviceManager.determinePerformanceProfile()) {
            DeviceCompatibilityManager.PerformanceProfile.ULTRA -> 1.0
            DeviceCompatibilityManager.PerformanceProfile.HIGH_END -> 0.8
            DeviceCompatibilityManager.PerformanceProfile.MID_RANGE -> 0.6
            DeviceCompatibilityManager.PerformanceProfile.LOW_END -> 0.4
        }

        return when {
            sourcePixels >= 3840 * 2160 -> (50000000 * bitrateFactor).toInt() // 4K: 50 Mbps
            sourcePixels >= 1920 * 1080 -> (20000000 * bitrateFactor).toInt() // 1080p: 20 Mbps
            sourcePixels >= 1280 * 720 -> (10000000 * bitrateFactor).toInt()  // 720p: 10 Mbps
            else -> (5000000 * bitrateFactor).toInt()                          // 480p: 5 Mbps
        }.coerceAtMost(sourceBitrate)
    }

    private fun calculateOptimalFrameRate(source: Size): Int {
        return when {
            source.width >= 3840 -> 60 // 4K at 60fps if capable
            source.width >= 1920 -> 30 // 1080p at 30fps
            else -> 24                  // Lower at 24fps
        }
    }

    private fun calculateOptimalAudioBitrate(): Int {
        return when (deviceManager.determinePerformanceProfile()) {
            DeviceCompatibilityManager.PerformanceProfile.ULTRA -> 320_000  // 320 kbps
            DeviceCompatibilityManager.PerformanceProfile.HIGH_END -> 256_000 // 256 kbps
            DeviceCompatibilityManager.PerformanceProfile.MID_RANGE -> 192_000 // 192 kbps
            DeviceCompatibilityManager.PerformanceProfile.LOW_END -> 128_000  // 128 kbps
        }
    }

    private fun getBestCodec(): VideoCodec {
        return when {
            deviceManager.isFeatureSupported("4K_EXPORT") -> VideoCodec.H265
            deviceManager.isFeatureSupported("GPU_ACCELERATION") -> VideoCodec.H264
            else -> VideoCodec.H264
        }
    }

    fun estimateFileSize(durationSec: Int): Long {
        val videoSize = (exportConfig.videoBitrate * durationSec) / 8
        val audioSize = (exportConfig.audioBitrate * durationSec) / 8
        val totalSize = (videoSize + audioSize) / (1024 * 1024) // MB

        notifyFileSizeEstimated(totalSize, durationSec)
        return totalSize
    }

    fun createMediaFormat(): MediaFormat {
        val format = when (exportConfig.videoCodec) {
            VideoCodec.H264 -> MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                exportConfig.resolution.width,
                exportConfig.resolution.height
            )
            VideoCodec.H265 -> MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                exportConfig.resolution.width,
                exportConfig.resolution.height
            )
            VideoCodec.VP9 -> MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_VP9,
                exportConfig.resolution.width,
                exportConfig.resolution.height
            )
            VideoCodec.AV1 -> MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AV1,
                exportConfig.resolution.width,
                exportConfig.resolution.height
            )
        }

        format.setInteger(MediaFormat.KEY_BIT_RATE, exportConfig.videoBitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, exportConfig.frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, exportConfig.keyFrameInterval)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

        // Set profile and level
        when (exportConfig.videoCodec) {
            VideoCodec.H264 -> {
                format.setInteger(MediaFormat.KEY_PROFILE, when (exportConfig.profile) {
                    "HIGH" -> MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                    "MAIN" -> MediaCodecInfo.CodecProfileLevel.AVCProfileMain
                    else -> MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                })
                format.setInteger(MediaFormat.KEY_LEVEL, when (exportConfig.level) {
                    "5.1" -> MediaCodecInfo.CodecProfileLevel.AVCLevel51
                    "5.0" -> MediaCodecInfo.CodecProfileLevel.AVCLevel5
                    "4.2" -> MediaCodecInfo.CodecProfileLevel.AVCLevel42
                    else -> MediaCodecInfo.CodecProfileLevel.AVCLevel4
                })
            }
            VideoCodec.H265 -> {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
                format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel5)
            }
            else -> {}
        }

        // Add HDR metadata if applicable
        if (exportConfig.colorStandard != "BT709") {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, when (exportConfig.colorStandard) {
                "BT2020" -> MediaFormat.COLOR_STANDARD_BT2020
                else -> MediaFormat.COLOR_STANDARD_BT709
            })
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, when (exportConfig.colorRange) {
                "FULL" -> MediaFormat.COLOR_RANGE_FULL
                else -> MediaFormat.COLOR_RANGE_LIMITED
            })
        }

        return format
    }

    fun getPresetConfig(preset: ExportPreset): ExportQualityConfig {
        return when (preset) {
            ExportPreset.SOCIAL_MEDIA -> ExportQualityConfig(
                videoBitrate = 15_000_000,
                resolution = Size(1920, 1080),
                frameRate = 30,
                keyFrameInterval = 2
            )
            ExportPreset.YOUTUBE -> ExportQualityConfig(
                videoBitrate = 50_000_000,
                resolution = Size(3840, 2160),
                frameRate = 60,
                keyFrameInterval = 1,
                videoCodec = VideoCodec.H265,
                enableTwoPass = true
            )
            ExportPreset.ARCHIVE -> ExportQualityConfig(
                videoBitrate = 100_000_000,
                resolution = Size(7680, 4320),
                frameRate = 60,
                keyFrameInterval = 1,
                videoCodec = VideoCodec.AV1,
                audioCodec = AudioCodec.FLAC,
                enableTwoPass = true,
                qualityPreset = "QUALITY"
            )
            ExportPreset.CUSTOM -> exportConfig
        }
    }

    fun applyPreset(preset: ExportPreset) {
        val newConfig = getPresetConfig(preset)
        exportConfig.apply {
            this.preset = preset
            videoBitrate = newConfig.videoBitrate
            resolution = newConfig.resolution
            frameRate = newConfig.frameRate
            keyFrameInterval = newConfig.keyFrameInterval
            videoCodec = newConfig.videoCodec
            audioCodec = newConfig.audioCodec
            enableTwoPass = newConfig.enableTwoPass
            qualityPreset = newConfig.qualityPreset
        }
        notifyQualityPresetApplied(preset)
    }

    fun addListener(listener: ExportQualityListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ExportQualityListener) {
        listeners.remove(listener)
    }

    private fun notifyQualityPresetApplied(preset: ExportPreset) {
        listeners.forEach { it.onQualityPresetApplied(preset) }
    }

    private fun notifyEncodingOptimized(settings: Map<String, Any>) {
        listeners.forEach { it.onEncodingOptimized(settings) }
    }

    private fun notifyFileSizeEstimated(sizeMb: Long, durationSec: Int) {
        listeners.forEach { it.onFileSizeEstimated(sizeMb, durationSec) }
    }
}
