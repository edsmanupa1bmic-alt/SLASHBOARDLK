package com.slashboard.keyboard.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserLearningDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createLearnedWords = """
            CREATE TABLE $TABLE_LEARNED_WORDS (
                $COL_WORD TEXT PRIMARY KEY,
                $COL_INPUT_PREFIX TEXT,
                $COL_FREQUENCY INTEGER DEFAULT 1,
                $COL_LAST_USED LONG
            )
        """.trimIndent()
        db.execSQL(createLearnedWords)

        val createLearnedBigrams = """
            CREATE TABLE $TABLE_LEARNED_BIGRAMS (
                $COL_PREV_WORD TEXT,
                $COL_NEXT_WORD TEXT,
                $COL_PAIR_FREQ INTEGER DEFAULT 1,
                PRIMARY KEY ($COL_PREV_WORD, $COL_NEXT_WORD)
            )
        """.trimIndent()
        db.execSQL(createLearnedBigrams)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED_WORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNED_BIGRAMS")
        onCreate(db)
    }

    fun upsertWord(word: String, inputPrefix: String = "") {
        val db = writableDatabase
        val cursor = db.query(TABLE_LEARNED_WORDS, arrayOf(COL_FREQUENCY), "$COL_WORD = ?", arrayOf(word), null, null, null)
        val exists = cursor.moveToFirst()
        val freq = if (exists) cursor.getInt(0) else 0
        cursor.close()

        val cv = ContentValues().apply {
            put(COL_WORD, word)
            put(COL_INPUT_PREFIX, inputPrefix)
            put(COL_FREQUENCY, freq + 1)
            put(COL_LAST_USED, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_LEARNED_WORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertBigram(prevWord: String, nextWord: String) {
        val db = writableDatabase
        val cursor = db.query(TABLE_LEARNED_BIGRAMS, arrayOf(COL_PAIR_FREQ), "$COL_PREV_WORD = ? AND $COL_NEXT_WORD = ?", arrayOf(prevWord, nextWord), null, null, null)
        val exists = cursor.moveToFirst()
        val freq = if (exists) cursor.getInt(0) else 0
        cursor.close()

        val cv = ContentValues().apply {
            put(COL_PREV_WORD, prevWord)
            put(COL_NEXT_WORD, nextWord)
            put(COL_PAIR_FREQ, freq + 1)
        }
        db.insertWithOnConflict(TABLE_LEARNED_BIGRAMS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getLearnedWords(prefix: String): List<LearnedWord> {
        val list = mutableListOf<LearnedWord>()
        val db = readableDatabase
        val query = "$COL_WORD LIKE ? OR $COL_INPUT_PREFIX LIKE ?"
        val args = arrayOf("$prefix%", "$prefix%")
        val cursor = db.query(TABLE_LEARNED_WORDS, null, query, args, null, null, "$COL_FREQUENCY DESC", "20")
        cursor.use {
            val wordIdx = it.getColumnIndexOrThrow(COL_WORD)
            val prefixIdx = it.getColumnIndexOrThrow(COL_INPUT_PREFIX)
            val freqIdx = it.getColumnIndexOrThrow(COL_FREQUENCY)
            val lastUsedIdx = it.getColumnIndexOrThrow(COL_LAST_USED)
            while (it.moveToNext()) {
                list.add(
                    LearnedWord(
                        word = it.getString(wordIdx),
                        inputPrefix = it.getString(prefixIdx),
                        frequency = it.getInt(freqIdx),
                        lastUsed = it.getLong(lastUsedIdx)
                    )
                )
            }
        }
        return list
    }
    
    fun getNextWords(prevWord: String): List<LearnedBigram> {
        val list = mutableListOf<LearnedBigram>()
        val db = readableDatabase
        val cursor = db.query(TABLE_LEARNED_BIGRAMS, null, "$COL_PREV_WORD = ?", arrayOf(prevWord), null, null, "$COL_PAIR_FREQ DESC", "10")
        cursor.use {
            val nextIdx = it.getColumnIndexOrThrow(COL_NEXT_WORD)
            val freqIdx = it.getColumnIndexOrThrow(COL_PAIR_FREQ)
            while (it.moveToNext()) {
                list.add(
                    LearnedBigram(
                        prevWord = prevWord,
                        nextWord = it.getString(nextIdx),
                        frequency = it.getInt(freqIdx)
                    )
                )
            }
        }
        return list
    }
    
    fun getWord(word: String): LearnedWord? {
        val db = readableDatabase
        val cursor = db.query(TABLE_LEARNED_WORDS, null, "$COL_WORD = ?", arrayOf(word), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                return LearnedWord(
                    word = it.getString(it.getColumnIndexOrThrow(COL_WORD)),
                    inputPrefix = it.getString(it.getColumnIndexOrThrow(COL_INPUT_PREFIX)),
                    frequency = it.getInt(it.getColumnIndexOrThrow(COL_FREQUENCY)),
                    lastUsed = it.getLong(it.getColumnIndexOrThrow(COL_LAST_USED))
                )
            }
        }
        return null
    }

    companion object {
        private const val DATABASE_NAME = "user_learning_dict.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_LEARNED_WORDS = "LearnedWords"
        private const val COL_WORD = "word"
        private const val COL_INPUT_PREFIX = "input_prefix"
        private const val COL_FREQUENCY = "frequency"
        private const val COL_LAST_USED = "last_used_timestamp"

        private const val TABLE_LEARNED_BIGRAMS = "LearnedBigrams"
        private const val COL_PREV_WORD = "previous_word"
        private const val COL_NEXT_WORD = "next_word"
        private const val COL_PAIR_FREQ = "pair_frequency"
    }
}

data class LearnedWord(
    val word: String,
    val inputPrefix: String,
    val frequency: Int,
    val lastUsed: Long
)

data class LearnedBigram(
    val prevWord: String,
    val nextWord: String,
    val frequency: Int
)
