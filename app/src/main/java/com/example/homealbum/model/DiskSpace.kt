package com.example.homealbum.model

data class DiskSpace(
    val totalSpaceBytes: Double,
    val availableSpaceBytes: Double,
    val usedSpaceBytes: Double
)