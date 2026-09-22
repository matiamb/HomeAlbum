package com.example.homealbum.workers

import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

interface DeleteScheduler{
    fun scheduleDelete(uriSet: Set<Uri>)
}

class WorkManagerDeleteScheduler(
    private val workManager: WorkManager
) : DeleteScheduler{
    override fun scheduleDelete(uriSet: Set<Uri>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DeleteWorker>()
            .addTag("delete")
            .setConstraints(constraints)
            .setInputData(
                workDataOf(DeleteWorker.KEY_URI to uriSet.map { it.toString() }.toTypedArray())
            )
            .build()
        workManager.enqueue(request)
    }
}