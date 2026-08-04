package com.example.homealbum.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.OfflinePhotoRepository
import com.example.homealbum.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.homealbum.data.NetworkPhotoRepository
import com.example.homealbum.data.OfflineSettingsRepository
import com.example.homealbum.model.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel(
    private val photosRepo: OfflinePhotoRepository,
    private val networkPhotoRepository: NetworkPhotoRepository,
    private val settingsRepository: OfflineSettingsRepository
) : ViewModel() {

    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

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
    fun uploadPhoto(
        uri: Uri
    ){
        viewModelScope.launch {
            if (settingsRepository.userSettingsFlow.first().isBackupEnabled){
                try {
                    val serverResponse = networkPhotoRepository.uploadPhoto(uri)
                    if (serverResponse.code() == 201){
                        //val message = "Photo exist on the server"
                        _toastMessage.emit("Photo uploaded successfully")
                    } else {
                        //val message = "Photo not present on server"
                        _toastMessage.emit("Photo could not be uploaded")
                    }
                } catch (e: Exception){
                    _toastMessage.emit("Connection error: Check your server or ip")
                }
            } else {
                _toastMessage.emit("Local backup is disabled!")
            }
        }
    }
   fun checkIfPhotoExists(uri: Uri){
       viewModelScope.launch {
           if (settingsRepository.userSettingsFlow.first().isBackupEnabled){
               try {
                   val serverResponse = networkPhotoRepository.checkIfPhotoExist(uri)
                   if (serverResponse.code() == 200){
                       //val message = "Photo exist on the server"
                       _toastMessage.emit("Photo exist on the server")
                   } else {
                       //val message = "Photo not present on server"
                       _toastMessage.emit("Photo not present on server")
                   }
               } catch (e: Exception){
                   _toastMessage.emit("Connection error: Check your server or ip")
               }
           } else {
               _toastMessage.emit("Local backup is disabled!")
           }
//           message = "Photo not present on server"
//           _toastMessage.emit(message)
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
                val photoRepository = application.container.offlinePhotoRepository
                val networkPhotoRepository = application.container.networkPhotoRepository
                val settingsRepository = application.container.offlineSettingsRepository
                GalleryViewModel(
                    photoRepository,
                    networkPhotoRepository,
                    settingsRepository
                    )
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


