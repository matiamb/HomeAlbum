package com.example.homealbum.data

import android.net.Uri
import android.util.Log
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
    suspend fun updateMediaItemStatus(uri: Uri, status: UploadStatus, fileHash: String? = "")
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
                    uploadStatus = UploadStatus.PENDING
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
        fileHash: String?
    ) {
        withContext(Dispatchers.IO){
            mediaItemDao.updateMediaItemStatus(uri.toString(), status, fileHash)
        }
    }
}