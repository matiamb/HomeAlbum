package com.example.homealbum.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.homealbum.data.MediaItemEntity

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaItems(mediaItemList: List<MediaItemEntity>)
    @Update
    suspend fun updateMediaItemStatus(mediaItem: MediaItemEntity)
    @Query("DELETE FROM MediaItemEntity WHERE uri IN (:uriList)")
    suspend fun deleteMediaItem(uriList: List<String>)
    @Query("SELECT * FROM MediaItemEntity WHERE uri = :uri")
    suspend fun getMediaItemByUri(uri: String): MediaItemEntity?
}