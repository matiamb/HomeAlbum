package com.example.homealbum.ui

import com.example.homealbum.model.GalleryItem
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.UploadStatus

data class GalleryUiState(
    val photoList: List<MediaItem> = listOf(),
    val galleryItems: List<GalleryItem> = listOf(),
    val isRefreshing: Boolean = false,
    val uploadStatus: UploadStatus = UploadStatus.IDLE
)