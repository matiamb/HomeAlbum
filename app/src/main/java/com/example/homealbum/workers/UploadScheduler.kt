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
//    fun scheduleUpload(
//        uri: Uri,
//        workerId: String,
//        allowUploadMobileData: Boolean
//    )
    fun scheduleUpload(
        uriList: Set<Uri>,
        workerId: String,
        allowUploadMobileData: Boolean
    )
}
private const val UPLOAD_TAG = "upload"
private const val MULTIPLE_UPLOAD_TAG = "multiple_upload"

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
                } -> UploadStatus.PENDING
                else -> UploadStatus.IDLE
            }
        }

//    override fun scheduleUpload(
//        uri: Uri,
//        workerId: String,
//        allowUploadMobileData: Boolean) {
//        val networkType = if (allowUploadMobileData){
//            NetworkType.CONNECTED
//        } else {
//            NetworkType.UNMETERED
//        }
//        val constraints= Constraints.Builder()
//            .setRequiredNetworkType(networkType)
//            .setRequiresBatteryNotLow(true)
//            .build()
//        val request = OneTimeWorkRequestBuilder<UploadWorker>()
//            .addTag(UPLOAD_TAG)
//            .setConstraints(constraints)
//            .setInputData(
//                workDataOf(UploadWorker.KEY_URI to uri.toString(),
//                    UploadWorker.WORKER_ID to workerId)
//            )
//            .build()
//        workManager.enqueueUniqueWork(
//            uniqueWorkName = "upload_$uri",
//            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
//            request = request
//        )
//    }

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
            .addTag(MULTIPLE_UPLOAD_TAG)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(UploadWorker.WORKER_ID to workerId)
            )
            .build()
        val sortedUriList = uriList.sortedBy { it.toString() }
        workManager.enqueueUniqueWork(
            uniqueWorkName = "multiple_upload_${sortedUriList.hashCode()}",
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request
        )
    }
}