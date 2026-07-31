package com.example.homealbum.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.homealbum.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    suspend fun saveServerIp(ip: String)
    suspend fun saveServerFolderName(folderName: String)
    suspend fun saveBackupEnabled(isBackupEnabled: Boolean)
}

class OfflineSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository{
    private object PreferencesKeys {
        val SERVER_IP = stringPreferencesKey(name = "server_ip")
        val FOLDER_NAME = stringPreferencesKey(name = "folder_name")
        val IS_BACKUP_ENABLED = booleanPreferencesKey(name = "is_backup_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            serverIp = preferences[PreferencesKeys.SERVER_IP] ?: "",
            serverFolderName = preferences[PreferencesKeys.FOLDER_NAME] ?: "Default Folder Name",
            isBackupEnabled = preferences[PreferencesKeys.IS_BACKUP_ENABLED] ?: false
        )
    }

    override suspend fun saveServerIp(ip: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_IP] = ip
        }
    }

    override suspend fun saveServerFolderName(folderName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_NAME] = folderName
        }
    }

    override suspend fun saveBackupEnabled(isBackupEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BACKUP_ENABLED] = isBackupEnabled
        }
    }
}