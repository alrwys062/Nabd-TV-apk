package com.example

import android.app.Application
import com.example.data.local.NabdDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.NabdRepository
import com.example.player.ProPlayerManager
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class NabdApplication : Application() {

    lateinit var okHttpClient: OkHttpClient
        private set
    lateinit var database: NabdDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var repository: NabdRepository
        private set
    lateinit var playerManager: ProPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        val cacheDir = File(cacheDir, "http_cache")
        okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 25L * 1024L * 1024L))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        database = NabdDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = NabdRepository(this, database.nabdDao(), settingsDataStore, okHttpClient)
        playerManager = ProPlayerManager(this, okHttpClient)
    }
}
