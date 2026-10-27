package com.example.homealbum.utils

import android.net.Uri
import com.example.homealbum.data.UploadQueueRepository
import com.example.homealbum.model.FileUploadInfo
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.MediaItemUriHash
import com.example.homealbum.model.UploadStatus
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUploadQueueRepo : UploadQueueRepository {
    override val uploadInfo: Flow<List<FileUploadInfo>> = flowOf(listOf(
        FileUploadInfo(mockk<Uri>(), UploadStatus.PENDING),
        FileUploadInfo(mockk<Uri>(), UploadStatus.PENDING),
        FileUploadInfo(mockk<Uri>(), UploadStatus.PENDING)
    ))

    override suspend fun addMediaItemsToDb(mediaItemList: List<MediaItem>) {

    }

    override suspend fun deleteMediaItemFromDb(uriSet: Set<Uri>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMediaItemStatus(
        uri: Uri,
        status: UploadStatus,
        fileHash: String?,
        workerId: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMediaItemStatus(
        uri: Uri,
        status: UploadStatus,
        workerId: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getPendingUploads(
        uploadStatus: UploadStatus,
        workerId: String
    ): List<MediaItemUriHash> {
        TODO("Not yet implemented")
    }

    override suspend fun prepareFilesForUpload(
        uriList: List<Uri>,
        status: UploadStatus,
        workerId: String
    ) {

    }

    override suspend fun getMediaItemsByWorker(workerId: String): List<Uri> {
        TODO("Not yet implemented")
    }

    override suspend fun cleanUpStuckFilesByWorker(
        uriList: List<Uri>,
        status: UploadStatus,
        workerId: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUris(): List<Uri> {
        TODO("Not yet implemented")
    }

    override suspend fun updateFileHash(uri: Uri, fileHash: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllFileHashes(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun updateMediaItemStatusByHash(
        fileHashList: List<String>,
        status: UploadStatus
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMediaItemStatusByUri(
        uri: String,
        status: UploadStatus
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun prepareFilesForAutoUpload(
        status: UploadStatus,
        workerId: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun restoreStatusAutoUploadCancelled(status: UploadStatus) {
        TODO("Not yet implemented")
    }
}