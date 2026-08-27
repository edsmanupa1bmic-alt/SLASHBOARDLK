package com.slashboard.keyboard.service

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.InputConnection

class AcceleratedBackspaceHandler(
    private val keyView: View,
    private val inputConnectionProvider: () -> InputConnection?,
    private val vibrateFeedback: () -> Unit,
    private val onDeleteCharacter: (() -> Unit)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private var startTime = 0L

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (!isDeleting) return
            
            val elapsed = System.currentTimeMillis() - startTime
            val delay: Long
            
            when {
                elapsed < 400 -> {
                    // Stage 1: Waiting to trigger repeat
                    delay = 400 - elapsed
                }
                elapsed < 1200 -> {
                    // Stage 2: Slow Repeat
                    deleteCharacter()
                    delay = 100
                }
                elapsed < 2400 -> {
                    // Stage 3: Fast Repeat
                    deleteCharacter()
                    delay = 40
                }
                else -> {
                    // Stage 4: High-Speed Word Deletion
                    deleteWord()
                    delay = 50
                }
            }
            handler.postDelayed(this, delay)
        }
    }

    fun onTouchDown() {
        isDeleting = true
        startTime = System.currentTimeMillis()
        deleteCharacter() // Initial delete
        handler.postDelayed(deleteRunnable, 400)
    }

    fun onTouchUpOrCancel() {
        isDeleting = false
        handler.removeCallbacks(deleteRunnable)
    }

    private fun deleteCharacter() {
        vibrateFeedback()
        if (onDeleteCharacter != null) {
            onDeleteCharacter.invoke()
            return
        }
        val ic = inputConnectionProvider() ?: return
        val currentText = ic.getTextBeforeCursor(1, 0)
        if (!currentText.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun deleteWord() {
        vibrateFeedback()
        val ic = inputConnectionProvider() ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0) ?: ""
        if (textBefore.isEmpty()) return

        var deleteCount = 0
        var foundChar = false
        
        for (i in textBefore.length - 1 downTo 0) {
            val c = textBefore[i]
            if (c.isWhitespace()) {
                if (foundChar) break // Reached the start of the previous word
                deleteCount++ // Delete trailing whitespace
            } else {
                foundChar = true
                deleteCount++ // Delete word character
            }
        }
        
        if (deleteCount > 0) {
            ic.deleteSurroundingText(deleteCount, 0)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }
}
