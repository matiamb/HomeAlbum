package com.example.homealbum

import android.app.Application
import com.example.homealbum.data.AppContainer
import com.example.homealbum.data.DefaultAppContainer

class HomeAlbumApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}