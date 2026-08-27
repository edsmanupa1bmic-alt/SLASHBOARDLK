package com.slashboard.keyboard.service

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.slashboard.keyboard.R

class VoiceOverlayController(
    private val rootView: View,
    private val voiceInputManager: VoiceInputManager,
    private val onVoiceFinished: () -> Unit
) {

    private val voiceOverlayView: View = rootView.findViewById(R.id.voice_overlay_view)
    private val mainKeyboardView: View = rootView.findViewById(R.id.main_keyboard_view)
    private val tvLiveTranscript: TextView = rootView.findViewById(R.id.tv_live_transcript)
    private val ivMicRing: ImageView = rootView.findViewById(R.id.iv_mic_ring)
    private val ivMicIcon: ImageView = rootView.findViewById(R.id.iv_mic_icon)
    private val btnCloseVoice: ImageButton = rootView.findViewById(R.id.btn_close_voice)
    private val tvLanguageIndicator: TextView = rootView.findViewById(R.id.tv_language_indicator)

    init {
        btnCloseVoice.setOnClickListener {
            voiceInputManager.stopListening()
            hideVoiceOverlay()
            onVoiceFinished()
        }

        ivMicIcon.setOnClickListener {
            voiceInputManager.stopListening()
            hideVoiceOverlay()
            onVoiceFinished()
        }

        voiceInputManager.onListeningStateChanged = { isListening ->
            if (!isListening) {
                hideVoiceOverlay()
                onVoiceFinished()
            }
        }

        voiceInputManager.onPartialResultListener = { partial ->
            updateTranscript(partial)
        }

        voiceInputManager.onRmsChangedListener = { rmsDb ->
            // Scale between 1.0f and 1.6f based on RMS
            val scale = 1.0f + (rmsDb.coerceIn(0f, 10f) / 10f) * 0.6f
            ivMicRing.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(50)
                .start()
        }
    }

    fun showVoiceOverlay(isSinhalaMode: Boolean) {
        tvLanguageIndicator.text = if (isSinhalaMode) "සිංහල (ශ්රී ලංකා)" else "English (US)"
        tvLiveTranscript.text = "Listening..."
        mainKeyboardView.visibility = View.GONE
        voiceOverlayView.visibility = View.VISIBLE
    }

    fun hideVoiceOverlay() {
        voiceOverlayView.visibility = View.GONE
        mainKeyboardView.visibility = View.VISIBLE
    }

    fun updateTranscript(partialResult: String) {
        tvLiveTranscript.text = partialResult
    }
}
