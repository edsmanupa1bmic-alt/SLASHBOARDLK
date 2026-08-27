package com.slashboard.keyboard.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.model.KeyCode
import com.slashboard.keyboard.data.model.KeyModel
import com.slashboard.keyboard.data.model.KeyboardLayout
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.data.repository.HelakuruSinglishParser
import com.slashboard.keyboard.data.repository.KeyboardSettings
import com.slashboard.keyboard.data.repository.OnlineThemeRepository
import com.slashboard.keyboard.data.repository.SuggestionManager
import com.slashboard.keyboard.data.repository.UserLearningManager

enum class KeyboardMode {
    ALPHA,
    SYMBOLS,
    MORE_SYMBOLS,
    NUMPAD,
    EMOJI,
    CLIPBOARD
}

@Composable
fun VirtualKeyboard(
    settings: KeyboardSettings,
    currentWord: String = "",
    fullText: String = "",
    modifier: Modifier = Modifier,
    onInsertText: (String) -> Unit,
    onDeleteCharacter: () -> Unit,
    onReplaceWord: (String, String) -> Unit = { oldW, newW ->
        for (i in 0 until oldW.length) onDeleteCharacter()
        onInsertText(newW)
    },
    onEnter: () -> Unit,
    onCursorMove: (Int) -> Unit = {},
    onClearText: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val db = SlashboardApp.instance.database
    val prefs = SlashboardApp.instance.preferencesRepository
    val clipboardItems by db.clipboardFlow.collectAsState()
    val dictionaryWords by db.dictionaryFlow.collectAsState()

    var keyboardMode by remember { mutableStateOf(KeyboardMode.ALPHA) }
    var shiftState by remember { mutableStateOf(if (settings.autoCapitalization) ShiftState.SHIFT else ShiftState.OFF) }

    var lastSpaceTimestamp by remember { mutableLongStateOf(0L) }
    var lastWasSpace by remember { mutableStateOf(false) }

    val activeTheme = remember(
        settings.themeId,
        settings.keyCornerRadius,
        settings.showKeyBorders,
        settings.keyBackgroundAlpha,
        settings.keyBorderAlpha
    ) {
        prefs.getActiveTheme()
    }
    val activeLayout = remember(settings.layoutId) {
        prefs.getActiveLayout()
    }

    var frequentEmojis by remember(settings) {
        mutableStateOf(prefs.getFrequentEmojis())
    }

    val isSinglish = activeLayout.id == "sinhala_singlish"

    val keyHeight = (46 * settings.heightScale).dp
    val totalKeyboardHeight = (270 * settings.heightScale).dp

    // Compute smart suggestions via unified SuggestionManager
    val context = LocalContext.current
    val learningManager = remember { UserLearningManager.getInstance(context) }
    
    val suggestions = remember(currentWord, fullText, dictionaryWords, settings.autoCorrect, isSinglish) {
        if (!settings.autoCorrect) {
            emptyList()
        } else {
            SuggestionManager.getSuggestions(
                fullText = fullText,
                currentComposing = currentWord,
                isSinglish = isSinglish,
                userDictionary = dictionaryWords,
                learningManager = learningManager
            )
        }
    }

    val handleSpacePress: () -> Unit = {
        val now = System.currentTimeMillis()
        if (isSinglish && currentWord.isNotEmpty()) {
            // Commit top Singlish suggestion on space
            val transliterated = HelakuruSinglishParser.parse(currentWord)
            onReplaceWord(currentWord, "$transliterated ")
            lastWasSpace = true
            lastSpaceTimestamp = now
        } else if (settings.doubleSpacePeriod && lastWasSpace && (now - lastSpaceTimestamp < 500)) {
            onDeleteCharacter()
            onInsertText(". ")
            lastWasSpace = false
            lastSpaceTimestamp = 0L
        } else {
            onInsertText(" ")
            lastWasSpace = true
            lastSpaceTimestamp = now
        }
    }

    val wallpaperPath = settings.customWallpaperPath
    val wallpaperBitmap = remember(wallpaperPath) {
        if (!wallpaperPath.isNullOrBlank()) {
            OnlineThemeRepository.loadWallpaperBitmap(wallpaperPath)
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("virtual_keyboard_root_box")
    ) {
        // Layer 0: Wallpaper background image or preset theme color
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap.asImageBitmap(),
                contentDescription = "Keyboard Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Layer 1: Dim Scrim
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = settings.wallpaperDim.coerceIn(0.0f, 0.95f)))
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(activeTheme.background)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .testTag("virtual_keyboard")
        ) {
        // Top Bar: Dynamic merge of ToolbarView (idle) and SuggestionBar (composing)
        if (settings.toolbarVisible && keyboardMode != KeyboardMode.EMOJI && keyboardMode != KeyboardMode.CLIPBOARD) {
            val showSuggestions = settings.autoCorrect && suggestions.isNotEmpty() && currentWord.isNotEmpty() && (keyboardMode == KeyboardMode.ALPHA || keyboardMode == KeyboardMode.SYMBOLS)

            Crossfade(
                targetState = showSuggestions,
                label = "top_bar_switch"
            ) { isComposingWithSuggestions ->
                if (isComposingWithSuggestions) {
                    SuggestionBar(
                        suggestions = suggestions,
                        theme = activeTheme,
                        onSelectSuggestion = { suggestion ->
                            if (currentWord.isNotEmpty()) {
                                onReplaceWord(currentWord, suggestion.replacement + " ")
                            } else {
                                onInsertText(suggestion.replacement + " ")
                            }
                            lastWasSpace = true
                            lastSpaceTimestamp = System.currentTimeMillis()
                        }
                    )
                } else {
                    ToolbarView(
                        theme = activeTheme,
                        activeLayoutName = activeLayout.name,
                        onOpenClipboard = { keyboardMode = KeyboardMode.CLIPBOARD },
                        onOpenEmoji = { keyboardMode = KeyboardMode.EMOJI },
                        onCycleLayout = {
                            val allLayouts = KeyboardLayout.AvailableLayouts
                            val curIdx = allLayouts.indexOfFirst { it.id == settings.layoutId }
                            val nextIdx = if (curIdx >= 0) (curIdx + 1) % allLayouts.size else 0
                            prefs.updateLayoutId(allLayouts[nextIdx].id)
                        },
                        onCursorLeft = { onCursorMove(-1) },
                        onCursorRight = { onCursorMove(1) },
                        onClearAll = onClearText,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }

        // Crossfade between Keyboard and Emoji / Clipboard Panels
        Crossfade(
            targetState = keyboardMode,
            label = "keyboard_panel_switch"
        ) { mode ->
            when (mode) {
                KeyboardMode.EMOJI -> {
                    EmojiPickerView(
                        theme = activeTheme,
                        height = totalKeyboardHeight,
                        onEmojiSelected = { emoji ->
                            onInsertText(emoji)
                            lastWasSpace = false
                        },
                        onBackspace = onDeleteCharacter,
                        onCloseEmoji = { keyboardMode = KeyboardMode.ALPHA }
                    )
                }
                KeyboardMode.CLIPBOARD -> {
                    ClipboardView(
                        items = clipboardItems,
                        theme = activeTheme,
                        height = totalKeyboardHeight,
                        onPasteClip = { clipText ->
                            onInsertText(clipText)
                            lastWasSpace = false
                            keyboardMode = KeyboardMode.ALPHA
                        },
                        onTogglePin = { db.togglePin(it) },
                        onDeleteClip = { db.deleteClipboardItem(it) },
                        onCloseClipboard = { keyboardMode = KeyboardMode.ALPHA }
                    )
                }
                else -> {
                    // Standard Keyboard Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val currentRows = when (mode) {
                            KeyboardMode.SYMBOLS -> KeyboardLayout.SymbolsLayout
                            KeyboardMode.MORE_SYMBOLS -> KeyboardLayout.MoreSymbolsLayout
                            KeyboardMode.NUMPAD -> KeyboardLayout.NumpadLayout
                            else -> activeLayout.rows
                        }

                        // Frequently Used Emoji Bar (Directly Above Number Row)
                        if (settings.showFrequentEmojiRow && (mode == KeyboardMode.ALPHA || mode == KeyboardMode.SYMBOLS)) {
                            FrequentEmojiBar(
                                theme = activeTheme,
                                emojis = frequentEmojis,
                                height = (36 * settings.heightScale).dp,
                                onEmojiSelected = { emoji ->
                                    onInsertText(emoji)
                                    prefs.saveRecentEmoji(emoji)
                                    frequentEmojis = prefs.getFrequentEmojis()
                                    lastWasSpace = false
                                }
                            )
                        }

                        // Number Row if enabled and in ALPHA mode
                        if (settings.showNumberRow && mode == KeyboardMode.ALPHA) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { num ->
                                    KeyView(
                                        key = KeyModel(primary = num),
                                        theme = activeTheme,
                                        shiftState = ShiftState.OFF,
                                        height = keyHeight * 0.85f,
                                        showSecondary = false,
                                        showPopup = settings.popupOnKeypress,
                                        hapticEnabled = settings.hapticFeedback,
                                        modifier = Modifier.weight(1f),
                                        onKeyPress = {
                                            onInsertText(num)
                                            lastWasSpace = false
                                        }
                                    )
                                }
                            }
                        }

                        // Layout Main Rows
                        currentRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    KeyView(
                                        key = key,
                                        theme = activeTheme,
                                        shiftState = shiftState,
                                        height = keyHeight,
                                        showSecondary = settings.showSecondaryLabels && mode == KeyboardMode.ALPHA,
                                        showPopup = settings.popupOnKeypress,
                                        hapticEnabled = settings.hapticFeedback,
                                        spaceLabel = "",
                                        modifier = Modifier.weight(key.weight),
                                        onKeyPress = { pressedKey ->
                                            when (pressedKey.code) {
                                                KeyCode.SHIFT -> {
                                                    shiftState = when (shiftState) {
                                                        ShiftState.OFF -> ShiftState.SHIFT
                                                        ShiftState.SHIFT -> ShiftState.CAPS_LOCK
                                                        ShiftState.CAPS_LOCK -> ShiftState.OFF
                                                    }
                                                }
                                                KeyCode.BACKSPACE -> {
                                                    onDeleteCharacter()
                                                    lastWasSpace = false
                                                }
                                                KeyCode.ENTER -> {
                                                    onEnter()
                                                    lastWasSpace = false
                                                }
                                                KeyCode.SPACE -> {
                                                    handleSpacePress()
                                                }
                                                KeyCode.SWITCH_SYMBOLS -> {
                                                    keyboardMode = KeyboardMode.SYMBOLS
                                                }
                                                KeyCode.SWITCH_LETTERS -> {
                                                    keyboardMode = KeyboardMode.ALPHA
                                                }
                                                KeyCode.SWITCH_MORE_SYMBOLS -> {
                                                    keyboardMode = KeyboardMode.MORE_SYMBOLS
                                                }
                                                KeyCode.SWITCH_NUMPAD -> {
                                                    keyboardMode = KeyboardMode.NUMPAD
                                                }
                                                KeyCode.EMOJI -> {
                                                    keyboardMode = KeyboardMode.EMOJI
                                                }
                                                KeyCode.CLIPBOARD -> {
                                                    keyboardMode = KeyboardMode.CLIPBOARD
                                                }
                                                KeyCode.SETTINGS -> {
                                                    onOpenSettings()
                                                }
                                                KeyCode.CURSOR_LEFT -> {
                                                    onCursorMove(-1)
                                                }
                                                KeyCode.CURSOR_RIGHT -> {
                                                    onCursorMove(1)
                                                }
                                                KeyCode.CLEAR_ALL -> {
                                                    onClearText()
                                                }
                                                KeyCode.CHARACTER -> {
                                                    val character = if (shiftState != ShiftState.OFF) {
                                                        pressedKey.shifted
                                                    } else {
                                                        pressedKey.primary
                                                    }
                                                    onInsertText(character)
                                                    lastWasSpace = false

                                                    // Auto turn off single shift
                                                    if (shiftState == ShiftState.SHIFT) {
                                                        shiftState = ShiftState.OFF
                                                    }
                                                }
                                            }
                                        },
                                        onLongPress = { longKey ->
                                            if (longKey.secondary.isNotEmpty()) {
                                                onInsertText(longKey.secondary)
                                                lastWasSpace = false
                                            }
                                        },
                                        onSpaceSlide = { direction ->
                                            onCursorMove(direction)
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom Spacebar Row (Space, Period, Symbols switch, Emoji switch, Enter)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 1. Symbol/ABC Switch
                            val symKey = if (mode == KeyboardMode.ALPHA) {
                                KeyModel("?123", isFunctional = true, code = KeyCode.SWITCH_SYMBOLS, weight = 1.3f)
                            } else {
                                KeyModel("ABC", isFunctional = true, code = KeyCode.SWITCH_LETTERS, weight = 1.3f)
                            }
                            KeyView(
                                key = symKey,
                                theme = activeTheme,
                                shiftState = ShiftState.OFF,
                                height = keyHeight,
                                showSecondary = false,
                                showPopup = false,
                                hapticEnabled = settings.hapticFeedback,
                                modifier = Modifier.weight(symKey.weight),
                                onKeyPress = {
                                    keyboardMode = if (mode == KeyboardMode.ALPHA) KeyboardMode.SYMBOLS else KeyboardMode.ALPHA
                                }
                            )

                            // 2. Emoji key
                            val emojiKey = KeyModel("😊", isFunctional = true, code = KeyCode.EMOJI, weight = 1.0f)
                            KeyView(
                                key = emojiKey,
                                theme = activeTheme,
                                shiftState = ShiftState.OFF,
                                height = keyHeight,
                                showSecondary = false,
                                showPopup = false,
                                hapticEnabled = settings.hapticFeedback,
                                modifier = Modifier.weight(emojiKey.weight),
                                onKeyPress = {
                                    keyboardMode = KeyboardMode.EMOJI
                                }
                            )

                            // 3. Spacebar
                            val spaceKey = KeyModel("space", isFunctional = false, code = KeyCode.SPACE, weight = 4.2f)
                            KeyView(
                                key = spaceKey,
                                theme = activeTheme,
                                shiftState = ShiftState.OFF,
                                height = keyHeight,
                                showSecondary = false,
                                showPopup = false,
                                hapticEnabled = settings.hapticFeedback,
                                spaceLabel = settings.customSpacebarLabel.ifBlank { "Slashboard" },
                                modifier = Modifier.weight(spaceKey.weight),
                                onKeyPress = {
                                    handleSpacePress()
                                },
                                onSpaceSlide = { direction ->
                                    onCursorMove(direction)
                                }
                            )

                            // 4. Period / Comma Key
                            val punctKey = KeyModel(".", secondary = ",", weight = 1.0f)
                            KeyView(
                                key = punctKey,
                                theme = activeTheme,
                                shiftState = ShiftState.OFF,
                                height = keyHeight,
                                showSecondary = true,
                                showPopup = settings.popupOnKeypress,
                                hapticEnabled = settings.hapticFeedback,
                                modifier = Modifier.weight(punctKey.weight),
                                onKeyPress = {
                                    onInsertText(".")
                                    lastWasSpace = false
                                },
                                onLongPress = {
                                    onInsertText(",")
                                    lastWasSpace = false
                                }
                            )

                            // 5. Enter / Return key
                            val enterKey = KeyModel("enter", isFunctional = true, code = KeyCode.ENTER, weight = 1.5f)
                            KeyView(
                                key = enterKey,
                                theme = activeTheme,
                                shiftState = ShiftState.OFF,
                                height = keyHeight,
                                showSecondary = false,
                                showPopup = false,
                                hapticEnabled = settings.hapticFeedback,
                                modifier = Modifier.weight(enterKey.weight),
                                onKeyPress = {
                                    onEnter()
                                    lastWasSpace = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
