package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

interface UploadScheduler {
    fun scheduleUpload(
        uri: Uri,
        allowUploadMobileData: Boolean
    )
}

class WorkManagerUploadScheduler(
    private val workManager: WorkManager
) : UploadScheduler{
    override fun scheduleUpload(
        uri: Uri,
        allowUploadMobileData: Boolean) {
        val networkType = if (allowUploadMobileData){
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }
        val constraints= Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("upload")
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.KEY_URI to uri.toString())
            )
            .build()
        //workManager.enqueue(request)
        workManager.enqueueUniqueWork(
            uniqueWorkName = "upload_$uri",
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request
        )
    }
}