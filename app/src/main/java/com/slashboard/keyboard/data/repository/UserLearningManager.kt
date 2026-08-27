package com.slashboard.keyboard.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserLearningManager(context: Context) {
    private val db = UserLearningDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    var lastCommittedWord: String? = null
        private set

    fun onWordCommitted(word: String, inputPrefix: String = "") {
        val trimmedWord = word.trim()
        if (trimmedWord.isBlank() || trimmedWord.length > 50) return
        
        // Extract words (handle punctuation)
        val tokens = trimmedWord.split("\\s+".toRegex())
        val cleanWord = tokens.lastOrNull()?.replace(Regex("[^a-zA-Z\\u0D80-\\u0DFF]"), "") ?: ""
        if (cleanWord.isBlank()) {
            lastCommittedWord = null
            return
        }
        
        scope.launch {
            // 1. Upsert word
            db.upsertWord(cleanWord, inputPrefix)
            
            // 2. Track bigram
            lastCommittedWord?.let { prevWord ->
                db.upsertBigram(prevWord, cleanWord)
            }
            
            lastCommittedWord = cleanWord
        }
    }
    
    fun onSpaceOrPunctuation(text: String) {
        val cleanWord = text.replace(Regex("[^a-zA-Z\\u0D80-\\u0DFF]"), "")
        if (cleanWord.isBlank()) {
            lastCommittedWord = null
            return
        }
        
        scope.launch {
            db.upsertWord(cleanWord, "")
            lastCommittedWord?.let { prevWord ->
                db.upsertBigram(prevWord, cleanWord)
            }
            lastCommittedWord = cleanWord
        }
    }
    
    fun resetContext() {
        lastCommittedWord = null
    }
    
    fun getNextWords(): List<LearnedBigram> {
        val prev = lastCommittedWord ?: return emptyList()
        return db.getNextWords(prev)
    }
    
    fun getPredictions(prefix: String): List<LearnedWord> {
        if (prefix.isBlank()) return emptyList()
        return db.getLearnedWords(prefix)
    }
    
    fun getLearnedWord(word: String) = db.getWord(word)
    fun getBigramFrequency(prev: String, next: String): Int {
        val bigrams = db.getNextWords(prev)
        return bigrams.find { it.nextWord == next }?.frequency ?: 0
    }
    
    companion object {
        @Volatile
        private var instance: UserLearningManager? = null
        
        fun getInstance(context: Context): UserLearningManager {
            return instance ?: synchronized(this) {
                instance ?: UserLearningManager(context.applicationContext).also { instance = it }
            }
        }
        
        fun initialize(context: Context) {
            getInstance(context)
        }
    }
}
