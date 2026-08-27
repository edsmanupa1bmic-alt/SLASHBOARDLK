package com.slashboard.keyboard.data.model

import androidx.compose.ui.graphics.Color

enum class ThemeCategory(val displayName: String, val iconName: String) {
    LIGHT("Light", "light_mode"),
    DARK("Dark", "dark_mode"),
    CUSTOM("Custom Color", "palette")
}

data class KeyboardTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val keyBackground: Color,
    val keyPressedBackground: Color,
    val functionalKeyBackground: Color,
    val keyTextColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val keyBorderColor: Color = Color.Transparent,
    val keyRadiusDp: Int = 8,
    val category: ThemeCategory = if (isDark) ThemeCategory.DARK else ThemeCategory.LIGHT
) {
    companion object {
        // --- LIGHT THEMES ---
        val CleanLight = KeyboardTheme(
            id = "clean_light",
            name = "Clean Light",
            isDark = false,
            background = Color(0xFFF1F5F9),
            surface = Color(0xFFE2E8F0),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFCBD5E1),
            functionalKeyBackground = Color(0xFFE2E8F0),
            keyTextColor = Color(0xFF0F172A),
            secondaryTextColor = Color(0xFF64748B),
            accentColor = Color(0xFF7C3AED),
            keyBorderColor = Color(0x40000000),
            keyRadiusDp = 8,
            category = ThemeCategory.LIGHT
        )

        val PastelCoral = KeyboardTheme(
            id = "pastel_coral",
            name = "Pastel Coral",
            isDark = false,
            background = Color(0xFFFFF1F2),
            surface = Color(0xFFFFE4E6),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFFECDD3),
            functionalKeyBackground = Color(0xFFFFE4E6),
            keyTextColor = Color(0xFF881337),
            secondaryTextColor = Color(0xFF9F1239),
            accentColor = Color(0xFFFB7185),
            keyBorderColor = Color(0x66FB7185),
            keyRadiusDp = 10,
            category = ThemeCategory.LIGHT
        )

        val GlassLight = KeyboardTheme(
            id = "glass_light",
            name = "Glass Light",
            isDark = false,
            background = Color(0x80FFFFFF),
            surface = Color(0x40000000),
            keyBackground = Color(0x99FFFFFF),
            keyPressedBackground = Color(0xCCFFFFFF),
            functionalKeyBackground = Color(0x66FFFFFF),
            keyTextColor = Color(0xFF000000),
            secondaryTextColor = Color(0x99000000),
            accentColor = Color(0xFF7C3AED),
            keyBorderColor = Color(0x1A000000),
            keyRadiusDp = 12,
            category = ThemeCategory.LIGHT
        )

        val LavenderMist = KeyboardTheme(
            id = "lavender_mist",
            name = "Lavender Mist",
            isDark = false,
            background = Color(0xFFF3E8FF),
            surface = Color(0xFFE9D5FF),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFD8B4FE),
            functionalKeyBackground = Color(0xFFEDE9FE),
            keyTextColor = Color(0xFF3B0764),
            secondaryTextColor = Color(0xFF6B21A8),
            accentColor = Color(0xFF9333EA),
            keyBorderColor = Color(0x449333EA),
            keyRadiusDp = 10,
            category = ThemeCategory.LIGHT
        )

        val CreamVanilla = KeyboardTheme(
            id = "cream_vanilla",
            name = "Cream Vanilla",
            isDark = false,
            background = Color(0xFFFAF7F2),
            surface = Color(0xFFF0EAE1),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFE6DCce),
            functionalKeyBackground = Color(0xFFEFE8DE),
            keyTextColor = Color(0xFF292524),
            secondaryTextColor = Color(0xFF78716C),
            accentColor = Color(0xFFD97706),
            keyBorderColor = Color(0x3378716C),
            keyRadiusDp = 8,
            category = ThemeCategory.LIGHT
        )

        val MintFresh = KeyboardTheme(
            id = "mint_fresh",
            name = "Mint Fresh",
            isDark = false,
            background = Color(0xFFECFDF5),
            surface = Color(0xFFD1FAE5),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFA7F3D0),
            functionalKeyBackground = Color(0xFFD1FAE5),
            keyTextColor = Color(0xFF064E3B),
            secondaryTextColor = Color(0xFF047857),
            accentColor = Color(0xFF10B981),
            keyBorderColor = Color(0x4410B981),
            keyRadiusDp = 8,
            category = ThemeCategory.LIGHT
        )

        val NordicSky = KeyboardTheme(
            id = "nordic_sky",
            name = "Nordic Sky",
            isDark = false,
            background = Color(0xFFF0F9FF),
            surface = Color(0xFFE0F2FE),
            keyBackground = Color(0xFFFFFFFF),
            keyPressedBackground = Color(0xFFBAE6FD),
            functionalKeyBackground = Color(0xFFE0F2FE),
            keyTextColor = Color(0xFF082F49),
            secondaryTextColor = Color(0xFF0284C7),
            accentColor = Color(0xFF0284C7),
            keyBorderColor = Color(0x440284C7),
            keyRadiusDp = 8,
            category = ThemeCategory.LIGHT
        )

        // --- DARK THEMES ---
        val CyberViolet = KeyboardTheme(
            id = "cyber_violet",
            name = "Cyber Violet",
            isDark = true,
            background = Color(0xFF0F0B1E),
            surface = Color(0xFF1E1738),
            keyBackground = Color(0xFF2B2052),
            keyPressedBackground = Color(0xFF4C1D95),
            functionalKeyBackground = Color(0xFF191233),
            keyTextColor = Color(0xFFF3F4F6),
            secondaryTextColor = Color(0xFF9CA3AF),
            accentColor = Color(0xFFA855F7),
            keyBorderColor = Color(0x88A855F7),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        val DeepAmoled = KeyboardTheme(
            id = "deep_amoled",
            name = "Deep AMOLED",
            isDark = true,
            background = Color(0xFF000000),
            surface = Color(0xFF121212),
            keyBackground = Color(0xFF1E1E1E),
            keyPressedBackground = Color(0xFF333333),
            functionalKeyBackground = Color(0xFF121212),
            keyTextColor = Color(0xFFFFFFFF),
            secondaryTextColor = Color(0xFF888888),
            accentColor = Color(0xFF38BDF8),
            keyBorderColor = Color(0x66FFFFFF),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        val SunsetGlow = KeyboardTheme(
            id = "sunset_glow",
            name = "Sunset Glow",
            isDark = true,
            background = Color(0xFF1A0B1E),
            surface = Color(0xFF2C1233),
            keyBackground = Color(0xFF451952),
            keyPressedBackground = Color(0xFF662549),
            functionalKeyBackground = Color(0xFF220E28),
            keyTextColor = Color(0xFFFDE2F3),
            secondaryTextColor = Color(0xFFE5B8F4),
            accentColor = Color(0xFFF39F5A),
            keyBorderColor = Color(0x88F39F5A),
            keyRadiusDp = 10,
            category = ThemeCategory.DARK
        )

        val EmeraldForest = KeyboardTheme(
            id = "emerald_forest",
            name = "Emerald Forest",
            isDark = true,
            background = Color(0xFF061A14),
            surface = Color(0xFF0D2E24),
            keyBackground = Color(0xFF134234),
            keyPressedBackground = Color(0xFF1C5E4A),
            functionalKeyBackground = Color(0xFF0A231B),
            keyTextColor = Color(0xFFE6F4EA),
            secondaryTextColor = Color(0xFFA3D9C9),
            accentColor = Color(0xFF34D399),
            keyBorderColor = Color(0x8834D399),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        val ElectricBlue = KeyboardTheme(
            id = "electric_blue",
            name = "Electric Blue",
            isDark = true,
            background = Color(0xFF0A1128),
            surface = Color(0xFF14213D),
            keyBackground = Color(0xFF1C315E),
            keyPressedBackground = Color(0xFF254687),
            functionalKeyBackground = Color(0xFF0E1A38),
            keyTextColor = Color(0xFFF1F5F9),
            secondaryTextColor = Color(0xFF94A3B8),
            accentColor = Color(0xFF38BDF8),
            keyBorderColor = Color(0x8838BDF8),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        val GlassDark = KeyboardTheme(
            id = "glass_dark",
            name = "Glass Dark",
            isDark = true,
            background = Color(0x80000000),
            surface = Color(0x40FFFFFF),
            keyBackground = Color(0x33FFFFFF),
            keyPressedBackground = Color(0x66FFFFFF),
            functionalKeyBackground = Color(0x1AFFFFFF),
            keyTextColor = Color(0xFFFFFFFF),
            secondaryTextColor = Color(0xB3FFFFFF),
            accentColor = Color(0xFF38BDF8),
            keyBorderColor = Color(0x1AFFFFFF),
            keyRadiusDp = 12,
            category = ThemeCategory.DARK
        )

        val NeonCyanGlass = KeyboardTheme(
            id = "neon_cyan_glass",
            name = "Neon Cyan Glass",
            isDark = true,
            background = Color(0x66001219),
            surface = Color(0x33005F73),
            keyBackground = Color(0x4D0A9396),
            keyPressedBackground = Color(0x8094D2BD),
            functionalKeyBackground = Color(0x33005F73),
            keyTextColor = Color(0xFFE9D8A6),
            secondaryTextColor = Color(0xB3E9D8A6),
            accentColor = Color(0xFF0A9396),
            keyBorderColor = Color(0x330A9396),
            keyRadiusDp = 10,
            category = ThemeCategory.DARK
        )

        val HackerMatrixGlass = KeyboardTheme(
            id = "hacker_matrix_glass",
            name = "Matrix Glass",
            isDark = true,
            background = Color(0x80001A00),
            surface = Color(0x40003300),
            keyBackground = Color(0x33004D00),
            keyPressedBackground = Color(0x66009900),
            functionalKeyBackground = Color(0x1A003300),
            keyTextColor = Color(0xFF00FF00),
            secondaryTextColor = Color(0xB300FF00),
            accentColor = Color(0xFF00FF00),
            keyBorderColor = Color(0x3300FF00),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        val FrostedPlum = KeyboardTheme(
            id = "frosted_plum",
            name = "Frosted Plum",
            isDark = true,
            background = Color(0x8023152F),
            surface = Color(0x403A2649),
            keyBackground = Color(0x4D4F3566),
            keyPressedBackground = Color(0x806A4C87),
            functionalKeyBackground = Color(0x333A2649),
            keyTextColor = Color(0xFFF3E8FF),
            secondaryTextColor = Color(0xB3F3E8FF),
            accentColor = Color(0xFFC084FC),
            keyBorderColor = Color(0x33C084FC),
            keyRadiusDp = 14,
            category = ThemeCategory.DARK
        )

        val MidnightSlate = KeyboardTheme(
            id = "midnight_slate",
            name = "Midnight Slate",
            isDark = true,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            keyBackground = Color(0xFF334155),
            keyPressedBackground = Color(0xFF475569),
            functionalKeyBackground = Color(0xFF1E293B),
            keyTextColor = Color(0xFFF8FAFC),
            secondaryTextColor = Color(0xFF94A3B8),
            accentColor = Color(0xFF60A5FA),
            keyBorderColor = Color(0x6660A5FA),
            keyRadiusDp = 8,
            category = ThemeCategory.DARK
        )

        // --- CUSTOM COLOR THEMES & CURATED PALETTES ---
        val RoyalGold = KeyboardTheme(
            id = "royal_gold",
            name = "Royal Obsidian Gold",
            isDark = true,
            background = Color(0xFF12100E),
            surface = Color(0xFF241F1A),
            keyBackground = Color(0xFF382F24),
            keyPressedBackground = Color(0xFF5A4B3A),
            functionalKeyBackground = Color(0xFF241F1A),
            keyTextColor = Color(0xFFFEF3C7),
            secondaryTextColor = Color(0xFFFDE68A),
            accentColor = Color(0xFFF59E0B),
            keyBorderColor = Color(0x88F59E0B),
            keyRadiusDp = 10,
            category = ThemeCategory.CUSTOM
        )

        val CherryRose = KeyboardTheme(
            id = "cherry_rose",
            name = "Cherry Blossom",
            isDark = true,
            background = Color(0xFF2A0815),
            surface = Color(0xFF431024),
            keyBackground = Color(0xFF5E1733),
            keyPressedBackground = Color(0xFF881E48),
            functionalKeyBackground = Color(0xFF360A1D),
            keyTextColor = Color(0xFFFFF1F2),
            secondaryTextColor = Color(0xFFFDA4AF),
            accentColor = Color(0xFFF43F5E),
            keyBorderColor = Color(0x88F43F5E),
            keyRadiusDp = 10,
            category = ThemeCategory.CUSTOM
        )

        val OceanTeal = KeyboardTheme(
            id = "ocean_teal",
            name = "Ocean Teal",
            isDark = true,
            background = Color(0xFF04202C),
            surface = Color(0xFF083344),
            keyBackground = Color(0xFF0E4E68),
            keyPressedBackground = Color(0xFF155E75),
            functionalKeyBackground = Color(0xFF083344),
            keyTextColor = Color(0xFFECFEFF),
            secondaryTextColor = Color(0xFF67E8F9),
            accentColor = Color(0xFF06B6D4),
            keyBorderColor = Color(0x8806B6D4),
            keyRadiusDp = 8,
            category = ThemeCategory.CUSTOM
        )

        val SolarOrange = KeyboardTheme(
            id = "solar_orange",
            name = "Solar Flame",
            isDark = true,
            background = Color(0xFF1C0D02),
            surface = Color(0xFF2E1504),
            keyBackground = Color(0xFF4A2307),
            keyPressedBackground = Color(0xFF7C3D0A),
            functionalKeyBackground = Color(0xFF2E1504),
            keyTextColor = Color(0xFFFFF7ED),
            secondaryTextColor = Color(0xFFFDBA74),
            accentColor = Color(0xFFEA580C),
            keyBorderColor = Color(0x88EA580C),
            keyRadiusDp = 8,
            category = ThemeCategory.CUSTOM
        )

        val NeonCyber = KeyboardTheme(
            id = "neon_cyber",
            name = "Neon Synthwave",
            isDark = true,
            background = Color(0xFF130924),
            surface = Color(0xFF241042),
            keyBackground = Color(0xFF3B1B6B),
            keyPressedBackground = Color(0xFF5B2B9E),
            functionalKeyBackground = Color(0xFF241042),
            keyTextColor = Color(0xFFFDF4FF),
            secondaryTextColor = Color(0xFFF0ABFC),
            accentColor = Color(0xFFD946EF),
            keyBorderColor = Color(0x88D946EF),
            keyRadiusDp = 10,
            category = ThemeCategory.CUSTOM
        )

        val ForestSage = KeyboardTheme(
            id = "forest_sage",
            name = "Forest Sage",
            isDark = true,
            background = Color(0xFF0E1A14),
            surface = Color(0xFF1A2E24),
            keyBackground = Color(0xFF284738),
            keyPressedBackground = Color(0xFF3A634F),
            functionalKeyBackground = Color(0xFF1A2E24),
            keyTextColor = Color(0xFFF0FDF4),
            secondaryTextColor = Color(0xFF86EFAC),
            accentColor = Color(0xFF22C55E),
            keyBorderColor = Color(0x8822C55E),
            keyRadiusDp = 8,
            category = ThemeCategory.CUSTOM
        )

        val LightThemes = listOf(
            CleanLight,
            PastelCoral,
            GlassLight,
            LavenderMist,
            CreamVanilla,
            MintFresh,
            NordicSky
        )

        val DarkThemes = listOf(
            CyberViolet,
            DeepAmoled,
            SunsetGlow,
            EmeraldForest,
            ElectricBlue,
            GlassDark,
            NeonCyanGlass,
            HackerMatrixGlass,
            FrostedPlum,
            MidnightSlate
        )

        val CustomColorThemes = listOf(
            RoyalGold,
            CherryRose,
            OceanTeal,
            SolarOrange,
            NeonCyber,
            ForestSage
        )

        val PresetThemes = LightThemes + DarkThemes + CustomColorThemes

        fun buildCustomTheme(
            id: String = "custom_user_theme",
            name: String = "My Custom Theme",
            background: Color,
            keyBackground: Color,
            accentColor: Color,
            textColor: Color,
            isDark: Boolean = true,
            radiusDp: Int = 8
        ): KeyboardTheme {
            val surface = if (isDark) {
                Color(
                    red = (background.red * 1.3f).coerceIn(0f, 1f),
                    green = (background.green * 1.3f).coerceIn(0f, 1f),
                    blue = (background.blue * 1.3f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            } else {
                Color(
                    red = (background.red * 0.92f).coerceIn(0f, 1f),
                    green = (background.green * 0.92f).coerceIn(0f, 1f),
                    blue = (background.blue * 0.92f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            }

            val keyPressedBg = if (isDark) {
                Color(
                    red = (keyBackground.red * 1.4f).coerceIn(0f, 1f),
                    green = (keyBackground.green * 1.4f).coerceIn(0f, 1f),
                    blue = (keyBackground.blue * 1.4f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            } else {
                Color(
                    red = (keyBackground.red * 0.85f).coerceIn(0f, 1f),
                    green = (keyBackground.green * 0.85f).coerceIn(0f, 1f),
                    blue = (keyBackground.blue * 0.85f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            }

            val secondaryTextColor = textColor.copy(alpha = 0.7f)

            return KeyboardTheme(
                id = id,
                name = name,
                isDark = isDark,
                background = background,
                surface = surface,
                keyBackground = keyBackground,
                keyPressedBackground = keyPressedBg,
                functionalKeyBackground = surface,
                keyTextColor = textColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                keyBorderColor = accentColor.copy(alpha = 0.5f),
                keyRadiusDp = radiusDp,
                category = ThemeCategory.CUSTOM
            )
        }

        fun getThemeById(id: String): KeyboardTheme {
            return PresetThemes.find { it.id == id } ?: CyberViolet
        }
    }
}

