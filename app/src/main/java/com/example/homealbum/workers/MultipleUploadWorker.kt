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
import com.example.homealbum.model.UploadStatus
import com.example.homealbum.workers.UploadWorker.Companion.NOTIFICATION_ID
import java.io.IOException

class MultipleUploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams){
    companion object {
        const val KEY_LIST_URI = ""
    }
    private val photoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    private val uploadQueueRepository = (context as HomeAlbumApplication).container.uploadQueueRepository
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        //val uriListString = inputData.getStringArray(KEY_LIST_URI) ?: return Result.failure()
        val uriList: List<Uri> = uploadQueueRepository.getPendingUploads(UploadStatus.PENDING)
//        for (uriString in uriListString){
//            uriList.add(uriString.toUri())
//        }
        val totalFiles = uriList.size
        var successFiles = 0
        var failedFiles = 0
        var statusChanged = false
        makeNotification(applicationContext, applicationContext.getString(R.string.starting_upload))
        if (uriList.isEmpty()){
            return Result.failure()
        }
        for (uri in uriList){
            uploadQueueRepository.updateMediaItemStatus(uri = uri, status = UploadStatus.UPLOADING)
            statusChanged = false
            try {
                val serverResponse = photoRepository.uploadPhoto(uri)
                if (serverResponse.serverResponse != null && serverResponse.serverResponse!!.isSuccessful){
                    //makeNotification(applicationContext, serverResponse.body()?.string())
                    uploadQueueRepository.updateMediaItemStatus(uri = uri, status = UploadStatus.UPLOADED, fileHash = serverResponse.fileHash)
                    statusChanged = true
                    successFiles++
                } else {
                    makeNotification(applicationContext,
                        applicationContext.getString(R.string.file_could_not_be_uploaded) + "${serverResponse.serverResponse!!.errorBody()?.string()}")
                    uploadQueueRepository.updateMediaItemStatus(uri = uri, status = UploadStatus.FAILED)
                    statusChanged = true
                    failedFiles++
                }
            } catch (e: IOException){
                uploadQueueRepository.updateMediaItemStatus(uri, UploadStatus.PENDING)
                statusChanged = true
                return Result.retry()
            } finally {
                if (!statusChanged){
                    uploadQueueRepository.updateMediaItemStatus(uri, UploadStatus.FAILED)
                }
            }
        }
    //catch (e: Exception){
//            makeNotification(applicationContext, "")
//            Result.failure()
//        }
        return if(failedFiles/totalFiles <= 0.5){
            makeNotification(context = applicationContext, message = "$successFiles of $totalFiles uploaded")
            Result.success()
        } else {
            makeNotification(context = applicationContext, message = "Warning. Only $successFiles of $totalFiles uploaded")
            Result.failure()
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