package com.example.homealbum.model


import okhttp3.ResponseBody
import retrofit2.Response

data class UploadResult (
    val fileHash: String?,
    var serverResponse: Response<ResponseBody>? = null
)