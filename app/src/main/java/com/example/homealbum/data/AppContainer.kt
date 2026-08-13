package com.example.homealbum.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.example.homealbum.network.ServerApiService
import com.example.homealbum.workers.UploadScheduler
import com.example.homealbum.workers.WorkManagerUploadScheduler
import retrofit2.Retrofit

interface AppContainer {
    val offlinePhotoRepository: OfflinePhotoRepository
    val offlineSettingsRepository: OfflineSettingsRepository
    val networkPhotoRepository: NetworkPhotoRepository
    val workManager: WorkManager
    val uploadScheduler: UploadScheduler
}
private val Context.dataStore by preferencesDataStore(name = "user_settings")
class DefaultAppContainer(context: Context) : AppContainer{
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost")
            .build()
    }
    private val retrofitService: ServerApiService by lazy {
        retrofit.create(ServerApiService::class.java)
    }

    override val offlinePhotoRepository: OfflinePhotoRepository by lazy {
        OfflinePhotoRepository(context = context)
    }
    override val offlineSettingsRepository: OfflineSettingsRepository by lazy {
        OfflineSettingsRepository(
            dataStore = context.dataStore,
            serverApiService = retrofitService
        )
    }
    override val networkPhotoRepository: NetworkPhotoRepository by lazy {
        NetworkPhotoRepository(
            serverApiService = retrofitService,
            offlineSettingsRepository = offlineSettingsRepository,
            context = context
        )
    }
    override val workManager: WorkManager by lazy{
        WorkManager.getInstance(context)
    }
    override val uploadScheduler: UploadScheduler by lazy {
        WorkManagerUploadScheduler(workManager = workManager)
    }
}