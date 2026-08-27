package com.slashboard.keyboard.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class VoiceModelDownloader(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val language = inputData.getString(KEY_LANGUAGE) ?: return@withContext Result.failure()
            val modelDir = File(applicationContext.filesDir, "voice_models/$language")
            
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            // Simulate downloading and extracting Vosk/Whisper-TFLite micro-engine models
            delay(2000)
            
            // Create a dummy file to represent the downloaded model
            val dummyFile = File(modelDir, "model.bin")
            if (!dummyFile.exists()) {
                dummyFile.createNewFile()
                dummyFile.writeText("Dummy model file for $language")
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val KEY_LANGUAGE = "language"
        
        fun checkAndDownloadModels(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
                
            listOf("si", "en").forEach { lang ->
                val modelDir = File(context.filesDir, "voice_models/$lang")
                if (!modelDir.exists() || modelDir.listFiles()?.isEmpty() != false) {
                    val request = OneTimeWorkRequestBuilder<VoiceModelDownloader>()
                        .setConstraints(constraints)
                        .setInputData(androidx.work.workDataOf(KEY_LANGUAGE to lang))
                        .build()
                    workManager.enqueue(request)
                }
            }
        }
    }
}
