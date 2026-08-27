package com.slashboard.keyboard.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.repository.SmartDictionaryEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background Cloud Vocabulary Sync Worker.
 * Periodically fetches remote word frequency updates and dictionary expansions
 * over unmetered Wi-Fi/Internet and merges them into local Trie and SQLite database.
 */
class DictionarySyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting DictionarySyncWorker task...")

        try {
            val app = applicationContext as? SlashboardApp
            val database = app?.database

            // Example remote dictionary sync endpoints (configurable via input data or fallback)
            val sinhalaSyncUrl = inputData.getString(KEY_SINHALA_SYNC_URL) ?: DEFAULT_SINHALA_URL
            val englishSyncUrl = inputData.getString(KEY_ENGLISH_SYNC_URL) ?: DEFAULT_ENGLISH_URL

            // 1. Fetch & Parse Sinhala Words
            val sinhalaWords = fetchRemoteDictionary(sinhalaSyncUrl)
            if (sinhalaWords.isNotEmpty()) {
                SmartDictionaryEngine.mergeRemoteWords(
                    words = sinhalaWords,
                    isSinhala = true,
                    database = database
                )
                Log.d(TAG, "Successfully synced ${sinhalaWords.size} Sinhala remote words")
            }

            // 2. Fetch & Parse English Words
            val englishWords = fetchRemoteDictionary(englishSyncUrl)
            if (englishWords.isNotEmpty()) {
                SmartDictionaryEngine.mergeRemoteWords(
                    words = englishWords,
                    isSinhala = false,
                    database = database
                )
                Log.d(TAG, "Successfully synced ${englishWords.size} English remote words")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DictionarySyncWorker failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun fetchRemoteDictionary(urlString: String): List<Pair<String, Int>> {
        val results = mutableListOf<Pair<String, Int>>()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Slashboard-IME-Android")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val text = reader.readText().trim()
                    if (text.startsWith("{") || text.startsWith("[")) {
                        // Parse JSON format
                        parseJsonWords(text, results)
                    } else {
                        // Parse TSV / Line format: "word\tfrequency"
                        parseTsvWords(text, results)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch dictionary from $urlString: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        return results
    }

    private fun parseJsonWords(jsonString: String, results: MutableList<Pair<String, Int>>) {
        try {
            if (jsonString.startsWith("[")) {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val word = obj.optString("word", "")
                        val freq = obj.optInt("frequency", 500)
                        if (word.isNotBlank()) results.add(word to freq)
                    } else {
                        val word = array.optString(i, "")
                        if (word.isNotBlank()) results.add(word to 500)
                    }
                }
            } else if (jsonString.startsWith("{")) {
                val obj = JSONObject(jsonString)
                val wordsArray = obj.optJSONArray("words") ?: JSONArray()
                for (i in 0 until wordsArray.length()) {
                    val item = wordsArray.getJSONObject(i)
                    val word = item.optString("word", "")
                    val freq = item.optInt("frequency", 500)
                    if (word.isNotBlank()) results.add(word to freq)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing JSON dictionary: ${e.message}")
        }
    }

    private fun parseTsvWords(tsvString: String, results: MutableList<Pair<String, Int>>) {
        val lines = tsvString.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("\t", " ", limit = 2)
                val word = parts[0].trim()
                val freq = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 500 else 500
                if (word.isNotEmpty()) {
                    results.add(word to freq)
                }
            }
        }
    }

    companion object {
        private const val TAG = "DictionarySyncWorker"
        const val KEY_SINHALA_SYNC_URL = "sinhala_sync_url"
        const val KEY_ENGLISH_SYNC_URL = "english_sync_url"

        private const val DEFAULT_SINHALA_URL = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_si.json"
        private const val DEFAULT_ENGLISH_URL = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_en.json"

        private const val PERIODIC_WORK_NAME = "slashboard_dictionary_periodic_sync"
        private const val ONE_TIME_WORK_NAME = "slashboard_dictionary_one_time_sync"

        /**
         * Enqueues a periodic sync worker (every 24 hours on unmetered network while device is charging/idle).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<DictionarySyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Enqueues an immediate one-time sync worker.
         */
        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<DictionarySyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
