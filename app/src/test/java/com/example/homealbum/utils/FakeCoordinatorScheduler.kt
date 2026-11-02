package com.example.homealbum.utils

import com.example.homealbum.workers.AutoUploadCoordinatorScheduler

class FakeCoordinatorScheduler : AutoUploadCoordinatorScheduler {
    var coordinatorScheduled = false
    var workerId = ""
    var coordinatorCancelled = false
    override fun scheduleSyncUpload(workerId: String) {
        coordinatorScheduled = true
        this.workerId = workerId
    }

    override fun cancelCoordinator() {
        coordinatorCancelled = true
    }
}