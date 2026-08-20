package com.example.homealbum.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.core.net.toUri
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.homealbum.R
import com.example.homealbum.data.ImageScreenRepository
import com.example.homealbum.data.PhotoRepository
import com.example.homealbum.model.MediaItem
import com.example.homealbum.ui.GalleryViewModel
import com.example.homealbum.workers.DeleteScheduler
import com.example.homealbum.workers.DeleteWorker
import com.example.homealbum.workers.UploadScheduler
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class FakePhotoRepository() : PhotoRepository{
    var failToLoad = false
    val media1 = MediaItem( uri = mockk<Uri>(), false, dateTaken = 1L)
    val media2 = MediaItem(uri = mockk<Uri>(), isVideo = false, dateTaken = 2L)
    val media3 = MediaItem(uri = mockk<Uri>(), isVideo = false, dateTaken = 3L)
    override suspend fun getLocalPhotos(): List<MediaItem> {
        if (failToLoad){
            throw SecurityException()
        } else {
            return listOf(
                media1,
                media2,
                media3
            )
        }
    }

    override suspend fun prepareToTrashPhoto(uri: Uri): IntentSenderRequest? {
        TODO("Not yet implemented")
    }

    override suspend fun getThumbnail(
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap? {
        TODO("Not yet implemented")
    }

}
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
//        if (photoExists && !throwException){
//            return Response.success("OK".toResponseBody())
//        } else {
//            return Response.error<ResponseBody>(404, "Not found".toResponseBody())
//        }
    }

    override suspend fun uploadPhoto(fileUri: Uri): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMediaFile(fileUri: Uri): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

}
class FakeUploadScheduler() : UploadScheduler {
    var scheduledUri: Uri? = null
    override fun scheduleUpload(uri: Uri) {
        scheduledUri = uri
    }

}

class FakeDeleteScheduler : DeleteScheduler{
    var scheduledUri: Uri? = null
    override fun scheduleDelete(uri: Uri) {
        scheduledUri = uri
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
            galleryViewModelTest.removeThrashedPhotoFromUi(fakePhotoRepository.media1.uri)
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
    fun removeMediaFromServer_mediaFileInServer_deleteSchedulerCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeNetworkRepository.photoExists = true
            galleryViewModelTest.removeMediaFromServer(uri)
            advanceUntilIdle()
            assertEquals(uri, fakeDeleteScheduler.scheduledUri)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun removeMediaFromServer_mediaFileNotInServer_deleteSchedulerNotCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeNetworkRepository.photoExists = false
            galleryViewModelTest.removeMediaFromServer(uri)
            advanceUntilIdle()
            assertFalse(uri == fakeDeleteScheduler.scheduledUri)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun removeMediaFromServer_serverNotReachable_deleteSchedulerCalled(){
        runTest {
            val uri = mockk<Uri>()
            fakeNetworkRepository.throwException = true
            galleryViewModelTest.removeMediaFromServer(uri)
            advanceUntilIdle()
            assertEquals(uri, fakeDeleteScheduler.scheduledUri)
        }
    }
}