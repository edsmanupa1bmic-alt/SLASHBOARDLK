package com.slashboard.keyboard.service

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.slashboard.keyboard.MainActivity
import com.slashboard.keyboard.R
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.repository.HelakuruSinglishParser
import com.slashboard.keyboard.data.repository.SuggestionManager
import com.slashboard.keyboard.util.ThemeEngine
import com.slashboard.keyboard.data.model.SmartbarAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Principal Android IME Service for Sinhala & English phonetic typing.
 *
 * Implements:
 * 1. Top-to-Bottom Row Hierarchy (Rows 0-6):
 *    - Layer 0: Root Container with Transparent/Translucent Wallpaper & Dim View
 *    - Row 0 (42dp): Topmost Dynamic Header:
 *        * #utility_settings_bar ([En], [Emoji], [Settings], [Aa], [Mic], [Clipboard], [Collapse])
 *        * #word_suggestion_bar (#suggestion_chips_container)
 *    - Row 1 (36dp): Dedicated Frequently Used Emoji Bar ([ 🥺 | 😔 | 💔 | 😇 | 😎 | 😁 | 😽 | ❤️ | 🤤 | 😘 ])
 *    - Row 2 (42dp): Dedicated Number Row [1 2 3 4 5 6 7 8 9 0]
 *    - Row 3 (46dp): QWERTY Row [q w e r t y u i o p] with secondary hints
 *    - Row 4 (46dp): Home Row [a s d f g h j k l] with secondary hints
 *    - Row 5 (46dp): Bottom Character Row [Shift, z x c v b n m, Backspace]
 *    - Row 6 (46dp): Action Row [123, Globe, ,, Spacebar - Red Pill, ., Enter - Blue Pill]
 * 2. Dynamic cross-fade transitions between #utility_settings_bar and #word_suggestion_bar.
 * 3. Real-time Singlish & English suggestion pipeline via SuggestionManager & HelakuruSinglishParser.
 */
open class SinhalaIME : LatinIME() {

    // Views
    protected var rootView: View? = null
    protected var utilitySettingsBar: LinearLayout? = null
    protected var wordSuggestionBar: HorizontalScrollView? = null
    protected var suggestionChipsContainer: LinearLayout? = null
    protected var emojiContainer: LinearLayout? = null
    protected var spacebarLabel: TextView? = null
    protected var shiftKey: ImageView? = null
    protected var langToggleBtn: TextView? = null
    protected var wallpaperView: ImageView? = null
    protected var dimView: View? = null

    // State
    protected val composingBuffer = StringBuilder()
    protected var isSinglishMode = true
    protected var isShiftActive = false
    protected var isCapsLocked = false
    protected var lastShiftPressTime = 0L
    protected var isSuggestionStripVisible = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceInputManager: VoiceInputManager? = null
    private var voiceOverlayController: VoiceOverlayController? = null

    // User-frequent emojis for Row 1
    private val defaultEmojis = listOf("🥺", "😔", "💔", "😇", "😎", "😁", "😽", "❤️", "🤤", "😘", "🔥", "😂", "👍", "🙏")

    private val themeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            applyCustomWallpaper()
            applyKeyStyles()
            applyKeyboardHeight()
            setupUtilityBarActions()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val filter = android.content.IntentFilter(ThemeEngine.ACTION_THEME_CHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(themeReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(themeReceiver, filter)
            }
        } catch (_: Throwable) {}

        serviceScope.launch {
            (application as? SlashboardApp)?.preferencesRepository?.settingsFlow?.collect {
                applyCustomWallpaper()
                applyKeyStyles()
                applyKeyboardHeight()
                setupUtilityBarActions()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(themeReceiver)
        } catch (_: Throwable) {}
        serviceScope.cancel()
        voiceInputManager?.destroy()
    }

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.keyboard_root, null) as ViewGroup
        this.rootView = rootView
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val navBarInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBarInset.bottom)
            insets
        }

        applyBottomPadding(rootView)

        val app = application as? SlashboardApp
        com.slashboard.keyboard.data.repository.SmartDictionaryEngine.initialize(this, app?.database)

        voiceInputManager = VoiceInputManager(this)
        voiceOverlayController = VoiceOverlayController(
            rootView = rootView,
            voiceInputManager = voiceInputManager!!,
            onVoiceFinished = {
                val micIcon = rootView.findViewById<ImageView>(R.id.btn_utility_mic)
                micIcon?.clearColorFilter()
            }
        )

        bindViews(rootView)

        // 1. Populate Emojis
        populateRecentEmojis(rootView)

        // 2. Bind Keys with Dynamic Theme/Borders
        bindKeyboardKeys(rootView)

        setupKeyListeners(rootView)
        setupUtilityBarActions()
        updateLanguageState(isSinglishMode)

        return rootView
    }

    private fun bindViews(root: View) {
        utilitySettingsBar = root.findViewById(R.id.smartbar_idle_layer)
        wordSuggestionBar = root.findViewById(R.id.smartbar_suggestion_layer)
        suggestionChipsContainer = root.findViewById(R.id.suggestion_chips_container)
        emojiContainer = root.findViewById(R.id.frequent_emoji_container)
        spacebarLabel = root.findViewById(R.id.spacebar_label)
        shiftKey = root.findViewById(R.id.key_shift)
        langToggleBtn = root.findViewById(R.id.btn_lang_toggle)
        wallpaperView = root.findViewById(R.id.keyboard_bg_image)
        dimView = root.findViewById(R.id.keyboard_bg_dim)

        applyCustomWallpaper()

        // Ensure default state: utility_settings_bar VISIBLE, word_suggestion_bar GONE
        utilitySettingsBar?.apply {
            visibility = View.VISIBLE
            alpha = 1.0f
            translationY = 0f
        }
        wordSuggestionBar?.apply {
            visibility = View.GONE
            alpha = 0.0f
            translationY = 15f
        }
        suggestionChipsContainer?.removeAllViews()
        isSuggestionStripVisible = false
    }

    private fun populateRecentEmojis(rootView: View) {
        val app = application as? SlashboardApp
        val emojis = app?.preferencesRepository?.getFrequentEmojis() ?: defaultEmojis

        val container = rootView.findViewById<LinearLayout>(R.id.frequent_emoji_container) ?: return
        container.removeAllViews()
        for (emoji in emojis) {
            val tv = TextView(this).apply {
                text = emoji
                textSize = 21f
                gravity = Gravity.CENTER
                setPadding(24, 2, 24, 2)
                isClickable = true
                isFocusable = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    commitComposingText()
                    currentInputConnection?.commitText(emoji, 1)
                    performHapticFeedback()
                    saveRecentEmoji(emoji)
                    this@SinhalaIME.rootView?.let { root -> populateRecentEmojis(root) }
                }
            }
            container.addView(tv)
        }
    }

    private fun setupRecentEmojiBar(rootView: View) {
        populateRecentEmojis(rootView)
    }

    fun bindKeyboardKeys(rootView: View) {
        applyKeyStyles()
        applyKeyboardHeight()
    }

    private fun applyCustomWallpaper() {
        val app = application as? SlashboardApp ?: return
        val settings = app.preferencesRepository.settingsFlow.value
        val wallpaperPath = settings.customWallpaperPath
        
        var bitmap: android.graphics.Bitmap? = null
        if (!wallpaperPath.isNullOrBlank()) {
            bitmap = com.slashboard.keyboard.data.repository.OnlineThemeRepository.loadWallpaperBitmap(wallpaperPath)
        }
        
        rootView?.let {
            com.slashboard.keyboard.util.ThemeEngine.applyCustomBackground(this, it, bitmap)
            if (bitmap != null) {
                it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                val activeTheme = app.preferencesRepository.getActiveTheme()
                val bgInt = android.graphics.Color.argb(
                    (activeTheme.background.alpha * 255).toInt(),
                    (activeTheme.background.red * 255).toInt(),
                    (activeTheme.background.green * 255).toInt(),
                    (activeTheme.background.blue * 255).toInt()
                )
                it.setBackgroundColor(bgInt)
            }
        }
    }

    private fun setupEmojiBar() {
        rootView?.let { populateRecentEmojis(it) }
    }

    private fun saveRecentEmoji(selectedEmoji: String) {
        try {
            val prefs = getSharedPreferences("${packageName}_preferences", android.content.Context.MODE_PRIVATE)
            val current = prefs.getString("frequent_emojis_csv", null)?.split(",")?.filter { it.isNotBlank() }?.toMutableList()
                ?: defaultEmojis.toMutableList()
            current.remove(selectedEmoji)
            current.add(0, selectedEmoji)
            val toSave = current.take(30).joinToString(",")
            prefs.edit().putString("frequent_emojis_csv", toSave).apply()
        } catch (_: Exception) {}
    }

    fun performHapticFeedback() {
        try {
            val app = application as? SlashboardApp
            val settings = app?.preferencesRepository?.settingsFlow?.value
            val hapticEnabled = settings?.hapticFeedback ?: true
            if (!hapticEnabled) return

            rootView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            val intensity = settings?.hapticIntensity ?: 20
            if (intensity > 0) {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createOneShot(
                                intensity.toLong(),
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(intensity.toLong())
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Composing Active Animation:
     * - Animate #utility_settings_bar from 1f to 0f (translationY = -15f), 150ms -> GONE
     * - Show #word_suggestion_bar: VISIBLE, animate from 0f to 1f (translationY = 0f), 150ms
     */
    fun showSuggestionBar() {
        if (isSuggestionStripVisible) return
        isSuggestionStripVisible = true

        val uBar = utilitySettingsBar
        val sStrip = wordSuggestionBar

        uBar?.animate()
            ?.alpha(0f)
            ?.translationY(-15f)
            ?.setDuration(150)
            ?.withEndAction {
                uBar.visibility = View.GONE
            }
            ?.start()

        sStrip?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 15f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(150)
                .start()
        }
    }

    /**
     * Composing Finished Animation:
     * - Animate #word_suggestion_bar from 1f to 0f (translationY = 15f), 150ms -> GONE, clear chips
     * - Show #utility_settings_bar: VISIBLE, animate from 0f to 1f (translationY = 0f), 150ms
     */
    fun showUtilityBar() {
        if (!isSuggestionStripVisible) return
        isSuggestionStripVisible = false

        val uBar = utilitySettingsBar
        val sStrip = wordSuggestionBar

        sStrip?.animate()
            ?.alpha(0f)
            ?.translationY(15f)
            ?.setDuration(150)
            ?.withEndAction {
                sStrip.visibility = View.GONE
                suggestionChipsContainer?.removeAllViews()
            }
            ?.start()

        uBar?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = -15f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(150)
                .start()
        }
    }

    /**
     * Trigger real-time suggestions and dynamic cross-fade
     */
    private fun updateSuggestions() {
        val sContainer = suggestionChipsContainer ?: return
        val currentQuery = composingBuffer.toString()
        val ic = currentInputConnection
        val fullText = ic?.getTextBeforeCursor(100, 0)?.toString() ?: ""
        
        val userLearningManager = com.slashboard.keyboard.data.repository.UserLearningManager.getInstance(this)

        // Generate candidate suggestions
        val suggestions = SuggestionManager.getSuggestions(
            fullText = fullText,
            currentComposing = currentQuery,
            isSinglish = isSinglishMode,
            learningManager = userLearningManager
        ).take(5)

        if (suggestions.isNotEmpty()) {
            SuggestionManager.renderSuggestionChips(
                container = sContainer,
                suggestions = suggestions
            ) { selectedItem ->
                vibrateFeedback()
                commitSelectedSuggestion(selectedItem.replacement, currentQuery)
            }
            showSuggestionBar()
        } else {
            showUtilityBar()
        }
    }

    private fun commitSelectedSuggestion(replacement: String, inputPrefix: String = "") {
        val ic = currentInputConnection ?: return
        ic.commitText("$replacement ", 1)
        composingBuffer.clear()
        
        com.slashboard.keyboard.data.repository.UserLearningManager.getInstance(this)
            .onWordCommitted(replacement, inputPrefix)
            
        updateSuggestions()
    }

    private fun commitComposingText(overrideWith: String? = null) {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val prefix = composingBuffer.toString()
            val textToCommit = overrideWith ?: if (isSinglishMode) {
                HelakuruSinglishParser.parse(prefix)
            } else {
                prefix
            }
            ic.commitText(textToCommit, 1)
            composingBuffer.clear()
            
            com.slashboard.keyboard.data.repository.UserLearningManager.getInstance(this)
                .onWordCommitted(textToCommit, prefix)
        }
        updateSuggestions()
    }

    /**
     * Key Bindings across Rows 2-6
     */
    private fun setupKeyListeners(root: View) {
        // ROW 2: Number Row [1 2 3 4 5 6 7 8 9 0]
        val numIds = intArrayOf(
            R.id.key_num_1, R.id.key_num_2, R.id.key_num_3, R.id.key_num_4, R.id.key_num_5,
            R.id.key_num_6, R.id.key_num_7, R.id.key_num_8, R.id.key_num_9, R.id.key_num_0
        )
        val nums = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        for (i in numIds.indices) {
            root.findViewById<TextView>(numIds[i])?.setOnClickListener {
                vibrateFeedback()
                commitComposingText()
                currentInputConnection?.commitText(nums[i], 1)
            }
        }

        // ROW 3 & 4 & 5: QWERTY Character Keys
        val charKeyMap = mapOf(
            R.id.key_q to "q", R.id.key_w to "w", R.id.key_e to "e", R.id.key_r to "r", R.id.key_t to "t",
            R.id.key_y to "y", R.id.key_u to "u", R.id.key_i to "i", R.id.key_o to "o", R.id.key_p to "p",
            R.id.key_a to "a", R.id.key_s to "s", R.id.key_d to "d", R.id.key_f to "f", R.id.key_g to "g",
            R.id.key_h to "h", R.id.key_j to "j", R.id.key_k to "k", R.id.key_l to "l",
            R.id.key_z to "z", R.id.key_x to "x", R.id.key_c to "c", R.id.key_v to "v", R.id.key_b to "b",
            R.id.key_n to "n", R.id.key_m to "m"
        )

        charKeyMap.forEach { (viewId, charStr) ->
            root.findViewById<TextView>(viewId)?.setOnClickListener {
                vibrateFeedback()
                handleCharacterInput(charStr)
            }
        }

        // ROW 5: Shift Key
        root.findViewById<ImageView>(R.id.key_shift)?.setOnClickListener {
            vibrateFeedback()
            handleShiftToggle()
        }

        // ROW 5: Backspace Key
        val backspaceKey = root.findViewById<ImageView>(R.id.key_backspace)
        if (backspaceKey != null) {
            val backspaceHandler = AcceleratedBackspaceHandler(
                keyView = backspaceKey,
                inputConnectionProvider = { currentInputConnection },
                vibrateFeedback = { vibrateFeedback() },
                onDeleteCharacter = { handleBackspace() }
            )
            backspaceKey.setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        backspaceHandler.onTouchDown()
                        true
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        backspaceHandler.onTouchUpOrCancel()
                        true
                    }
                    else -> false
                }
            }
        }

        // ROW 6: ?123 Symbols Toggle
        root.findViewById<TextView>(R.id.key_sym_toggle)?.setOnClickListener {
            vibrateFeedback()
            commitComposingText()
        }

        // ROW 6: Language Toggle [EN / Si]
        root.findViewById<View>(R.id.key_globe)?.setOnClickListener {
            vibrateFeedback()
            toggleLanguage()
        }

        // ROW 6: Comma [,]
        root.findViewById<TextView>(R.id.key_comma)?.setOnClickListener {
            vibrateFeedback()
            commitComposingText()
            currentInputConnection?.commitText(",", 1)
        }

        // ROW 6: Red Pill Spacebar
        root.findViewById<View>(R.id.key_spacebar)?.setOnClickListener {
            vibrateFeedback()
            handleSpacePress()
        }

        // ROW 6: Period [.]
        root.findViewById<TextView>(R.id.key_period)?.setOnClickListener {
            vibrateFeedback()
            commitComposingText()
            currentInputConnection?.commitText(".", 1)
        }

        // ROW 6: Blue Pill Enter
        root.findViewById<ImageView>(R.id.key_enter)?.setOnClickListener {
            vibrateFeedback()
            handleEnterPress()
        }
    }

    private fun handleCharacterInput(baseChar: String) {
        val ic = currentInputConnection ?: return
        val effectiveChar = if (isShiftActive || isCapsLocked) {
            baseChar.uppercase()
        } else {
            baseChar.lowercase()
        }

        if (isShiftActive && !isCapsLocked) {
            isShiftActive = false
            updateShiftUI()
        }

        if (isSinglishMode) {
            composingBuffer.append(effectiveChar)
            val transliterated = HelakuruSinglishParser.parse(composingBuffer.toString())
            ic.setComposingText(transliterated, 1)
            updateSuggestions()
        } else {
            composingBuffer.append(effectiveChar)
            ic.setComposingText(composingBuffer.toString(), 1)
            updateSuggestions()
        }
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (composingBuffer.isNotEmpty()) {
                val transliterated = if (isSinglishMode) {
                    HelakuruSinglishParser.parse(composingBuffer.toString())
                } else {
                    composingBuffer.toString()
                }
                ic.setComposingText(transliterated, 1)
            } else {
                ic.finishComposingText()
            }
            updateSuggestions()
        } else {
            val selected = ic.getSelectedText(0)
            if (selected.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
            showUtilityBar()
        }
    }

    private fun handleSpacePress() {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            commitComposingText()
            ic.commitText(" ", 1)
        } else {
            ic.commitText(" ", 1)
        }
        showUtilityBar()
    }

    private fun handleEnterPress() {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            commitComposingText()
        }
        val editorInfo = currentInputEditorInfo
        val actionId = editorInfo?.actionId ?: 0
        val imeOptions = editorInfo?.imeOptions ?: 0
        val actionMasked = imeOptions and EditorInfo.IME_MASK_ACTION

        if (actionMasked != EditorInfo.IME_ACTION_NONE && actionMasked != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionMasked)
        } else if (actionId != 0) {
            ic.performEditorAction(actionId)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        showUtilityBar()
    }

    private fun handleShiftToggle() {
        val now = System.currentTimeMillis()
        if (now - lastShiftPressTime < 400) {
            // Double tap -> Caps lock
            isCapsLocked = !isCapsLocked
            isShiftActive = isCapsLocked
        } else {
            isCapsLocked = false
            isShiftActive = !isShiftActive
        }
        lastShiftPressTime = now
        updateShiftUI()
    }

    private fun updateShiftUI() {
        shiftKey?.apply {
            when {
                isCapsLocked -> {
                    alpha = 1.0f
                    setColorFilter(0xFFFFD54F.toInt())
                }
                isShiftActive -> {
                    alpha = 1.0f
                    clearColorFilter()
                }
                else -> {
                    alpha = 0.6f
                    clearColorFilter()
                }
            }
        }
    }

    private var currentTextCaseIndex = 0

    private fun populateSmartbarActions(actions: List<String>) {
        val container = rootView?.findViewById<LinearLayout>(R.id.smartbar_action_icons_container) ?: return
        container.removeAllViews()

        val orderedActions = listOf(
            SmartbarAction.LANGUAGE_SWITCH,
            SmartbarAction.EMOJI,
            SmartbarAction.SETTINGS,
            SmartbarAction.TEXT_EDIT,
            SmartbarAction.VOICE_MIC,
            SmartbarAction.CLIPBOARD,
            SmartbarAction.COLLAPSE
        )

        for (action in orderedActions) {
            val view: View = if (action == SmartbarAction.LANGUAGE_SWITCH) {
                FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(34f), 1.0f).apply {
                        setMargins(dpToPx(2f), 0, dpToPx(2f), 0)
                    }
                    setBackgroundResource(R.drawable.bg_utility_icon)
                    isClickable = true
                    isFocusable = false

                    val tv = TextView(this@SinhalaIME).apply {
                        id = R.id.btn_lang_toggle
                        gravity = Gravity.CENTER
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 13f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        contentDescription = "Language Toggle"
                    }
                    langToggleBtn = tv
                    updateLanguageState(isSinglishMode)
                    addView(tv)

                    setOnClickListener {
                        vibrateFeedback()
                        toggleLanguage()
                    }
                }
            } else {
                ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(34f), 1.0f).apply {
                        setMargins(dpToPx(2f), 0, dpToPx(2f), 0)
                    }
                    val pad = dpToPx(7f)
                    setPadding(pad, pad, pad, pad)

                    val resId = resources.getIdentifier(action.iconResName, "drawable", packageName)
                    if (resId != 0) {
                        setImageResource(resId)
                    }
                    setColorFilter(android.graphics.Color.WHITE)
                    setBackgroundResource(R.drawable.bg_utility_icon)
                    contentDescription = action.title
                    isClickable = true
                    isFocusable = false

                    if (action == SmartbarAction.VOICE_MIC) {
                        id = R.id.btn_utility_mic
                    }

                    setOnClickListener {
                        vibrateFeedback()
                        handleSmartbarAction(action)
                    }
                }
            }
            container.addView(view)
        }
    }

    private fun handleSmartbarAction(action: SmartbarAction) {
        when (action) {
            SmartbarAction.LANGUAGE_SWITCH -> {
                toggleLanguage()
            }
            SmartbarAction.EMOJI -> {
                // Scroll or toggle frequent emoji bar visibility / focus
                rootView?.findViewById<View>(R.id.frequent_emoji_bar)?.let { bar ->
                    bar.visibility = View.VISIBLE
                }
            }
            SmartbarAction.SETTINGS -> {
                LatinIME.launchSettingsActivity(this)
            }
            SmartbarAction.TEXT_EDIT -> {
                handleTextStylerAction()
            }
            SmartbarAction.VOICE_MIC -> {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(this, com.slashboard.keyboard.VoicePermissionActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    return
                }
                if (voiceInputManager?.isListening == true) {
                    voiceInputManager?.stopListening()
                    voiceOverlayController?.hideVoiceOverlay()
                } else {
                    voiceOverlayController?.showVoiceOverlay(isSinglishMode)
                    voiceInputManager?.startListening(isSinglishMode)
                }
            }
            SmartbarAction.CLIPBOARD -> {
                handleClipboardAction()
            }
            SmartbarAction.COLLAPSE -> {
                requestHideSelf(0)
            }
            else -> {}
        }
    }

    private fun handleTextStylerAction() {
        val ic = currentInputConnection ?: return
        val styles = com.slashboard.keyboard.util.TextCaseStyler.CaseStyle.values()
        val targetStyle = styles[currentTextCaseIndex % styles.size]
        currentTextCaseIndex++

        if (composingBuffer.isNotEmpty()) {
            val text = composingBuffer.toString()
            val styled = com.slashboard.keyboard.util.TextCaseStyler.transform(text, targetStyle)
            composingBuffer.clear()
            composingBuffer.append(styled)
            ic.setComposingText(styled, 1)
            updateSuggestions()
        } else {
            val selected = ic.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) {
                val styled = com.slashboard.keyboard.util.TextCaseStyler.transform(selected, targetStyle)
                ic.commitText(styled, 1)
            } else {
                val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
                val lastWord = before.split("\\s+".toRegex()).lastOrNull { it.isNotEmpty() }
                if (!lastWord.isNullOrEmpty()) {
                    val styled = com.slashboard.keyboard.util.TextCaseStyler.transform(lastWord, targetStyle)
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText(styled, 1)
                }
            }
        }
    }

    private fun handleClipboardAction() {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasteText = clip.getItemAt(0).coerceToText(this)?.toString()
            if (!pasteText.isNullOrEmpty()) {
                commitComposingText()
                currentInputConnection?.commitText(pasteText, 1)
            }
        }
    }

    private fun setupUtilityBarActions() {
        val app = application as? SlashboardApp
        val actions = app?.preferencesRepository?.settingsFlow?.value?.smartbarActiveActions
            ?: SmartbarAction.DEFAULT_ACTIVE.map { it.id }
        populateSmartbarActions(actions)
    }

    private fun toggleLanguage() {
        commitComposingText()
        isSinglishMode = !isSinglishMode
        updateLanguageState(isSinglishMode)
    }

    private fun updateLanguageState(singlish: Boolean) {
        val app = application as? SlashboardApp
        val customLabel = app?.preferencesRepository?.settingsFlow?.value?.customSpacebarLabel
            ?: getSharedPreferences("${packageName}_preferences", android.content.Context.MODE_PRIVATE)
                .getString("custom_spacebar_label", "Skeyboard") ?: "Skeyboard"
        spacebarLabel?.text = customLabel
        spacebarLabel?.setTextColor(android.graphics.Color.WHITE)
        spacebarLabel?.visibility = View.VISIBLE
        val label = if (singlish) "Si" else "EN"
        langToggleBtn?.text = label
        rootView?.findViewById<TextView>(R.id.key_globe)?.text = label
    }

    private fun vibrateFeedback() {
        performHapticFeedback()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun applyKeyStyles() {
        val root = rootView ?: return
        val app = application as? SlashboardApp
        val settings = app?.preferencesRepository?.settingsFlow?.value
        val cornerRadius = settings?.keyCornerRadius ?: 8
        val bgAlpha = (settings?.keyBackgroundAlpha ?: 1.0f).coerceIn(0f, 1f)
        val borderAlpha = (settings?.keyBorderAlpha ?: 1.0f).coerceIn(0f, 1f)
        val showBorders = settings?.showKeyBorders ?: ThemeEngine.isKeyBordersEnabled(this)
        val activeTheme = app?.preferencesRepository?.getActiveTheme() ?: ThemeEngine.getActiveTheme(this)

        fun applyAlpha(color: androidx.compose.ui.graphics.Color, alphaScale: Float): Int {
            val a = (color.alpha * alphaScale).coerceIn(0f, 1f)
            return android.graphics.Color.argb(
                (a * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        }

        val normalColor = applyAlpha(activeTheme.keyBackground, bgAlpha)
        val specialColor = applyAlpha(activeTheme.functionalKeyBackground, bgAlpha)
        val rawBorderColor = if (activeTheme.keyBorderColor != androidx.compose.ui.graphics.Color.Transparent) {
            activeTheme.keyBorderColor
        } else if (activeTheme.isDark) {
            androidx.compose.ui.graphics.Color(0x66FFFFFF)
        } else {
            androidx.compose.ui.graphics.Color(0x40000000)
        }
        val borderColor = if (showBorders) applyAlpha(rawBorderColor, borderAlpha) else android.graphics.Color.TRANSPARENT

        val normalKeyBg = {
            ThemeEngine.createKeyRippleDrawable(
                context = this,
                fillColor = normalColor,
                strokeColor = borderColor,
                radiusDp = cornerRadius.toFloat(),
                strokeWidthDp = 1.2f,
                bordersEnabled = showBorders
            )
        }
        val specialKeyBg = {
            ThemeEngine.createKeyRippleDrawable(
                context = this,
                fillColor = specialColor,
                strokeColor = borderColor,
                radiusDp = cornerRadius.toFloat(),
                strokeWidthDp = 1.2f,
                bordersEnabled = showBorders
            )
        }
        val spacebarBg = {
            ThemeEngine.createKeyRippleDrawable(
                context = this,
                fillColor = android.graphics.Color.parseColor("#B30000"), // Crimson / Ruby Red Pill
                strokeColor = android.graphics.Color.parseColor("#4DFF8A80"),
                radiusDp = 24f,
                strokeWidthDp = 1.2f,
                bordersEnabled = true
            )
        }
        val enterBg = {
            ThemeEngine.createKeyRippleDrawable(
                context = this,
                fillColor = android.graphics.Color.parseColor("#0066FF"), // Vibrant Blue Pill
                strokeColor = android.graphics.Color.parseColor("#4D80B3FF"),
                radiusDp = 24f,
                strokeWidthDp = 1.2f,
                bordersEnabled = true
            )
        }

        // Apply to number keys
        val numIds = intArrayOf(
            R.id.key_num_1, R.id.key_num_2, R.id.key_num_3, R.id.key_num_4, R.id.key_num_5,
            R.id.key_num_6, R.id.key_num_7, R.id.key_num_8, R.id.key_num_9, R.id.key_num_0
        )
        numIds.forEach { id ->
            root.findViewById<View>(id)?.background = normalKeyBg()
        }

        // Apply to char key containers
        val charIds = intArrayOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )
        charIds.forEach { id ->
            (root.findViewById<View>(id)?.parent as? View)?.background = normalKeyBg()
        }

        // Apply to special & action keys
        root.findViewById<View>(R.id.key_shift)?.background = specialKeyBg()
        root.findViewById<View>(R.id.key_backspace)?.background = specialKeyBg()
        root.findViewById<View>(R.id.key_sym_toggle)?.background = specialKeyBg()
        root.findViewById<View>(R.id.key_globe)?.background = specialKeyBg()
        root.findViewById<View>(R.id.key_comma)?.background = normalKeyBg()
        root.findViewById<View>(R.id.key_period)?.background = normalKeyBg()
        val spaceView = root.findViewById<View>(R.id.key_spacebar)
        spaceView?.background = spacebarBg()
        spaceView?.visibility = View.VISIBLE
        
        val customLabel = settings?.customSpacebarLabel
            ?: getSharedPreferences("${packageName}_preferences", android.content.Context.MODE_PRIVATE)
                .getString("custom_spacebar_label", "Slashboard")
            ?: getSharedPreferences("slashboard_prefs", android.content.Context.MODE_PRIVATE)
                .getString("custom_spacebar_label", "Slashboard")
            ?: "Slashboard"
        spacebarLabel?.text = customLabel
        spacebarLabel?.setTextColor(android.graphics.Color.WHITE)
        spacebarLabel?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        spacebarLabel?.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        spacebarLabel?.visibility = View.VISIBLE
        root.findViewById<View>(R.id.key_enter)?.background = enterBg()
    }

    /**
     * Dynamically updates the layout parameters and row heights of the root container
     * based on user height scale settings.
     */
    private fun applyKeyboardHeight() {
        val root = rootView ?: return
        val app = application as? SlashboardApp
        val settings = app?.preferencesRepository?.settingsFlow?.value ?: return
        val scale = settings.heightScale.coerceIn(0.70f, 1.40f)

        // Calculate proportional heights in px
        val headerHeightPx = dpToPx(42f * scale)
        val emojiHeightPx = dpToPx(38f * scale)
        val numRowHeightPx = dpToPx(42f * scale)
        val charRowHeightPx = dpToPx(46f * scale)

        // Adjust root container layout params if needed
        val rootContainer = root.findViewById<FrameLayout>(R.id.keyboard_root_container) ?: root as? FrameLayout
        rootContainer?.let { container ->
            val currentLp = container.layoutParams ?: ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            container.layoutParams = currentLp
            container.setPadding(0, 0, 0, dpToPx(settings.bottomSpaceHeight.toFloat()))
        }

        // Dynamically resize Header Bar
        (root.findViewById<View>(R.id.smartbar_container) ?: root.findViewById<View>(R.id.smartbar_idle_layer))?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, headerHeightPx)
            lp.height = headerHeightPx
            v.layoutParams = lp
        }

        // Dynamically resize Frequent Emoji Bar (Above Number Row)
        root.findViewById<View>(R.id.frequent_emoji_bar)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, emojiHeightPx)
            lp.height = emojiHeightPx
            v.layoutParams = lp
        }

        // Dynamically resize Number Row
        root.findViewById<View>(R.id.number_row)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, numRowHeightPx)
            lp.height = numRowHeightPx
            v.layoutParams = lp
        }

        // Dynamically resize QWERTY Rows
        root.findViewById<View>(R.id.row_qwerty_top)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, charRowHeightPx)
            lp.height = charRowHeightPx
            v.layoutParams = lp
        }

        root.findViewById<View>(R.id.row_qwerty_home)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, charRowHeightPx)
            lp.height = charRowHeightPx
            v.layoutParams = lp
        }

        root.findViewById<View>(R.id.row_qwerty_bottom)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, charRowHeightPx)
            lp.height = charRowHeightPx
            v.layoutParams = lp
        }

        root.findViewById<View>(R.id.row_actions)?.let { v ->
            val lp = v.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, charRowHeightPx)
            lp.height = charRowHeightPx
            v.layoutParams = lp
        }

        root.requestLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        voiceInputManager?.setInputConnection(currentInputConnection)
        composingBuffer.clear()
        applyCustomWallpaper()
        applyKeyStyles()
        applyKeyboardHeight()
        rootView?.let { root ->
            applyBottomPadding(root)
            setupRecentEmojiBar(root)
        }
        updateLanguageState(isSinglishMode)
        // Reset state directly
        utilitySettingsBar?.apply {
            visibility = View.VISIBLE
            alpha = 1.0f
            translationY = 0f
        }
        wordSuggestionBar?.apply {
            visibility = View.GONE
            alpha = 0.0f
            translationY = 15f
        }
        suggestionChipsContainer?.removeAllViews()
        isSuggestionStripVisible = false
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        rootView?.let { root ->
            outInsets.contentTopInsets = root.top
            outInsets.visibleTopInsets = root.top
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        commitComposingText()
    }
}
