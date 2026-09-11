package com.example.homealbum.ui

import android.net.Uri
import com.example.homealbum.model.MediaItem

data class TrashUiState(
    val trashedFilesList: List<MediaItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val multipleSelectionSet: Set<Uri> = emptySet()
)