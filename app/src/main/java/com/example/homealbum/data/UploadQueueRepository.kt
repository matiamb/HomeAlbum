package com.example.homealbum.data

import android.net.Uri
import androidx.core.net.toUri
import com.example.homealbum.dao.MediaItemDao
import com.example.homealbum.model.FileUploadInfo
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface UploadQueueRepository{
    val uploadInfo: Flow<List<FileUploadInfo>>
    suspend fun addMediaItemsToDb(mediaItemList: List<MediaItem>)
    suspend fun deleteMediaItemFromDb(uriSet: Set<Uri>)
    suspend fun updateMediaItemStatus(uri: Uri, status: UploadStatus, fileHash: String?, workerId: String)
    suspend fun updateMediaItemStatus(uri: Uri, status: UploadStatus, workerId: String)
    suspend fun getPendingUploads(uploadStatus: UploadStatus, workerId: String): List<Uri>
    suspend fun prepareFilesForUpload(uriList: List<Uri>, status: UploadStatus, workerId: String)
    suspend fun getMediaItemsByWorker(workerId: String): List<Uri>
    suspend fun cleanUpStuckFilesByWorker(uriList: List<Uri>, status: UploadStatus, workerId: String)
    suspend fun getAllUris(): List<Uri>
    suspend fun updateFileHash(uri: Uri, fileHash: String?)
    suspend fun getAllFileHashes(): List<String>
    suspend fun updateMediaItemStatusByHash(fileHashList: List<String>, status: UploadStatus)
    suspend fun updateMediaItemStatusByUri(uri: String, status: UploadStatus)
}
class LocalUploadQueueRepository(val mediaItemDao: MediaItemDao) : UploadQueueRepository{
    override val uploadInfo: Flow<List<FileUploadInfo>> =
        mediaItemDao.observeAllMediaItems().map { entities ->
            entities.map {
                FileUploadInfo(
                    uri = it.uri.toUri(),
                    uploadStatus = it.uploadStatus
                )
            }
        }
    override suspend fun addMediaItemsToDb(mediaItemList: List<MediaItem>) {
        withContext(Dispatchers.IO){
            val mediaItemEntityList = mutableListOf<MediaItemEntity>()
            for (item in mediaItemList){
                mediaItemEntityList.add(MediaItemEntity(
                    uri = item.uri.toString(),
                    uploadStatus = UploadStatus.NOT_UPLOADED
                ))
            }
            mediaItemDao.insertMediaItems(mediaItemEntityList)
        }
    }

    override suspend fun deleteMediaItemFromDb(uriSet: Set<Uri>) {
        withContext(Dispatchers.IO){
            val uriSetString = mutableListOf<String>()
            for(uri in uriSet){
                uriSetString.add(uri.toString())
            }
            //val mediaItemEntity = mediaItemDao.getMediaItemByUri(uri.toString())
            //Log.d("Mati", mediaItemEntity.toString())
                mediaItemDao.deleteMediaItem(uriSetString)
        }
    }

    override suspend fun updateMediaItemStatus(
        uri: Uri,
        status: UploadStatus,
        fileHash: String?,
        workerId: String
    ) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateMediaItemStatus(uri.toString(), status, fileHash, workerId)
        }
    }

    override suspend fun updateMediaItemStatus(
        uri: Uri,
        status: UploadStatus,
        workerId: String
    ) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateMediaItemStatus(uri.toString(), status, workerId)
        }
    }

    override suspend fun getPendingUploads(uploadStatus: UploadStatus, workerId: String): List<Uri> =
        withContext(Dispatchers.IO){
            val mediaItemsEntities = mediaItemDao.getMediaItemsForWorker(uploadStatus, workerId)
            val uriList = mutableListOf<Uri>()
            for (item in mediaItemsEntities){
                uriList.add(item.uri.toUri())
            }
            return@withContext uriList
        }
    override suspend fun prepareFilesForUpload(
        uriList: List<Uri>,
        status: UploadStatus,
        workerId: String
    ) {
        val uriListString = mutableListOf<String>()
        uriList.forEach { uri ->
            uriListString.add(uri.toString())
        }
        withContext(Dispatchers.IO){
            mediaItemDao.prepareFilesForUpload(uriListString, status, workerId)
        }
    }

    override suspend fun getMediaItemsByWorker(workerId: String): List<Uri> = withContext(
        Dispatchers.IO) {
        val mediaEntities = mediaItemDao.getMediaItemsByWorker(workerId)
        val uriList = mutableListOf<Uri>()
        for (item in mediaEntities){
            uriList.add(item.uri.toUri())
        }
        return@withContext uriList
    }

    override suspend fun cleanUpStuckFilesByWorker(
        uriList: List<Uri>,
        status: UploadStatus,
        workerId: String
    ) {
        val uriListString = mutableListOf<String>()
        for (uri in uriList){
            uriListString.add(uri.toString())
        }
        withContext(Dispatchers.IO){
            mediaItemDao.cleanupStuckFilesByWorker(uriListString, status, workerId)
        }
    }

    override suspend fun getAllUris(): List<Uri> = withContext(Dispatchers.IO){
        val uriStrings = mediaItemDao.getAllUris()
        val uriList = mutableListOf<Uri>()
        for (uriString in uriStrings){
            uriList.add(uriString.toUri())
        }
        uriList
    }

    override suspend fun updateFileHash(uri: Uri, fileHash: String?) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateFileHash(uri.toString(), fileHash)
        }
    }

    override suspend fun getAllFileHashes(): List<String> = withContext(Dispatchers.IO){
        mediaItemDao.getAllFileHashes()
    }

    override suspend fun updateMediaItemStatusByHash(
        fileHashList: List<String>,
        status: UploadStatus
    ) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateMediaItemStatusByFileHash(fileHashList, status)
        }
    }

    override suspend fun updateMediaItemStatusByUri(
        uri: String,
        status: UploadStatus
    ) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateMediaItemStatusByUri(uri, status)
        }
    }
}