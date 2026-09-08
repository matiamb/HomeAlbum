package com.example.homealbum.ui

import android.util.Log
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
    init {
        loadTrashedFiles()
    }
    private val _trashUiState = MutableStateFlow(TrashUiState())
    val trashUiState: StateFlow<TrashUiState> = _trashUiState.asStateFlow()
    fun loadTrashedFiles(){
        viewModelScope.launch {
            val trashedFilesList = photoRepository.getTrashedFiles()
            _trashUiState.update {
                it.copy(trashedFilesList = trashedFilesList)
            }
            Log.d("Mati", "trashFilesList size: ${trashUiState.value.trashedFilesList.size}")
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
}