package com.example.homealbum.data

import androidx.lifecycle.ViewModel
import com.example.homealbum.data.ImageDataSource.imageList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GalleryViewModel : ViewModel() {
    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    private fun loadPhotos(){
        _galleryUiState.value = GalleryUiState(photoList = imageList)
    }

    init {
        loadPhotos()
    }
}