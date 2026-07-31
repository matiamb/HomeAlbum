package com.example.homealbum.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

interface ServerApiService{
    @GET("llamada/ami/api")
    suspend fun checkIfPhotoExist(
        @Query("hash") fileHash: String
    ): Response<Boolean>
    @Multipart
    @POST
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Url savedUrl: String,
        @Part("mimeType") mimeType: RequestBody,
        @Part("fileName") fileName: RequestBody,
        @Part("folderName") folderName: RequestBody
    )
}