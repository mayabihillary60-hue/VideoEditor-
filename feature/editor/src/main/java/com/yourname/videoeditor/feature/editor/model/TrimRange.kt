package com.yourname.videoeditor.feature.editor.model

data class TrimRange(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long
        get() = endMs - startMs
}