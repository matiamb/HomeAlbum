package com.example.homealbum.ui

import com.example.homealbum.model.MediaItem

data class GalleryUiState(
    val photoList: List<MediaItem> = listOf(),
    val isRefreshing: Boolean = false
)