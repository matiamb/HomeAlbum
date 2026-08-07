package com.example.homealbum.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.OfflineSettingsRepository
import com.example.homealbum.model.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: OfflineSettingsRepository) : ViewModel() {
    val uiState: StateFlow<UserSettings> = settingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettings("", "", false)
    )
    private var _toastMessage = MutableSharedFlow<String?>()
    val toastMessage = _toastMessage.asSharedFlow()
    private var _isChecking = mutableStateOf(false)
    val isChecking = _isChecking
    fun saveServerSettings(ip: String, folderName: String){
        _isChecking.value = true
        viewModelScope.launch {
            try {
                val serverResponse = settingsRepository.checkServerConnection(ip)
                if (serverResponse.isSuccessful){
                    _toastMessage.emit(serverResponse.body()?.string())
                    settingsRepository.saveServerSettings(ip, folderName)
                    _isChecking.value = false
                }
            } catch (e: Exception){
                _toastMessage.emit("Server connection failed")
            } finally {
                _isChecking.value = false
            }
        }
    }
    fun saveBackupEnabled(isBackupEnabled: Boolean){
        viewModelScope.launch {
            settingsRepository.saveBackupEnabled(isBackupEnabled)
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