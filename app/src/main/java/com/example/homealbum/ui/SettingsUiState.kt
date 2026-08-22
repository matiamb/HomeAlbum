package com.example.homealbum.ui

import com.example.homealbum.model.ServerConnectionStatus

data class SettingsUiState(
    val serverConnectionStatus: ServerConnectionStatus = ServerConnectionStatus.FAILED,
    val isChecking: Boolean = false
)