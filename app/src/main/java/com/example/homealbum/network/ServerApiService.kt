package com.example.homealbum.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

interface ServerApiService{
    @GET
    suspend fun checkIfPhotoExist(
        @Url dynamicUrl: String,
        @Query("fileHash") fileHash: String?
    ): Response<ResponseBody>
    @Multipart
    @POST
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Url savedUrl: String,
        @Query("fileHash") fileHash: String?,
        //@Part("mimeType") mimeType: RequestBody,
        //@Part("fileName") fileName: RequestBody,
        @Part("folderName") folderName: RequestBody
    ): Response<ResponseBody>
    @GET
    suspend fun checkServerConnection(
        @Url savedUrl: String
    ): Response<ResponseBody>
    @DELETE
    suspend fun deleteMediaFile(
        @Url savedUrl: String,
        @Query("fileHash") fileHash: String?
    ): Response<ResponseBody>
}