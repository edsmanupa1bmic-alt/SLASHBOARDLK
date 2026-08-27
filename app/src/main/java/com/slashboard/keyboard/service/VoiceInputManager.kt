package com.slashboard.keyboard.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputConnection
import android.widget.Toast

class VoiceInputManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var inputConnection: InputConnection? = null
    var isListening = false
        private set

    var onListeningStateChanged: ((Boolean) -> Unit)? = null
    var onRmsChangedListener: ((Float) -> Unit)? = null
    var onPartialResultListener: ((String) -> Unit)? = null
    
    fun setInputConnection(ic: InputConnection?) {
        this.inputConnection = ic
    }

    fun startListening(isSinhalaMode: Boolean) {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }

        val locale = if (isSinhalaMode) "si-LK" else "en-US"
        
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
        
        speechRecognizer?.startListening(speechIntent)
        isListening = true
        onListeningStateChanged?.invoke(true)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        onListeningStateChanged?.invoke(false)
        onRmsChangedListener?.invoke(0f)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        onRmsChangedListener?.invoke(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        isListening = false
        onListeningStateChanged?.invoke(false)
        onRmsChangedListener?.invoke(0f)
    }

    override fun onError(error: Int) {
        isListening = false
        onListeningStateChanged?.invoke(false)
        onRmsChangedListener?.invoke(0f)
        
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        
        // Revert any composing state
        inputConnection?.finishComposingText()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val finalResult = matches[0]
            inputConnection?.finishComposingText()
            inputConnection?.commitText("$finalResult ", 1)
        }
        isListening = false
        onListeningStateChanged?.invoke(false)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partialString = matches[0]
            inputConnection?.setComposingText(partialString, 1)
            onPartialResultListener?.invoke(partialString)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
