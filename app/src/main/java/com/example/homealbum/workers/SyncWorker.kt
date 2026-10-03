package com.example.homealbum.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.model.UploadStatus
import okio.IOException

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    private val uploadQueueRepository = (context as HomeAlbumApplication).container.uploadQueueRepository
    override suspend fun doWork(): Result {
        val uriList = uploadQueueRepository.getAllUris()
        Log.d("Mati", uriList.toString())
        uriList.forEach { uri ->
            val hash = photoRepository.getFileHash(uri)
            uploadQueueRepository.updateFileHash(uri, hash)
        }
        try {
            val hashList = uploadQueueRepository.getAllFileHashes()
            //val missingHashes = photoRepository.syncMediaFiles(hashList)
            //uploadQueueRepository.updateMediaItemStatusByHash(missingHashes.missingHashes, UploadStatus.PENDING)
            //val syncedList = hashList.toSet() - missingHashes.missingHashes.toSet()
            //uploadQueueRepository.updateMediaItemStatusByHash(syncedList.toList(), UploadStatus.UPLOADED)
            return Result.success()
        } catch (e: IOException){
            return Result.retry()
        }
    }
}