package com.slashboard.keyboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipboardItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val category: String = "general",
    val copyCount: Int = 1
)
