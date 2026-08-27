package com.slashboard.keyboard.data.repository

import android.content.Context
import android.util.Log
import com.slashboard.keyboard.data.db.KeyboardDatabase
import com.slashboard.keyboard.data.model.DictionaryWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Offline-first, high-speed Dual-Language Trie Dictionary & Self-Learning Engine.
 *
 * Capabilities:
 * - Sub-5ms prefix candidate retrieval on 60fps keystroke cycles.
 * - Bundled assets loader for top frequent Sinhala and English vocabulary.
 * - Integration with SQLite/Room local database for user dictionary words & shortcuts.
 * - Word frequency boosting and self-learning upon committing suggestions.
 * - Thread-safe concurrency for background cloud synchronizations.
 */
object SmartDictionaryEngine {

    private const val TAG = "SmartDictionaryEngine"

    private val sinhalaTrie = Trie()
    private val englishTrie = Trie()

    @Volatile
    private var isInitialized = false
    private var initJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default)

    /**
     * Initializes the dual-language tries asynchronously from asset dictionaries and user DB.
     */
    fun initialize(context: Context, database: KeyboardDatabase? = null) {
        if (isInitialized) return

        initJob = engineScope.launch {
            val startTime = System.currentTimeMillis()

            // 1. Load Sinhala Frequent Words from Assets
            loadAssetDictionary(
                context = context,
                assetPath = "dictionaries/sinhala_frequent_words.txt",
                trie = sinhalaTrie,
                defaultFreq = 900
            )

            // 2. Load English Frequent Words from Assets
            loadAssetDictionary(
                context = context,
                assetPath = "dictionaries/english_frequent_words.txt",
                trie = englishTrie,
                defaultFreq = 800
            )

            // 3. Load User-Learned Words from local Database
            database?.let { db ->
                loadUserDatabaseWords(db)
            }

            isInitialized = true
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "SmartDictionaryEngine successfully initialized in ${elapsed}ms")
        }
    }

    /**
     * Fast Prefix Search for Sinhala Vocabulary.
     */
    fun searchSinhala(prefix: String, limit: Int = 5): List<TrieCandidate> {
        if (prefix.isBlank()) return emptyList()
        return sinhalaTrie.searchPrefix(prefix.trim(), limit)
    }

    /**
     * Fast Prefix Search for English Vocabulary.
     */
    fun searchEnglish(prefix: String, limit: Int = 5): List<TrieCandidate> {
        if (prefix.isBlank()) return emptyList()
        val query = prefix.trim().lowercase()
        return englishTrie.searchPrefix(query, limit)
    }

    /**
     * Self-Learning Word Commit: Boosts in-memory frequency and saves to local SQLite database.
     */
    fun learnWord(word: String, isSinhala: Boolean, database: KeyboardDatabase? = null) {
        val trimmed = word.trim()
        if (trimmed.length < 2) return

        engineScope.launch {
            if (isSinhala) {
                sinhalaTrie.boostFrequency(trimmed, increment = 30)
            } else {
                englishTrie.boostFrequency(trimmed.lowercase(), increment = 30)
            }

            // Persist learned word to local database
            database?.addOrUpdateDictionaryWord(
                word = trimmed,
                shortcut = "",
                frequency = 350
            )
        }
    }

    /**
     * Merges remote cloud words into the in-memory Trie and persistent database.
     */
    fun mergeRemoteWords(
        words: List<Pair<String, Int>>,
        isSinhala: Boolean,
        database: KeyboardDatabase? = null
    ) {
        val targetTrie = if (isSinhala) sinhalaTrie else englishTrie
        engineScope.launch {
            for ((word, freq) in words) {
                val cleanWord = if (isSinhala) word.trim() else word.trim().lowercase()
                targetTrie.insert(cleanWord, freq, isUserLearned = true)
                database?.addOrUpdateDictionaryWord(cleanWord, "", freq)
            }
        }
    }

    /**
     * Synchronizes custom user dictionary words from database into memory Trie.
     */
    fun loadUserDatabaseWords(database: KeyboardDatabase) {
        val userWords = database.dictionaryFlow.value
        for (item in userWords) {
            val isSinhala = isSinhalaText(item.word)
            val trie = if (isSinhala) sinhalaTrie else englishTrie
            trie.insert(
                word = if (isSinhala) item.word else item.word.lowercase(),
                frequency = item.frequency,
                isUserLearned = true
            )
        }
    }

    private suspend fun loadAssetDictionary(
        context: Context,
        assetPath: String,
        trie: Trie,
        defaultFreq: Int
    ) = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetPath).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val parts = trimmed.split("\t", " ", limit = 2)
                            val word = parts[0].trim()
                            val freq = if (parts.size > 1) {
                                parts[1].trim().toIntOrNull() ?: defaultFreq
                            } else {
                                defaultFreq
                            }
                            if (word.isNotEmpty()) {
                                trie.insert(word, freq, isUserLearned = false)
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading asset dictionary from $assetPath: ${e.message}")
        }
    }

    private fun isSinhalaText(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code in 0x0D80..0x0DFF) return true
        }
        return false
    }

    fun isReady(): Boolean = isInitialized
}
