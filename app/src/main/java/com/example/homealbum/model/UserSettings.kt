package com.example.homealbum.model

data class UserSettings(
    val serverIp: String,
    val serverFolderName: String,
    val isBackupEnabled: Boolean
)