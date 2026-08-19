package com.example.homealbum.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.example.homealbum.network.ServerApiService
import com.example.homealbum.workers.DeleteScheduler
import com.example.homealbum.workers.UploadScheduler
import com.example.homealbum.workers.WorkManagerDeleteScheduler
import com.example.homealbum.workers.WorkManagerUploadScheduler
import retrofit2.Retrofit

interface AppContainer {
    val offlinePhotoRepository: PhotoRepository
    val offlineSettingsRepository: SettingsRepository
    val networkPhotoRepository: ImageScreenRepository
    val workManager: WorkManager
    val uploadScheduler: UploadScheduler
    val deleteScheduler: DeleteScheduler
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

    override val offlinePhotoRepository: PhotoRepository by lazy {
        OfflinePhotoRepository(context = context)
    }
    override val offlineSettingsRepository: SettingsRepository by lazy {
        OfflineSettingsRepository(
            dataStore = context.dataStore,
            serverApiService = retrofitService
        )
    }
    override val networkPhotoRepository: ImageScreenRepository by lazy {
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
    override val deleteScheduler: DeleteScheduler by lazy {
        WorkManagerDeleteScheduler(workManager = workManager)
    }
}