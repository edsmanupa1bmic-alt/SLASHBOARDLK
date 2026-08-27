package com.slashboard.keyboard.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SmartbarAction(val id: String, val title: String, val iconResName: String) {
    LANGUAGE_SWITCH("language_switch", "Language Toggle", "ic_globe"),
    EMOJI("emoji", "Emoji / Sticker Panel", "ic_emoji_smile"),
    SETTINGS("settings", "Keyboard Settings", "ic_settings_gear"),
    TEXT_EDIT("text_edit", "Text Styler & Case Selector", "ic_text_format"),
    VOICE_MIC("voice_mic", "Voice Typing", "ic_mic"),
    CLIPBOARD("clipboard", "Clipboard History", "ic_content_paste"),
    COLLAPSE("collapse", "Collapse Keyboard", "ic_keyboard_hide"),
    THEME_PICKER("theme_picker", "Themes", "ic_settings_gear");

    companion object {
        val DEFAULT_ACTIVE = listOf(
            LANGUAGE_SWITCH,
            EMOJI,
            SETTINGS,
            TEXT_EDIT,
            VOICE_MIC,
            CLIPBOARD,
            COLLAPSE
        )
        val DEFAULT_DISABLED = listOf(THEME_PICKER)
        
        fun fromId(id: String): SmartbarAction? {
            return values().find { it.id == id }
        }
    }
}

