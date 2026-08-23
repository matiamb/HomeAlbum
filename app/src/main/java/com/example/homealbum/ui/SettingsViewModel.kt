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
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UserSettings
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

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val userSettings: StateFlow<UserSettings> = settingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettings(serverIp = "", serverFolderName = "", isBackupEnabled = false, allowUploadMobileData = false)
    )
    private var _toastMessage = MutableSharedFlow<ToastText>()
    val toastMessage = _toastMessage.asSharedFlow()
//    private var _isChecking = mutableStateOf(false)
//    val isChecking = _isChecking
    private var _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState = _settingsUiState.asStateFlow()
    init {
        viewModelScope.launch {
            userSettings.first { it.serverIp.isNotBlank() }
            checkServerConnection()
            checkServerDiskSpace()
        }
    }
    fun saveServerSettings(ip: String, folderName: String){
//        _isChecking.value = true
        viewModelScope.launch {
            settingsRepository.saveServerSettings(ip, folderName)
            try {
                _settingsUiState.update {
                    it.copy(serverConnectionStatus = ServerConnectionStatus.CHECKING, isChecking = true)
                }
                settingsRepository.saveServerSettings(ip, folderName)
                val serverResponse = settingsRepository.checkServerConnection(ip)
                if (serverResponse.isSuccessful){
                    _settingsUiState.update {
                        it.copy(serverConnectionStatus = ServerConnectionStatus.CONNECTED,
                            isChecking = false)
                    }
                    _toastMessage.emit(ToastText(message = R.string.server_connection_ok))
                    //settingsRepository.saveServerSettings(ip, folderName)
                    //_isChecking.value = false
                } else {
                    _settingsUiState.update {
                        it.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                    }
                    _toastMessage.emit(ToastText(message = R.string.server_connection_failed))
                }
            } catch (e: IOException){
                _settingsUiState.update {
                    it.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                }
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
            try {
                _settingsUiState.update {
                    it.copy(serverConnectionStatus = ServerConnectionStatus.CHECKING)
                }
                val serverResponse = settingsRepository.checkServerConnection(userSettings.value.serverIp)
                if (serverResponse.isSuccessful){
                    _settingsUiState.update {
                        it.copy(serverConnectionStatus = ServerConnectionStatus.CONNECTED)
                    }
                } else {
                    _settingsUiState.update {
                        it.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                    }
                }
            } catch (e: IOException){
                _settingsUiState.update {
                    it.copy(serverConnectionStatus = ServerConnectionStatus.FAILED)
                }
            }
        }
    }
    fun saveBackupEnabled(isBackupEnabled: Boolean){
        viewModelScope.launch {
            settingsRepository.saveBackupEnabled(isBackupEnabled)
        }
    }
    fun saveMobileDataUpload(allowUpload : Boolean){
        viewModelScope.launch {
            settingsRepository.saveMobileDataUpload(allowUpload = allowUpload)
        }
    }
    fun checkServerDiskSpace(){
        viewModelScope.launch {
            try {
                val serverDiskSpace = settingsRepository.checkServerDiskSpace(userSettings.value.serverIp)
                _settingsUiState.update {
                    it.copy(serverDiskSpace = serverDiskSpace)
                }
            } catch (e: IOException){

            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as HomeAlbumApplication)
                val settingRepository = application.container.offlineSettingsRepository
                SettingsViewModel(settingRepository)
            }
        }
    }
}