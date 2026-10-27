package com.example.homealbum.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import androidx.core.net.toUri
import com.example.homealbum.R
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.IOException
import kotlin.time.Duration.Companion.milliseconds

class DeleteTestUploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val KEY_URI = "photo_uri"
        const val WORKER_ID = ""
        const val NOTIFICATION_ID = 1
    }
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    private val uploadQueueRepository = (context as HomeAlbumApplication).container.uploadQueueRepository
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val workerId = inputData.getString(WORKER_ID) ?: return Result.failure()
        val uri = uriString.toUri()
        var statusChanged = false
        makeNotification(applicationContext, applicationContext.getString(R.string.starting_upload))
        return try {
            uploadQueueRepository.updateMediaItemStatus(uri = uri, status = UploadStatus.UPLOADING, workerId = workerId)
            statusChanged = false
            val serverResponse = photoRepository.uploadPhoto(uri.toString())
            if (serverResponse.serverResponse != null && serverResponse.serverResponse!!.isSuccessful ){
                makeNotification(applicationContext, serverResponse.serverResponse!!.body()?.string())
                uploadQueueRepository.updateMediaItemStatus(uri, UploadStatus.UPLOADED, serverResponse.fileHash, workerId = workerId)
                statusChanged = true
                Result.success()
            } else {
                makeNotification(applicationContext,
                    applicationContext.getString(R.string.file_could_not_be_uploaded) + "${serverResponse.serverResponse!!.errorBody()?.string()}")
                uploadQueueRepository.updateMediaItemStatus(uri, UploadStatus.FAILED, workerId = workerId)
                statusChanged = true
                Result.failure()
            }
        } catch (e: IOException){
            uploadQueueRepository.updateMediaItemStatus(uri, UploadStatus.PENDING, workerId = workerId)
            statusChanged = true
            Result.retry()
        } finally {
            if (!statusChanged){
                withContext(NonCancellable){
                    withTimeoutOrNull(1000.milliseconds){
                        val targetStatus = if (isStopped) UploadStatus.PENDING else UploadStatus.FAILED
                        uploadQueueRepository.updateMediaItemStatus(uri, targetStatus, workerId = workerId)
                    }
                }
            }
        }
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun makeNotification(context: Context, message: String?){
        val name = context.getString(R.string.upload_notification_channel_name)
        val description = context.getString(R.string.upload_notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("Upload_notifications", name, importance)
        channel.description = description

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(context, "Upload_notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.upload_status_notification_title))
            .setContentText(message)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}