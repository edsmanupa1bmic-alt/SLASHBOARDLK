package com.slashboard.keyboard.data.model

data class KeyModel(
    val primary: String,
    val secondary: String = "",
    val shifted: String = primary.uppercase(),
    val weight: Float = 1.0f,
    val isFunctional: Boolean = false,
    val code: KeyCode = KeyCode.CHARACTER
)

enum class KeyCode {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    ENTER,
    SWITCH_SYMBOLS,
    SWITCH_LETTERS,
    SWITCH_MORE_SYMBOLS,
    SWITCH_NUMPAD,
    SPACE,
    EMOJI,
    CLIPBOARD,
    SETTINGS,
    CURSOR_LEFT,
    CURSOR_RIGHT,
    CLEAR_ALL
}

data class KeyboardLayout(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val rows: List<List<KeyModel>>
) {
    companion object {
        val Qwerty = KeyboardLayout(
            id = "qwerty",
            name = "English (QWERTY)",
            category = "English",
            description = "Standard English Latin keyboard layout",
            rows = listOf(
                listOf(
                    KeyModel("q", "1"), KeyModel("w", "2"), KeyModel("e", "3"), KeyModel("r", "4"),
                    KeyModel("t", "5"), KeyModel("y", "6"), KeyModel("u", "7"), KeyModel("i", "8"),
                    KeyModel("o", "9"), KeyModel("p", "0")
                ),
                listOf(
                    KeyModel("a", "@"), KeyModel("s", "#"), KeyModel("d", "$"), KeyModel("f", "%"),
                    KeyModel("g", "&"), KeyModel("h", "-"), KeyModel("j", "+"), KeyModel("k", "("),
                    KeyModel("l", ")")
                ),
                listOf(
                    KeyModel("shift", "", isFunctional = true, code = KeyCode.SHIFT, weight = 1.3f),
                    KeyModel("z", "*"), KeyModel("x", "\""), KeyModel("c", "'"), KeyModel("v", ":"),
                    KeyModel("b", ";"), KeyModel("n", "!"), KeyModel("m", "?"),
                    KeyModel("backspace", "", isFunctional = true, code = KeyCode.BACKSPACE, weight = 1.3f)
                )
            )
        )

        val Singlish = KeyboardLayout(
            id = "sinhala_singlish",
            name = "Singlish (සිංග්ලිෂ්)",
            category = "Sinhala",
            description = "Helakuru-standard phonetic Sinhala typing layout (e.g. 'mama' -> 'මම')",
            rows = listOf(
                listOf(
                    KeyModel("q", "1"), KeyModel("w", "2"), KeyModel("e", "3"), KeyModel("r", "4"),
                    KeyModel("t", "5"), KeyModel("y", "6"), KeyModel("u", "7"), KeyModel("i", "8"),
                    KeyModel("o", "9"), KeyModel("p", "0")
                ),
                listOf(
                    KeyModel("a", "@"), KeyModel("s", "#"), KeyModel("d", "$"), KeyModel("f", "%"),
                    KeyModel("g", "&"), KeyModel("h", "-"), KeyModel("j", "+"), KeyModel("k", "("),
                    KeyModel("l", ")")
                ),
                listOf(
                    KeyModel("shift", "", isFunctional = true, code = KeyCode.SHIFT, weight = 1.3f),
                    KeyModel("z", "*"), KeyModel("x", "\""), KeyModel("c", "'"), KeyModel("v", ":"),
                    KeyModel("b", ";"), KeyModel("n", "!"), KeyModel("m", "?"),
                    KeyModel("backspace", "", isFunctional = true, code = KeyCode.BACKSPACE, weight = 1.3f)
                )
            )
        )

        val SymbolsLayout = listOf(
            listOf(
                KeyModel("1"), KeyModel("2"), KeyModel("3"), KeyModel("4"),
                KeyModel("5"), KeyModel("6"), KeyModel("7"), KeyModel("8"),
                KeyModel("9"), KeyModel("0")
            ),
            listOf(
                KeyModel("@"), KeyModel("#"), KeyModel("$"), KeyModel("_"),
                KeyModel("&"), KeyModel("-"), KeyModel("+"), KeyModel("("),
                KeyModel(")"), KeyModel("/")
            ),
            listOf(
                KeyModel("=/<", "", isFunctional = true, code = KeyCode.SWITCH_MORE_SYMBOLS, weight = 1.3f),
                KeyModel("*"), KeyModel("\""), KeyModel("'"), KeyModel(":"),
                KeyModel(";"), KeyModel("!"), KeyModel("?"),
                KeyModel("backspace", "", isFunctional = true, code = KeyCode.BACKSPACE, weight = 1.3f)
            )
        )

        val MoreSymbolsLayout = listOf(
            listOf(
                KeyModel("~"), KeyModel("`"), KeyModel("|"), KeyModel("•"),
                KeyModel("√"), KeyModel("π"), KeyModel("÷"), KeyModel("×"),
                KeyModel("¶"), KeyModel("∆")
            ),
            listOf(
                KeyModel("£"), KeyModel("¥"), KeyModel("€"), KeyModel("¢"),
                KeyModel("^"), KeyModel("°"), KeyModel("="), KeyModel("{"),
                KeyModel("}"), KeyModel("\\")
            ),
            listOf(
                KeyModel("?123", "", isFunctional = true, code = KeyCode.SWITCH_SYMBOLS, weight = 1.3f),
                KeyModel("%"), KeyModel("©"), KeyModel("®"), KeyModel("™"),
                KeyModel("<"), KeyModel(">"), KeyModel("§"),
                KeyModel("backspace", "", isFunctional = true, code = KeyCode.BACKSPACE, weight = 1.3f)
            )
        )

        val NumpadLayout = listOf(
            listOf(
                KeyModel("1"), KeyModel("2"), KeyModel("3"), KeyModel("+", isFunctional = true)
            ),
            listOf(
                KeyModel("4"), KeyModel("5"), KeyModel("6"), KeyModel("-", isFunctional = true)
            ),
            listOf(
                KeyModel("7"), KeyModel("8"), KeyModel("9"), KeyModel("*", isFunctional = true)
            ),
            listOf(
                KeyModel(".", secondary = ","), KeyModel("0"), KeyModel("="),
                KeyModel("backspace", "", isFunctional = true, code = KeyCode.BACKSPACE)
            )
        )

        val AvailableLayouts = listOf(
            Qwerty,
            Singlish
        )

        fun getLayoutById(id: String?): KeyboardLayout {
            if (id.isNullOrBlank()) return Qwerty
            return AvailableLayouts.find { it.id.equals(id, ignoreCase = true) } ?: Qwerty
        }
    }
}
