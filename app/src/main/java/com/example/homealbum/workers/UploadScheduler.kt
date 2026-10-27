package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

interface UploadScheduler {
//    val uploadStatus: Flow<UploadStatus>
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
//    fun scheduleAutoUpload(
//        workerId: String
//    )
    fun cancelAutoUpload()
    fun buildRequest(workerId: String): OneTimeWorkRequest
}
private const val UPLOAD_TAG = "upload"
const val MANUAL_UPLOAD_TAG = "manual_multiple_upload"
const val AUTOMATIC_UPLOAD_TAG = "automatic_upload"

class WorkManagerUploadScheduler(
    private val workManager: WorkManager
) : UploadScheduler{
//    override val uploadStatus: Flow<UploadStatus> =
//        workManager.getWorkInfosByTagFlow(UPLOAD_TAG).map { workInfos ->
//            when {
//                workInfos.any {
//                    it.state == WorkInfo.State.RUNNING
//                } -> UploadStatus.UPLOADING
//                workInfos.any {
//                    it.state == WorkInfo.State.ENQUEUED ||
//                            it.state == WorkInfo.State.BLOCKED
//                } -> UploadStatus.PENDING
//                else -> UploadStatus.NOT_UPLOADED
//            }
//        }

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

//    override fun scheduleAutoUpload(workerId: String) {
//        val networkType = NetworkType.UNMETERED
//        val constraints= Constraints.Builder()
//            .setRequiredNetworkType(networkType)
//            .setRequiresBatteryNotLow(true)
//            .build()
//        val request = PeriodicWorkRequestBuilder<UploadWorker>(1, TimeUnit.DAYS)
//            .addTag(AUTOMATIC_UPLOAD_TAG)
//            .setConstraints(constraints)
//            .setInputData(
//                workDataOf(UploadWorker.WORKER_ID to workerId,
//                    UploadWorker.JOB_TYPE to AUTOMATIC_UPLOAD_TAG)
//            )
//            .build()
//        workManager.cancelAllWorkByTag(MANUAL_UPLOAD_TAG)
//        workManager.enqueueUniquePeriodicWork(
//            uniqueWorkName = AUTOMATIC_UPLOAD_TAG,
//            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE,
//            request = request
//        )
//    }

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