package com.example.homealbum.data

import android.content.Context
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.homealbum.data.ImageDataSource.imageList
import com.example.homealbum.model.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryViewModel(private val photosRepo: PhotoRepository) : ViewModel() {

    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    fun loadPhotos(){
        viewModelScope.launch {
            _galleryUiState.value = GalleryUiState(photoList = photosRepo.getLocalPhotos())
        }
    }

    fun requestTrashPhoto(uri: Uri, onIntentReady: (IntentSenderRequest) -> Unit){
        viewModelScope.launch {
            val intentSenderRequest = photosRepo.prepareToTrashPhoto(uri)
            if (intentSenderRequest != null) {
                onIntentReady(intentSenderRequest)
            } else {
                removeThrashedPhotoFromUi(uri)
            }
        }
    }

    fun removeThrashedPhotoFromUi(uri: Uri){
        _galleryUiState.update { state ->
            state.copy(photoList = state.photoList.filter { it != uri })
        }
    }

    init {
        //loadPhotos()
    }
}
class GalleryViewModelFactory(private val photosRepo: PhotoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(photosRepo) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}