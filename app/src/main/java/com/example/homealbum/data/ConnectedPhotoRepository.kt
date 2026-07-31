package com.example.homealbum.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.homealbum.network.ServerApiService
import kotlinx.coroutines.flow.first
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import retrofit2.Response

interface ConnectedPhotoRepository {
    suspend fun checkIfPhotoExist(fileHash: String): Response<Boolean>
    suspend fun uploadPhoto(
        fileUri: Uri
    )
}

class NetworkPhotoRepository(
    private val serverApiService: ServerApiService,
    private val offlineSettingsRepository: OfflineSettingsRepository,
    private val context: Context
) : ConnectedPhotoRepository{
    override suspend fun checkIfPhotoExist(fileHash: String): Response<Boolean> {
        return serverApiService.checkIfPhotoExist(fileHash)
    }

    override suspend fun uploadPhoto(
        fileUri: Uri
    ) {
        val settings = offlineSettingsRepository.userSettingsFlow.first()
        val serverIp = settings.serverIp
        val folderName = settings.serverFolderName
        if (serverIp.isBlank()){
            throw IllegalArgumentException("Please enter a valid ip")
        }
        val baseUrl = serverIp.trimEnd('/')
        val endpoint = "$baseUrl/upload"
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: "application/octet_stream"
        val fileName = getFileNameFromUri(context, fileUri) ?: "unnamed_file"
        val folderRequestBody = folderName.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameRequestBody = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
        val mimeTypeRequestBody = mimeType.toRequestBody("text/plain".toMediaTypeOrNull())

        val mediaRequestBody = object : RequestBody() {
            override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    sink.writeAll(inputStream.source())
                }
            }

        }
        val filePart = MultipartBody.Part.createFormData("file", fileName, mediaRequestBody)//the file string has to be the same as the one the server expects

        serverApiService.uploadPhoto(
            file = filePart,
            savedUrl = endpoint,
            mimeType = mimeTypeRequestBody,
            fileName = nameRequestBody,
            folderName = folderRequestBody
        )
    }

    private fun getFileNameFromUri(
        context: Context,
        uri: Uri
    ): String?{
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()){
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1){
                    return it.getString(index)
                }
            }
        }
        return null
    }

}