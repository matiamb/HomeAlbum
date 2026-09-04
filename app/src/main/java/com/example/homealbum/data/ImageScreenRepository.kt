package com.example.homealbum.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.homealbum.model.FileToUpload
import com.example.homealbum.network.ServerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import retrofit2.Response
import java.security.MessageDigest
import java.util.Collections.emptyList
import java.util.Collections.emptySet

interface ImageScreenRepository {
    suspend fun checkIfPhotoExist(uri: Uri): Response<ResponseBody>
    suspend fun uploadPhoto(fileUri: Uri): Response<ResponseBody>
    suspend fun deleteMediaFile(fileUri: Uri): Response<ResponseBody>
    suspend fun uploadMultipleFiles(uriList: List<Uri>): Response<ResponseBody>
}

class NetworkPhotoRepository(
    private val serverApiService: ServerApiService,
    private val offlineSettingsRepository: SettingsRepository,
    private val context: Context
) : ImageScreenRepository{

    override suspend fun checkIfPhotoExist(uri: Uri): Response<ResponseBody> = withContext(Dispatchers.IO){
        val fileHash = getFileHash(uri)
        val settings = offlineSettingsRepository.userSettingsFlow.first()
        val serverIp = settings.serverIp
        val dynamicUrl = serverIp.trimEnd('/')
        val endpoint = "http://$dynamicUrl/api/v1/media/exists"
        serverApiService.checkIfPhotoExist(endpoint, fileHash)
    }

    override suspend fun uploadPhoto(
        fileUri: Uri
    ): Response<ResponseBody> = withContext(Dispatchers.IO){
        val settings = offlineSettingsRepository.userSettingsFlow.first()
        val serverIp = settings.serverIp
        val folderName = settings.serverFolderName
        val fileHash = getFileHash(fileUri)
        if (serverIp.isBlank()){
            throw IllegalArgumentException("Please enter a valid ip")
        }
        val dynamicUrl = serverIp.trimEnd('/')
        val endpoint = "http://$dynamicUrl/api/v1/media/upload"
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: "application/octet_stream"
        val fileName = getFileNameFromUri(context, fileUri) ?: "unnamed_file"
        val folderRequestBody = folderName.toRequestBody("text/plain".toMediaTypeOrNull())


        val mediaRequestBody = object : RequestBody() {
            override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    sink.writeAll(inputStream.source())
                }
            }

        }
        val filePart = MultipartBody.Part.createFormData("file", fileName, mediaRequestBody)

        serverApiService.uploadPhoto(
            file = filePart,
            savedUrl = endpoint,
            fileHash = fileHash,
            folderName = folderRequestBody
        )
    }

    override suspend fun deleteMediaFile(fileUri: Uri): Response<ResponseBody> = withContext(Dispatchers.IO) {
        val serverIp = offlineSettingsRepository.userSettingsFlow.first().serverIp
        val endpoint = "http://$serverIp/api/v1/media/delete"
        val fileHash = getFileHash(fileUri)
        serverApiService.deleteMediaFile(endpoint, fileHash)
    }

    override suspend fun uploadMultipleFiles(uriList: List<Uri>) = withContext(Dispatchers.IO){
        val settings = offlineSettingsRepository.userSettingsFlow.first()
        val serverIp = settings.serverIp
        val folderName = settings.serverFolderName
        val fileToUploadList = mutableListOf<FileToUpload>()
        val folderRequestBody = folderName.toRequestBody("text/plain".toMediaTypeOrNull())

        val dynamicUrl = serverIp.trimEnd('/')
        val endpoint = "http://$dynamicUrl/api/v1/media/multipleUpload"
        if (serverIp.isBlank()){
            throw IllegalArgumentException("Please enter a valid ip")
        }
        for (uri in uriList){
            val fileHash = getFileHash(uri)
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet_stream"
            val fileName = getFileNameFromUri(context, uri) ?: "unnamed_file"

            val mediaRequestBody = object : RequestBody() {
                override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        sink.writeAll(inputStream.source())
                    }
                }

            }
            val filePart = MultipartBody.Part.createFormData("file", fileName, mediaRequestBody)
            fileToUploadList.add(
                FileToUpload(
                    filePart = filePart,
                    fileHash = fileHash!!
                )
            )
        }
        val files = fileToUploadList.map { it.filePart }
        val hashes = fileToUploadList.map { it.fileHash }
        serverApiService.uploadMultipleFiles(
            fileList = files,
            savedUrl = endpoint,
            fileHashList = hashes,
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
    private suspend fun getFileHash(uri: Uri): String?{
        return withContext(Dispatchers.IO){
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1){
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                val hashBytes = digest.digest()
                hashBytes.joinToString(""){"%02x".format(it)}
            } catch (e: Exception){
                e.printStackTrace()
                null
            }
        }
    }

}