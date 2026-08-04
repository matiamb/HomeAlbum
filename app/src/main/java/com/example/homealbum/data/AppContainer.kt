package com.example.homealbum.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.homealbum.network.ServerApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import retrofit2.Retrofit

interface AppContainer {
    val offlinePhotoRepository: OfflinePhotoRepository
    val offlineSettingsRepository: OfflineSettingsRepository
    val networkPhotoRepository: NetworkPhotoRepository
}
private val Context.dataStore by preferencesDataStore(name = "user_settings")
class DefaultAppContainer(context: Context) : AppContainer{

//    private val mockServerInterceptor = Interceptor { chain ->
//        val request = chain.request()
//        Thread.sleep(1000)
//        Response.Builder()
//            .code(404)
//            .request(request)
//            .message("Ok")
//            .protocol(Protocol.HTTP_1_1)
//            .build()
//    }
//    private val okHttpClient = OkHttpClient.Builder()
//        .addInterceptor(mockServerInterceptor)
//        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost")
            //.client(okHttpClient)
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
}