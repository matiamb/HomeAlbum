package com.example.homealbum.model

import android.net.Uri

data class FileUploadInfo(
    val uri: Uri,
    val uploadStatus: UploadStatus
)