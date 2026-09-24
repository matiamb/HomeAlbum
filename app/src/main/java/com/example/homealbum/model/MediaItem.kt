package com.example.homealbum.model

import android.graphics.Bitmap
import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val dateTaken: Long,
    val thumbnail: Bitmap?,
    val uploadStatus: UploadStatus = UploadStatus.IDLE
)