package com.slashboard.keyboard.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.slashboard.keyboard.R
import com.slashboard.keyboard.data.model.DictionaryWord
import com.slashboard.keyboard.ui.components.SuggestionItem

object SuggestionManager {

    /**
     * Unified suggestion candidate generation for Singlish and English.
     */
    fun getSuggestions(
        fullText: String,
        currentComposing: String,
        isSinglish: Boolean,
        userDictionary: List<DictionaryWord> = emptyList(),
        learningManager: UserLearningManager? = null
    ): List<SuggestionItem> {
        val trimmedComposing = currentComposing.trim()
        val results = LinkedHashSet<SuggestionItem>()
        
        // 1. Next-Word Prediction (Before user types any character)
        if (trimmedComposing.isEmpty()) {
            if (learningManager != null) {
                val nextWords = learningManager.getNextWords()
                for (bw in nextWords.take(3)) {
                    results.add(
                        SuggestionItem(
                            display = bw.nextWord,
                            replacement = bw.nextWord,
                            isPrimary = results.isEmpty()
                        )
                    )
                }
            }
            return results.toList()
        }

        // Candidates map: Word -> Score
        val scoredCandidates = mutableMapOf<String, Long>()

        // Helper to add and score a candidate
        fun addCandidate(word: String, baseScore: Long, isShortcut: Boolean = false) {
            if (word.isBlank()) return
            var finalScore = baseScore
            if (learningManager != null) {
                val learned = learningManager.getLearnedWord(word)
                if (learned != null) {
                    val userFreq = learned.frequency
                    val recencyBonus = if (System.currentTimeMillis() - learned.lastUsed < 86400000) 50L else 0L // Bonus if used in last 24h
                    finalScore += (userFreq * 15L) + recencyBonus
                }
                
                // Bigram Match Bonus
                val prevWord = learningManager.lastCommittedWord
                if (prevWord != null) {
                    val bigramFreq = learningManager.getBigramFrequency(prevWord, word)
                    if (bigramFreq > 0) {
                        finalScore += (bigramFreq * 25L)
                    }
                }
            }
            
            // Add or update max score
            val currentScore = scoredCandidates[word] ?: 0L
            if (finalScore > currentScore) {
                scoredCandidates[word] = finalScore
            }
        }

        // A. User Learning Predictions (Prefix Match)
        if (learningManager != null) {
            val learnedPredictions = learningManager.getPredictions(trimmedComposing)
            for (learned in learnedPredictions) {
                val recencyBonus = if (System.currentTimeMillis() - learned.lastUsed < 86400000) 50L else 0L
                val score = (learned.frequency * 15L) + recencyBonus + 10000L // Massive boost for learned prefix matches
                addCandidate(learned.word, score)
            }
        }

        if (isSinglish) {
            // 2. Singlish Mode
            val shortcutMatch = userDictionary.find { it.shortcut.equals(trimmedComposing, ignoreCase = true) }
            if (shortcutMatch != null) {
                results.add(
                    SuggestionItem(
                        display = shortcutMatch.word,
                        replacement = shortcutMatch.word,
                        isPrimary = true,
                        isShortcut = true
                    )
                )
            }

            val transliterations = HelakuruSinglishParser.getSuggestions(trimmedComposing)
            if (transliterations.isNotEmpty()) {
                val primary = transliterations.first()
                addCandidate(primary, 5000L) // Base score for exact transliteration

                val trieCompletions = SmartDictionaryEngine.searchSinhala(primary.trim(), limit = 10)
                for ((index, candidate) in trieCompletions.withIndex()) {
                    addCandidate(candidate.word, 1000L - index)
                }

                for ((index, alt) in transliterations.drop(1).withIndex()) {
                    addCandidate(alt, 500L - index)
                }
            }
        } else {
            // 3. English Mode
            val lower = trimmedComposing.lowercase()
            val shortcutMatch = userDictionary.find { it.shortcut.equals(lower, ignoreCase = true) }
            if (shortcutMatch != null) {
                results.add(
                    SuggestionItem(
                        display = shortcutMatch.word,
                        replacement = shortcutMatch.word,
                        isPrimary = true,
                        isShortcut = true
                    )
                )
            }

            val trieCompletions = SmartDictionaryEngine.searchEnglish(lower, limit = 10)
            for ((index, candidate) in trieCompletions.withIndex()) {
                addCandidate(candidate.word, 1000L - index)
            }

            if (scoredCandidates.isEmpty()) {
                addCandidate(trimmedComposing, 5000L)
            }
        }

        // Sort candidates by score descending and take top 5
        val sortedCandidates = scoredCandidates.entries.sortedByDescending { it.value }.take(5)
        for ((index, entry) in sortedCandidates.withIndex()) {
            results.add(
                SuggestionItem(
                    display = entry.key,
                    replacement = entry.key,
                    isPrimary = results.isEmpty() && index == 0
                )
            )
        }

        // If no results, offer raw text
        if (results.isEmpty() && trimmedComposing.isNotEmpty()) {
            results.add(
                SuggestionItem(
                    display = trimmedComposing,
                    replacement = trimmedComposing,
                    isPrimary = true
                )
            )
        }

        return results.take(5).toList()
    }

    fun renderSuggestionChips(
        container: LinearLayout,
        suggestions: List<SuggestionItem>,
        onSelect: (SuggestionItem) -> Unit
    ) {
        val context = container.context
        container.removeAllViews()
        if (suggestions.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        val horizontalPadding = dpToPx(context, 14f)
        val verticalPadding = dpToPx(context, 6f)
        val marginHorizontal = dpToPx(context, 4f)

        suggestions.forEachIndexed { index, item ->
            val chip = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(marginHorizontal, 0, marginHorizontal, 0)
                }
                text = item.display
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = if (item.isPrimary) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                gravity = Gravity.CENTER
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

                try {
                    setBackgroundResource(R.drawable.bg_suggestion_chip)
                } catch (e: Exception) {
                    setBackgroundColor(Color.parseColor("#26FFFFFF"))
                }

                if (item.isPrimary) {
                    setTextColor(Color.parseColor("#FFE082"))
                }
                isClickable = true
                isFocusable = true
                contentDescription = "Suggestion: ${item.display}"
                setOnClickListener {
                    onSelect(item)
                }
            }
            container.addView(chip)
        }
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}
