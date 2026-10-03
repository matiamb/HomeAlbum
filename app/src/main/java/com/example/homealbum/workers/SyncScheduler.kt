package com.example.homealbum.workers

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

interface SyncScheduler {
    fun scheduleSync()
}
private const val SYNC_TAG = "serverSync"
class WorkManagerSyncScheduler(
    private val workManager: WorkManager
) : SyncScheduler {

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