package com.example.homealbum.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.homealbum.network.ServerApiService
import retrofit2.Retrofit

interface AppContainer {
    val offlinePhotoRepository: OfflinePhotoRepository
    val offlineSettingsRepository: OfflineSettingsRepository
    val networkPhotoRepository: NetworkPhotoRepository
}
private val Context.dataStore by preferencesDataStore(name = "user_settings")
class DefaultAppContainer(context: Context) : AppContainer{

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http//localhost")
            .build()
    }
    private val retrofitService: ServerApiService by lazy {
        retrofit.create(ServerApiService::class.java)
    }

    override val offlinePhotoRepository: OfflinePhotoRepository by lazy {
        OfflinePhotoRepository(context = context)
    }
    override val offlineSettingsRepository: OfflineSettingsRepository by lazy {
        OfflineSettingsRepository(dataStore = context.dataStore)
    }
    override val networkPhotoRepository: NetworkPhotoRepository by lazy {
        NetworkPhotoRepository(
            serverApiService = retrofitService,
            offlineSettingsRepository = offlineSettingsRepository,
            context = context
        )
    }
}