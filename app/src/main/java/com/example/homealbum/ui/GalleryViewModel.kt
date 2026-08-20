package com.example.homealbum.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.homealbum.R
import com.example.homealbum.data.ImageScreenRepository
import com.example.homealbum.data.NetworkPhotoRepository
import com.example.homealbum.data.OfflineSettingsRepository
import com.example.homealbum.data.PhotoRepository
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.model.GalleryItem
import com.example.homealbum.workers.DeleteScheduler
import com.example.homealbum.workers.UploadScheduler
import com.example.homealbum.workers.UploadWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

class GalleryViewModel(
    private val photosRepo: PhotoRepository,
    private val networkPhotoRepository: ImageScreenRepository,
    private val settingsRepository: SettingsRepository,
    private val uploadScheduler: UploadScheduler,
    private val deleteScheduler: DeleteScheduler
) : ViewModel() {

    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<ToastText>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun loadPhotos(){
        viewModelScope.launch {
            _galleryUiState.update { it.copy(isRefreshing = true) }
            try {
                val photoList = photosRepo.getLocalPhotos()
                _galleryUiState.update { it.copy(
                    photoList = photoList,
                    galleryItems = groupPhotosByDate(photoList)
                    ) }
            } catch (e: Exception){
                _toastMessage.emit(ToastText(message =  R.string.failed_to_load_local_photos_msg))
            } finally {
                _galleryUiState.update { it.copy(isRefreshing = false) }
            }

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
//            try {
//                val isFileInServer = networkPhotoRepository.checkIfPhotoExist(uri)
//                if (isFileInServer.isSuccessful){
//                    networkPhotoRepository.deleteMediaFile(uri)
//                    if (intentSenderRequest != null) {
//                        onIntentReady(intentSenderRequest)
//                    } else {
//                        removeThrashedPhotoFromUi(uri)
//                    }
//                } else {
//                    if (intentSenderRequest != null) {
//                        onIntentReady(intentSenderRequest)
//                    } else {
//                        removeThrashedPhotoFromUi(uri)
//                    }
//                }
//            } catch (e: Exception){
//                if (intentSenderRequest != null) {
//                    onIntentReady(intentSenderRequest)
//                } else {
//                    removeThrashedPhotoFromUi(uri)
//                }
//            }
            if (intentSenderRequest != null) {
                onIntentReady(intentSenderRequest)
            } else {
                removeThrashedPhotoFromUi(uri)
            }
        }
    }
    fun removeMediaFromServer(uri: Uri){
        viewModelScope.launch {
            try {
                val isFileInServer = networkPhotoRepository.checkIfPhotoExist(uri)
                if (isFileInServer.isSuccessful){
                    deleteScheduler.scheduleDelete(uri)
                }
            } catch (e: IOException){
                //_toastMessage.emit(ToastText(message = R.string.connection_error_msg))
                deleteScheduler.scheduleDelete(uri)
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
            val userSettings = settingsRepository.userSettingsFlow.first()
//            when {
//                userSettings.isBackupEnabled && !userSettings.allowUploadMobileData-> {
//                    uploadScheduler.scheduleUpload(uri, userSettings.)
//                }
//                userSettings.isBackupEnabled && userSettings.allowUploadMobileData -> {
//                    uploadScheduler.scheduleUploadWithMobileData(uri)
//                }
//                else -> {
//                    _toastMessage.emit(ToastText(message = R.string.local_backup_is_disabled_msg))
//                }
//            }
            if (userSettings.isBackupEnabled){
                uploadScheduler.scheduleUpload(uri, userSettings.allowUploadMobileData)
            } else {
                _toastMessage.emit(ToastText(message = R.string.local_backup_is_disabled_msg))
            }
        }
    }
   fun checkIfPhotoExists(uri: Uri){
       viewModelScope.launch {
           if (settingsRepository.userSettingsFlow.first().isBackupEnabled){
               try {
                   val serverResponse = networkPhotoRepository.checkIfPhotoExist(uri)
                   if (serverResponse.isSuccessful){
                       _toastMessage.emit(ToastText(message = R.string.photo_exists_on_the_server_msg))
                   } else {
                       _toastMessage.emit(ToastText(message = R.string.file_not_found_in_server_msg))
                   }
               } catch (e: Exception){
                   _toastMessage.emit(ToastText(message = R.string.connection_error_msg))
               }
           } else {
               _toastMessage.emit(ToastText(message = R.string.local_backup_is_disabled_msg))
           }
       }
    }
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
                    settingsRepository,
                    application.container.uploadScheduler,
                    application.container.deleteScheduler
                    )
            }
        }
    }
}
private fun groupPhotosByDate(photos: List<MediaItem>): List<GalleryItem>{
    return photos.withIndex()
        .groupBy { itemIndexed ->
            Instant.ofEpochMilli(itemIndexed.value.dateTaken)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.flatMap { (date, photosForDate) ->
            buildList {
                add(
                    GalleryItem.DateHeader(date)
                )
                addAll(
                    photosForDate.map { itemIndexed ->
                        GalleryItem.Photo(
                            mediaItem = itemIndexed.value,
                            originalIndex = itemIndexed.index
                        )
                    }
                )
            }
        }
}
data class ToastText(
    @StringRes val message: Int = 0
)


