package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

interface UploadScheduler {
    fun scheduleUpload(
        uriList: Set<Uri>,
        workerId: String,
        allowUploadMobileData: Boolean
    )
    fun cancelAutoUpload()
    fun buildRequest(workerId: String): OneTimeWorkRequest
}
const val MANUAL_UPLOAD_TAG = "manual_multiple_upload"
const val AUTOMATIC_UPLOAD_TAG = "automatic_upload"

class WorkManagerUploadScheduler(
    private val workManager: WorkManager
) : UploadScheduler{
    override fun scheduleUpload(
        uriList: Set<Uri>,
        workerId: String,
        allowUploadMobileData: Boolean
    ) {
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
            .addTag(MANUAL_UPLOAD_TAG)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.WORKER_ID to workerId
                , UploadWorker.JOB_TYPE to MANUAL_UPLOAD_TAG)
            )
            .build()
        val sortedUriList = uriList.sortedBy { it.toString() }
        workManager.enqueueUniqueWork(
            uniqueWorkName = "multiple_upload_${sortedUriList.hashCode()}",
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request
        )
    }

    override fun cancelAutoUpload() {
        workManager.cancelAllWorkByTag(AUTOMATIC_UPLOAD_TAG)
    }

    override fun buildRequest(workerId: String): OneTimeWorkRequest {
        val networkType = NetworkType.UNMETERED
        val constraints= Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()
        return OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag(AUTOMATIC_UPLOAD_TAG)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.WORKER_ID to workerId,
                    UploadWorker.JOB_TYPE to AUTOMATIC_UPLOAD_TAG)
            )
            .build()
    }
}