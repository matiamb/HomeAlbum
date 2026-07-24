package com.example.homealbum.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {
    suspend fun getLocalPhotos(): List<Uri> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<Uri>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ){
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val columns = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val cameraOnlyImages = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val argumentSelection = arrayOf("Camera")

        val displayOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            columns,
            cameraOnlyImages,
            argumentSelection,
            displayOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val photoUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                photoList.add(photoUri)
            }
        }
        return@withContext photoList
    }
    suspend fun prepareToTrashPhoto(uri: Uri): IntentSenderRequest? = withContext(Dispatchers.IO){
        val contentResolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createTrashRequest(
                contentResolver,
                listOf(uri),
                true
            )
            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        } else {
            try {
                contentResolver.delete(uri,null,null)
            } catch (e: SecurityException) {
                throw e
            }
            null
        }
    }
}