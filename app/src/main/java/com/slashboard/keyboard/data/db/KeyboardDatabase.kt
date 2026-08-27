package com.slashboard.keyboard.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.slashboard.keyboard.data.model.ClipboardItem
import com.slashboard.keyboard.data.model.DictionaryWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _clipboardFlow = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardFlow: StateFlow<List<ClipboardItem>> = _clipboardFlow.asStateFlow()

    private val _dictionaryFlow = MutableStateFlow<List<DictionaryWord>>(emptyList())
    val dictionaryFlow: StateFlow<List<DictionaryWord>> = _dictionaryFlow.asStateFlow()

    init {
        refreshClipboard()
        refreshDictionary()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CLIPBOARD (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_TEXT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_PINNED INTEGER DEFAULT 0,
                $COL_CATEGORY TEXT DEFAULT 'general',
                $COL_COPY_COUNT INTEGER DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DICTIONARY (
                $COL_DICT_ID INTEGER PRIMARY KEY,
                $COL_WORD TEXT NOT NULL,
                $COL_SHORTCUT TEXT DEFAULT '',
                $COL_FREQUENCY INTEGER DEFAULT 250,
                $COL_DATE_ADDED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Seed default useful clipboard items
        insertDefaultClipboards(db)
        // Seed default shortcuts
        insertDefaultDictionary(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIPBOARD")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DICTIONARY")
        onCreate(db)
    }

    private fun insertDefaultClipboards(db: SQLiteDatabase) {
        val samples = listOf(
            ClipboardItem(text = "Welcome to xSlashboardx Keyboard! 🚀", isPinned = true),
            ClipboardItem(text = "Tap on any clipboard snippet to quickly paste into your text field.", isPinned = true),
            ClipboardItem(text = "contact@example.com", isPinned = false),
            ClipboardItem(text = "https://github.com/dinushlakmal/xSlashboardx", isPinned = false)
        )
        for (item in samples) {
            val cv = ContentValues().apply {
                put(COL_ID, item.id + samples.indexOf(item))
                put(COL_TEXT, item.text)
                put(COL_TIMESTAMP, item.timestamp)
                put(COL_IS_PINNED, if (item.isPinned) 1 else 0)
                put(COL_CATEGORY, item.category)
                put(COL_COPY_COUNT, item.copyCount)
            }
            db.insert(TABLE_CLIPBOARD, null, cv)
        }
    }

    private fun insertDefaultDictionary(db: SQLiteDatabase) {
        val shortcuts = listOf(
            DictionaryWord(word = "Be right back!", shortcut = "brb", frequency = 300),
            DictionaryWord(word = "On my way!", shortcut = "omw", frequency = 300),
            DictionaryWord(word = "Thank you very much!", shortcut = "tyvm", frequency = 280),
            DictionaryWord(word = "Let me know what you think.", shortcut = "lmk", frequency = 260),
            DictionaryWord(word = "As soon as possible", shortcut = "asap", frequency = 290)
        )
        for (item in shortcuts) {
            val cv = ContentValues().apply {
                put(COL_DICT_ID, item.id + shortcuts.indexOf(item))
                put(COL_WORD, item.word)
                put(COL_SHORTCUT, item.shortcut)
                put(COL_FREQUENCY, item.frequency)
                put(COL_DATE_ADDED, item.dateAdded)
            }
            db.insert(TABLE_DICTIONARY, null, cv)
        }
    }

    // --- Clipboard Operations ---

    fun refreshClipboard() {
        val list = mutableListOf<ClipboardItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLIPBOARD,
            null,
            null,
            null,
            null,
            null,
            "$COL_IS_PINNED DESC, $COL_TIMESTAMP DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val textIdx = it.getColumnIndexOrThrow(COL_TEXT)
            val tsIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)
            val pinnedIdx = it.getColumnIndexOrThrow(COL_IS_PINNED)
            val catIdx = it.getColumnIndexOrThrow(COL_CATEGORY)
            val copyIdx = it.getColumnIndexOrThrow(COL_COPY_COUNT)

            while (it.moveToNext()) {
                list.add(
                    ClipboardItem(
                        id = it.getLong(idIdx),
                        text = it.getString(textIdx),
                        timestamp = it.getLong(tsIdx),
                        isPinned = it.getInt(pinnedIdx) == 1,
                        category = it.getString(catIdx),
                        copyCount = it.getInt(copyIdx)
                    )
                )
            }
        }
        _clipboardFlow.value = list
    }

    fun addClipboardItem(text: String, isPinned: Boolean = false): Long {
        if (text.isBlank()) return -1L
        val trimmed = text.trim()
        val db = writableDatabase

        // If duplicate exists, bump timestamp
        val existing = _clipboardFlow.value.find { it.text == trimmed }
        if (existing != null) {
            val cv = ContentValues().apply {
                put(COL_TIMESTAMP, System.currentTimeMillis())
                put(COL_COPY_COUNT, existing.copyCount + 1)
            }
            db.update(TABLE_CLIPBOARD, cv, "$COL_ID = ?", arrayOf(existing.id.toString()))
            refreshClipboard()
            return existing.id
        }

        val id = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put(COL_ID, id)
            put(COL_TEXT, trimmed)
            put(COL_TIMESTAMP, id)
            put(COL_IS_PINNED, if (isPinned) 1 else 0)
            put(COL_CATEGORY, if (trimmed.startsWith("http")) "link" else if (trimmed.contains("@")) "email" else "text")
            put(COL_COPY_COUNT, 1)
        }
        db.insert(TABLE_CLIPBOARD, null, cv)
        refreshClipboard()
        return id
    }

    fun togglePin(id: Long) {
        val item = _clipboardFlow.value.find { it.id == id } ?: return
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_PINNED, if (item.isPinned) 0 else 1)
        }
        db.update(TABLE_CLIPBOARD, cv, "$COL_ID = ?", arrayOf(id.toString()))
        refreshClipboard()
    }

    fun deleteClipboardItem(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_CLIPBOARD, "$COL_ID = ?", arrayOf(id.toString()))
        refreshClipboard()
    }

    fun clearUnpinnedClipboard() {
        val db = writableDatabase
        db.delete(TABLE_CLIPBOARD, "$COL_IS_PINNED = 0", null)
        refreshClipboard()
    }

    // --- Dictionary Operations ---

    fun refreshDictionary() {
        val list = mutableListOf<DictionaryWord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DICTIONARY,
            null,
            null,
            null,
            null,
            null,
            "$COL_WORD ASC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_DICT_ID)
            val wordIdx = it.getColumnIndexOrThrow(COL_WORD)
            val shortcutIdx = it.getColumnIndexOrThrow(COL_SHORTCUT)
            val freqIdx = it.getColumnIndexOrThrow(COL_FREQUENCY)
            val dateIdx = it.getColumnIndexOrThrow(COL_DATE_ADDED)

            while (it.moveToNext()) {
                list.add(
                    DictionaryWord(
                        id = it.getLong(idIdx),
                        word = it.getString(wordIdx),
                        shortcut = it.getString(shortcutIdx),
                        frequency = it.getInt(freqIdx),
                        dateAdded = it.getLong(dateIdx)
                    )
                )
            }
        }
        _dictionaryFlow.value = list
    }

    fun addOrUpdateDictionaryWord(word: String, shortcut: String = "", frequency: Int = 250): Long {
        if (word.isBlank()) return -1L
        val trimmedWord = word.trim()
        val trimmedShortcut = shortcut.trim().lowercase()
        val db = writableDatabase

        val existing = _dictionaryFlow.value.find { it.word.equals(trimmedWord, ignoreCase = true) }
        if (existing != null) {
            val cv = ContentValues().apply {
                put(COL_SHORTCUT, trimmedShortcut)
                put(COL_FREQUENCY, frequency)
            }
            db.update(TABLE_DICTIONARY, cv, "$COL_DICT_ID = ?", arrayOf(existing.id.toString()))
            refreshDictionary()
            return existing.id
        }

        val id = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put(COL_DICT_ID, id)
            put(COL_WORD, trimmedWord)
            put(COL_SHORTCUT, trimmedShortcut)
            put(COL_FREQUENCY, frequency)
            put(COL_DATE_ADDED, id)
        }
        db.insert(TABLE_DICTIONARY, null, cv)
        refreshDictionary()
        return id
    }

    fun deleteDictionaryWord(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_DICTIONARY, "$COL_DICT_ID = ?", arrayOf(id.toString()))
        refreshDictionary()
    }

    fun findShortcutExpansion(shortcut: String): String? {
        val s = shortcut.trim().lowercase()
        if (s.isEmpty()) return null
        return _dictionaryFlow.value.find { it.shortcut.lowercase() == s }?.word
    }

    fun searchDictionary(query: String): List<DictionaryWord> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _dictionaryFlow.value
        return _dictionaryFlow.value.filter {
            it.word.lowercase().contains(q) || it.shortcut.lowercase().contains(q)
        }
    }

    companion object {
        private const val DATABASE_NAME = "slashboard_keyboard.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CLIPBOARD = "clipboard_items"
        private const val COL_ID = "id"
        private const val COL_TEXT = "text"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IS_PINNED = "is_pinned"
        private const val COL_CATEGORY = "category"
        private const val COL_COPY_COUNT = "copy_count"

        private const val TABLE_DICTIONARY = "dictionary_words"
        private const val COL_DICT_ID = "dict_id"
        private const val COL_WORD = "word"
        private const val COL_SHORTCUT = "shortcut"
        private const val COL_FREQUENCY = "frequency"
        private const val COL_DATE_ADDED = "date_added"
    }
}
