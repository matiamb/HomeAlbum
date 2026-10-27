package com.example.homealbum.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import java.util.UUID

class PeriodicUploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val WORKER_ID = "worker_id"
    }
    private val syncScheduler = (context as HomeAlbumApplication).container.syncScheduler
    private val uploadScheduler = (context as HomeAlbumApplication).container.uploadScheduler
    override suspend fun doWork(): Result {
        val workerId = inputData.getString(WORKER_ID) ?: return Result.failure()
        val syncRequest = syncScheduler.buildRequest()
        val uploadRequest = uploadScheduler.buildRequest(workerId)
        WorkManager.getInstance(applicationContext)
            .beginUniqueWork(
                "CoordinatorWorker",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            ).then(
                uploadRequest
            ).enqueue()
        return Result.success()
    }
}