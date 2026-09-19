package com.example.homealbum.utils

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import com.example.homealbum.data.PhotoRepository
import com.example.homealbum.model.MediaItem
import io.mockk.mockk

class FakePhotoRepository() : PhotoRepository{
    var failToLoad = false
    val media1 = MediaItem( uri = mockk<Uri>(), false, dateTaken = 1L, thumbnail = mockk())
    val media2 = MediaItem(uri = mockk<Uri>(), isVideo = false, dateTaken = 2L, thumbnail = mockk())
    val media3 = MediaItem(uri = mockk<Uri>(), isVideo = false, dateTaken = 3L, thumbnail = mockk())
    var restoreResult: IntentSenderRequest? = null
    var failRestore = false
    var restoredUris: Set<Uri> = emptySet()
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

    override suspend fun prepareToTrashPhoto(uriSet: Set<Uri>): IntentSenderRequest? {
        TODO("Not yet implemented")
    }

    override suspend fun getThumbnail(
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap? {
        TODO("Not yet implemented")
    }

    override suspend fun getTrashedFiles(): List<MediaItem> {
        return if (failToLoad){
            throw SecurityException()
        } else {
            listOf(
                media1,
                media2,
                media3
            )
        }
    }

    override suspend fun restoreFiles(uriSet: Set<Uri>): IntentSenderRequest? {
        restoredUris = uriSet
        if (failRestore){
            throw SecurityException()
        } else {
            return restoreResult
        }
    }

}