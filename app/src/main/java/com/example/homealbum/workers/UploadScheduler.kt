package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UploadScheduler {
    val uploadStatus: Flow<UploadStatus>
    fun scheduleUpload(
        uri: Uri,
        allowUploadMobileData: Boolean
    )
}
private const val UPLOAD_TAG = "upload"

class WorkManagerUploadScheduler(
    private val workManager: WorkManager
) : UploadScheduler{
    override val uploadStatus: Flow<UploadStatus> =
        workManager.getWorkInfosByTagFlow(UPLOAD_TAG).map { workInfos ->
            when {
                workInfos.any {
                    it.state == WorkInfo.State.RUNNING
                } -> UploadStatus.UPLOADING
                workInfos.any {
                    it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.BLOCKED
                } -> UploadStatus.SCHEDULED
                else -> UploadStatus.IDLE
            }
        }

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
            .addTag(UPLOAD_TAG)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.KEY_URI to uri.toString())
            )
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName = "upload_$uri",
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request
        )
    }
}