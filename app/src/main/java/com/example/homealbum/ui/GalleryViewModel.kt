package com.example.homealbum.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.PhotoRepository
import com.example.homealbum.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY

class GalleryViewModel(private val photosRepo: PhotoRepository) : ViewModel() {

    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    fun loadPhotos(){
        viewModelScope.launch {
            _galleryUiState.value = GalleryUiState(photoList = photosRepo.getLocalPhotos())
        }
    }

    /**
     * This is just a passthrough function to connect the UI with the repo. Since the repo function
     * is a suspend, this function needs to be suspended as well
     */
    suspend fun getThumbnail(
        mediaItem: MediaItem,
        width: Int,
        height: Int
    ): Bitmap?{
        return photosRepo.getThumbnail(
            uri = mediaItem.uri,
            width = width,
            height = height
        )
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
            state.copy(photoList = state.photoList.filter { it.uri != uri })
        }
    }

//    init {
//        viewModelScope.launch {
//            photosRepo.photoList.collect { photoList ->
//                _galleryUiState.update { it.copy(photoList = photoList) }
//            }
//        }
//
//    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as HomeAlbumApplication)
                val photoRepository = application.container.photoRepository
                GalleryViewModel(photoRepository)
            }
        }
    }
}
//class GalleryViewModelFactory(private val photosRepo: PhotoRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return GalleryViewModel(photosRepo) as T
//        }
//        throw IllegalArgumentException("Clase ViewModel desconocida")
//    }


