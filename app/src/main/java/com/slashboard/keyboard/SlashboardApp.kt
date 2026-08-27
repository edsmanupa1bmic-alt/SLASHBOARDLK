package com.slashboard.keyboard

import android.app.Application
import com.slashboard.keyboard.data.db.KeyboardDatabase
import com.slashboard.keyboard.data.repository.KeyboardPreferencesRepository

class SlashboardApp : Application() {

    lateinit var database: KeyboardDatabase
        private set

    lateinit var preferencesRepository: KeyboardPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = KeyboardDatabase(this)
        preferencesRepository = KeyboardPreferencesRepository(this)

        // Initialize high-speed in-memory dual dictionary Trie engine
        com.slashboard.keyboard.data.repository.SmartDictionaryEngine.initialize(this, database)

        // Schedule background cloud dictionary sync worker
        try {
            com.slashboard.keyboard.data.worker.DictionarySyncWorker.schedule(this)
            com.slashboard.keyboard.service.VoiceModelDownloader.checkAndDownloadModels(this)
        } catch (_: Throwable) {}
    }

    companion object {
        lateinit var instance: SlashboardApp
            private set
    }
}
