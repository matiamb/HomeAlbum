package com.example.homealbum.utils

import androidx.work.OneTimeWorkRequest
import com.example.homealbum.workers.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSyncScheduler : SyncScheduler {
    var syncSchedulerCalled = false
    override val syncStatus: Flow<Boolean>
        get() = flowOf(false)

    override fun scheduleSync() {
        syncSchedulerCalled = true
    }

    override fun cancelSync() {
        TODO("Not yet implemented")
    }

    override fun buildRequest(): OneTimeWorkRequest {
        TODO("Not yet implemented")
    }
}