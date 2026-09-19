package com.example.homealbum.viewmodel

import android.net.Uri
import com.example.homealbum.R
import com.example.homealbum.data.ImageScreenRepository
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UploadStatus
import com.example.homealbum.ui.GalleryViewModel
import com.example.homealbum.utils.FakePhotoRepository
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

    override suspend fun uploadPhoto(fileUri: Uri): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMediaFile(uriList: List<Uri>): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun uploadMultipleFiles(uriList: List<Uri>): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

}
class FakeUploadScheduler() : UploadScheduler {
    var scheduledUri: Uri? = null
    var scheduledUriSet: Set<Uri> = emptySet()
    var fakeAllowUploadMobilData = false
    override fun scheduleUpload(uri: Uri, allowUploadMobileData: Boolean) {
        scheduledUri = uri
    }

    override fun scheduleMultipleUpload(
        uriList: Set<Uri>,
        allowUploadMobileData: Boolean
    ) {
        scheduledUriSet = uriList
    }

    override val uploadStatus: Flow<UploadStatus> = flowOf(UploadStatus.IDLE)

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
    @Before
    fun setup(){
        fakePhotoRepository = FakePhotoRepository()
        fakeNetworkRepository = FakeNetworkRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeUploadScheduler = FakeUploadScheduler()
        fakeDeleteScheduler = FakeDeleteScheduler()
        galleryViewModelTest = GalleryViewModel(
            photosRepo = fakePhotoRepository,
            networkPhotoRepository = fakeNetworkRepository,
            settingsRepository = fakeSettingsRepository,
            uploadScheduler = fakeUploadScheduler,
            deleteScheduler = fakeDeleteScheduler
        )
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkIfPhotoExists_backupEnabledAndPhotoExists_emitsCorrectToast(){
        runTest {
            fakeNetworkRepository.photoExists = true
            fakeSettingsRepository.saveBackupEnabled(true)
            galleryViewModelTest.checkIfPhotoExists(mockk<Uri>())
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.photo_exists_on_the_server_msg, toast.message)
            }
            advanceUntilIdle()
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkIfPhotoExists_backupDisabled_emitsCorrectToast(){
        runTest {
            fakeSettingsRepository.saveBackupEnabled(false)
            galleryViewModelTest.checkIfPhotoExists(mockk<Uri>())
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.local_backup_is_disabled_msg, toast.message)
            }
            advanceUntilIdle()
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkIfPhotoExists_backupEnabledAndNotExists_emitsCorrectToast(){
        runTest {
            fakeNetworkRepository.photoExists = false
            fakeSettingsRepository.saveBackupEnabled(true)
            galleryViewModelTest.checkIfPhotoExists(mockk<Uri>())
            val toastFlow = backgroundScope.launch {
                val toast = galleryViewModelTest.toastMessage.first()
                assertEquals(R.string.file_not_found_in_server_msg, toast.message)
            }
            advanceUntilIdle()
        }
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
            galleryViewModelTest.uploadPhoto(uri)
            advanceUntilIdle()
            assertEquals(uri, fakeUploadScheduler.scheduledUri)
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
            assertFalse(uri == fakeUploadScheduler.scheduledUri)
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
    fun startMultipleUpload_backupEnabledAndMultipleSelectionSet_uploadSchedulerCalled(){
        runTest {
            fakeSettingsRepository.saveBackupEnabled(true)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media3.uri)
            assertTrue(galleryViewModelTest.galleryUiState.value.multipleSelectionSet.isNotEmpty())
            galleryViewModelTest.startMultipleUpload()
            advanceUntilIdle()
            assertEquals(
                setOf(
                    fakePhotoRepository.media1.uri,
                    fakePhotoRepository.media2.uri,
                    fakePhotoRepository.media3.uri),
                fakeUploadScheduler.scheduledUriSet
            )
            assertTrue(galleryViewModelTest.galleryUiState.value.multipleSelectionSet.isEmpty())
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startMultipleUpload_backupDisabled_uploadSchedulerNotCalled(){
        runTest {
            fakeSettingsRepository.saveBackupEnabled(false)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media1.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media2.uri)
            galleryViewModelTest.multipleSelection(fakePhotoRepository.media3.uri)
            galleryViewModelTest.startMultipleUpload()
            advanceUntilIdle()
            assertTrue(fakeUploadScheduler.scheduledUriSet.isEmpty())
        }
    }
}