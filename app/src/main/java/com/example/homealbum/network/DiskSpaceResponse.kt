package com.example.homealbum.network
data class DiskSpaceResponse (
    val totalSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val usedSpaceBytes: Long
)