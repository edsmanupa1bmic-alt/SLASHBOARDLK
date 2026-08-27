package com.slashboard.keyboard.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.slashboard.keyboard.data.model.KeyboardLayout
import com.slashboard.keyboard.data.model.KeyboardTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KeyboardSettings(
    val themeId: String = "cyber_violet",
    val layoutId: String = "qwerty",
    val hapticFeedback: Boolean = true,
    val hapticIntensity: Int = 50,
    val soundFeedback: Boolean = false,
    val popupOnKeypress: Boolean = true,
    val heightScale: Float = 1.0f,
    val autoCapitalization: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val autoCorrect: Boolean = true,
    val showNumberRow: Boolean = false,
    val showFrequentEmojiRow: Boolean = true,
    val showSecondaryLabels: Boolean = true,
    val toolbarVisible: Boolean = true,
    val bottomSpaceHeight: Int = 0,
    val keyCornerRadius: Int = 8,
    val keyBackgroundAlpha: Float = 1.0f,
    val keyBorderAlpha: Float = 1.0f,
    val showKeyBorders: Boolean = true,
    val customWallpaperPath: String? = null,
    val wallpaperDim: Float = 0.45f,
    val hasCompletedSetup: Boolean = false,
    val smartbarActiveActions: List<String> = emptyList(),
    val smartbarDisabledActions: List<String> = emptyList(),
    val customThemeBg: Long = 0xFF111827L,
    val customThemeKeyBg: Long = 0xFF1F2937L,
    val customThemeAccent: Long = 0xFF38BDF8L,
    val customThemeTextColor: Long = 0xFFF9FAFBL,
    val customThemeIsDark: Boolean = true,
    val customSpacebarLabel: String = "Slashboard"
)

class KeyboardPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<KeyboardSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): KeyboardSettings {
        val defaultPrefs = appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
        val bordersVal = if (defaultPrefs.contains("key_borders_enabled")) {
            defaultPrefs.getBoolean("key_borders_enabled", false)
        } else if (prefs.contains("key_borders_enabled")) {
            prefs.getBoolean("key_borders_enabled", false)
        } else if (prefs.contains(KEY_KEY_BORDERS)) {
            prefs.getBoolean(KEY_KEY_BORDERS, true)
        } else {
            true
        }
        val heightScaleVal = try {
            if (prefs.contains(KEY_HEIGHT_SCALE)) {
                prefs.getFloat(KEY_HEIGHT_SCALE, 1.0f)
            } else if (prefs.contains("keyboard_height_scale")) {
                prefs.getFloat("keyboard_height_scale", 1.0f)
            } else if (defaultPrefs.contains("keyboard_height_scale")) {
                defaultPrefs.getFloat("keyboard_height_scale", 1.0f)
            } else if (defaultPrefs.contains(KEY_HEIGHT_SCALE)) {
                defaultPrefs.getFloat(KEY_HEIGHT_SCALE, 1.0f)
            } else {
                1.0f
            }
        } catch (_: Throwable) {
            1.0f
        }
        val activeActionsStr = prefs.getString(KEY_SMARTBAR_ACTIVE, null)
        val disabledActionsStr = prefs.getString(KEY_SMARTBAR_DISABLED, null)
        
        val activeActions = if (activeActionsStr != null) {
            activeActionsStr.split(",").filter { it.isNotEmpty() }
        } else {
            listOf("language_switch", "emoji", "settings", "clipboard", "voice_mic")
        }
        
        val disabledActions = if (disabledActionsStr != null) {
            disabledActionsStr.split(",").filter { it.isNotEmpty() }
        } else {
            listOf("theme_picker", "text_edit", "collapse")
        }

        val bgAlphaVal = if (prefs.contains("key_bg_alpha")) {
            prefs.getFloat("key_bg_alpha", 1.0f)
        } else if (defaultPrefs.contains("key_bg_alpha")) {
            defaultPrefs.getFloat("key_bg_alpha", 1.0f)
        } else {
            1.0f
        }
        val borderAlphaVal = if (prefs.contains("key_border_alpha")) {
            prefs.getFloat("key_border_alpha", 1.0f)
        } else if (defaultPrefs.contains("key_border_alpha")) {
            defaultPrefs.getFloat("key_border_alpha", 1.0f)
        } else {
            1.0f
        }

        val customSpacebarVal = if (prefs.contains(KEY_CUSTOM_SPACEBAR_LABEL)) {
            prefs.getString(KEY_CUSTOM_SPACEBAR_LABEL, "Slashboard") ?: "Slashboard"
        } else if (defaultPrefs.contains(KEY_CUSTOM_SPACEBAR_LABEL)) {
            defaultPrefs.getString(KEY_CUSTOM_SPACEBAR_LABEL, "Slashboard") ?: "Slashboard"
        } else {
            "Slashboard"
        }

        val bottomPaddingVal = if (prefs.contains("ime_bottom_padding_dp")) {
            prefs.getInt("ime_bottom_padding_dp", 0)
        } else if (defaultPrefs.contains("ime_bottom_padding_dp")) {
            defaultPrefs.getInt("ime_bottom_padding_dp", 0)
        } else if (prefs.contains(KEY_BOTTOM_SPACE)) {
            prefs.getInt(KEY_BOTTOM_SPACE, 0)
        } else if (defaultPrefs.contains(KEY_BOTTOM_SPACE)) {
            defaultPrefs.getInt(KEY_BOTTOM_SPACE, 0)
        } else {
            0
        }

        return KeyboardSettings(
            themeId = prefs.getString(KEY_THEME_ID, "cyber_violet") ?: "cyber_violet",
            layoutId = prefs.getString(KEY_LAYOUT_ID, "qwerty") ?: "qwerty",
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            hapticIntensity = prefs.getInt(KEY_HAPTIC_INTENSITY, 50),
            soundFeedback = prefs.getBoolean(KEY_SOUND, false),
            popupOnKeypress = prefs.getBoolean(KEY_POPUP, true),
            heightScale = heightScaleVal,
            autoCapitalization = prefs.getBoolean(KEY_AUTO_CAP, true),
            doubleSpacePeriod = prefs.getBoolean(KEY_DOUBLE_SPACE, true),
            autoCorrect = prefs.getBoolean(KEY_AUTO_CORRECT, true),
            showNumberRow = prefs.getBoolean(KEY_NUMBER_ROW, false),
            showFrequentEmojiRow = prefs.getBoolean(KEY_FREQUENT_EMOJI_ROW, true),
            showSecondaryLabels = prefs.getBoolean(KEY_SECONDARY_LABELS, true),
            toolbarVisible = prefs.getBoolean(KEY_TOOLBAR, true),
            bottomSpaceHeight = bottomPaddingVal,
            keyCornerRadius = prefs.getInt(KEY_KEY_RADIUS, 8),
            keyBackgroundAlpha = bgAlphaVal,
            keyBorderAlpha = borderAlphaVal,
            showKeyBorders = bordersVal,
            customWallpaperPath = prefs.getString(KEY_CUSTOM_WALLPAPER, null),
            wallpaperDim = prefs.getFloat(KEY_WALLPAPER_DIM, 0.45f),
            hasCompletedSetup = prefs.getBoolean(KEY_HAS_COMPLETED_SETUP, false),
            smartbarActiveActions = activeActions,
            smartbarDisabledActions = disabledActions,
            customThemeBg = prefs.getLong("custom_theme_bg", 0xFF111827L),
            customThemeKeyBg = prefs.getLong("custom_theme_key_bg", 0xFF1F2937L),
            customThemeAccent = prefs.getLong("custom_theme_accent", 0xFF38BDF8L),
            customThemeTextColor = prefs.getLong("custom_theme_text_color", 0xFFF9FAFBL),
            customThemeIsDark = prefs.getBoolean("custom_theme_is_dark", true),
            customSpacebarLabel = customSpacebarVal
        )
    }

    fun updateCustomSpacebarLabel(label: String) {
        val cleanLabel = label.trim().take(12).ifBlank { "Slashboard" }
        prefs.edit().putString(KEY_CUSTOM_SPACEBAR_LABEL, cleanLabel).apply()
        try {
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit().putString(KEY_CUSTOM_SPACEBAR_LABEL, cleanLabel).apply()
        } catch (_: Throwable) {}
        _settingsFlow.value = _settingsFlow.value.copy(customSpacebarLabel = cleanLabel)
    }

    fun updateThemeId(themeId: String) {
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeId = themeId)
    }

    fun updateCustomTheme(bg: Color, keyBg: Color, accent: Color, textColor: Color, isDark: Boolean) {
        val bgVal = (bg.value.toLong())
        val keyBgVal = (keyBg.value.toLong())
        val accentVal = (accent.value.toLong())
        val textVal = (textColor.value.toLong())
        prefs.edit()
            .putLong("custom_theme_bg", bgVal)
            .putLong("custom_theme_key_bg", keyBgVal)
            .putLong("custom_theme_accent", accentVal)
            .putLong("custom_theme_text_color", textVal)
            .putBoolean("custom_theme_is_dark", isDark)
            .putString("theme_mode", "CUSTOM_COLOR")
            .putString(KEY_THEME_ID, "custom_user_theme")
            .apply()

        try {
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putInt("theme_custom_bg_color", bgVal.toInt())
                .putInt("theme_custom_key_color", keyBgVal.toInt())
                .putInt("theme_custom_accent_color", accentVal.toInt())
                .putInt("theme_custom_text_color", textVal.toInt())
                .putString("theme_mode", "CUSTOM_COLOR")
                .putString(KEY_THEME_ID, "custom_user_theme")
                .apply()
        } catch (_: Throwable) {}

        _settingsFlow.value = _settingsFlow.value.copy(
            customThemeBg = bgVal,
            customThemeKeyBg = keyBgVal,
            customThemeAccent = accentVal,
            customThemeTextColor = textVal,
            customThemeIsDark = isDark,
            themeId = "custom_user_theme"
        )

        try {
            val intent = Intent("com.slashboard.keyboard.ACTION_THEME_CHANGED").apply {
                setPackage(appContext.packageName)
            }
            appContext.sendBroadcast(intent)
        } catch (_: Throwable) {}
    }

    fun updateLayoutId(layoutId: String) {
        prefs.edit().putString(KEY_LAYOUT_ID, layoutId).apply()
        _settingsFlow.value = _settingsFlow.value.copy(layoutId = layoutId)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(hapticFeedback = enabled)
    }

    fun updateHapticIntensity(intensity: Int) {
        prefs.edit().putInt(KEY_HAPTIC_INTENSITY, intensity).apply()
        _settingsFlow.value = _settingsFlow.value.copy(hapticIntensity = intensity)
    }

    fun updateSoundFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(soundFeedback = enabled)
    }

    fun updatePopupOnKeypress(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_POPUP, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(popupOnKeypress = enabled)
    }

    fun updateHeightScale(scale: Float) {
        val clamped = (scale.coerceIn(0.70f, 1.40f) * 100).toInt() / 100f
        prefs.edit()
            .putFloat(KEY_HEIGHT_SCALE, clamped)
            .putFloat("keyboard_height_scale", clamped)
            .apply()
        try {
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_HEIGHT_SCALE, clamped)
                .putFloat("keyboard_height_scale", clamped)
                .apply()
        } catch (_: Throwable) {}
        _settingsFlow.value = _settingsFlow.value.copy(heightScale = clamped)
    }

    fun updateAutoCapitalization(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CAP, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoCapitalization = enabled)
    }

    fun updateDoubleSpacePeriod(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOUBLE_SPACE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(doubleSpacePeriod = enabled)
    }

    fun updateAutoCorrect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CORRECT, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoCorrect = enabled)
    }

    fun updateShowNumberRow(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NUMBER_ROW, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showNumberRow = enabled)
    }

    fun updateShowFrequentEmojiRow(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FREQUENT_EMOJI_ROW, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showFrequentEmojiRow = enabled)
    }

    fun getFrequentEmojis(): List<String> {
        val defaultList = listOf("❤️", "😂", "🔥", "🙏", "😍", "👍", "🥺", "✨", "🎉", "👏", "😊", "🤣", "💯", "🥰", "🙌", "😘", "😎", "🥳", "💙", "🌸")
        val saved = prefs.getString(KEY_FREQUENT_EMOJIS_CSV, null)
            ?: appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .getString(KEY_FREQUENT_EMOJIS_CSV, null)
        return if (!saved.isNullOrBlank()) {
            val list = saved.split(",").filter { it.isNotBlank() }
            if (list.isNotEmpty()) list else defaultList
        } else {
            defaultList
        }
    }

    fun saveRecentEmoji(selectedEmoji: String) {
        try {
            val current = getFrequentEmojis().toMutableList()
            current.remove(selectedEmoji)
            current.add(0, selectedEmoji)
            val toSave = current.take(30).joinToString(",")
            prefs.edit().putString(KEY_FREQUENT_EMOJIS_CSV, toSave).apply()
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit().putString(KEY_FREQUENT_EMOJIS_CSV, toSave).apply()
        } catch (_: Throwable) {}
    }

    fun updateShowSecondaryLabels(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECONDARY_LABELS, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showSecondaryLabels = enabled)
    }

    fun updateToolbarVisible(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOOLBAR, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(toolbarVisible = enabled)
    }

    fun updateKeyCornerRadius(radius: Int) {
        val clamped = radius.coerceIn(2, 20)
        prefs.edit().putInt(KEY_KEY_RADIUS, clamped).apply()
        _settingsFlow.value = _settingsFlow.value.copy(keyCornerRadius = clamped)
    }

    fun updateKeyBackgroundAlpha(alpha: Float) {
        prefs.edit().putFloat("key_bg_alpha", alpha).apply()
        _settingsFlow.value = _settingsFlow.value.copy(keyBackgroundAlpha = alpha)
    }

    fun updateKeyBorderAlpha(alpha: Float) {
        prefs.edit().putFloat("key_border_alpha", alpha).apply()
        _settingsFlow.value = _settingsFlow.value.copy(keyBorderAlpha = alpha)
    }

    fun updateShowKeyBorders(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_KEY_BORDERS, enabled)
            .putBoolean("key_borders_enabled", enabled)
            .apply()
        try {
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("key_borders_enabled", enabled)
                .putBoolean(KEY_KEY_BORDERS, enabled)
                .apply()
        } catch (_: Throwable) {}
        _settingsFlow.value = _settingsFlow.value.copy(showKeyBorders = enabled)
    }

    fun updateCustomWallpaper(wallpaperPath: String?) {
        if (wallpaperPath == null) {
            prefs.edit().remove(KEY_CUSTOM_WALLPAPER).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_WALLPAPER, wallpaperPath).apply()
        }
        _settingsFlow.value = _settingsFlow.value.copy(customWallpaperPath = wallpaperPath)
    }

    fun updateWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0.1f, 0.9f)
        prefs.edit().putFloat(KEY_WALLPAPER_DIM, clamped).apply()
        _settingsFlow.value = _settingsFlow.value.copy(wallpaperDim = clamped)
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_SETUP, completed).apply()
        _settingsFlow.value = _settingsFlow.value.copy(hasCompletedSetup = completed)
    }

    fun updateBottomSpaceHeight(height: Int) {
        val clamped = height.coerceIn(0, 48)
        prefs.edit()
            .putInt(KEY_BOTTOM_SPACE, clamped)
            .putInt("ime_bottom_padding_dp", clamped)
            .apply()
        try {
            appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putInt("ime_bottom_padding_dp", clamped)
                .putInt(KEY_BOTTOM_SPACE, clamped)
                .apply()
        } catch (_: Throwable) {}
        _settingsFlow.value = _settingsFlow.value.copy(bottomSpaceHeight = clamped)
    }

    fun updateSmartbarActions(active: List<String>, disabled: List<String>) {
        val activeStr = active.joinToString(",")
        val disabledStr = disabled.joinToString(",")
        prefs.edit()
            .putString(KEY_SMARTBAR_ACTIVE, activeStr)
            .putString(KEY_SMARTBAR_DISABLED, disabledStr)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            smartbarActiveActions = active,
            smartbarDisabledActions = disabled
        )
    }

    fun hasCompletedSetup(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_SETUP, false)
    }

    fun getActiveTheme(): KeyboardTheme {
        val currentThemeId = _settingsFlow.value.themeId
        val baseTheme = if (currentThemeId == "custom_user_theme") {
            KeyboardTheme.buildCustomTheme(
                id = "custom_user_theme",
                name = "My Custom Theme",
                background = Color(_settingsFlow.value.customThemeBg.toULong()),
                keyBackground = Color(_settingsFlow.value.customThemeKeyBg.toULong()),
                accentColor = Color(_settingsFlow.value.customThemeAccent.toULong()),
                textColor = Color(_settingsFlow.value.customThemeTextColor.toULong()),
                isDark = _settingsFlow.value.customThemeIsDark,
                radiusDp = _settingsFlow.value.keyCornerRadius
            )
        } else {
            KeyboardTheme.getThemeById(currentThemeId)
        }
        val showBorders = _settingsFlow.value.showKeyBorders
        val bgAlpha = _settingsFlow.value.keyBackgroundAlpha.coerceIn(0.0f, 1.0f)
        val borderAlpha = _settingsFlow.value.keyBorderAlpha.coerceIn(0.0f, 1.0f)

        fun adjustAlpha(color: Color, factor: Float): Color {
            return if (color == Color.Transparent) {
                Color.Transparent
            } else {
                color.copy(alpha = (color.alpha * factor).coerceIn(0f, 1f))
            }
        }

        val rawBorderColor = if (baseTheme.keyBorderColor != Color.Transparent) {
            baseTheme.keyBorderColor
        } else if (baseTheme.isDark) {
            Color(0x66FFFFFF)
        } else {
            Color(0x40000000)
        }

        val borderColor = if (showBorders) {
            adjustAlpha(rawBorderColor, borderAlpha)
        } else {
            Color.Transparent
        }

        return baseTheme.copy(
            keyRadiusDp = _settingsFlow.value.keyCornerRadius,
            keyBackground = adjustAlpha(baseTheme.keyBackground, bgAlpha),
            keyPressedBackground = adjustAlpha(baseTheme.keyPressedBackground, bgAlpha),
            functionalKeyBackground = adjustAlpha(baseTheme.functionalKeyBackground, bgAlpha),
            keyBorderColor = borderColor
        )
    }

    fun getActiveLayout(): KeyboardLayout {
        return KeyboardLayout.getLayoutById(_settingsFlow.value.layoutId)
    }

    companion object {
        private const val PREFS_NAME = "slashboard_prefs"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_LAYOUT_ID = "layout_id"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_SOUND = "sound_feedback"
        private const val KEY_POPUP = "popup_on_keypress"
        private const val KEY_HEIGHT_SCALE = "height_scale"
        private const val KEY_AUTO_CAP = "auto_capitalization"
        private const val KEY_DOUBLE_SPACE = "double_space_period"
        private const val KEY_AUTO_CORRECT = "auto_correct"
        private const val KEY_NUMBER_ROW = "show_number_row"
        private const val KEY_FREQUENT_EMOJI_ROW = "show_frequent_emoji_row"
        private const val KEY_FREQUENT_EMOJIS_CSV = "frequent_emojis_csv"
        private const val KEY_SECONDARY_LABELS = "show_secondary_labels"
        private const val KEY_TOOLBAR = "toolbar_visible"
        private const val KEY_KEY_RADIUS = "key_corner_radius"
        private const val KEY_KEY_BORDERS = "show_key_borders"
        private const val KEY_CUSTOM_WALLPAPER = "custom_wallpaper_path"
        private const val KEY_WALLPAPER_DIM = "wallpaper_dim_alpha"
        private const val KEY_HAS_COMPLETED_SETUP = "has_completed_setup"
        private const val KEY_BOTTOM_SPACE = "bottom_space"
        private const val KEY_SMARTBAR_ACTIVE = "smartbar_active_actions"
        private const val KEY_SMARTBAR_DISABLED = "smartbar_disabled_actions"
        private const val KEY_CUSTOM_SPACEBAR_LABEL = "custom_spacebar_label"
    }
}
