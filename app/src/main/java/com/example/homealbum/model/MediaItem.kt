package com.example.homealbum.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean
)