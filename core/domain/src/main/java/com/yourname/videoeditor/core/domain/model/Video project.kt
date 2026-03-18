package com.yourname.videoeditor.core.domain.model

import android.net.Uri
import java.util.UUID

data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val timeline: Timeline = Timeline(),
    val resolution: Resolution = Resolution.FULL_HD,
    val aspectRatio: AspectRatio = AspectRatio.SIXTEEN_BY_NINE
)

data class Timeline(
    val videoTracks: List<VideoTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val durationMs: Long = 0
)

data class VideoTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: List<VideoClip> = emptyList(),
    val isMuted: Boolean = false
)

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: List<AudioClip> = emptyList(),
    val volume: Float = 1.0f
)

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourcePath: String,
    val startTimeMs: Long = 0,
    val endTimeMs: Long,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val rotation: Int = 0,
    val scale: Float = 1.0f
)

data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val sourcePath: String,
    val startTimeMs: Long = 0,
    val endTimeMs: Long,
    val volume: Float = 1.0f
)

enum class Resolution(val width: Int, val height: Int) {
    SD(640, 480),
    HD(1280, 720),
    FULL_HD(1920, 1080),
    FOUR_K(3840, 2160)
}

enum class AspectRatio(val value: Float) {
    SIXTEEN_BY_NINE(16f/9f),
    FOUR_BY_THREE(4f/3f),
    ONE_BY_ONE(1f),
    NINE_BY_SIXTEEN(9f/16f)
}