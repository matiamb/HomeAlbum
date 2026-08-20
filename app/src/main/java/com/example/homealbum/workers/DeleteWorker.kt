package com.example.homealbum.workers

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.IOException
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication

class DeleteWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object{
        const val KEY_URI = "photo_uri"
    }
    private val imageScreenRepo = (context as HomeAlbumApplication).container.networkPhotoRepository
    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = uriString.toUri()
        try {
            val serverResponse = imageScreenRepo.deleteMediaFile(uri)
            return when{
                serverResponse.isSuccessful -> {
                    Result.success()
                }

                serverResponse.code() in 500..599 -> {
                    Result.retry()
                }
                else -> {
                    Result.failure()
                }
            }
        } catch (e: IOException){
            return Result.retry()
        }
    }
}