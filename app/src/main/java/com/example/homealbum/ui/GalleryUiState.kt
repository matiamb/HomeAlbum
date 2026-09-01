package com.example.homealbum.ui

import android.net.Uri
import com.example.homealbum.model.GalleryItem
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UploadStatus

data class GalleryUiState(
    val photoList: List<MediaItem> = listOf(),
    val galleryItems: List<GalleryItem> = listOf(),
    val isRefreshing: Boolean = false,
    val uploadStatus: UploadStatus = UploadStatus.IDLE,
    val serverConnectionStatus: ServerConnectionStatus = ServerConnectionStatus.FAILED,
    val multipleSelectionSet: Set<Uri> = emptySet()
)