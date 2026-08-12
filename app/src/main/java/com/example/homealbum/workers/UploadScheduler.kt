package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

interface UploadScheduler {
    fun scheduleUpload(uri: Uri)
}

class WorkManagerUploadScheduler(
    private val workManager: WorkManager
) : UploadScheduler{
    override fun scheduleUpload(uri: Uri) {
        val constraints= Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("upload")
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.KEY_URI to uri.toString())
            )
            .build()
        workManager.enqueue(request)
    }
}