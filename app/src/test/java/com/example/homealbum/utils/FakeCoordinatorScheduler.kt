package com.example.homealbum.utils

import com.example.homealbum.workers.AutoUploadCoordinatorScheduler

class FakeCoordinatorScheduler : AutoUploadCoordinatorScheduler {
    override fun scheduleSyncUpload(workerId: String) {
        TODO("Not yet implemented")
    }

    override fun cancelCoordinator() {
        TODO("Not yet implemented")
    }
}