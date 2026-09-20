package com.example.homealbum.viewmodel

import androidx.activity.result.IntentSenderRequest
import com.example.homealbum.ui.TrashViewModel
import com.example.homealbum.utils.FakePhotoRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TrashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var fakePhotoRepository: FakePhotoRepository
    private lateinit var trashViewModelTest: TrashViewModel
    @Before
    fun setup(){
        fakePhotoRepository = FakePhotoRepository()
        trashViewModelTest = TrashViewModel(photoRepository = fakePhotoRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadTrashedFiles_repoLoadsFiles_uiStateUpdatedWithFiles(){
        runTest {
            fakePhotoRepository.failToLoad = false
            trashViewModelTest.loadTrashedFiles()
            advanceUntilIdle()
            assertTrue(trashViewModelTest.trashUiState.value.trashedFilesList.isNotEmpty())
            assertFalse(trashViewModelTest.trashUiState.value.isRefreshing)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadTrashedFiles_repoFailsToLoadFiles_uiStateIsRefreshingFalse(){
        runTest{
            fakePhotoRepository.failToLoad = true
            trashViewModelTest.loadTrashedFiles()
            advanceUntilIdle()
            assertFalse(trashViewModelTest.trashUiState.value.isRefreshing)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun restoreFiles_repositoryReceivesFiles_andCallbackReceivesIntent(){
        runTest {
            fakePhotoRepository.failRestore = false
            val expectedRequest = mockk<IntentSenderRequest>()
            fakePhotoRepository.restoreResult = expectedRequest
            var receivedRequest: IntentSenderRequest? = null
            val setToRestore = setOf(
                fakePhotoRepository.media1.uri,
                fakePhotoRepository.media2.uri
            )
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media1.uri)
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media2.uri)
            trashViewModelTest.restoreFiles { request ->
                receivedRequest = request
            }
            advanceUntilIdle()
            assertTrue(receivedRequest == expectedRequest)
            assertEquals(setToRestore, fakePhotoRepository.restoredUris)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun enableMultipleSelection_updatesUiStateSetCorrectly(){
        runTest {
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media1.uri)
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media2.uri)
            val expectedSet = setOf(
                fakePhotoRepository.media1.uri,
                fakePhotoRepository.media2.uri
            )
            assertEquals(expectedSet, trashViewModelTest.trashUiState.value.multipleSelectionSet)
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media1.uri)
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media2.uri)
            assertTrue(trashViewModelTest.trashUiState.value.multipleSelectionSet.isEmpty())
        }
    }
    @Test
    fun clearMultipleSelection_clearsUiStateCorrectly(){
        runTest {
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media1.uri)
            trashViewModelTest.enableMultipleSelection(fakePhotoRepository.media2.uri)
            trashViewModelTest.clearMultipleSelection()
            assertTrue(trashViewModelTest.trashUiState.value.multipleSelectionSet.isEmpty())
        }
    }
}