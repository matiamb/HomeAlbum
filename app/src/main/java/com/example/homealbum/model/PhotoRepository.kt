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
            //MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val columns = arrayOf(
//            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

//        val cameraOnlyImages = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val cameraOnlyImages = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ?"
        val argumentSelection = arrayOf("Camera")

//        val displayOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val displayOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            columns,
            cameraOnlyImages,
            argumentSelection,
            displayOrder
        )?.use { cursor ->
//            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(typeColumn)
                //val photoUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val contentUri: Uri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                }

                photoList.add(contentUri)
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