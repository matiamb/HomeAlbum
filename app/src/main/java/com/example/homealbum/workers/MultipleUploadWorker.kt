package com.example.homealbum.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.R
import com.example.homealbum.workers.UploadWorker.Companion.NOTIFICATION_ID
import java.io.IOException

class MultipleUploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams){
    companion object {
        const val KEY_LIST_URI = ""
    }
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val uriListString = inputData.getStringArray(KEY_LIST_URI) ?: return Result.failure()
        val uriList: MutableList<Uri> = mutableListOf()
        for (uriString in uriListString){
            uriList.add(uriString.toUri())
        }
        makeNotification(applicationContext, applicationContext.getString(R.string.starting_upload))
//        if (uriList.isEmpty()){
//            Result.failure()
//        }
        return try {
            val serverResponse = photoRepository.uploadMultipleFiles(uriList)
            if (serverResponse.isSuccessful){
                makeNotification(applicationContext, serverResponse.body()?.string())
                Result.success()
            } else {
                makeNotification(applicationContext,
                    applicationContext.getString(R.string.file_could_not_be_uploaded) + "${serverResponse.errorBody()?.string()}")
                Result.failure()
            }
        } catch (e: IOException){
            Result.retry()
        } //catch (e: Exception){
//            makeNotification(applicationContext, "")
//            Result.failure()
//        }
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