package com.example.homealbum.ui

import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrashViewModel(
    private val photoRepository: PhotoRepository
) : ViewModel() {
    private val _trashUiState = MutableStateFlow(TrashUiState())
    val trashUiState: StateFlow<TrashUiState> = _trashUiState.asStateFlow()
    fun loadTrashedFiles(){
        viewModelScope.launch {
            try {
                _trashUiState.update {
                    it.copy(isRefreshing = true)
                }
                val trashedFilesList = photoRepository.getTrashedFiles()
                _trashUiState.update {
                    it.copy(trashedFilesList = trashedFilesList)
                }
                _trashUiState.update {
                    it.copy(isRefreshing = false)
                }
            } catch (e: SecurityException){
                _trashUiState.update {
                    it.copy(isRefreshing = false)
                }
            }
        }
    }
    fun enableMultipleSelection(uri: Uri){
        if (trashUiState.value.multipleSelectionSet.contains(uri)){
            _trashUiState.update {
                it.copy( multipleSelectionSet = it.multipleSelectionSet - uri)
            }
        } else {
            _trashUiState.update {
                it.copy(multipleSelectionSet = it.multipleSelectionSet + uri)
            }
        }
    }
    fun clearMultipleSelection(){
        _trashUiState.update {
            it.copy(multipleSelectionSet = emptySet())
        }
    }
    fun restoreFiles(onIntentReady: (IntentSenderRequest) -> Unit){
        viewModelScope.launch {
            try {
                val intentSenderRequest = photoRepository.restoreFiles(trashUiState.value.multipleSelectionSet)
                if (intentSenderRequest != null){
                    onIntentReady(intentSenderRequest)
                }
            } catch (e: SecurityException){

            }
        }
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as HomeAlbumApplication)
                val photoRepository = application.container.offlinePhotoRepository
                TrashViewModel(
                    photoRepository = photoRepository
                )
            }
        }
    }
    init {
        loadTrashedFiles()
    }
}