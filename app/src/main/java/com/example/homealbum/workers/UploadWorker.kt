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
import okio.IOException

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        const val KEY_URI = "photo_uri"
    }
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = uriString.toUri()
        makeNotification(applicationContext, "Starting upload")
        return try {
            val serverResponse = photoRepository.uploadPhoto(uri)
            if (serverResponse.isSuccessful){
                makeNotification(applicationContext, serverResponse.body()?.string())
                Result.success()
            } else {
                makeNotification(applicationContext, "File could not be uploaded: ${serverResponse.errorBody()?.string()}")
                Result.failure()
            }
        } catch (e: IOException){
            Result.retry()
        } catch (e: Exception){
            makeNotification(applicationContext, e.message.toString())
            Result.failure()
        }
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun makeNotification(context: Context, message: String?){
        val name = "Upload notification"
        val description = "Posts notifications of the status of an upload"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("Upload_notifications", name, importance)
        channel.description = description

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(context, "Upload_notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upload status")
            .setContentText(message)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

        NotificationManagerCompat.from(context).notify(1, builder.build())
    }
}