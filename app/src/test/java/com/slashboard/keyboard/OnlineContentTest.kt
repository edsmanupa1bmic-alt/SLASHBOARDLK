package com.slashboard.keyboard

import com.slashboard.keyboard.data.repository.OnlineThemeRepository
import com.slashboard.keyboard.data.repository.OnlineWordPackRepository
import com.slashboard.keyboard.data.repository.Trie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineContentTest {

    @Test
    fun testOnlineThemeRepositoryIntegrity() {
        val themes = OnlineThemeRepository.onlineThemes
        assertTrue("Online themes list must not be empty", themes.isNotEmpty())

        val sigiriya = themes.find { it.id == "sigiriya_sunset" }
        assertNotNull(sigiriya)
        assertEquals("Sri Lanka", sigiriya?.category)
        assertTrue(sigiriya?.previewUrl?.startsWith("https://") == true)
        assertTrue(sigiriya?.downloadUrl?.startsWith("https://") == true)
    }

    @Test
    fun testOnlineWordPackRepositoryIntegrity() {
        val packs = OnlineWordPackRepository.availablePacks
        assertTrue("Available word packs must not be empty", packs.isNotEmpty())

        val colloquialPack = packs.find { it.id == "pack_si_colloquial" }
        assertNotNull(colloquialPack)
        assertTrue(colloquialPack!!.isSinhala)
        assertTrue(colloquialPack.builtInPack.isNotEmpty())

        // Test inserting word pack into in-memory Trie
        val trie = Trie()
        colloquialPack.builtInPack.forEach { (word, freq) ->
            trie.insert(word, freq)
        }

        val results = trie.searchPrefix("එල", limit = 3)
        assertTrue(results.isNotEmpty())
        assertEquals("එලකිරි", results[0].word)
    }

    @Test
    fun testTechWordPackEnglishTrieIntegration() {
        val techPack = OnlineWordPackRepository.availablePacks.find { it.id == "pack_tech_it" }
        assertNotNull(techPack)

        val trie = Trie()
        techPack!!.builtInPack.forEach { (word, freq) ->
            trie.insert(word, freq)
        }

        val results = trie.searchPrefix("dev", limit = 3)
        assertTrue(results.isNotEmpty())
        assertEquals("developer", results[0].word)
    }
}
