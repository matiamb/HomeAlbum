package com.example.homealbum.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.example.homealbum.model.UploadStatus

@Entity(indices = [Index(value = ["uri"], unique = true)])
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String,
    val uploadStatus: UploadStatus,
    val fileHash: String = ""
)