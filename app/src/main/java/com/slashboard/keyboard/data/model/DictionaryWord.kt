package com.slashboard.keyboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DictionaryWord(
    val id: Long = System.currentTimeMillis(),
    val word: String,
    val shortcut: String = "",
    val frequency: Int = 250,
    val dateAdded: Long = System.currentTimeMillis()
)
