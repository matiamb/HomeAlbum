package com.example.homealbum.utils

import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.model.DiskSpace
import com.example.homealbum.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import retrofit2.Response

class FakeSettingsRepository : SettingsRepository{
    var connectionSuccessful = true
    var shouldThrowException = false

    private var _userSettingsFlow = MutableStateFlow(
        UserSettings(
            "",
            "",
            isBackupEnabled = true,
            allowUploadMobileData = false,
            isAutomaticBackupEnabled = false
        )
    )
    var diskSpaceItem = DiskSpace(totalSpaceBytes = 100.0, availableSpaceBytes = 30.0, usedSpaceBytes = 70.0)

    override val userSettingsFlow: Flow<UserSettings> =
        _userSettingsFlow

    override suspend fun saveServerSettings(ip: String, folderName: String) {
        _userSettingsFlow.value = _userSettingsFlow.value.copy(
            serverIp = ip,
            serverFolderName = folderName
        )
    }

    override suspend fun saveBackupEnabled(isBackupEnabled: Boolean) {
        _userSettingsFlow.value = _userSettingsFlow.value.copy(
            isBackupEnabled = isBackupEnabled
        )
    }

    override suspend fun checkServerConnection(serverIp: String): Response<ResponseBody> {
        if (shouldThrowException){
            throw IOException()
        }
        return if (connectionSuccessful){
            Response.success("OK".toResponseBody())
        } else {
            Response.error(504, "Server error".toResponseBody())
        }
    }

    override suspend fun saveMobileDataUpload(allowUpload: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun checkServerDiskSpace(serverIp: String): DiskSpace {
        if(shouldThrowException){
            throw IOException()
        } else {
            return diskSpaceItem
        }
    }

    override suspend fun saveAutomaticBackupEnabled(isAutomaticBackupEnabled: Boolean) {
        _userSettingsFlow.value = _userSettingsFlow.value.copy(
            isAutomaticBackupEnabled = isAutomaticBackupEnabled
        )
    }

    fun setConnectionResult(result: Boolean){
        connectionSuccessful = result
    }
    fun setServerIp(ip: String){
        _userSettingsFlow.value = _userSettingsFlow.value.copy(
            serverIp = ip
        )
    }
}