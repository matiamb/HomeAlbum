package com.example.homealbum.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.homealbum.model.UserSettings
import com.example.homealbum.network.ServerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

interface SettingsRepository {
    suspend fun saveServerSettings(ip: String, folderName: String)
    suspend fun saveBackupEnabled(isBackupEnabled: Boolean)
    suspend fun checkServerConnection(serverIp: String): Response<ResponseBody>
}

class OfflineSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val serverApiService: ServerApiService
) : SettingsRepository{
    private object PreferencesKeys {
        val SERVER_IP = stringPreferencesKey(name = "server_ip")
        val FOLDER_NAME = stringPreferencesKey(name = "folder_name")
        val IS_BACKUP_ENABLED = booleanPreferencesKey(name = "is_backup_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            serverIp = preferences[PreferencesKeys.SERVER_IP] ?: "",
            serverFolderName = preferences[PreferencesKeys.FOLDER_NAME] ?: "",
            isBackupEnabled = preferences[PreferencesKeys.IS_BACKUP_ENABLED] ?: false
        )
    }

    override suspend fun saveServerSettings(ip: String, folderName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_IP] = ip
            preferences[PreferencesKeys.FOLDER_NAME] = folderName
        }
    }

    override suspend fun saveBackupEnabled(isBackupEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BACKUP_ENABLED] = isBackupEnabled
        }
    }

    override suspend fun checkServerConnection(serverIp: String): Response<ResponseBody> =
        withContext(Dispatchers.IO){
            val endpoint = "http://$serverIp/api/v1/media/ping"
            serverApiService.checkServerConnection(endpoint)
        }
}