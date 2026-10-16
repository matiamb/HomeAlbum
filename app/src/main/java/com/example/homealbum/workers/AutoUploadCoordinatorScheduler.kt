package com.example.homealbum.workers

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

interface AutoUploadCoordinatorScheduler {
    fun scheduleSyncUpload(workerId: String)
    fun cancelCoordinator()
}
private const val PERIODIC_COORDINATOR = "Periodic_Coordinator"
class WorkManagerAutoUploadCoordinatorScheduler(
    private val workManager: WorkManager
) : AutoUploadCoordinatorScheduler{
    override fun scheduleSyncUpload(workerId: String) {
        val request = PeriodicWorkRequestBuilder<PeriodicUploadWorker>(1, TimeUnit.DAYS)
            .setInputData(
                workDataOf(PeriodicUploadWorker.WORKER_ID to workerId)
            )
            .addTag(PERIODIC_COORDINATOR)
            .build()
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName = PERIODIC_COORDINATOR,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE,
            request = request
        )
    }

    override fun cancelCoordinator() {
        workManager.cancelAllWorkByTag(AUTOMATIC_UPLOAD_TAG)
        workManager.cancelAllWorkByTag(SYNC_TAG)
        workManager.cancelAllWorkByTag(PERIODIC_COORDINATOR)
    }
}