package com.example.homealbum.ui

import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.homealbum.R
import com.example.homealbum.data.ImageScreenRepository
import com.example.homealbum.data.PhotoRepository
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.data.UploadQueueRepository
import com.example.homealbum.model.GalleryItem
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UploadStatus
import com.example.homealbum.model.UserSettings
import com.example.homealbum.workers.DeleteScheduler
import com.example.homealbum.workers.SyncScheduler
import com.example.homealbum.workers.UploadScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class GalleryViewModel(
    private val photosRepo: PhotoRepository,
    private val networkPhotoRepository: ImageScreenRepository,
    private val settingsRepository: SettingsRepository,
    private val uploadQueueRepository: UploadQueueRepository,
    private val uploadScheduler: UploadScheduler,
    private val deleteScheduler: DeleteScheduler,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _galleryUiState = MutableStateFlow(GalleryUiState())
    val galleryUiState: StateFlow<GalleryUiState> = _galleryUiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<ToastText>()
    val toastMessage = _toastMessage.asSharedFlow()
    val userSettings = settingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings("", "", false, false, false)
    )
    private val _rawPhotoList = MutableStateFlow<List<MediaItem>>(emptyList())
    fun loadPhotos(){
        viewModelScope.launch {
            _galleryUiState.update { it.copy(isRefreshing = true) }
            try {
                val photoList = photosRepo.getLocalPhotos()
                _rawPhotoList.value= photoList
                uploadQueueRepository.addMediaItemsToDb(photoList)
            } catch (e: IOException){
                _toastMessage.emit(ToastText(message =  R.string.failed_to_load_local_photos_msg))
            } catch (e: SecurityException){
                _toastMessage.emit(ToastText(message =  R.string.failed_to_load_local_photos_msg))
            }
            finally {
                _galleryUiState.update { it.copy(isRefreshing = false) }
            }

        }
    }

    fun requestTrashPhoto(uri: Uri, onIntentReady: (IntentSenderRequest) -> Unit){
        viewModelScope.launch {
            val intentSenderRequest = photosRepo.prepareToTrashPhoto(setOf(uri))
            if (intentSenderRequest != null) {
                onIntentReady(intentSenderRequest)
            } else {
                removeThrashedPhotoFromUi(setOf(uri))
            }
        }
    }
    fun requestTrashPhoto(onIntentReady: (IntentSenderRequest) -> Unit){
        viewModelScope.launch {
            val intentSenderRequest = photosRepo.prepareToTrashPhoto(galleryUiState.value.multipleSelectionSet)
            if (intentSenderRequest != null) {
                onIntentReady(intentSenderRequest)
            } else {
                removeThrashedPhotoFromUi(galleryUiState.value.multipleSelectionSet)
            }
        }
    }
    fun removeMediaFromServer(uriSet: Set<Uri>){
        viewModelScope.launch {
            try {
                deleteScheduler.scheduleDelete(uriSet)
            } catch (e: IOException){
                deleteScheduler.scheduleDelete(uriSet)
            }
        }
    }
    fun removeThrashedPhotoFromUi(uriSet: Set<Uri>){
        _rawPhotoList.update { currentList ->
            currentList.filter { it.uri !in uriSet }
        }
    }
    fun uploadPhoto(
        uri: Uri
    ){
        viewModelScope.launch {
            if (userSettings.value.serverIp.isBlank()){
                _toastMessage.emit(ToastText(message = R.string.msg_server_ip_not_set))
                return@launch
            }
            if (userSettings.value.isBackupEnabled){
                val workerId = generateWorkerId()
                uploadQueueRepository.prepareFilesForUpload(listOf(uri), UploadStatus.PENDING, workerId)
                uploadScheduler.scheduleUpload(setOf(uri), workerId, userSettings.value.allowUploadMobileData)
                _galleryUiState.update {
                    it.copy()
                }
            } else {
                _toastMessage.emit(ToastText(message = R.string.local_backup_is_disabled_msg))
            }
        }
    }
   fun checkIfPhotoExists(uri: Uri){
       viewModelScope.launch {
           if (userSettings.value.serverIp.isBlank()){
               _toastMessage.emit(ToastText(message = R.string.msg_server_ip_not_set))
               return@launch
           }
           if (userSettings.value.isBackupEnabled){
               try {
                   syncScheduler.scheduleSync()
               } catch (e: IOException){
                   _toastMessage.emit(ToastText(message = R.string.connection_error_msg))
               }
           } else {
               _toastMessage.emit(ToastText(message = R.string.local_backup_is_disabled_msg))
           }
       }
    }
    fun checkServerConnection(){
        viewModelScope.launch {
            try {
                if (userSettings.value.isBackupEnabled){
                    _galleryUiState.update { state ->
                        state.copy(serverConnectionStatus = ServerConnectionStatus.CHECKING)
                    }
                    val serverResponse = settingsRepository.checkServerConnection(userSettings.value.serverIp)
                    if (serverResponse.isSuccessful){
                        _galleryUiState.update { state ->
                            state.copy(serverConnectionStatus = ServerConnectionStatus.CONNECTED)
                        }
                    } else {
                        _galleryUiState.update { state ->
                            state.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                        }
                    }
                } else {
                    _galleryUiState.update { state ->
                        state.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                    }
                }
            } catch (e: IOException){
                _galleryUiState.update { state ->
                    state.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                }
            }

        }
    }
    fun multipleSelection(uri: Uri){
        if (galleryUiState.value.multipleSelectionSet.contains(uri)){
            _galleryUiState.update {
                it.copy(multipleSelectionSet = it.multipleSelectionSet - uri)
            }
        } else {
            _galleryUiState.update {
                it.copy(multipleSelectionSet = it.multipleSelectionSet + uri)
            }
        }
    }
    fun clearMultipleSelectionSet(){
        _galleryUiState.update {
            it.copy(multipleSelectionSet = emptySet())
        }
    }
    fun startMultipleUpload(){
        viewModelScope.launch {
            if (userSettings.value.serverIp.isBlank()){
                _toastMessage.emit(ToastText(message = R.string.msg_server_ip_not_set))
                return@launch
            }
            if (galleryUiState.value.multipleSelectionSet.isNotEmpty() && userSettings.value.isBackupEnabled){
                val workerId = generateWorkerId()
                uploadQueueRepository.prepareFilesForUpload(galleryUiState.value.multipleSelectionSet.toList(),
                    UploadStatus.PENDING, workerId)
                uploadScheduler.scheduleUpload(
                    uriList = galleryUiState.value.multipleSelectionSet,
                    workerId = workerId,
                    allowUploadMobileData = userSettings.value.allowUploadMobileData
                )
                clearMultipleSelectionSet()
            } else {
                _toastMessage.emit(
                    ToastText(message = R.string.local_backup_is_disabled_msg)
                )
            }
        }
    }
    fun deleteMediaFileFromDb(uriSet: Set<Uri>){
        viewModelScope.launch {
            uploadQueueRepository.deleteMediaItemFromDb(uriSet)
        }
    }
    fun syncMediaFiles(){
        viewModelScope.launch {
            syncScheduler.scheduleSync()
        }
    }
    fun scheduleAutoUpload(){
        viewModelScope.launch {
            if (userSettings.value.isBackupEnabled && userSettings.value.serverIp.isBlank()){
                return@launch
            }
            if (userSettings.value.isBackupEnabled && userSettings.value.isAutomaticBackupEnabled){
                val workerId = generateWorkerId()
                uploadQueueRepository.prepareFilesForAutoUpload(UploadStatus.PENDING, workerId)
            }
        }
    }
    private fun observeSyncStatus(){
        viewModelScope.launch {
            syncScheduler.syncStatus.collect {
                _galleryUiState.update { state ->
                    state.copy(syncInProgress = it)
                }
            }
        }
    }
    private fun observeUpload(){
        viewModelScope.launch {
            combine(
                _rawPhotoList,
                uploadQueueRepository.uploadInfo
            ){ photos, uploadInfos ->
                val statusMap = uploadInfos.associate { it.uri to it.uploadStatus }
                photos.map { photo ->
                    photo.copy(uploadStatus = statusMap[photo.uri] ?: UploadStatus.NOT_UPLOADED)
                }
            }.collect { updatedFiles ->
                _galleryUiState.update { state ->
                    state.copy(photoList = updatedFiles,
                        galleryItems = groupPhotosByDate(updatedFiles))
                }
            }
        }
    }
    init {
        observeSyncStatus()
        observeUpload()
        viewModelScope.launch {
            userSettings.first { it.serverIp.isNotBlank() }
            checkServerConnection()
        }
        loadPhotos()
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as HomeAlbumApplication)
                val photoRepository = application.container.offlinePhotoRepository
                val networkPhotoRepository = application.container.networkPhotoRepository
                val settingsRepository = application.container.offlineSettingsRepository
                val uploadQueueRepository = application.container.uploadQueueRepository
                GalleryViewModel(
                    photoRepository,
                    networkPhotoRepository,
                    settingsRepository,
                    uploadQueueRepository,
                    application.container.uploadScheduler,
                    application.container.deleteScheduler,
                    application.container.syncScheduler
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
fun generateWorkerId(): String{
    return UUID.randomUUID().toString()
}
data class ToastText(
    @StringRes val message: Int = 0
)


