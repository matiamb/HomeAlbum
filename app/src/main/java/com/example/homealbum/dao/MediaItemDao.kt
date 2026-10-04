package com.example.homealbum.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.homealbum.data.MediaItemEntity
import com.example.homealbum.model.MediaItemUriHash
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaItems(mediaItemList: List<MediaItemEntity>)
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status, fileHash = :fileHash WHERE uri = :uri AND workerId = :workerId")
    suspend fun updateMediaItemStatus(uri: String, status: UploadStatus, fileHash: String?, workerId: String?)
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status WHERE uri = :uri AND workerId = :workerId")
    suspend fun updateMediaItemStatus(uri: String, status: UploadStatus, workerId: String?)
    @Query("DELETE FROM MediaItemEntity WHERE uri IN (:uriList)")
    suspend fun deleteMediaItem(uriList: List<String>)
    @Query("SELECT uri FROM MediaItemEntity WHERE fileHash IS NULL")
    suspend fun getAllUris(): List<String>
    @Query("SELECT * FROM MediaItemEntity")
    fun observeAllMediaItems(): Flow<List<MediaItemEntity>>
    @Query("SELECT uri, fileHash FROM MediaItemEntity WHERE uploadStatus = :status AND workerId = :workerId")
    fun getMediaItemsForWorker(status: UploadStatus, workerId: String): List<MediaItemUriHash>
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status, workerId = :workerId WHERE uri IN (:uriList)")
    fun prepareFilesForUpload(uriList: List<String>, status: UploadStatus, workerId: String)
    @Query("SELECT * FROM MediaItemEntity WHERE workerId = :workerId")
    fun getMediaItemsByWorker(workerId: String): List<MediaItemEntity>
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status WHERE uri IN (:uriList) AND workerId = :workerId")
    fun cleanupStuckFilesByWorker(uriList: List<String>, status: UploadStatus, workerId: String)
    @Query("UPDATE MediaItemEntity SET fileHash = :fileHash WHERE uri = :uri")
    fun updateFileHash(uri: String, fileHash: String?)
    @Query("SELECT fileHash FROM MediaItemEntity WHERE fileHash IS NOT NULL")
    fun getAllFileHashes(): List<String>
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status WHERE fileHash IN (:fileHash)")
    fun updateMediaItemStatusByFileHash(fileHash: List<String>, status: UploadStatus)
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status WHERE uri = :uri")
    fun updateMediaItemStatusByUri(uri: String, status: UploadStatus)
}