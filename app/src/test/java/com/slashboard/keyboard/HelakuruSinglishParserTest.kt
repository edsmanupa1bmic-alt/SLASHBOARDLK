package com.slashboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.slashboard.keyboard.data.repository.HelakuruSinglishParser

class HelakuruSinglishParserTest {

    @Test
    fun testIndependentVowels() {
        // Short / Pure Independent Vowels
        assertEquals("අ", HelakuruSinglishParser.parse("a"))
        assertEquals("ආ", HelakuruSinglishParser.parse("aa"))

        // Ae (ඇ) - Helakuru standard mappings
        assertEquals("ඇ", HelakuruSinglishParser.parse("A"))
        assertEquals("ඇ", HelakuruSinglishParser.parse("ae"))

        // Aae (ඈ) - Helakuru standard mappings
        assertEquals("ඈ", HelakuruSinglishParser.parse("AA"))
        assertEquals("ඈ", HelakuruSinglishParser.parse("Aa"))
        assertEquals("ඈ", HelakuruSinglishParser.parse("aae"))
        assertEquals("ඈ", HelakuruSinglishParser.parse("Ae"))

        // Other Independent Vowels
        assertEquals("ඉ", HelakuruSinglishParser.parse("i"))
        assertEquals("ඊ", HelakuruSinglishParser.parse("ii"))
        assertEquals("ඊ", HelakuruSinglishParser.parse("I"))
        assertEquals("උ", HelakuruSinglishParser.parse("u"))
        assertEquals("ඌ", HelakuruSinglishParser.parse("uu"))
        assertEquals("ඌ", HelakuruSinglishParser.parse("U"))
        assertEquals("එ", HelakuruSinglishParser.parse("e"))
        assertEquals("ඒ", HelakuruSinglishParser.parse("ee"))
        assertEquals("ඒ", HelakuruSinglishParser.parse("E"))
        assertEquals("ඔ", HelakuruSinglishParser.parse("o"))
        assertEquals("ඕ", HelakuruSinglishParser.parse("oo"))
        assertEquals("ඕ", HelakuruSinglishParser.parse("O"))
        assertEquals("ඖ", HelakuruSinglishParser.parse("au"))
        assertEquals("ඓ", HelakuruSinglishParser.parse("ai"))
    }

    @Test
    fun testAeAndAaeDependentPillamModifiers() {
        // Consonant + "A" / "ae" -> ඇ-පිල්ල (ැ / \u0DD0)
        assertEquals("කැ", HelakuruSinglishParser.parse("kA"))
        assertEquals("මැ", HelakuruSinglishParser.parse("mA"))
        assertEquals("ටැ", HelakuruSinglishParser.parse("tA"))
        assertEquals("පැ", HelakuruSinglishParser.parse("pA"))
        assertEquals("කැ", HelakuruSinglishParser.parse("kae"))
        assertEquals("මැ", HelakuruSinglishParser.parse("mae"))

        // Consonant + "AA" / "Aa" / "aae" / "Ae" -> ඈ-පිල්ල (ෑ / \u0DD1)
        assertEquals("කෑ", HelakuruSinglishParser.parse("kAA"))
        assertEquals("කෑ", HelakuruSinglishParser.parse("kAa"))
        assertEquals("මෑ", HelakuruSinglishParser.parse("mAA"))
        assertEquals("මෑ", HelakuruSinglishParser.parse("mAa"))
        assertEquals("ටෑ", HelakuruSinglishParser.parse("tAA"))
        assertEquals("ටෑ", HelakuruSinglishParser.parse("tAa"))
        assertEquals("පෑ", HelakuruSinglishParser.parse("pAA"))
        assertEquals("පෑ", HelakuruSinglishParser.parse("pAa"))
        assertEquals("කෑ", HelakuruSinglishParser.parse("kaae"))
        assertEquals("කෑ", HelakuruSinglishParser.parse("kAe"))

        // Consonant + "aa" -> Pure Aalapilla (ා / \u0DCF)
        assertEquals("කා", HelakuruSinglishParser.parse("kaa"))
        assertEquals("මා", HelakuruSinglishParser.parse("maa"))
        assertEquals("ටා", HelakuruSinglishParser.parse("taa"))
        assertEquals("පා", HelakuruSinglishParser.parse("paa"))

        // Consonant + "a" -> Inherited vowel (removes virama)
        assertEquals("ක", HelakuruSinglishParser.parse("ka"))
        assertEquals("ම", HelakuruSinglishParser.parse("ma"))
        assertEquals("ට", HelakuruSinglishParser.parse("ta"))
        assertEquals("ප", HelakuruSinglishParser.parse("pa"))
    }

    @Test
    fun testAnusvarayaMappings() {
        // Standalone 'x' / 'X'
        assertEquals("ං", HelakuruSinglishParser.parse("x"))
        assertEquals("ං", HelakuruSinglishParser.parse("X"))

        // Post-consonant / vowel combinations
        assertEquals("කං", HelakuruSinglishParser.parse("kx"))
        assertEquals("කං", HelakuruSinglishParser.parse("kax"))
        assertEquals("කං", HelakuruSinglishParser.parse("kaX"))
        assertEquals("සමං", HelakuruSinglishParser.parse("samax"))
        assertEquals("ලංක", HelakuruSinglishParser.parse("laxka"))
        assertEquals("ලංක", HelakuruSinglishParser.parse("lanxka"))

        // Suggestions for Lanka
        val lankaSuggestions = HelakuruSinglishParser.getSuggestions("lanka")
        assertTrue(lankaSuggestions.contains("ලංකා"))

        val laxkaSuggestions = HelakuruSinglishParser.getSuggestions("laxka")
        assertTrue(laxkaSuggestions.contains("ලංකා"))

        val lanxkaSuggestions = HelakuruSinglishParser.getSuggestions("lanxka")
        assertTrue(lanxkaSuggestions.contains("ලංකා"))
    }

    @Test
    fun testSanyakaLettersWithZPrefix() {
        // Sanyaka Ga (ඟ)
        assertEquals("ඟ්", HelakuruSinglishParser.parse("zg"))
        assertEquals("ඟ", HelakuruSinglishParser.parse("zga"))
        assertEquals("ඟා", HelakuruSinglishParser.parse("zgaa"))
        assertEquals("ඟි", HelakuruSinglishParser.parse("zgi"))
        assertEquals("ඟු", HelakuruSinglishParser.parse("zgu"))
        assertEquals("ඟෙ", HelakuruSinglishParser.parse("zge"))
        assertEquals("ඟො", HelakuruSinglishParser.parse("zgo"))

        // Sanyaka Ja (ඦ)
        assertEquals("ඦ්", HelakuruSinglishParser.parse("zj"))
        assertEquals("ඦ", HelakuruSinglishParser.parse("zja"))
        assertEquals("ඦා", HelakuruSinglishParser.parse("zjaa"))
        assertEquals("ඦි", HelakuruSinglishParser.parse("zji"))
        assertEquals("ඦු", HelakuruSinglishParser.parse("zju"))
        assertEquals("ඦෙ", HelakuruSinglishParser.parse("zje"))
        assertEquals("ඦො", HelakuruSinglishParser.parse("zjo"))

        // Sanyaka Dda (ඬ)
        assertEquals("ඬ්", HelakuruSinglishParser.parse("zd"))
        assertEquals("ඬ", HelakuruSinglishParser.parse("zda"))
        assertEquals("ඬා", HelakuruSinglishParser.parse("zdaa"))
        assertEquals("ඬි", HelakuruSinglishParser.parse("zdi"))
        assertEquals("ඬු", HelakuruSinglishParser.parse("zdu"))
        assertEquals("ඬෙ", HelakuruSinglishParser.parse("zde"))
        assertEquals("ඬො", HelakuruSinglishParser.parse("zdo"))

        // Sanyaka Da (ඳ)
        assertEquals("ඳ්", HelakuruSinglishParser.parse("zdh"))
        assertEquals("ඳ", HelakuruSinglishParser.parse("zdha"))
        assertEquals("ඳා", HelakuruSinglishParser.parse("zdhaa"))
        assertEquals("ඳි", HelakuruSinglishParser.parse("zdhi"))
        assertEquals("ඳු", HelakuruSinglishParser.parse("zdhu"))
        assertEquals("ඳෙ", HelakuruSinglishParser.parse("zdhe"))
        assertEquals("ඳො", HelakuruSinglishParser.parse("zdho"))

        // Sanyaka Ba (ඹ)
        assertEquals("ඹ්", HelakuruSinglishParser.parse("zb"))
        assertEquals("ඹ", HelakuruSinglishParser.parse("zba"))
        assertEquals("ඹා", HelakuruSinglishParser.parse("zbaa"))
        assertEquals("ඹි", HelakuruSinglishParser.parse("zbi"))
        assertEquals("ඹු", HelakuruSinglishParser.parse("zbu"))
        assertEquals("ඹෙ", HelakuruSinglishParser.parse("zbe"))
        assertEquals("ඹො", HelakuruSinglishParser.parse("zbo"))
    }

    @Test
    fun testDigraphFallbacks() {
        assertEquals("ඟ", HelakuruSinglishParser.parse("nga"))
        assertEquals("ඟ", HelakuruSinglishParser.parse("n-ga"))
        assertEquals("ඬ", HelakuruSinglishParser.parse("nda"))
        assertEquals("ඬ", HelakuruSinglishParser.parse("n-da"))
        assertEquals("ඳ", HelakuruSinglishParser.parse("ndha"))
        assertEquals("ඳ", HelakuruSinglishParser.parse("n-dha"))
        assertEquals("ඹ", HelakuruSinglishParser.parse("mba"))
        assertEquals("ඹ", HelakuruSinglishParser.parse("m-ba"))
        assertEquals("ඦ", HelakuruSinglishParser.parse("nja"))
        assertEquals("ඦ", HelakuruSinglishParser.parse("n-ja"))
    }

    @Test
    fun testLigaturesAndSpecialRules() {
        // Rakaransaya
        val kra = HelakuruSinglishParser.parse("kra")
        assertTrue(kra.startsWith("ක") && kra.contains("ර"))

        // Yansaya
        val kya = HelakuruSinglishParser.parse("kya")
        assertTrue(kya.startsWith("ක") && kya.contains("ය"))

        // Repaya
        assertEquals("ර්ක", HelakuruSinglishParser.parse("Rka"))

        // Bandi
        assertEquals("ඤ", HelakuruSinglishParser.parse("kna"))
        assertEquals("ඥ", HelakuruSinglishParser.parse("gna"))
    }

    @Test
    fun testCommonWordsAndSuggestions() {
        assertEquals("මම", HelakuruSinglishParser.parse("mama"))
        assertEquals("ඔයා", HelakuruSinglishParser.parse("oyaa"))
        assertEquals("කරන්න", HelakuruSinglishParser.parse("karanna"))

        val mamaSuggestions = HelakuruSinglishParser.getSuggestions("mama")
        assertTrue(mamaSuggestions.contains("මම"))
        assertTrue(mamaSuggestions.contains("මාමා"))

        val oyaSuggestions = HelakuruSinglishParser.getSuggestions("oya")
        assertTrue(oyaSuggestions.contains("ඔයා"))
    }
}
