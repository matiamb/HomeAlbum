package com.example.homealbum.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.homealbum.dao.MediaItemDao
@Database(entities = [MediaItemEntity::class], version = 1)
abstract class MediaItemDatabase : RoomDatabase(){
    abstract fun mediaItemDao(): MediaItemDao
}