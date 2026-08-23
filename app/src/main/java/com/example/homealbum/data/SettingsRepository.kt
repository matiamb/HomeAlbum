package com.example.homealbum.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.homealbum.model.DiskSpace
import com.example.homealbum.model.UserSettings
import com.example.homealbum.network.ServerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import kotlin.math.round

interface SettingsRepository {
    val userSettingsFlow: Flow<UserSettings>
    suspend fun saveServerSettings(ip: String, folderName: String)
    suspend fun saveBackupEnabled(isBackupEnabled: Boolean)
    suspend fun checkServerConnection(serverIp: String): Response<ResponseBody>
    suspend fun saveMobileDataUpload(allowUpload : Boolean)
    suspend fun checkServerDiskSpace(serverIp: String): DiskSpace
}

class OfflineSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val serverApiService: ServerApiService
) : SettingsRepository{
    private object PreferencesKeys {
        val SERVER_IP = stringPreferencesKey(name = "server_ip")
        val FOLDER_NAME = stringPreferencesKey(name = "folder_name")
        val IS_BACKUP_ENABLED = booleanPreferencesKey(name = "is_backup_enabled")
        val ALLOW_UPLOAD_MOBILE_DATA = booleanPreferencesKey(name = "allow_upload_mobile_data")
    }

    override val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            serverIp = preferences[PreferencesKeys.SERVER_IP] ?: "",
            serverFolderName = preferences[PreferencesKeys.FOLDER_NAME] ?: "",
            isBackupEnabled = preferences[PreferencesKeys.IS_BACKUP_ENABLED] ?: false,
            allowUploadMobileData = preferences[PreferencesKeys.ALLOW_UPLOAD_MOBILE_DATA] ?: false
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

    override suspend fun saveMobileDataUpload(allowUpload: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOW_UPLOAD_MOBILE_DATA] = allowUpload
        }
    }

    override suspend fun checkServerDiskSpace(serverIp: String): DiskSpace =
        withContext(Dispatchers.IO){
            val endpoint = "http://$serverIp/api/v1/media/diskspace"
            val serverResponse = serverApiService.checkDiskSpace(endpoint)
            val totalSpaceGb = round((serverResponse.totalSpaceBytes) /(1024*1024*1024).toDouble()*100)/100
            val availableSpaceGb = round((serverResponse.availableSpaceBytes)/(1024*1024*1024).toDouble()*100)/100
            val usedSpaceGb = round(((serverResponse.usedSpaceBytes)/(1024*1024*1024)).toDouble()*100)/100
            DiskSpace(
                totalSpaceBytes = totalSpaceGb,
                availableSpaceBytes = availableSpaceGb,
                usedSpaceBytes = usedSpaceGb
            )
    }
}