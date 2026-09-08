package com.example.homealbum.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import com.example.homealbum.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PhotoRepository{
    suspend fun getLocalPhotos(): List<MediaItem>
    suspend fun prepareToTrashPhoto(uriSet: Set<Uri>): IntentSenderRequest?
    suspend fun getThumbnail(
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap?
    suspend fun getTrashedFiles(): List<MediaItem>
}
class OfflinePhotoRepository(private val context: Context) : PhotoRepository {
    /**
     * This method will request android to retrieve the image and videos it has from
     * the Camera folder, attempting to grab only the images taken by the user with the camera.
     */

    override suspend fun getLocalPhotos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN
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
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(typeColumn)
                val dateTaken = cursor.getLong(dateColumn)
                val mediaItem: MediaItem =
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        MediaItem(
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            isVideo = false,
                            dateTaken = dateTaken
                        )
                    } else {
                        MediaItem(
                            uri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            isVideo = true,
                            dateTaken = dateTaken
                        )
                    }

                photoList.add(mediaItem)
            }
        }
        return@withContext photoList
    }
    override suspend fun prepareToTrashPhoto(uriSet: Set<Uri>): IntentSenderRequest? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createTrashRequest(
                contentResolver,
                uriSet,
                true
            )
            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        } else {
            try {
                for (uri in uriSet){
                    contentResolver.delete(uri, null, null)
                }
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
    override suspend fun getThumbnail(
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

    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun getTrashedFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val trashedFilesList = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN
        )

        val queryArgs = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION,"${MediaStore.Files.FileColumns.IS_TRASHED} = ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("1"))
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC")
        }
//        val cameraOnlyFiles = "${MediaStore.Files.FileColumns.IS_TRASHED} = ?"
//        val argumentSelection = arrayOf("1")
//
//        val displayOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            columns,
            queryArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(typeColumn)
                val dateTaken = cursor.getLong(dateColumn)
                val mediaItem: MediaItem =
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        MediaItem(
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            isVideo = false,
                            dateTaken = dateTaken
                        )
                    } else {
                        MediaItem(
                            uri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            ),
                            isVideo = true,
                            dateTaken = dateTaken
                        )
                    }

                trashedFilesList.add(mediaItem)
            }
        }
        return@withContext trashedFilesList
    }
}