package com.example.homealbum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.OfflineSettingsRepository
import com.example.homealbum.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: OfflineSettingsRepository) : ViewModel() {
    val uiState: StateFlow<UserSettings> = settingsRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettings("", "", false)
    )
    fun saveServerIp(ip: String){
        viewModelScope.launch {
            settingsRepository.saveServerIp(ip)
        }
    }
    fun saveServerFolderName(folderName: String){
        viewModelScope.launch {
            settingsRepository.saveServerFolderName(folderName)
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