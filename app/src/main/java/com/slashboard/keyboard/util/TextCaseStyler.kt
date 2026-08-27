package com.slashboard.keyboard.util

object TextCaseStyler {

    enum class CaseStyle(val label: String, val preview: String) {
        UPPERCASE("UPPERCASE", "HELLO WORLD"),
        LOWERCASE("lowercase", "hello world"),
        TITLE_CASE("Title Case", "Hello World"),
        BOLD_SANS("Bold Sans", "𝗛𝗲𝗹𝗹𝗼 𝗪𝗼𝗿𝗹𝗱"),
        FANCY_SCRIPT("Fancy Script", "𝓗𝓮𝓵𝓵𝓸 𝓦𝓸𝓻𝓵𝓭"),
        MONOSPACE("Monospace", "𝙷𝚎𝚕𝚕𝚘 𝚆𝚘𝚛𝚕𝚍"),
        CIRCLED("Circled", "Ⓗⓔⓛⓛⓞ Ⓦⓞⓡⓛⓓ")
    }

    fun transform(text: String, style: CaseStyle): String {
        if (text.isEmpty()) return text
        return when (style) {
            CaseStyle.UPPERCASE -> text.uppercase()
            CaseStyle.LOWERCASE -> text.lowercase()
            CaseStyle.TITLE_CASE -> text.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            CaseStyle.BOLD_SANS -> mapChars(text) { c ->
                when (c) {
                    in 'A'..'Z' -> String(Character.toChars(0x1D5D4 + (c - 'A')))
                    in 'a'..'z' -> String(Character.toChars(0x1D5EE + (c - 'a')))
                    in '0'..'9' -> String(Character.toChars(0x1D7EC + (c - '0')))
                    else -> c.toString()
                }
            }
            CaseStyle.FANCY_SCRIPT -> mapChars(text) { c ->
                when (c) {
                    in 'A'..'Z' -> String(Character.toChars(0x1D4D0 + (c - 'A')))
                    in 'a'..'z' -> String(Character.toChars(0x1D4EA + (c - 'a')))
                    else -> c.toString()
                }
            }
            CaseStyle.MONOSPACE -> mapChars(text) { c ->
                when (c) {
                    in 'A'..'Z' -> String(Character.toChars(0x1D670 + (c - 'A')))
                    in 'a'..'z' -> String(Character.toChars(0x1D68A + (c - 'a')))
                    in '0'..'9' -> String(Character.toChars(0x1D7F6 + (c - '0')))
                    else -> c.toString()
                }
            }
            CaseStyle.CIRCLED -> mapChars(text) { c ->
                when (c) {
                    in 'A'..'Z' -> String(Character.toChars(0x24B6 + (c - 'A')))
                    in 'a'..'z' -> String(Character.toChars(0x24D0 + (c - 'a')))
                    in '1'..'9' -> String(Character.toChars(0x2460 + (c - '1')))
                    '0' -> "⓪"
                    else -> c.toString()
                }
            }
        }
    }

    private fun mapChars(text: String, transform: (Char) -> String): String {
        val sb = StringBuilder()
        for (c in text) {
            sb.append(transform(c))
        }
        return sb.toString()
    }
}
