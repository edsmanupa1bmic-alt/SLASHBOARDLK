package com.slashboard.keyboard.data.repository

/**
 * Production-ready Helakuru-standard Singlish Phonetic Parser for Sinhala Unicode (U+0D80 to U+0DFF).
 * Fully implements Helakuru specifications for:
 * - Standalone & Dependent Anusvaraya (අනුස්වාරය - ං / U+0D82) via 'x' / 'X' / 'M' / 'nx'
 * - Sanyaka letters (සඤ්ඤක අකුරු) via 'z' prefix combinations (zg -> ඟ, zj -> ඦ, zd -> ඬ, zdh -> ඳ, zb -> ඹ)
 *   plus traditional digraph fallbacks (ng -> ඟ, nd -> ඬ, ndh -> ඳ, mb -> ඹ, nj -> ඦ)
 * - Standalone Independent Vowels (ස්වර: අ, ආ, ඇ, ඈ, ඉ, ඊ, උ, ඌ, එ, ඒ, ඔ, ඕ, ඖ, ඓ)
 * - Consonants (ව්යඤ්ජන) with inherited "a" vs Hal-lakuna (් / Virama U+0DCA)
 * - Dependent Vowel Signs (පිල්ලම්: ා, ැ, ෑ, ි, ී, ු, ූ, ෘ, ෲ, ෙ, ේ, ෛ, ො, ෝ, ෞ, ං, ඃ)
 * - Ligatures: Rakaransaya (ක්‍ර), Yansaya (ක්‍ය), Repaya (ර්ක), Bandi Akuru (ඥ, ඤ, etc.)
 * - Zero Width Joiner (ZWJ U+200D) combinations
 */
object HelakuruSinglishParser {

    const val ZWJ = "\u200D"
    const val VIRAMA = "\u0DCA" // ් (Hal lakuna)
    const val ANUSVARA = "\u0D82" // ං (Binduwa / Anusvaraya)
    const val VISARGA = "\u0D83" // ඃ

    // Standalone Independent Vowels (when starting a syllable or not preceded by consonant)
    private val independentVowels: Map<String, String> = sortedMapByLengthDesc(
        mapOf(
            // Anusvaraya standalone / syllable starters
            "nx" to ANUSVARA,
            "nX" to ANUSVARA,
            "x" to ANUSVARA,
            "X" to ANUSVARA,
            "M" to ANUSVARA,
            ".m" to ANUSVARA,
            ".h" to VISARGA,
            "H" to VISARGA,

            // Trigraphs / Digraphs & Long Vowels (Sorted by length descending)
            "aae" to "ඈ",
            "aai" to "ඓ",
            "aau" to "ඖ",
            "AA" to "ඈ",
            "Aa" to "ඈ",
            "Ae" to "ඈ",
            "aa" to "ආ",
            "ae" to "ඇ",
            "ii" to "ඊ",
            "I" to "ඊ",
            "uu" to "ඌ",
            "U" to "ඌ",
            "ee" to "ඒ",
            "E" to "ඒ",
            "ai" to "ඓ",
            "oo" to "ඕ",
            "O" to "ඕ",
            "au" to "ඖ",

            // Short Vowels & Single Character Modifiers
            "A" to "ඇ",
            "a" to "අ",
            "i" to "ඉ",
            "u" to "උ",
            "e" to "එ",
            "o" to "ඔ",
            "R" to "ඍ",
            "RR" to "ඎ",
            "IL" to "ඏ",
            "ILL" to "ඐ"
        )
    )

    // Dependent Vowel Signs (පිල්ලම්) when directly following a consonant base
    private val vowelPillam: Map<String, String> = sortedMapByLengthDesc(
        mapOf(
            // Anusvaraya / Visarga as dependent modifier
            "nx" to ANUSVARA,
            "nX" to ANUSVARA,
            "x" to ANUSVARA,
            "X" to ANUSVARA,
            "M" to ANUSVARA,
            ".m" to ANUSVARA,
            ".h" to VISARGA,
            "H" to VISARGA,

            // Pillam signs: 3-char and 2-char combinations
            "aae" to "ෑ", // U+0DD1 (Aalapilla / Diga Aeda Pilla)
            "aai" to "ෛ", // U+0DDB
            "aau" to "ෞ", // U+0DDE
            "AA" to "ෑ",  // U+0DD1 (Helakuru standard capital double AA -> ෑ)
            "Aa" to "ෑ",  // U+0DD1 (Helakuru standard capital Aa -> ෑ)
            "Ae" to "ෑ",  // U+0DD1 (Helakuru standard Ae -> ෑ)
            "aa" to "ා",  // U+0DCF (Pure Aalapilla)
            "ae" to "ැ",  // U+0DD0 (Aeda Pilla)
            "ii" to "ී",  // U+0DD3
            "I" to "ී",   // U+0DD3
            "uu" to "ූ",  // U+0DD6
            "U" to "ූ",   // U+0DD6
            "ee" to "ේ",  // U+0DDA
            "E" to "ේ",   // U+0DDA
            "ai" to "ෛ",  // U+0DDB
            "oo" to "ෝ",  // U+0DDC
            "O" to "ෝ",   // U+0DDC
            "au" to "ෞ",  // U+0DDE

            // Single char dependent modifiers
            "A" to "ැ",  // U+0DD0 (Helakuru standard capital single A -> ඇ-පිල්ල / ැ)
            "i" to "ි",  // U+0DD2
            "u" to "ු",  // U+0DD4
            "e" to "ෙ",  // U+0DD9
            "o" to "ො",  // U+0DD8
            "a" to "",   // Inherited vowel 'a' removes the virama (්)
            "R" to "ෘ",  // U+0DD8
            "RR" to "ෲ"  // U+0DF2
        )
    )

    // Consonant base characters without virama
    // Mapped in descending order of length to greedily match trigraphs/digraphs
    private val consonants: Map<String, String> = sortedMapByLengthDesc(
        mapOf(
            // --- Helakuru 'z' Modifier Sanyaka Letters (සඤ්ඤක අකුරු) ---
            "zndh" to "ඳ", // Sanyaka Da (U+0DB3)
            "zdh" to "ඳ",  // Sanyaka Da (U+0DB3)
            "zDh" to "ඳ",  // Sanyaka Da (U+0DB3)
            "znd" to "ඬ",  // Sanyaka Dda (U+0DAC)
            "zd" to "ඬ",   // Sanyaka Dda (U+0DAC)
            "zD" to "ඬ",   // Sanyaka Dda (U+0DAC)
            "zmb" to "ඹ",  // Sanyaka Ba (U+0DB9)
            "zb" to "ඹ",   // Sanyaka Ba (U+0DB9)
            "zB" to "ඹ",   // Sanyaka Ba (U+0DB9)
            "zg" to "ඟ",   // Sanyaka Ga (U+0D9F)
            "zG" to "ඟ",   // Sanyaka Ga (U+0D9F)
            "zj" to "ඦ",   // Sanyaka Ja (U+0DA6)
            "zJ" to "ඦ",   // Sanyaka Ja (U+0DA6)

            // --- Traditional Digraph Fallbacks for Sanyaka & Ligatures ---
            "n-dh" to "ඳ",
            "ndh" to "ඳ",
            "n-d" to "ඬ",
            "nd" to "ඬ",
            "m-b" to "ඹ",
            "mb" to "ඹ",
            "n-g" to "ඟ",
            "ng" to "ඟ",
            "n-j" to "ඦ",
            "nj" to "ඦ",
            "gn" to "ඥ",
            "kn" to "ඤ",
            "tth" to "ට්ඨ",

            // --- Aspirated & Multi-character Consonants ---
            "th" to "ත",
            "Th" to "ථ",
            "dh" to "ද",
            "Dh" to "ධ",
            "ch" to "ච",
            "Ch" to "ඡ",
            "sh" to "ශ",
            "Sh" to "ෂ",
            "ph" to "ඵ",
            "bh" to "භ",
            "kh" to "ඛ",
            "gh" to "ඝ",
            "jh" to "ඣ",

            // --- Standard Consonants ---
            "k" to "ක",
            "K" to "ඛ",
            "g" to "ග",
            "G" to "ඝ",
            "c" to "ච",
            "C" to "ඡ",
            "j" to "ජ",
            "J" to "ඣ",
            "t" to "ට",
            "T" to "ඨ",
            "d" to "ඩ",
            "D" to "ඪ",
            "n" to "න",
            "N" to "ණ",
            "p" to "ප",
            "P" to "ඵ",
            "b" to "බ",
            "B" to "භ",
            "m" to "ම",
            "y" to "ය",
            "r" to "ර",
            "l" to "ල",
            "L" to "ළ",
            "w" to "ව",
            "v" to "ව",
            "s" to "ස",
            "S" to "ෂ",
            "h" to "හ",
            "f" to "ෆ",
            "F" to "ෆ",
            "z" to "ස",
            "Z" to "ශ"
        )
    )

    // Common Singlish word dictionary for high-accuracy candidate generation & colloquial forms
    private val singlishDictionary: Map<String, List<String>> = mapOf(
        "mama" to listOf("මම", "මාමා", "මැම"),
        "mamat" to listOf("මමත්", "මාමත්"),
        "oya" to listOf("ඔයා", "ඔය", "ඕයා"),
        "oyaa" to listOf("ඔයා", "ඔයාට"),
        "oyata" to listOf("ඔයාට", "ඔයට"),
        "api" to listOf("අපි", "ඇපි"),
        "apita" to listOf("අපිට", "අපිටත්"),
        "eyaa" to listOf("එයා", "එයාලා"),
        "eya" to listOf("එයා", "එය"),
        "karanna" to listOf("කරන්න", "කරන්ඩ", "කරන්නෙ", "කරන්නම්"),
        "karanawa" to listOf("කරනවා", "කරන්නේ"),
        "kala" to listOf("කළා", "කලා", "කල"),
        "hari" to listOf("හරි", "හාරි", "හරියට"),
        "honda" to listOf("හොඳ", "හොද"),
        "hondai" to listOf("හොඳයි", "හොදයි"),
        "godak" to listOf("ගොඩක්", "ගොඩාක්"),
        "sthuthi" to listOf("ස්තූතියි", "ස්තුති"),
        "sthutiyi" to listOf("ස්තූතියි", "ස්තුතියි"),
        "suba" to listOf("සුබ", "සුභ"),
        "dawasak" to listOf("දවසක්", "දවස්"),
        "kohomada" to listOf("කොහොමද", "කොහොම"),
        "mokakda" to listOf("මොකක්ද", "මොකද"),
        "mokada" to listOf("මොකද", "මොකද මේ"),
        "sinhala" to listOf("සිංහල", "සිංහලෙන්"),
        "singlish" to listOf("සිංග්ලිෂ්", "සිංලිෂ්"),
        "lanka" to listOf("ලංකා", "ලංකාව"),
        "laxka" to listOf("ලංකා", "ලංකාව"),
        "lanxka" to listOf("ලංකා", "ලංකාව"),
        "lankawa" to listOf("ලංකාව", "ශ්‍රී ලංකාව"),
        "enna" to listOf("එන්න", "එන්ඩ"),
        "yanna" to listOf("යන්න", "යන්ඩ"),
        "gedara" to listOf("ගෙදර", "ගෙදරට"),
        "wada" to listOf("වැඩ", "වඩ"),
        "monawada" to listOf("මොනවද", "මොනවාද"),
        "dan" to listOf("දැන්", "දාන්"),
        "heta" to listOf("හෙට", "හේට"),
        "iye" to listOf("ඊයේ", "ඉයේ"),
        "passe" to listOf("පස්සේ", "පස්සෙ"),
        "kiyanna" to listOf("කියන්න", "කියන්ඩ"),
        "balanna" to listOf("බලන්න", "බලන්ඩ"),
        "ganna" to listOf("ගන්න", "ගන්ඩ"),
        "denna" to listOf("දෙන්න", "දෙන්ඩ"),
        "yaluwa" to listOf("යාලුවා", "යාළුවා"),
        "mithraya" to listOf("මිත්‍රයා", "මිතුරිය"),
        "aayubowan" to listOf("ආයුබෝවන්", "ආයුබොවන්"),
        "ayubowan" to listOf("ආයුබෝවන්", "ආයුබොවන්"),
        "ow" to listOf("ඔව්", "ඕව්"),
        "nehe" to listOf("නැහැ", "නැහෑ"),
        "naha" to listOf("නැහැ", "නැහා"),
        "ne" to listOf("නේ", "නෑ"),
        "na" to listOf("නෑ", "නා"),
        "pulsar" to listOf("පල්සර්"),
        "phone" to listOf("ෆෝන්", "දුරකථන"),
        "msg" to listOf("මැසේජ්"),
        "love" to listOf("ආදරෙයි", "ලව්"),
        "adare" to listOf("ආදරේ", "ආදරෙයි"),
        "adarei" to listOf("ආදරෙයි", "ආදරේ"),
        "mata" to listOf("මට", "මාතා"),
        "thawa" to listOf("තව", "තවත්"),
        "tikak" to listOf("ටිකක්", "ටික"),
        "puluwan" to listOf("පුළුවන්", "පුලුවන්"),
        "be" to listOf("බෑ", "බැහැ"),
        "bahe" to listOf("බැහැ", "බෑ"),
        "eka" to listOf("එක", "එකක්"),
        "oyaata" to listOf("ඔයාට", "ඔයාටත්")
    )

    private fun <V> sortedMapByLengthDesc(map: Map<String, V>): Map<String, V> {
        return map.toList()
            .sortedByDescending { it.first.length }
            .toMap(LinkedHashMap())
    }

    /**
     * Primary deterministic transliteration parser from Singlish string to Sinhala Unicode.
     */
    fun parse(input: String): String {
        if (input.isEmpty()) return ""

        val result = StringBuilder()
        var i = 0
        val len = input.length

        while (i < len) {
            // Check for special prefix: Repaya "R" before consonant (e.g., "Rka" -> "ර්ක")
            if (input[i] == 'R' && i + 1 < len) {
                val nextConsonant = findMatchingConsonant(input, i + 1)
                if (nextConsonant != null) {
                    result.append("ර්")
                    i++
                    continue
                }
            }

            // Check for explicit Anusvaraya digraphs ("nx" / "nX")
            if (input.startsWith("nx", i) || input.startsWith("nX", i)) {
                result.append(ANUSVARA)
                i += 2
                continue
            }

            // Check if current position matches a consonant
            val matchedConsonant = findMatchingConsonant(input, i)
            if (matchedConsonant != null) {
                val (consonantKey, sinhalaBase) = matchedConsonant
                i += consonantKey.length

                // Check for Rakaransaya / Yansaya (e.g. "kra" -> "ක්" + ZWJ + "ර" or "kya" -> "ක්" + ZWJ + "ය")
                var hasRakaransaya = false
                var hasYansaya = false

                if (i < len) {
                    if (input[i] == 'r' && (i + 1 == len || isVowelChar(input[i + 1]))) {
                        hasRakaransaya = true
                        i++ // consume 'r'
                    } else if (input[i] == 'y' && (i + 1 == len || isVowelChar(input[i + 1]))) {
                        hasYansaya = true
                        i++ // consume 'y'
                    }
                }

                // Check for dependent vowel / Anusvaraya following consonant
                val matchedVowel = findMatchingVowel(input, i)
                if (matchedVowel != null) {
                    val (vowelKey, pillam) = matchedVowel
                    i += vowelKey.length

                    if (hasRakaransaya) {
                        result.append(sinhalaBase)
                        result.append(VIRAMA)
                        result.append(ZWJ)
                        result.append("ර")
                        result.append(pillam)
                    } else if (hasYansaya) {
                        result.append(sinhalaBase)
                        result.append(VIRAMA)
                        result.append(ZWJ)
                        result.append("ය")
                        result.append(pillam)
                    } else {
                        result.append(sinhalaBase)
                        result.append(pillam)
                    }
                } else {
                    // No vowel follows -> attach virama (Hal-lakuna)
                    if (hasRakaransaya) {
                        result.append(sinhalaBase)
                        result.append(VIRAMA)
                        result.append(ZWJ)
                        result.append("ර්")
                    } else if (hasYansaya) {
                        result.append(sinhalaBase)
                        result.append(VIRAMA)
                        result.append(ZWJ)
                        result.append("ය්")
                    } else {
                        result.append(sinhalaBase)
                        result.append(VIRAMA)
                    }
                }
            } else {
                // Not a consonant -> Check for independent vowel or standalone Anusvaraya ('x' / 'X')
                val matchedIndepVowel = findMatchingIndependentVowel(input, i)
                if (matchedIndepVowel != null) {
                    val (vowelKey, sinhalaVowel) = matchedIndepVowel
                    result.append(sinhalaVowel)
                    i += vowelKey.length
                } else {
                    // Direct passthrough for non-alphabetic characters / punctuation
                    result.append(input[i])
                    i++
                }
            }
        }

        return result.toString()
    }

    /**
     * Returns candidate suggestions for Singlish input:
     * - Direct dictionary matches (e.g. "mama" -> ["මම", "මාමා", "මැම", "mama"])
     * - Primary rule-based transliteration
     * - Secondary variations (e.g. long vowels or alternative aspirated forms)
     * - Original Latin token as fallback
     */
    fun getSuggestions(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        val normalized = input.trim()
        val lower = normalized.lowercase()
        val results = LinkedHashSet<String>()

        // 1. High-frequency Singlish Dictionary lookup
        val dictMatches = singlishDictionary[lower]
        if (dictMatches != null) {
            dictMatches.forEach { results.add(it) }
        }

        // 2. Primary Rule-based Transliteration
        val primaryParsed = parse(normalized)
        if (primaryParsed.isNotEmpty()) {
            results.add(primaryParsed)
        }

        // 3. Alternative variations (e.g., trying with short/long terminal vowel)
        if (lower.endsWith("a") && !lower.endsWith("aa")) {
            val longVariant = parse(normalized + "a")
            if (longVariant.isNotEmpty()) results.add(longVariant)
        } else if (lower.endsWith("i") && !lower.endsWith("ii")) {
            val longVariant = parse(normalized + "i")
            if (longVariant.isNotEmpty()) results.add(longVariant)
        } else if (lower.endsWith("u") && !lower.endsWith("uu")) {
            val longVariant = parse(normalized + "u")
            if (longVariant.isNotEmpty()) results.add(longVariant)
        }

        // 4. English raw input option
        results.add(normalized)

        return results.toList()
    }

    private fun findMatchingConsonant(input: String, startIndex: Int): Pair<String, String>? {
        for ((key, value) in consonants) {
            if (input.startsWith(key, startIndex)) {
                return key to value
            }
        }
        return null
    }

    private fun findMatchingVowel(input: String, startIndex: Int): Pair<String, String>? {
        for ((key, value) in vowelPillam) {
            if (input.startsWith(key, startIndex)) {
                return key to value
            }
        }
        return null
    }

    private fun findMatchingIndependentVowel(input: String, startIndex: Int): Pair<String, String>? {
        for ((key, value) in independentVowels) {
            if (input.startsWith(key, startIndex)) {
                return key to value
            }
        }
        return null
    }

    private fun isVowelChar(c: Char): Boolean {
        return c in "aeiouAEIOURM.xX"
    }
}
