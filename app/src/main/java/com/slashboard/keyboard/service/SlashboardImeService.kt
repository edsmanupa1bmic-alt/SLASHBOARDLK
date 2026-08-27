package com.slashboard.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.repository.HelakuruSinglishParser
import com.slashboard.keyboard.data.repository.KeyboardPreferencesRepository
import com.slashboard.keyboard.data.repository.KeyboardSettings
import com.slashboard.keyboard.ui.components.VirtualKeyboard
import com.slashboard.keyboard.ui.theme.SlashboardTheme

/**
 * Android InputMethodService with full Helakuru Singlish transliteration & composing text support.
 */
class SlashboardImeService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner {

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private val vmStore: ViewModelStore = ViewModelStore()
    
    // Composing state buffer for Singlish phonetic typing
    private val composingBuffer = StringBuilder()
    
    private val backDispatcher = OnBackPressedDispatcher {
        requestHideSelf(0)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = vmStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
        
    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = backDispatcher

    private val preferencesRepository: KeyboardPreferencesRepository by lazy {
        try {
            SlashboardApp.instance.preferencesRepository
        } catch (_: Throwable) {
            KeyboardPreferencesRepository(applicationContext)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        } catch (_: Throwable) {}
    }

    private fun attachViewTreeOwners(targetView: View) {
        try {
            targetView.setViewTreeLifecycleOwner(this)
            targetView.setViewTreeSavedStateRegistryOwner(this)
            targetView.setViewTreeViewModelStoreOwner(this)
            targetView.setViewTreeOnBackPressedDispatcherOwner(this)
        } catch (_: Throwable) {}
    }

    private fun extractCurrentContext(): Pair<String, String> {
        return try {
            val ic = currentInputConnection ?: return "" to ""
            val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            val word = if (before.isEmpty() || before.endsWith(" ")) ""
            else before.substringAfterLast(" ")
            before to word
        } catch (_: Throwable) {
            "" to ""
        }
    }

    /**
     * Flush composing buffer into actual committed text
     */
    private fun commitComposingText(overrideWith: String? = null) {
        val ic = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val textToCommit = overrideWith ?: HelakuruSinglishParser.parse(composingBuffer.toString())
            ic.commitText(textToCommit, 1)
            composingBuffer.clear()
        }
    }

    override fun onCreateInputView(): View {
        try {
            window?.window?.decorView?.let { decor ->
                attachViewTreeOwners(decor)
            }
        } catch (_: Throwable) {}

        val composeView = ComposeView(this).apply {
            attachViewTreeOwners(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@SlashboardImeService))

            setContent {
                SlashboardTheme(darkTheme = true) {
                    val settingsState = preferencesRepository.settingsFlow.collectAsState(initial = KeyboardSettings())
                    val settings = settingsState.value

                    var currentContext by remember { mutableStateOf("" to "") }
                    val isSinglish = settings.layoutId == "sinhala_singlish"

                    Column {
                        VirtualKeyboard(
                            settings = settings,
                            currentWord = if (isSinglish && composingBuffer.isNotEmpty()) composingBuffer.toString() else currentContext.second,
                            fullText = currentContext.first,
                            onInsertText = { text ->
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                val um = com.slashboard.keyboard.data.repository.UserLearningManager.getInstance(this@SlashboardImeService)
                                if (isSinglish) {
                                    // Check if standard alphanumeric Latin character for Singlish composing
                                    if (text.length == 1 && (text[0].isLetter() || text[0] == '.' || text[0] == '\'')) {
                                        composingBuffer.append(text)
                                        val transliterated = HelakuruSinglishParser.parse(composingBuffer.toString())
                                        ic.setComposingText(transliterated, 1)
                                    } else {
                                        // Non-alphabetic token -> commit composing buffer then insert
                                        val prefix = composingBuffer.toString()
                                        val wordToCommit = if (prefix.isNotEmpty()) HelakuruSinglishParser.parse(prefix) else ""
                                        commitComposingText()
                                        if (wordToCommit.isNotEmpty()) {
                                            um.onWordCommitted(wordToCommit, prefix)
                                        }
                                        ic.commitText(text, 1)
                                        if (text.isBlank() || !text[0].isLetter()) {
                                            um.onSpaceOrPunctuation(text)
                                        }
                                    }
                                } else {
                                    ic.commitText(text, 1)
                                    if (text.isBlank() || !text[0].isLetter()) {
                                        um.onSpaceOrPunctuation(text)
                                    }
                                }
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        },
                        onDeleteCharacter = {
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                if (isSinglish && composingBuffer.isNotEmpty()) {
                                    composingBuffer.deleteCharAt(composingBuffer.length - 1)
                                    if (composingBuffer.isNotEmpty()) {
                                        val transliterated = HelakuruSinglishParser.parse(composingBuffer.toString())
                                        ic.setComposingText(transliterated, 1)
                                    } else {
                                        ic.finishComposingText()
                                    }
                                } else {
                                    val selectedText = ic.getSelectedText(0)
                                    if (selectedText.isNullOrEmpty()) {
                                        ic.deleteSurroundingText(1, 0)
                                    } else {
                                        ic.commitText("", 1)
                                    }
                                }
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        },
                        onReplaceWord = { oldWord, newWord ->
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                val um = com.slashboard.keyboard.data.repository.UserLearningManager.getInstance(this@SlashboardImeService)
                                if (isSinglish && composingBuffer.isNotEmpty()) {
                                    val prefix = composingBuffer.toString()
                                    ic.commitText("$newWord ", 1)
                                    composingBuffer.clear()
                                    um.onWordCommitted(newWord, prefix)
                                } else {
                                    if (oldWord.isNotEmpty()) {
                                        ic.deleteSurroundingText(oldWord.length, 0)
                                    }
                                    ic.commitText("$newWord ", 1)
                                    um.onWordCommitted(newWord)
                                }
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        },
                        onEnter = {
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                if (isSinglish && composingBuffer.isNotEmpty()) {
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
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        },
                        onCursorMove = { offset ->
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                if (isSinglish && composingBuffer.isNotEmpty()) {
                                    commitComposingText()
                                }
                                if (offset < 0) {
                                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                                } else {
                                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                                }
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        },
                        onClearText = {
                            try {
                                val ic = currentInputConnection ?: return@VirtualKeyboard
                                composingBuffer.clear()
                                ic.deleteSurroundingText(1000, 1000)
                                currentContext = extractCurrentContext()
                            } catch (_: Throwable) {}
                        }
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(settings.bottomSpaceHeight.dp)
                            .background(androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composingBuffer.clear()
        try {
            window?.window?.decorView?.let { decor ->
                attachViewTreeOwners(decor)
            }
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        } catch (_: Throwable) {}
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        commitComposingText()
        try {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        try {
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            vmStore.clear()
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}
