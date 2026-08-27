package com.slashboard.keyboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.ui.components.VirtualKeyboard
import com.slashboard.keyboard.ui.screens.ThemePresetCard
import com.slashboard.keyboard.ui.theme.SlashboardTheme
import com.slashboard.keyboard.util.ThemeEngine

/**
 * ThemeSelectorActivity:
 * Comprehensive Theme picker with full support for Light, Dark, Custom Color studio,
 * and Preset hex swatches [Deep Purple, Midnight Blue, Emerald, Crimson, Cyberpunk, Matte Black].
 */
class ThemeSelectorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTabIndex = intent.getIntExtra("initial_tab_index", 0)

        setContent {
            SlashboardTheme {
                ThemeSelectorScreen(
                    initialTabIndex = initialTabIndex,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorScreen(
    initialTabIndex: Int = 0,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val prefs = SlashboardApp.instance.preferencesRepository
    val settings by prefs.settingsFlow.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex.coerceIn(0, 3)) }
    var previewText by remember { mutableStateOf("") }

    // Custom Color State
    var customBgColor by remember(settings.customThemeBg) {
        mutableStateOf(Color(ThemeEngine.getCustomBgColor(context)))
    }
    var customKeyColor by remember(settings.customThemeKeyBg) {
        mutableStateOf(Color(ThemeEngine.getCustomKeyColor(context)))
    }
    var customTextColor by remember(settings.customThemeTextColor) {
        mutableStateOf(Color(ThemeEngine.getCustomTextColor(context)))
    }
    var customAccentColor by remember(settings.customThemeAccent) {
        mutableStateOf(Color(ThemeEngine.getCustomAccentColor(context)))
    }

    val activeCustomTheme = remember(customBgColor, customKeyColor, customTextColor, customAccentColor) {
        KeyboardTheme.buildCustomTheme(
            id = "custom_user_theme",
            name = "Custom Color Theme",
            background = customBgColor,
            keyBackground = customKeyColor,
            accentColor = customAccentColor,
            textColor = customTextColor,
            isDark = true,
            radiusDp = settings.keyCornerRadius
        )
    }

    val activeTheme = if (selectedTabIndex == 2) activeCustomTheme else prefs.getActiveTheme()

    val tabs = listOf(
        "Light" to Icons.Default.LightMode,
        "Dark" to Icons.Default.DarkMode,
        "Custom Color" to Icons.Default.Palette,
        "Wallpapers" to Icons.Default.Wallpaper
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Themes", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().testTag("theme_selector_tabs")
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            // Keyboard Live Preview Top Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "LIVE THEME PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.08.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        VirtualKeyboard(
                            settings = settings.copy(
                                themeId = if (selectedTabIndex == 2) "custom_user_theme" else settings.themeId
                            ),
                            currentWord = previewText,
                            fullText = previewText,
                            onInsertText = { previewText += it },
                            onDeleteCharacter = { if (previewText.isNotEmpty()) previewText = previewText.dropLast(1) },
                            onEnter = { previewText += "\n" },
                            onClearText = { previewText = "" },
                            onOpenSettings = {}
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Light Themes List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(KeyboardTheme.LightThemes, key = { it.id }) { theme ->
                            ThemePresetCard(
                                theme = theme,
                                isSelected = settings.themeId == theme.id && settings.customWallpaperPath == null,
                                onSelect = {
                                    prefs.updateThemeId(theme.id)
                                    prefs.updateCustomWallpaper(null)
                                    Toast.makeText(context, "${theme.name} applied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Dark Themes List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(KeyboardTheme.DarkThemes, key = { it.id }) { theme ->
                            ThemePresetCard(
                                theme = theme,
                                isSelected = settings.themeId == theme.id && settings.customWallpaperPath == null,
                                onSelect = {
                                    prefs.updateThemeId(theme.id)
                                    prefs.updateCustomWallpaper(null)
                                    Toast.makeText(context, "${theme.name} applied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                2 -> {
                    // Custom Color Tab (Tab Index 2)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Preset Popular Palettes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Preset Swatches: Deep Purple, Midnight Blue, Emerald, Crimson, Cyberpunk, Matte Black
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeEngine.PRESET_SWATCHES.forEach { preset ->
                                val isCurrent = customBgColor.toArgb() == preset.bgColor && customKeyColor.toArgb() == preset.keyColor
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(preset.bgColor),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .width(105.dp)
                                        .clickable {
                                            customBgColor = Color(preset.bgColor)
                                            customKeyColor = Color(preset.keyColor)
                                            customAccentColor = Color(preset.accentColor)
                                            customTextColor = Color(preset.textColor)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.bgColor)))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.keyColor)))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.accentColor)))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = preset.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Color Tuning Sliders (Background, Key, Text, Accent)
                        ColorSliderItem(
                            label = "Background Color",
                            color = customBgColor,
                            onColorChange = { customBgColor = it }
                        )

                        ColorSliderItem(
                            label = "Key Color",
                            color = customKeyColor,
                            onColorChange = { customKeyColor = it }
                        )

                        ColorSliderItem(
                            label = "Key Text Color",
                            color = customTextColor,
                            onColorChange = { customTextColor = it }
                        )

                        ColorSliderItem(
                            label = "Accent / Enter Key Color",
                            color = customAccentColor,
                            onColorChange = { customAccentColor = it }
                        )

                        // Action Buttons: Apply / Reset
                        Button(
                            onClick = {
                                val bgInt = customBgColor.toArgb()
                                val keyInt = customKeyColor.toArgb()
                                val textInt = customTextColor.toArgb()
                                val accentInt = customAccentColor.toArgb()

                                ThemeEngine.saveCustomThemeColors(
                                    context = context,
                                    bgColor = bgInt,
                                    keyColor = keyInt,
                                    textColor = textInt,
                                    accentColor = accentInt,
                                    isDark = true
                                )
                                prefs.updateCustomWallpaper(null)
                                Toast.makeText(context, "Custom Color Theme Applied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_apply_custom_theme")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("APPLY CUSTOM COLOR THEME", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                3 -> {
                    // Wallpapers Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Photo & Curated Backgrounds",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(KeyboardTheme.DarkThemes.take(4), key = { it.id }) { theme ->
                            ThemePresetCard(
                                theme = theme,
                                isSelected = settings.themeId == theme.id,
                                onSelect = {
                                    prefs.updateThemeId(theme.id)
                                    Toast.makeText(context, "${theme.name} applied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSliderItem(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var r by remember(color) { mutableStateOf(color.red) }
    var g by remember(color) { mutableStateOf(color.green) }
    var b by remember(color) { mutableStateOf(color.blue) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, Color(0x66FFFFFF), CircleShape)
                    )
                    Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                val hex = String.format("#%02X%02X%02X", (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
                Text(text = hex, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Red slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("R", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.width(16.dp))
                Slider(
                    value = r,
                    onValueChange = {
                        r = it
                        onColorChange(Color(r, g, b, 1f))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Green slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.width(16.dp))
                Slider(
                    value = g,
                    onValueChange = {
                        g = it
                        onColorChange(Color(r, g, b, 1f))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Blue slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), modifier = Modifier.width(16.dp))
                Slider(
                    value = b,
                    onValueChange = {
                        b = it
                        onColorChange(Color(r, g, b, 1f))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
