package com.yourname.videoeditor.feature.gallery.model

import android.net.Uri

data class VideoItem(
    val id: String,
    val uri: Uri,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val resolution: String,
    val thumbnail: Uri?
)