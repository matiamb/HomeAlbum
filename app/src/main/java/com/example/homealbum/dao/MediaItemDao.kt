package com.example.homealbum.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.homealbum.data.MediaItemEntity
import com.example.homealbum.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaItems(mediaItemList: List<MediaItemEntity>)
    @Query("UPDATE MediaItemEntity SET uploadStatus = :status, fileHash = :fileHash WHERE uri = :uri")
    suspend fun updateMediaItemStatus(uri: String, status: UploadStatus, fileHash: String?)
    @Query("DELETE FROM MediaItemEntity WHERE uri IN (:uriList)")
    suspend fun deleteMediaItem(uriList: List<String>)
    @Query("SELECT * FROM MediaItemEntity WHERE uri = :uri")
    suspend fun getMediaItemByUri(uri: String): MediaItemEntity?
    @Query("SELECT * FROM MediaItemEntity")
    fun observeAllMediaItems(): Flow<List<MediaItemEntity>>
}