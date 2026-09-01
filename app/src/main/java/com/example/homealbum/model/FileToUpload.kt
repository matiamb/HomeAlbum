package com.example.homealbum.model

import okhttp3.MultipartBody

data class FileToUpload(
    val filePart: MultipartBody.Part,
    val fileHash: String
)