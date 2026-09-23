package com.example.homealbum.data

import android.net.Uri
import android.util.Log
import com.example.homealbum.dao.MediaItemDao
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UploadQueueRepository{
    suspend fun addMediaItemsToDb(mediaItemList: List<MediaItem>)
    suspend fun deleteMediaItemFromDb(uriSet: Set<Uri>)
}
class LocalUploadQueueRepository(val mediaItemDao: MediaItemDao) : UploadQueueRepository{

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
}