package com.slashboard.keyboard.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.slashboard.keyboard.R
import com.slashboard.keyboard.SettingsActivity

/**
 * SmartbarView: Top Action Toolbar implementing the exact 7 circular action chips from left to right:
 * 1. En (Language Toggle): Toggles between Sinhala (Singlish/Helakuru standard) and English (US).
 * 2. Emoji Icon (☺): Opens the comprehensive Emoji / Sticker panel.
 * 3. Settings / Grid Icon (⚙): Directly opens Keyboard Settings Hub.
 * 4. Aa Icon: Opens Text Styler & Case Selector (Uppercase, Lowercase, Title Case, Fancy Fonts).
 * 5. Voice Mic Icon (🎤): Launches the Real-Time Sinhala/English Voice Typing Overlay.
 * 6. Clipboard Icon (📋): Opens Copied History & Pinned text manager.
 * 7. Collapse Arrow (˅): Calls requestHideSelf(0) to dismiss the keyboard.
 */
class SmartbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        fun launchSettingsActivity(context: Context) {
            try {
                val intent = Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {}
            }
        }
    }

    interface SmartbarActionListener {
        fun onLanguageToggle()
        fun onEmojiClick()
        fun onSettingsClick()
        fun onTextStylerClick()
        fun onVoiceMicClick()
        fun onClipboardClick()
        fun onCollapseClick()
    }

    private var listener: SmartbarActionListener? = null
    private var langTextView: TextView? = null
    private var isSinglish: Boolean = true

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = dpToPx(4f)
        setPadding(padH, 0, padH, 0)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(42f)
        )
        setupActionChips()
    }

    fun setSmartbarActionListener(listener: SmartbarActionListener) {
        this.listener = listener
    }

    fun updateLanguageState(singlish: Boolean) {
        isSinglish = singlish
        langTextView?.text = if (singlish) "Si" else "En"
    }

    private fun setupActionChips() {
        removeAllViews()

        // 1. Language Toggle Chip (En / Si)
        val langChip = createLanguageChip()
        addView(langChip)

        // 2. Emoji Icon (☺)
        val emojiChip = createActionIconButton(
            iconRes = R.drawable.ic_emoji_smile,
            contentDesc = "Emoji Panel"
        ) {
            listener?.onEmojiClick()
        }
        addView(emojiChip)

        // 3. Settings / Grid Icon (⚙)
        val settingsChip = createActionIconButton(
            iconRes = R.drawable.ic_settings_gear,
            contentDesc = "Settings Hub"
        ) {
            if (listener != null) {
                listener?.onSettingsClick()
            } else {
                launchSettingsActivity(context)
            }
        }
        addView(settingsChip)

        // 4. Aa Icon (Text Styler & Case Selector)
        val textStylerChip = createActionIconButton(
            iconRes = R.drawable.ic_text_format,
            contentDesc = "Text Styler & Case Selector"
        ) {
            listener?.onTextStylerClick()
        }
        addView(textStylerChip)

        // 5. Voice Mic Icon (🎤)
        val voiceMicChip = createActionIconButton(
            iconRes = R.drawable.ic_mic,
            contentDesc = "Voice Typing"
        ) {
            listener?.onVoiceMicClick()
        }
        voiceMicChip.id = R.id.btn_utility_mic
        addView(voiceMicChip)

        // 6. Clipboard Icon (📋)
        val clipboardChip = createActionIconButton(
            iconRes = R.drawable.ic_content_paste,
            contentDesc = "Clipboard History"
        ) {
            listener?.onClipboardClick()
        }
        addView(clipboardChip)

        // 7. Collapse Arrow (˅)
        val collapseChip = createActionIconButton(
            iconRes = R.drawable.ic_keyboard_hide,
            contentDesc = "Collapse Keyboard"
        ) {
            listener?.onCollapseClick()
        }
        addView(collapseChip)
    }

    private fun createLanguageChip(): View {
        val size = dpToPx(34f)
        val lp = LayoutParams(0, size, 1.0f).apply {
            val margin = dpToPx(2f)
            setMargins(margin, 0, margin, 0)
        }

        val container = FrameLayout(context).apply {
            layoutParams = lp
            background = createCircularRippleDrawable()
            isClickable = true
            isFocusable = false
        }

        val textView = TextView(context).apply {
            text = if (isSinglish) "Si" else "En"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        langTextView = textView
        container.addView(textView)

        container.setOnClickListener {
            listener?.onLanguageToggle()
        }
        return container
    }

    private fun createActionIconButton(
        iconRes: Int,
        contentDesc: String,
        onClick: () -> Unit
    ): View {
        val size = dpToPx(34f)
        val lp = LayoutParams(0, size, 1.0f).apply {
            val margin = dpToPx(2f)
            setMargins(margin, 0, margin, 0)
        }

        val imageView = ImageView(context).apply {
            layoutParams = lp
            val pad = dpToPx(7f)
            setPadding(pad, pad, pad, pad)
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            contentDescription = contentDesc
            background = createCircularRippleDrawable()
            isClickable = true
            isFocusable = false
            setOnClickListener {
                onClick()
            }
        }
        return imageView
    }

    private fun createCircularRippleDrawable(): RippleDrawable {
        val contentShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#26FFFFFF")) // Translucent circular background
        }
        val maskShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
        }
        val rippleColor = ColorStateList.valueOf(Color.parseColor("#4DFFFFFF"))
        return RippleDrawable(rippleColor, contentShape, maskShape)
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
