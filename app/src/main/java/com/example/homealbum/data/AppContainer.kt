package com.example.homealbum.data

import android.content.Context

interface AppContainer {
    val photoRepository: PhotoRepository
}

class DefaultAppContainer(context: Context) : AppContainer{

    override val photoRepository: PhotoRepository by lazy {
        PhotoRepository(context = context)
    }
}