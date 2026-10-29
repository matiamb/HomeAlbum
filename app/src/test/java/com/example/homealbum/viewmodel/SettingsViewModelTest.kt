package com.example.homealbum.viewmodel

import com.example.homealbum.R
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.model.DiskSpace
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UserSettings
import com.example.homealbum.ui.SettingsViewModel
import com.example.homealbum.utils.FakeCoordinatorScheduler
import com.example.homealbum.utils.FakeUploadQueueRepo
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
import org.junit.Assert.assertFalse
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

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var settingsViewModelTest: SettingsViewModel
    private lateinit var fakeUploadQueueRepository: FakeUploadQueueRepo
    private lateinit var fakeCoordinatorScheduler: FakeCoordinatorScheduler

    @Before
    fun setup(){
        fakeSettingsRepository = FakeSettingsRepository()
        fakeUploadQueueRepository = FakeUploadQueueRepo()
        fakeCoordinatorScheduler = FakeCoordinatorScheduler()
        settingsViewModelTest = SettingsViewModel(
            settingsRepository = fakeSettingsRepository,
            uploadQueueRepository = fakeUploadQueueRepository,
            coordinatorUploadScheduler = fakeCoordinatorScheduler
        )
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
    fun saveServerSettings_serverConnectionFailed_uiStateUpdatesAndIsCheckingFalse(){
        runTest {
            val serverIpToSave = "100.0.0.1"
            val folderNameToSave = "Test Folder"
            fakeSettingsRepository.setConnectionResult(false)
            settingsViewModelTest.saveServerSettings(ip = serverIpToSave , folderName = folderNameToSave)
            advanceUntilIdle()
            val settingsUiState = settingsViewModelTest.settingsUiState.value
            assertEquals(ServerConnectionStatus.FAILED, settingsUiState.serverConnectionStatus)
            assertFalse(settingsUiState.isChecking)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerDiskSpace_requestSuccessful_uiStateUpdated(){
        runTest {
            fakeSettingsRepository.shouldThrowException = false
            settingsViewModelTest.checkServerDiskSpace()
            advanceUntilIdle()
            val settingsUiState = settingsViewModelTest.settingsUiState.value
            assertFalse(settingsUiState.isChecking)
            assertEquals(fakeSettingsRepository.diskSpaceItem, settingsUiState.serverDiskSpace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerDiskSpace_requestFailed_isCheckingFalse(){
        runTest {
            fakeSettingsRepository.shouldThrowException = true
            settingsViewModelTest.checkServerDiskSpace()
            advanceUntilIdle()
            val settingsUiState = settingsViewModelTest.settingsUiState.value
            assertFalse(settingsUiState.isChecking)
            assertEquals(DiskSpace(0.0, 0.0, 0.0), settingsUiState.serverDiskSpace)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveAutomaticBackup_enableAutomaticBackup_coordinatorScheduled(){
        runTest {
            fakeCoordinatorScheduler.coordinatorScheduled = false
            fakeSettingsRepository.saveBackupEnabled(true)
            settingsViewModelTest.saveAutomaticBackup(true)
            advanceUntilIdle()
            assertEquals(true, fakeCoordinatorScheduler.coordinatorScheduled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveAutomaticBackup_disableAutomaticBackup_coordinatorCancelled(){
        runTest {
            fakeCoordinatorScheduler.coordinatorCancelled = false
            fakeSettingsRepository.saveBackupEnabled(true)
            settingsViewModelTest.saveAutomaticBackup(false)
            advanceUntilIdle()
            assertEquals(true, fakeCoordinatorScheduler.coordinatorCancelled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveAutomaticBackup_automaticBackupEnabledAndBackupDisabled_coordinatorNotScheduled(){
        runTest {
            fakeCoordinatorScheduler.coordinatorScheduled = false
            fakeSettingsRepository.saveBackupEnabled(false)
            settingsViewModelTest.saveAutomaticBackup(true)
            advanceUntilIdle()
            assertEquals(false, fakeCoordinatorScheduler.coordinatorScheduled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun saveBackupEnabled_backupDisabledWhileAutomaticBackupEnabled_coordinatorCancellationCalled(){
        runTest {
            fakeCoordinatorScheduler.coordinatorCancelled = false
            fakeSettingsRepository.saveAutomaticBackupEnabled(true)
            settingsViewModelTest.saveBackupEnabled(false)
            advanceUntilIdle()
            assertEquals(true, fakeCoordinatorScheduler.coordinatorCancelled)
        }
    }
}
