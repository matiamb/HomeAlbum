package com.example.homealbum.model

import java.time.LocalDate

sealed interface GalleryItem {
    data class DateHeader(
        val date: LocalDate
    ) : GalleryItem
    data class Photo(
        val mediaItem: MediaItem,
        val originalIndex: Int
    ) : GalleryItem
}