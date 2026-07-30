package com.example.homealbum.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.result.IntentSenderRequest
import com.example.homealbum.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {
    /**
     * This method will request android to retrieve the image and videos it has from
     * the Camera folder, attempting to grab only the images taken by the user with the camera.
     */

    suspend fun getLocalPhotos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )


        val cameraOnlyFiles = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ?"
        val argumentSelection = arrayOf("Camera")

        val displayOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            columns,
            cameraOnlyFiles,
            argumentSelection,
            displayOrder
        )?.use { cursor ->
//            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(typeColumn)
                val mediaItem: MediaItem =
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        MediaItem(
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            ), isVideo = false
                        )
                    } else {
                        MediaItem(
                            uri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            isVideo = true
                        )
                    }

                photoList.add(mediaItem)
            }
        }
        return@withContext photoList
    }
    suspend fun prepareToTrashPhoto(uri: Uri): IntentSenderRequest? = withContext(Dispatchers.IO) {
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
                contentResolver.delete(uri, null, null)
            } catch (e: SecurityException) {
                throw e
            }
            null
        }
    }

    /**
     * This function receives a uri of the image to search for the thumbnail in android
     * using contentResolver.loadThumbnail this only works for versions newer than Q.
     * At this moment for older versions it will return null.
     * It is a suspend function since it is executing IO and storage search actions
     */
    suspend fun getThumbnail(
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(uri, Size(width, height), null)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}