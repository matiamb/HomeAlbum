package com.example.homealbum.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.data.NetworkPhotoRepository
import androidx.core.net.toUri
import okio.IOException

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val KEY_URI = "photo_uri"
    }
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = uriString.toUri()
        return try {
            photoRepository.uploadPhoto(uri)
            Result.success()
        } catch (e: IOException){
            Result.retry()
        } catch (e: Exception){
            Result.failure()
        }
    }
}