package com.slashboard.keyboard.data.repository

import com.slashboard.keyboard.data.model.DictionaryWord
import kotlin.math.min

data class SmartSuggestion(
    val word: String,
    val replacement: String,
    val score: Int = 0,
    val isPrimary: Boolean = false,
    val isShortcut: Boolean = false,
    val isCorrection: Boolean = false
)

object SmartWordEngine {

    // High frequency common English vocabulary with base weights
    private val CommonEnglishWords: List<Pair<String, Int>> = listOf(
        "the" to 1000, "be" to 950, "to" to 940, "of" to 930, "and" to 920,
        "a" to 910, "in" to 900, "that" to 890, "have" to 880, "I" to 870,
        "it" to 860, "for" to 850, "not" to 840, "on" to 830, "with" to 820,
        "he" to 810, "as" to 800, "you" to 790, "do" to 780, "at" to 770,
        "this" to 760, "but" to 750, "his" to 740, "by" to 730, "from" to 720,
        "they" to 710, "we" to 700, "say" to 690, "her" to 680, "she" to 670,
        "or" to 660, "an" to 650, "will" to 640, "my" to 630, "one" to 620,
        "all" to 610, "would" to 600, "there" to 590, "their" to 580, "what" to 570,
        "so" to 560, "up" to 550, "out" to 540, "if" to 530, "about" to 520,
        "who" to 510, "get" to 500, "which" to 490, "go" to 480, "me" to 470,
        "when" to 460, "make" to 450, "can" to 440, "like" to 430, "time" to 420,
        "no" to 410, "just" to 400, "him" to 390, "know" to 380, "take" to 370,
        "people" to 360, "into" to 350, "year" to 340, "your" to 330, "good" to 320,
        "some" to 310, "could" to 300, "them" to 290, "see" to 280, "other" to 270,
        "than" to 260, "then" to 250, "now" to 240, "look" to 230, "only" to 220,
        "come" to 210, "its" to 200, "over" to 195, "think" to 190, "also" to 185,
        "back" to 180, "after" to 175, "use" to 170, "two" to 165, "how" to 160,
        "our" to 155, "work" to 150, "first" to 145, "well" to 140, "way" to 135,
        "even" to 130, "new" to 125, "want" to 120, "because" to 115, "any" to 110,
        "these" to 105, "give" to 100, "day" to 95, "most" to 90, "us" to 85,
        "great" to 80, "why" to 75, "help" to 70, "please" to 65, "today" to 60,
        "thanks" to 58, "thank" to 56, "welcome" to 54, "hello" to 52, "message" to 50,
        "meeting" to 48, "tomorrow" to 46, "morning" to 44, "night" to 42, "phone" to 40,
        "keyboard" to 38, "android" to 36, "application" to 34, "perfect" to 32, "amazing" to 30,
        "always" to 28, "friend" to 26, "family" to 24, "happy" to 22, "love" to 20,
        "where" to 19, "which" to 18, "something" to 17, "anything" to 16, "everything" to 15,
        "awesome" to 14, "beautiful" to 13, "important" to 12, "problem" to 11, "information" to 10
    )

    // Common Sinhala Wijesekara words
    private val CommonSinhalaWords: List<Pair<String, Int>> = listOf(
        "ඔව්" to 500, "නැහැ" to 490, "මම" to 480, "ඔබ" to 470, "අපි" to 460,
        "ස්තූතියි" to 450, "සුබ" to 440, "දවසක්" to 430, "උදෑසනක්" to 420, "රාත්‍රියක්" to 410,
        "කොහොමද" to 400, "හොඳයි" to 390, "ලංකාව" to 380, "සිංහල" to 370, "ගොඩක්" to 360,
        "මොකක්ද" to 350, "එන්න" to 340, "යන්න" to 330, "කරන්න" to 320, "බලන්න" to 310,
        "කියන්න" to 300, "ගන්න" to 290, "දෙන්න" to 280, "වෙලාව" to 270, "අද" to 260,
        "හෙට" to 250, "ඊයේ" to 240, "පස්සේ" to 230, "දැන්" to 220, "නියමයි" to 210,
        "සතුටුයි" to 200, "ආයුබෝවන්" to 190, "යාලුවා" to 180, "ගෙදර" to 170, "වැඩ" to 160
    )

    // Common next word prediction mapping
    private val NextWordPredictions: Map<String, List<String>> = mapOf(
        "how" to listOf("are", "is", "was", "can", "do"),
        "how are" to listOf("you", "things", "we"),
        "thank" to listOf("you", "very", "so"),
        "thank you" to listOf("so", "very", "much", "for"),
        "what" to listOf("is", "are", "do", "time", "about"),
        "what is" to listOf("the", "your", "this", "that"),
        "i" to listOf("am", "have", "will", "would", "want", "think", "can"),
        "i am" to listOf("going", "not", "here", "ready", "happy"),
        "i will" to listOf("be", "call", "send", "see", "come"),
        "i have" to listOf("been", "a", "no", "to", "the"),
        "where" to listOf("are", "is", "were", "can"),
        "good" to listOf("morning", "night", "afternoon", "job", "luck"),
        "see" to listOf("you", "it", "that", "how"),
        "see you" to listOf("soon", "tomorrow", "later", "there"),
        "let" to listOf("me", "us", "them"),
        "let me" to listOf("know", "check", "see", "help"),
        "please" to listOf("let", "find", "call", "help", "send"),
        "on" to listOf("my", "the", "time", "your"),
        "on my" to listOf("way", "own", "phone"),
        "as" to listOf("soon", "well", "if", "much"),
        "as soon" to listOf("as", "possible"),
        "as soon as" to listOf("possible", "you"),
        "nice" to listOf("to", "one", "day"),
        "nice to" to listOf("meet", "see", "hear"),
        "have" to listOf("a", "to", "been", "you"),
        "have a" to listOf("great", "good", "nice", "wonderful"),
        "සුබ" to listOf("දවසක්", "උදෑසනක්", "රාත්‍රියක්", "පැතුම්"),
        "කොහොමද" to listOf("ඔයාට", "වැඩ", "විස්තර"),
        "ස්තූතියි" to listOf("බොහෝම", "ඔබට", "යාලුවා")
    )

    /**
     * Compute smart suggestions:
     * 1. Next word prediction if current word is blank.
     * 2. Shortcut expansions (e.g. "brb" -> "Be right back!").
     * 3. Prefix matching from user dictionary + high frequency corpus.
     * 4. Typo correction via Levenshtein edit distance.
     */
    fun getSmartSuggestions(
        fullText: String,
        currentWord: String,
        userDictionary: List<DictionaryWord>,
        isSinhala: Boolean = false
    ): List<SmartSuggestion> {
        val trimmedInput = fullText.trimEnd()

        // 1. Next Word Prediction (when at a word boundary / space)
        if (currentWord.isBlank()) {
            return getNextWordSuggestions(trimmedInput, isSinhala)
        }

        val wordLower = currentWord.lowercase()
        val results = mutableListOf<SmartSuggestion>()
        val seenWords = mutableSetOf<String>()

        // 2. Shortcut Expansion check (High priority)
        val shortcutMatch = userDictionary.find { it.shortcut.lowercase() == wordLower }
        if (shortcutMatch != null) {
            results.add(
                SmartSuggestion(
                    word = shortcutMatch.word,
                    replacement = shortcutMatch.word,
                    score = 2000,
                    isPrimary = true,
                    isShortcut = true
                )
            )
            seenWords.add(shortcutMatch.word.lowercase())
        }

        // 3. User Custom Dictionary Prefix Matches
        userDictionary.forEach { dict ->
            val dLower = dict.word.lowercase()
            if (dLower.startsWith(wordLower) && !seenWords.contains(dLower)) {
                val isExact = dLower == wordLower
                results.add(
                    SmartSuggestion(
                        word = dict.word,
                        replacement = dict.word,
                        score = dict.frequency + if (isExact) 300 else 100,
                        isPrimary = isExact && results.isEmpty()
                    )
                )
                seenWords.add(dLower)
            }
        }

        // 4. Built-in Corpus Prefix Matches
        val corpus = if (isSinhala) CommonSinhalaWords else CommonEnglishWords
        corpus.forEach { (word, weight) ->
            val wLower = word.lowercase()
            if (wLower.startsWith(wordLower) && !seenWords.contains(wLower)) {
                // Preserve capitalization if user typed uppercase
                val formatted = if (currentWord.first().isUpperCase()) {
                    word.replaceFirstChar { it.uppercase() }
                } else word

                val isExact = wLower == wordLower
                results.add(
                    SmartSuggestion(
                        word = formatted,
                        replacement = formatted,
                        score = weight + if (isExact) 200 else 0,
                        isPrimary = isExact && results.isEmpty()
                    )
                )
                seenWords.add(wLower)
            }
        }

        // 5. Typo & Autocorrect suggestion via Levenshtein distance if prefix list is short
        if (results.size < 3 && wordLower.length >= 3 && !isSinhala) {
            val typoCandidates = mutableListOf<Pair<String, Int>>()
            CommonEnglishWords.forEach { (corpusWord, weight) ->
                if (!seenWords.contains(corpusWord.lowercase())) {
                    val distance = levenshteinDistance(wordLower, corpusWord.lowercase())
                    if (distance == 1 || (wordLower.length >= 5 && distance == 2)) {
                        typoCandidates.add(corpusWord to (weight - distance * 100))
                    }
                }
            }
            typoCandidates.sortByDescending { it.second }
            typoCandidates.take(3 - results.size).forEach { (candidate, score) ->
                val formatted = if (currentWord.first().isUpperCase()) {
                    candidate.replaceFirstChar { it.uppercase() }
                } else candidate
                results.add(
                    SmartSuggestion(
                        word = formatted,
                        replacement = formatted,
                        score = score,
                        isCorrection = true
                    )
                )
                seenWords.add(candidate.lowercase())
            }
        }

        // 6. Fallback if still empty
        if (results.isEmpty()) {
            results.add(SmartSuggestion(currentWord, currentWord, score = 100, isPrimary = true))
            val cap = currentWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            if (cap != currentWord) {
                results.add(SmartSuggestion(cap, cap, score = 90))
            }
            if (!isSinhala && !currentWord.endsWith("s")) {
                results.add(SmartSuggestion("${currentWord}s", "${currentWord}s", score = 80))
            }
        }

        // Sort by score descending and take top 3
        results.sortByDescending { it.score }
        val top = results.take(3)
        // Ensure one item is marked primary for center highlight
        val hasPrimary = top.any { it.isPrimary }
        return if (!hasPrimary && top.isNotEmpty()) {
            top.mapIndexed { idx, item -> if (idx == 0) item.copy(isPrimary = true) else item }
        } else {
            top
        }
    }

    private fun getNextWordSuggestions(fullText: String, isSinhala: Boolean): List<SmartSuggestion> {
        val words = fullText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return if (isSinhala) {
                listOf(
                    SmartSuggestion("ආයුබෝවන්", "ආයුබෝවන්", isPrimary = true),
                    SmartSuggestion("සුබ", "සුබ"),
                    SmartSuggestion("මම", "මම")
                )
            } else {
                listOf(
                    SmartSuggestion("I", "I"),
                    SmartSuggestion("The", "The", isPrimary = true),
                    SmartSuggestion("How", "How")
                )
            }
        }

        val lastWord = words.last().lowercase()
        val lastTwoWords = if (words.size >= 2) "${words[words.size - 2].lowercase()} $lastWord" else null

        val predictions = (if (lastTwoWords != null) NextWordPredictions[lastTwoWords] else null)
            ?: NextWordPredictions[lastWord]

        if (!predictions.isNullOrEmpty()) {
            return predictions.take(3).mapIndexed { idx, w ->
                SmartSuggestion(
                    word = w,
                    replacement = w,
                    score = 500 - idx * 50,
                    isPrimary = idx == 0
                )
            }
        }

        return if (isSinhala) {
            listOf(
                SmartSuggestion("ඔබ", "ඔබ"),
                SmartSuggestion("සහ", "සහ", isPrimary = true),
                SmartSuggestion("නැත", "නැත")
            )
        } else {
            listOf(
                SmartSuggestion("you", "you"),
                SmartSuggestion("to", "to", isPrimary = true),
                SmartSuggestion("the", "the")
            )
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
