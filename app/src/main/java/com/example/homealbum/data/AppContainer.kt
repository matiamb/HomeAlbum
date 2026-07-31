package com.example.homealbum.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

interface AppContainer {
    val photoRepository: PhotoRepository
    val offlineSettingsRepository: OfflineSettingsRepository
}
private val Context.dataStore by preferencesDataStore(name = "user_settings")
class DefaultAppContainer(context: Context) : AppContainer{

    override val photoRepository: PhotoRepository by lazy {
        PhotoRepository(context = context)
    }
    override val offlineSettingsRepository: OfflineSettingsRepository by lazy {
        OfflineSettingsRepository(dataStore = context.dataStore)
    }
}