package com.example.homealbum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.R
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.data.UploadQueueRepository
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UploadStatus
import com.example.homealbum.model.UserSettings
import com.example.homealbum.workers.AutoUploadCoordinatorScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val uploadQueueRepository: UploadQueueRepository,
    private val coordinatorUploadScheduler: AutoUploadCoordinatorScheduler
) : ViewModel() {
    val userSettings: StateFlow<UserSettings> = settingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettings(serverIp = "", serverFolderName = "", isBackupEnabled = false, allowUploadMobileData = false, isAutomaticBackupEnabled = false)
    )
    private suspend fun getCurrentUserSettings(): UserSettings =
        settingsRepository.userSettingsFlow.first()
    private var _toastMessage = MutableSharedFlow<ToastText>()
    val toastMessage = _toastMessage.asSharedFlow()
    private var _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState = _settingsUiState.asStateFlow()
    fun saveServerSettings(ip: String, folderName: String){
        viewModelScope.launch {
            try {
                _settingsUiState.update {
                    it.copy(
                        isChecking = true)
                }
                settingsRepository.saveServerSettings(ip, folderName)
                val serverResponse = settingsRepository.checkServerConnection(ip)
                if (serverResponse.isSuccessful){
                    checkServerDiskSpace()
                    _toastMessage.emit(ToastText(message = R.string.server_connection_ok))
                } else {
                    _toastMessage.emit(ToastText(message = R.string.server_connection_failed))
                }
            } catch (e: IOException){
                _toastMessage.emit(ToastText(message = R.string.server_connection_failed))
            } finally {
                _settingsUiState.update {
                    it.copy(
                        isChecking = false)
                }
            }
        }
    }
    fun checkServerConnection(){
        viewModelScope.launch {
            val userSettings = getCurrentUserSettings()
            try {
                settingsRepository.checkServerConnection(userSettings.serverIp)
            } catch (e: IOException){
                _toastMessage.emit(ToastText(message = R.string.server_connection_failed))
            }
        }
    }
    fun saveBackupEnabled(isBackupEnabled: Boolean){
        viewModelScope.launch {
            settingsRepository.saveBackupEnabled(isBackupEnabled)
            if(!isBackupEnabled){
                saveAutomaticBackup(false)
            }
        }
    }
    fun saveMobileDataUpload(allowUpload: Boolean){
        viewModelScope.launch {
            settingsRepository.saveMobileDataUpload(allowUpload = allowUpload)
        }
    }
    fun checkServerDiskSpace(){
        viewModelScope.launch {
            val userSettings = getCurrentUserSettings()
            try {
                _settingsUiState.update {
                    it.copy(isChecking = true)
                }
                val serverDiskSpace = settingsRepository.checkServerDiskSpace(userSettings.serverIp)
                _settingsUiState.update {
                    it.copy(isChecking = false, serverDiskSpace = serverDiskSpace)
                }
            } catch (e: IOException){

            } finally {
                _settingsUiState.update {
                    it.copy(isChecking = false)
                }
            }
        }
    }
    fun saveAutomaticBackup(isAutomaticBackup: Boolean){
        viewModelScope.launch {
            settingsRepository.saveAutomaticBackupEnabled(isAutomaticBackup)
            if (isAutomaticBackup){
                scheduleAutoUpload()
            } else {
                coordinatorUploadScheduler.cancelCoordinator()
                uploadQueueRepository.restoreStatusAutoUploadCancelled(UploadStatus.NOT_UPLOADED)
            }
        }
    }
    private suspend fun scheduleAutoUpload(){
        val userSettings = getCurrentUserSettings()
        if (userSettings.isBackupEnabled && userSettings.isAutomaticBackupEnabled){
            val workerId = generateWorkerId()
            uploadQueueRepository.prepareFilesForAutoUpload(UploadStatus.PENDING, workerId)
            coordinatorUploadScheduler.scheduleSyncUpload(workerId)
        }
    }
    init {
        viewModelScope.launch {
            settingsRepository.serverConnectionFlow.collect {status ->
                _settingsUiState.update {
                    it.copy(serverConnectionStatus = status)
                }
            }
        }
        viewModelScope.launch {
            userSettings.first { it.serverIp.isNotBlank() }
            checkServerConnection()
            checkServerDiskSpace()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as HomeAlbumApplication)
                val settingRepository = application.container.offlineSettingsRepository
                val uploadQueueRepository = application.container.uploadQueueRepository
                SettingsViewModel(
                    settingsRepository = settingRepository,
                    uploadQueueRepository = uploadQueueRepository,
                    coordinatorUploadScheduler = application.container.syncUploadCoordinator
                )
            }
        }
    }
}