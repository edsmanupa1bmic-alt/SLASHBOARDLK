package com.slashboard.keyboard

import com.slashboard.keyboard.data.repository.SuggestionManager
import com.slashboard.keyboard.data.repository.Trie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class SmartDictionaryEngineTest {

    @Test
    fun testTriePrefixSearchAndRanking() {
        val trie = Trie()
        trie.insert("ලංකාව", 1000)
        trie.insert("ලස්සන", 980)
        trie.insert("ලෝකය", 960)
        trie.insert("ලැබේ", 940)
        trie.insert("ලොකු", 920)
        trie.insert("ලියා", 900)
        trie.insert("ලියන්න", 890)

        val results = trie.searchPrefix("ල", limit = 5)
        assertEquals(5, results.size)
        assertEquals("ලංකාව", results[0].word)
        assertEquals("ලස්සන", results[1].word)
        assertEquals("ලෝකය", results[2].word)
        assertEquals("ලැබේ", results[3].word)
        assertEquals("ලොකු", results[4].word)
    }

    @Test
    fun testTrieSelfLearningBoost() {
        val trie = Trie()
        trie.insert("ලැබේ", 900)
        trie.insert("ලස්සන", 950)

        // Initial search: "ලස්සන" is top
        val initial = trie.searchPrefix("ල", limit = 2)
        assertEquals("ලස්සන", initial[0].word)

        // Boost "ලැබේ" frequency by 100
        trie.boostFrequency("ලැබේ", increment = 100)

        // After boost: "ලැබේ" (1000) beats "ලස්සන" (950)
        val afterBoost = trie.searchPrefix("ල", limit = 2)
        assertEquals("ලැබේ", afterBoost[0].word)
        assertTrue(afterBoost[0].isUserLearned)
    }

    @Test
    fun testTrieEnglishPrefixSearch() {
        val trie = Trie()
        trie.insert("beautiful", 980)
        trie.insert("because", 990)
        trie.insert("before", 970)
        trie.insert("between", 960)
        trie.insert("best", 950)
        trie.insert("better", 940)

        val results = trie.searchPrefix("be", limit = 5)
        assertEquals(5, results.size)
        assertEquals("because", results[0].word)
        assertEquals("beautiful", results[1].word)
        assertEquals("before", results[2].word)
        assertEquals("between", results[3].word)
        assertEquals("best", results[4].word)
    }

    @Test
    fun testTrieSearchPerformanceUnder5ms() {
        val trie = Trie()
        // Insert 10,000 synthetic words
        for (i in 0 until 10000) {
            trie.insert("word$i", i % 1000)
        }

        // Measure query time across multiple iterations
        val elapsed = measureTimeMillis {
            for (j in 0 until 50) {
                val matches = trie.searchPrefix("word12", limit = 5)
                assertTrue(matches.isNotEmpty())
            }
        }

        val avgQueryTimeMs = elapsed / 50.0
        println("Average Trie search prefix latency: $avgQueryTimeMs ms")
        assertTrue("Prefix query must execute in < 5ms for 60fps typing", avgQueryTimeMs < 5.0)
    }

    @Test
    fun testSinglishModeSuggestionManagerPipeline() {
        // Query "la" in Singlish mode
        val suggestions = SuggestionManager.getSuggestions(
            fullText = "la",
            currentComposing = "la",
            isSinglish = true
        )

        assertTrue(suggestions.isNotEmpty())
        assertEquals("ල", suggestions[0].display)
    }

    @Test
    fun testEnglishModeSuggestionManagerPipeline() {
        val suggestions = SuggestionManager.getSuggestions(
            fullText = "be",
            currentComposing = "be",
            isSinglish = false
        )

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.display == "be" || it.display.startsWith("be") })
    }
}
