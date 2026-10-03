package com.example.homealbum.workers

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SyncScheduler {
    val syncStatus: Flow<Boolean>
    fun scheduleSync()
}
private const val SYNC_TAG = "serverSync"
class WorkManagerSyncScheduler(
    private val workManager: WorkManager
) : SyncScheduler {
    override val syncStatus: Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(SYNC_TAG).map { workInfos ->
            when{
                workInfos.any {
                    it.state == WorkInfo.State.RUNNING
                } -> true
                else -> false
            }
    }

    override fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag(SYNC_TAG)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName = SYNC_TAG,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = request
        )
    }
}