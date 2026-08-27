package com.slashboard.keyboard

import com.slashboard.keyboard.data.model.KeyCode
import com.slashboard.keyboard.data.model.KeyModel
import com.slashboard.keyboard.data.model.KeyboardLayout
import com.slashboard.keyboard.data.model.KeyboardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLogicTest {

    @Test
    fun testKeyboardLayoutsAvailability() {
        val layouts = KeyboardLayout.AvailableLayouts
        assertEquals(2, layouts.size)
        assertNotNull(layouts.find { it.id == "qwerty" })
        assertNotNull(layouts.find { it.id == "sinhala_singlish" })
    }

    @Test
    fun testKeyboardThemesAvailability() {
        val themes = KeyboardTheme.PresetThemes
        assertTrue(themes.isNotEmpty())
        assertNotNull(themes.find { it.id == "cyber_violet" })
        assertNotNull(themes.find { it.id == "deep_amoled" })
        assertNotNull(themes.find { it.id == "sunset_glow" })
        assertNotNull(themes.find { it.id == "clean_light" })
    }

    @Test
    fun testSpaceKeyCodeProperties() {
        val spaceKey = KeyModel(" ", isFunctional = false, code = KeyCode.SPACE, weight = 4.4f)
        assertEquals(KeyCode.SPACE, spaceKey.code)
        assertEquals(" ", spaceKey.primary)
        assertEquals(4.4f, spaceKey.weight)
    }

    @Test
    fun testLayoutFallback() {
        val defaultLayout = KeyboardLayout.getLayoutById(null)
        assertEquals("qwerty", defaultLayout.id)

        val singlish = KeyboardLayout.getLayoutById("sinhala_singlish")
        assertEquals("sinhala_singlish", singlish.id)

        val fallback = KeyboardLayout.getLayoutById("non_existing_layout")
        assertEquals("qwerty", fallback.id)
    }

    @Test
    fun testSuggestionManagerSinglishPipeline() {
        val oyaSuggestions = com.slashboard.keyboard.data.repository.SuggestionManager.getSuggestions(
            fullText = "oya",
            currentComposing = "oya",
            isSinglish = true
        )
        assertTrue(oyaSuggestions.isNotEmpty())
        assertTrue(oyaSuggestions.any { it.display == "ඔයා" })

        val mamaSuggestions = com.slashboard.keyboard.data.repository.SuggestionManager.getSuggestions(
            fullText = "mama",
            currentComposing = "mama",
            isSinglish = true
        )
        assertTrue(mamaSuggestions.any { it.display == "මම" })
    }

    @Test
    fun testSuggestionManagerEnglishPipeline() {
        val engSuggestions = com.slashboard.keyboard.data.repository.SuggestionManager.getSuggestions(
            fullText = "hel",
            currentComposing = "hel",
            isSinglish = false
        )
        assertTrue(engSuggestions.isNotEmpty())
        assertTrue(engSuggestions.any { it.display.startsWith("hel", ignoreCase = true) })
    }
}
