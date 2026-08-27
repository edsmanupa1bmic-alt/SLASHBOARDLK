package com.slashboard.keyboard.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.slashboard.keyboard.R
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.data.model.ThemeCategory

fun Int.dpToPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}

fun Float.dpToPx(context: Context): Float {
    val density = context.resources.displayMetrics.density
    return this * density
}

object ThemeEngine {

    const val ACTION_THEME_CHANGED = "com.slashboard.keyboard.ACTION_THEME_CHANGED"

    // Preset popular hex swatches for Custom Color mode
    val PRESET_SWATCHES = listOf(
        CustomColorPreset("Deep Purple", Color.parseColor("#1F1A24"), Color.parseColor("#2E2738"), Color.parseColor("#BB86FC"), Color.parseColor("#FFFFFF")),
        CustomColorPreset("Midnight Blue", Color.parseColor("#0D1B2A"), Color.parseColor("#1B263B"), Color.parseColor("#415A77"), Color.parseColor("#E0E1DD")),
        CustomColorPreset("Emerald", Color.parseColor("#064E3B"), Color.parseColor("#065F46"), Color.parseColor("#34D399"), Color.parseColor("#ECFDF5")),
        CustomColorPreset("Crimson", Color.parseColor("#450A0A"), Color.parseColor("#7F1D1D"), Color.parseColor("#F87171"), Color.parseColor("#FEF2F2")),
        CustomColorPreset("Cyberpunk", Color.parseColor("#120E2E"), Color.parseColor("#241442"), Color.parseColor("#F43F5E"), Color.parseColor("#38BDF8")),
        CustomColorPreset("Matte Black", Color.parseColor("#121212"), Color.parseColor("#1E1E1E"), Color.parseColor("#BB86FC"), Color.parseColor("#E0E0E0"))
    )

    data class CustomColorPreset(
        val name: String,
        val bgColor: Int,
        val keyColor: Int,
        val accentColor: Int,
        val textColor: Int
    )

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    fun isKeyBordersEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (prefs.contains("key_borders_enabled")) {
            return prefs.getBoolean("key_borders_enabled", false)
        }
        val customPrefs = context.getSharedPreferences("slashboard_prefs", Context.MODE_PRIVATE)
        if (customPrefs.contains("key_borders_enabled")) {
            return customPrefs.getBoolean("key_borders_enabled", false)
        }
        if (customPrefs.contains("show_key_borders")) {
            return customPrefs.getBoolean("show_key_borders", false)
        }
        return prefs.getBoolean("show_key_borders", false)
    }

    fun isCustomColorMode(context: Context): Boolean {
        val prefs = getPrefs(context)
        val mode = prefs.getString("theme_mode", "") ?: ""
        val themeId = prefs.getString("theme_id", "") ?: ""
        return mode == "CUSTOM_COLOR" || themeId == "custom_user_theme"
    }

    fun getCustomBgColor(context: Context): Int {
        val prefs = getPrefs(context)
        val app = context.applicationContext as? SlashboardApp
        val repoVal = app?.preferencesRepository?.settingsFlow?.value?.customThemeBg
        if (repoVal != null && repoVal != 0L) {
            return repoVal.toInt()
        }
        return prefs.getInt("theme_custom_bg_color", Color.parseColor("#111827"))
    }

    fun getCustomKeyColor(context: Context): Int {
        val prefs = getPrefs(context)
        val app = context.applicationContext as? SlashboardApp
        val repoVal = app?.preferencesRepository?.settingsFlow?.value?.customThemeKeyBg
        if (repoVal != null && repoVal != 0L) {
            return repoVal.toInt()
        }
        return prefs.getInt("theme_custom_key_color", Color.parseColor("#1F2937"))
    }

    fun getCustomTextColor(context: Context): Int {
        val prefs = getPrefs(context)
        val app = context.applicationContext as? SlashboardApp
        val repoVal = app?.preferencesRepository?.settingsFlow?.value?.customThemeTextColor
        if (repoVal != null && repoVal != 0L) {
            return repoVal.toInt()
        }
        return prefs.getInt("theme_custom_text_color", Color.parseColor("#F9FAFB"))
    }

    fun getCustomAccentColor(context: Context): Int {
        val prefs = getPrefs(context)
        val app = context.applicationContext as? SlashboardApp
        val repoVal = app?.preferencesRepository?.settingsFlow?.value?.customThemeAccent
        if (repoVal != null && repoVal != 0L) {
            return repoVal.toInt()
        }
        return prefs.getInt("theme_custom_accent_color", Color.parseColor("#38BDF8"))
    }

    fun saveCustomThemeColors(
        context: Context,
        bgColor: Int,
        keyColor: Int,
        textColor: Int,
        accentColor: Int,
        isDark: Boolean = true
    ) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putInt("theme_custom_bg_color", bgColor)
            .putInt("theme_custom_key_color", keyColor)
            .putInt("theme_custom_text_color", textColor)
            .putInt("theme_custom_accent_color", accentColor)
            .putString("theme_mode", "CUSTOM_COLOR")
            .putString("theme_id", "custom_user_theme")
            .apply()

        // Also sync with main repo
        try {
            val app = context.applicationContext as? SlashboardApp
            app?.preferencesRepository?.updateCustomTheme(
                bg = androidx.compose.ui.graphics.Color(bgColor),
                keyBg = androidx.compose.ui.graphics.Color(keyColor),
                accent = androidx.compose.ui.graphics.Color(accentColor),
                textColor = androidx.compose.ui.graphics.Color(textColor),
                isDark = isDark
            )
        } catch (_: Throwable) {}

        broadcastThemeChange(context)
    }

    fun getActiveTheme(context: Context): KeyboardTheme {
        val app = context.applicationContext as? SlashboardApp
        if (app != null) {
            return app.preferencesRepository.getActiveTheme()
        }
        if (isCustomColorMode(context)) {
            val bg = getCustomBgColor(context)
            val keyBg = getCustomKeyColor(context)
            val text = getCustomTextColor(context)
            val accent = getCustomAccentColor(context)
            return KeyboardTheme.buildCustomTheme(
                id = "custom_user_theme",
                name = "Custom Color Theme",
                background = androidx.compose.ui.graphics.Color(bg),
                keyBackground = androidx.compose.ui.graphics.Color(keyBg),
                accentColor = androidx.compose.ui.graphics.Color(accent),
                textColor = androidx.compose.ui.graphics.Color(text),
                isDark = true
            )
        }
        val prefs = getPrefs(context)
        val themeId = prefs.getString("theme_id", "cyber_violet") ?: "cyber_violet"
        return KeyboardTheme.getThemeById(themeId)
    }

    fun broadcastThemeChange(context: Context) {
        try {
            val intent = Intent(ACTION_THEME_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}
    }

    /**
     * Dynamically generates the Key Background GradientDrawable based on current border preference
     */
    fun createKeyDrawable(context: Context, isPressed: Boolean): Drawable {
        val bordersEnabled = isKeyBordersEnabled(context)
        val borderWidthPx = if (bordersEnabled) 1.dpToPx(context) else 0
        val borderColor = if (bordersEnabled) Color.parseColor("#4DFFFFFF") else Color.TRANSPARENT
        val normalDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dpToPx(context).toFloat()
            setColor(if (isPressed) Color.parseColor("#33FFFFFF") else Color.parseColor("#14FFFFFF"))
            setStroke(borderWidthPx, borderColor)
        }
        return normalDrawable
    }

    /**
     * Creates a reactive ripple drawable for standard/special IME keys
     */
    fun createKeyRippleDrawable(
        context: Context,
        fillColor: Int = Color.parseColor("#24FFFFFF"),
        strokeColor: Int = Color.parseColor("#66FFFFFF"),
        radiusDp: Float = 8f,
        strokeWidthDp: Float = 1.2f,
        bordersEnabled: Boolean = isKeyBordersEnabled(context)
    ): Drawable {
        val strokeWidthPx = if (bordersEnabled) (strokeWidthDp * context.resources.displayMetrics.density).toInt() else 0
        val actualStrokeColor = if (bordersEnabled) strokeColor else Color.TRANSPARENT
        val radiusPx = radiusDp * context.resources.displayMetrics.density

        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = radiusPx
            if (strokeWidthPx > 0 && bordersEnabled) {
                setStroke(strokeWidthPx, actualStrokeColor)
            } else {
                setStroke(0, Color.TRANSPARENT)
            }
        }

        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = radiusPx
        }

        return RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
            content,
            mask
        )
    }

    fun applyCustomBackground(context: Context, rootView: View, bitmap: android.graphics.Bitmap?) {
        val bgImageView = rootView.findViewById<ImageView>(R.id.keyboard_bg_image) ?: return
        val dimView = rootView.findViewById<View>(R.id.keyboard_bg_dim)

        if (bitmap != null) {
            bgImageView.visibility = View.VISIBLE
            bgImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            bgImageView.setImageBitmap(bitmap)

            val prefs = getPrefs(context)
            val dimLevel = try {
                prefs.getInt("theme_bg_dim_level", 40) / 100f
            } catch (e: Exception) {
                0.4f
            }
            dimView?.visibility = View.VISIBLE
            dimView?.alpha = dimLevel
        } else {
            bgImageView.visibility = View.GONE
            dimView?.visibility = View.GONE
        }
    }
}
