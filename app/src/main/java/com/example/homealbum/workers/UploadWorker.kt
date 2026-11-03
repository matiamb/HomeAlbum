package com.example.homealbum.workers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.homealbum.HomeAlbumApplication
import com.example.homealbum.R
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class UploadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams){
    companion object {
        const val WORKER_ID = "worker_id"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_FOREGROUND = 2
        const val JOB_TYPE = "job_type"
    }
    private val networkPhotoRepository = (context as HomeAlbumApplication).container.networkPhotoRepository
    private val uploadQueueRepository = (context as HomeAlbumApplication).container.uploadQueueRepository
    private val offlinePhotoRepository = (context as HomeAlbumApplication).container.offlinePhotoRepository
    private val settingsRepository = (context as HomeAlbumApplication).container.offlineSettingsRepository
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val workerId = inputData.getString(WORKER_ID) ?: return Result.failure()
        val jobType = inputData.getString(JOB_TYPE) ?: return Result.failure()

        if (jobType == MANUAL_UPLOAD_TAG){
            val cleanupItems = uploadQueueRepository.getPendingUploads(UploadStatus.UPLOADING, workerId)
            val cleanupUris = mutableListOf<Uri>()
            for ((uri) in cleanupItems){
                cleanupUris.add(uri.toUri())
            }
            uploadQueueRepository.cleanUpStuckFilesByWorker(cleanupUris, UploadStatus.PENDING, workerId)
        }
        if (jobType == AUTOMATIC_UPLOAD_TAG){
            val userSettings = settingsRepository.userSettingsFlow.first()
            if (!userSettings.isBackupEnabled || !userSettings.isAutomaticBackupEnabled){
                return Result.success()
            } else {
                val updatedItemsList = offlinePhotoRepository.getLocalPhotos()
                uploadQueueRepository.addMediaItemsToDb(updatedItemsList)
                uploadQueueRepository.prepareFilesForAutoUpload(UploadStatus.PENDING, workerId)
            }
        }
        val uriList = uploadQueueRepository.getPendingUploads(UploadStatus.PENDING, workerId)
        val tempTotalFiles = uriList.size
        var tempSuccessFiles = 0
        var statusChanged = false
        if (uriList.isEmpty()){
            makeNotification(applicationContext,
                applicationContext.getString(R.string.all_up_to_date_autoupload_worker_notification),
                NOTIFICATION_ID)
            return Result.success()
        }
        setForeground(getForegroundInfo())
        //makeNotification(applicationContext, applicationContext.getString(R.string.starting_upload))
        for ((uri1, fileHash) in uriList){
            uploadQueueRepository.updateMediaItemStatus(uri = uri1.toUri(), status = UploadStatus.UPLOADING, workerId = workerId)
            statusChanged = false
            try {
                val serverResponse = if (fileHash == null){
                    networkPhotoRepository.uploadPhoto(uri1)
                } else {
                    networkPhotoRepository.uploadPhoto(uri1, fileHash)
                }
                if (serverResponse.serverResponse != null && serverResponse.serverResponse!!.isSuccessful){
                    uploadQueueRepository.updateMediaItemStatus(uri = uri1.toUri(), status = UploadStatus.UPLOADED, fileHash = serverResponse.fileHash, workerId = workerId)
                    statusChanged = true
                    tempSuccessFiles++
                    makeNotification(
                        applicationContext,
                        "$tempSuccessFiles of $tempTotalFiles uploaded",
                        NOTIFICATION_ID_FOREGROUND,
                        true)
                } else {
                    makeNotification(applicationContext,
                        applicationContext.getString(R.string.file_could_not_be_uploaded) + "${serverResponse.serverResponse!!.errorBody()?.string()}",
                        NOTIFICATION_ID_FOREGROUND,
                        true)
                    uploadQueueRepository.updateMediaItemStatus(uri = uri1.toUri(), status = UploadStatus.FAILED,workerId = workerId)
                    statusChanged = true
                }
            } catch (e: IOException){
                uploadQueueRepository.updateMediaItemStatus(uri1.toUri(), UploadStatus.PENDING, workerId = workerId)
                statusChanged = true
                return Result.retry()
            } finally {
                if (!statusChanged){
                    withContext(NonCancellable){
                        withTimeoutOrNull(1000.milliseconds){
                            val targetStatus = if (isStopped) UploadStatus.PENDING else UploadStatus.FAILED
                            uploadQueueRepository.updateMediaItemStatus(uri1.toUri(), targetStatus, workerId = workerId)
                        }
                    }
                }
            }
        }
        val totalFiles = uploadQueueRepository.getMediaItemsByWorker(workerId)
        val successFiles = uploadQueueRepository.getPendingUploads(UploadStatus.UPLOADED, workerId)
        return if(totalFiles.isNotEmpty() && successFiles.size.toDouble()/totalFiles.size.toDouble() >= 0.5){
            makeNotification(
                context = applicationContext,
                message = "Upload completed: ${successFiles.size} of ${totalFiles.size} uploaded",
                notificationId = NOTIFICATION_ID)
            Result.success()
        } else {
            makeNotification(
                context = applicationContext,
                message = "Warning. Only ${successFiles.size} of ${totalFiles.size} uploaded",
                notificationId = NOTIFICATION_ID)
            Result.failure()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID_FOREGROUND,
            makeNotification(applicationContext, "Upload in progress", NOTIFICATION_ID_FOREGROUND, true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun makeNotification(context: Context, message: String?, notificationId: Int, showProgress: Boolean = false): Notification{
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
            .setOnlyAlertOnce(true)
            .setContentText(message)
        val notification = if (showProgress){
                builder.setProgress(0,0, true)
                    .build()
            } else {
                builder.build()
            }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return notification
    }
}