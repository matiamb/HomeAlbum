package com.example.homealbum.viewmodel

import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.work.OneTimeWorkRequest
import com.example.homealbum.R
import com.example.homealbum.data.ImageScreenRepository
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UploadResult
import com.example.homealbum.model.UploadStatus
import com.example.homealbum.ui.GalleryViewModel
import com.example.homealbum.utils.FakeCoordinatorScheduler
import com.example.homealbum.utils.FakePhotoRepository
import com.example.homealbum.utils.FakeSettingsRepository
import com.example.homealbum.utils.FakeSyncScheduler
import com.example.homealbum.utils.FakeUploadQueueRepo
import com.example.homealbum.workers.DeleteScheduler
import com.example.homealbum.workers.UploadScheduler
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
class FakeNetworkRepository() : ImageScreenRepository{
    var photoExists = false
    var throwException = false
    override suspend fun checkIfPhotoExist(uri: Uri): Response<ResponseBody> {
        when {
            photoExists && !throwException -> {
                return Response.success("OK".toResponseBody())
            }
            !photoExists && !throwException -> {
                return Response.error<ResponseBody>(404, "Not found".toResponseBody())
            }
            else -> {
                throw IOException()
            }
        }
    }

    override suspend fun uploadPhoto(
        fileUriString: String,
        fileHash: String?
    ): UploadResult {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMediaFile(uriList: List<Uri>): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun uploadMultipleFiles(uriList: List<Uri>): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun syncMediaFiles(fileHashList: List<String>): List<String> {
        if (throwException){
            throw IOException()
        }
        return emptyList()
    }

    override suspend fun getFileHash(uri: Uri): String? {
        TODO("Not yet implemented")
    }

}
class FakeUploadScheduler() : UploadScheduler {
    var scheduledUri: Uri? = null
    var scheduledUriSet: Set<Uri> = emptySet()
    var fakeAllowUploadMobilData = false
    override fun scheduleUpload(
        uriList: Set<Uri>,
        workerId: String,
        allowUploadMobileData: Boolean
    ) {
        scheduledUriSet = emptySet()
        scheduledUriSet = uriList
        fakeAllowUploadMobilData = allowUploadMobileData
    }

    override fun cancelAutoUpload() {
        TODO("Not yet implemented")
    }

    override fun buildRequest(workerId: String): OneTimeWorkRequest {
        TODO("Not yet implemented")
    }

}

class FakeDeleteScheduler : DeleteScheduler{
    var scheduledUri: Set<Uri> = emptySet()

    override fun scheduleDelete(uriSet: Set<Uri>) {
        scheduledUri = uriSet
    }
}

class GalleryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var fakePhotoRepository: FakePhotoRepository
    private lateinit var fakeNetworkRepository: FakeNetworkRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var galleryViewModelTest: GalleryViewModel
    private lateinit var fakeUploadScheduler: FakeUploadScheduler
    private lateinit var fakeDeleteScheduler: FakeDeleteScheduler
    private lateinit var fakeUploadQueueRepo: FakeUploadQueueRepo
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    @Before
    fun setup(){
        fakePhotoRepository = FakePhotoRepository()
        fakeNetworkRepository = FakeNetworkRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeUploadScheduler = FakeUploadScheduler()
        fakeDeleteScheduler = FakeDeleteScheduler()
        fakeUploadQueueRepo = FakeUploadQueueRepo()
        fakeSyncScheduler = FakeSyncScheduler()
        galleryViewModelTest = GalleryViewModel(
            photosRepo = fakePhotoRepository,
            settingsRepository = fakeSettingsRepository,
            uploadScheduler = fakeUploadScheduler,
            deleteScheduler = fakeDeleteScheduler,
            uploadQueueRepository = fakeUploadQueueRepo,
            syncScheduler = fakeSyncScheduler
        )
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadPhotos_repoLoadsPhotos_galleryUiStateUpdatedWithPhotos(){
        runTest {
            fakePhotoRepository.failToLoad = false
            galleryViewModelTest.loadPhotos()
            advanceUntilIdle()
            val photoListTest = galleryViewModelTest.galleryUiState.first()
            assertEquals(false, photoListTest.isRefreshing)
            assertEquals(3, photoListTest.photoList.size)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadPhotos_repoFailsToLoadPhotos_expectedToastIsShown(){
        runTest {
            fakePhotoRepository.failToLoad = true
            galleryViewModelTest.loadPhotos()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.failed_to_load_local_photos_msg, toast.message)
            }
            advanceUntilIdle()
            val photoListTest = galleryViewModelTest.galleryUiState.first()
            assertEquals(false, photoListTest.isRefreshing)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun removePhotoFromUi_photoRemovedFromUi_correctPhotoRemovedFromUiState(){
        runTest {
            fakePhotoRepository.failToLoad = false
            galleryViewModelTest.loadPhotos()
            advanceUntilIdle()
            galleryViewModelTest.removeThrashedPhotoFromUi(setOf(fakePhotoRepository.media1.uri))
            val photoListTest = galleryViewModelTest.galleryUiState.first()
            assertFalse(photoListTest.photoList.contains(fakePhotoRepository.media1))
            assertEquals(false, photoListTest.isRefreshing)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uploadPhoto_requestToUploadBackupEnabled_uploadSchedulerIsCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeSettingsRepository.saveBackupEnabled(true)
            fakeSettingsRepository.setServerIp("testIp")
            galleryViewModelTest.uploadPhoto(uri)
            advanceUntilIdle()
            assertTrue( fakeUploadScheduler.scheduledUriSet.contains(uri))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uploadPhoto_requestToUploadBackupDisabled_schedulerIsNotCalledAndExpectedToastShown(){
        runTest {
            fakeSettingsRepository.saveBackupEnabled(false)
            val uri = mockk<Uri>()
            galleryViewModelTest.uploadPhoto(uri)
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.local_backup_is_disabled_msg, toast.message)
            }
            advanceUntilIdle()
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(uri))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun uploadPhoto_serverIpNotSet_expectedToastIsShownAndUploadSchedulerIsNotCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeSettingsRepository.setServerIp("")
            galleryViewModelTest.uploadPhoto(uri)
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.msg_server_ip_not_set, toast.message)
            }
            advanceUntilIdle()
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(uri))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun removeMediaFromServer_serverNotReachable_deleteSchedulerCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeNetworkRepository.throwException = true
            galleryViewModelTest.removeMediaFromServer(setOf(uri))
            advanceUntilIdle()
            assertEquals(setOf(uri), fakeDeleteScheduler.scheduledUri)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerConnection_serverConnectionSuccessful_settingsUiStateConnected(){
        runTest {
            fakeSettingsRepository.connectionSuccessful = true
            fakeSettingsRepository.shouldThrowException = false
            galleryViewModelTest.checkServerConnection()
            //assertEquals(ServerConnectionStatus.CHECKING, settingsUiState.serverConnectionStatus)
            advanceUntilIdle()
            val galleryUiState = galleryViewModelTest.galleryUiState.value
            assertEquals(ServerConnectionStatus.CONNECTED, galleryUiState.serverConnectionStatus)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkServerConnection_serverConnectionFailed_settingsUiStateFailed(){
        runTest {
            fakeSettingsRepository.connectionSuccessful = false
            fakeSettingsRepository.shouldThrowException = false
            galleryViewModelTest.checkServerConnection()
            //assertEquals(ServerConnectionStatus.CHECKING, settingsUiState.serverConnectionStatus)
            advanceUntilIdle()
            val galleryUiState = galleryViewModelTest.galleryUiState.value
            assertEquals(ServerConnectionStatus.FAILED, galleryUiState.serverConnectionStatus)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requestTrashPhoto_repoReceivesFilesAndCallbackReceivesIntent(){
        runTest {
            fakePhotoRepository.failToDelete = false
            val expectedRequest = mockk<IntentSenderRequest>()
            fakePhotoRepository.deleteResult = expectedRequest
            var receivedRequest: IntentSenderRequest? = null
            val setToDelete = setOf(
                fakePhotoRepository.media1.uri,
                fakePhotoRepository.media2.uri
            )
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.requestTrashPhoto { request ->
                receivedRequest = request
            }
            advanceUntilIdle()
            assertEquals(expectedRequest, receivedRequest)
            assertEquals(setToDelete, fakePhotoRepository.deletedUris)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun removeTrashedPhotoFromUi_updatesUiStateCorrectly(){
        runTest {
            galleryViewModelTest.loadPhotos()
            val setToDelete = setOf(
                fakePhotoRepository.media1.uri,
                fakePhotoRepository.media2.uri
            )
            advanceUntilIdle()
            galleryViewModelTest.removeThrashedPhotoFromUi(setToDelete)
            assertFalse(galleryViewModelTest.galleryUiState.value.photoList.contains(fakePhotoRepository.media1))
            assertFalse(galleryViewModelTest.galleryUiState.value.photoList.contains(fakePhotoRepository.media2))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSync_serverIpNotSet_expectedToastIsShownAndSyncSchedulerNotCalled(){
        runTest {
            fakeSettingsRepository.setServerIp("")
            galleryViewModelTest.triggerSync()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.msg_server_ip_not_set, toast.message)
            }
            advanceUntilIdle()
            assertFalse(fakeSyncScheduler.syncSchedulerCalled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSync_serverIpSet_syncSchedulerCalled(){
        runTest {
            fakeSettingsRepository.setServerIp("testIp")
            fakeSettingsRepository.saveBackupEnabled(true)
            galleryViewModelTest.triggerSync()
            advanceUntilIdle()
            assertTrue(fakeSyncScheduler.syncSchedulerCalled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSync_serverIpSetAndLocalBackupDisabled_expectedToastIsShownAndSyncSchedulerNotCalled(){
        runTest {
            fakeSettingsRepository.setServerIp("testIp")
            fakeSettingsRepository.saveBackupEnabled(false)
            galleryViewModelTest.triggerSync()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.local_backup_is_disabled_msg, toast.message)
            }
            advanceUntilIdle()
            assertFalse(fakeSyncScheduler.syncSchedulerCalled)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSync_throwsException_expectedToastShown(){
        runTest {
            fakeNetworkRepository.throwException = true
            galleryViewModelTest.triggerSync()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.connection_error_msg, toast.message)
            }
            advanceUntilIdle()
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startMultipleUpload_serverIpNotSet_expectedToastIsShownAndUploadSchedulerIsNotCalled(){
        runTest{
            fakeSettingsRepository.setServerIp("")
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.startMultipleUpload()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.msg_server_ip_not_set, toast.message)
            }
            advanceUntilIdle()
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media1.uri))
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media2.uri))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startMultipleUpload_serverIpSet_uploadSchedulerCalled(){
        runTest {
            fakeSettingsRepository.setServerIp("testIp")
            fakeSettingsRepository.saveBackupEnabled(true)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.startMultipleUpload()
            advanceUntilIdle()
            assertTrue(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media1.uri))
            assertTrue(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media2.uri))
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startMultipleUpload_serverIpSetAndLocalBackupDisabled_expectedToastIsShownAndUploadSchedulerIsNotCalled(){
        runTest {
            fakeSettingsRepository.setServerIp("testIp")
            fakeSettingsRepository.saveBackupEnabled(false)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.startMultipleUpload()
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.msg_server_ip_not_set, toast.message)
            }
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media1.uri))
            assertFalse(fakeUploadScheduler.scheduledUriSet.contains(fakePhotoRepository.media2.uri))
        }
    }
}
