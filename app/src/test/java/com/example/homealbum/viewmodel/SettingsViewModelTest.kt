package com.example.homealbum.viewmodel

import androidx.compose.runtime.collectAsState
import com.example.homealbum.R
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UserSettings
import com.example.homealbum.ui.SettingsUiState
import com.example.homealbum.ui.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response


class MainDispatcherRule @OptIn(ExperimentalCoroutinesApi::class) constructor(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}

class FakeSettingsRepository : SettingsRepository{
    var connectionSuccessful = true
    var shouldThrowException = false

    private var _userSettingsFlow = MutableStateFlow(
        UserSettings("", "", true, false)
    )

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

    fun setConnectionResult(result: Boolean){
        connectionSuccessful = result
    }
}

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var settingsViewModelTest: SettingsViewModel

    @Before
    fun setup(){
        fakeSettingsRepository = FakeSettingsRepository()
        settingsViewModelTest = SettingsViewModel(settingsRepository = fakeSettingsRepository)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveServerSettings_settingsAreSaved_emitsExpectedToast(){
        runTest{
            val serverIpToSave = "100.0.0.1"
            val folderNameToSave = "Test Folder"
            fakeSettingsRepository.setConnectionResult(true)
            settingsViewModelTest.saveServerSettings(ip = serverIpToSave , folderName = folderNameToSave)
            val toastFlow = backgroundScope.launch {
                val toast = settingsViewModelTest.toastMessage.first()
                assertEquals(R.string.server_connection_ok, toast.message)
            }
            advanceUntilIdle()
            val serverSettings = fakeSettingsRepository.userSettingsFlow.first()
            assertEquals(serverIpToSave, serverSettings.serverIp)
            assertEquals(folderNameToSave, serverSettings.serverFolderName)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveServerSettings_serverConnectionFailed_emitsExpectedToastAndSettingsAreSaved(){
        runTest {
            val serverIpToSave = "100.0.0.1"
            val folderNameToSave = "Test Folder"
            fakeSettingsRepository.setConnectionResult(false)
            settingsViewModelTest.saveServerSettings(serverIpToSave, folderNameToSave)
            val toastFlow = backgroundScope.launch {
                val toast = settingsViewModelTest.toastMessage.first()
                assertEquals(R.string.server_connection_failed, toast.message)
            }
            advanceUntilIdle()
            val serverSettings = fakeSettingsRepository.userSettingsFlow.first()
            assertEquals("100.0.0.1", serverSettings.serverIp)
            assertEquals("Test Folder", serverSettings.serverFolderName)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerConnection_serverConnectionSuccessful_settingsUiStateConnected(){
        runTest {
            fakeSettingsRepository.connectionSuccessful = true
            fakeSettingsRepository.shouldThrowException = false
            settingsViewModelTest.checkServerConnection()
            //assertEquals(ServerConnectionStatus.CHECKING, settingsUiState.serverConnectionStatus)
            advanceUntilIdle()
            val settingsUiState = settingsViewModelTest.settingsUiState.value
            assertEquals(ServerConnectionStatus.CONNECTED, settingsUiState.serverConnectionStatus)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerConnection_serverConnectionFailed_settingsUiStateFailed(){
        runTest {
            fakeSettingsRepository.connectionSuccessful = false
            fakeSettingsRepository.shouldThrowException = false
            settingsViewModelTest.checkServerConnection()
            //assertEquals(ServerConnectionStatus.CHECKING, settingsUiState.serverConnectionStatus)
            advanceUntilIdle()
            val settingsUiState = settingsViewModelTest.settingsUiState.value
            assertEquals(ServerConnectionStatus.FAILED, settingsUiState.serverConnectionStatus)
        }
    }
}