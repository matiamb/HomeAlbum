package com.example.homealbum.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.model.UploadStatus
import okio.IOException
import retrofit2.HttpException

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    private val uploadQueueRepository = (context as HomeAlbumApplication).container.uploadQueueRepository
    override suspend fun doWork(): Result {
        try {
            val uriList = uploadQueueRepository.getAllUris()
            Log.d("Mati", uriList.toString())
            uriList.forEach { uri ->
                val hash = photoRepository.getFileHash(uri)
                if (hash != null){
                    uploadQueueRepository.updateFileHash(uri, hash)
                } else {
                    uploadQueueRepository.updateMediaItemStatusByUri(uri.toString(), UploadStatus.FAILED)
                }
            }
            val hashList = uploadQueueRepository.getAllFileHashes()
            if (hashList.isEmpty()){
                return Result.success()
            }
            val missingHashes = photoRepository.syncMediaFiles(hashList)
            if (missingHashes.isNotEmpty()){
                uploadQueueRepository.updateMediaItemStatusByHash(
                    missingHashes, UploadStatus.NOT_UPLOADED)
                }
            val syncedList = hashList.toSet() - missingHashes.toSet()
            if (syncedList.isNotEmpty()){
                uploadQueueRepository.updateMediaItemStatusByHash(
                    syncedList.toList(), UploadStatus.UPLOADED)
            }
            return Result.success()
        } catch (e: IOException){
            return Result.retry()
        } catch (e: HttpException){
            return Result.failure()
        }
    }
}