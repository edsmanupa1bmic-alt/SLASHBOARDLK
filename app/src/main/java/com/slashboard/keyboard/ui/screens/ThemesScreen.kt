package com.slashboard.keyboard.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.data.model.ThemeCategory
import com.slashboard.keyboard.data.repository.OnlineThemeRepository
import com.slashboard.keyboard.ui.components.VirtualKeyboard
import kotlinx.coroutines.launch

enum class ThemeTab(val title: String, val category: ThemeCategory?, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIGHT("Light", ThemeCategory.LIGHT, Icons.Default.LightMode),
    DARK("Dark", ThemeCategory.DARK, Icons.Default.DarkMode),
    CUSTOM("Custom Color", ThemeCategory.CUSTOM, Icons.Default.Palette),
    WALLPAPERS("Wallpapers", null, Icons.Default.Wallpaper)
}

@Composable
fun ThemesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = SlashboardApp.instance.preferencesRepository
    val settings by prefs.settingsFlow.collectAsState()

    var selectedTab by remember { mutableStateOf(ThemeTab.LIGHT) }
    var showTuningPanel by remember { mutableStateOf(false) }
    var previewText by remember { mutableStateOf("") }

    // Custom Theme Builder Local State
    var customBgColor by remember(settings.customThemeBg) {
        mutableStateOf(Color(settings.customThemeBg.toULong()))
    }
    var customKeyBgColor by remember(settings.customThemeKeyBg) {
        mutableStateOf(Color(settings.customThemeKeyBg.toULong()))
    }
    var customAccentColor by remember(settings.customThemeAccent) {
        mutableStateOf(Color(settings.customThemeAccent.toULong()))
    }
    var customTextColor by remember(settings.customThemeTextColor) {
        mutableStateOf(Color(settings.customThemeTextColor.toULong()))
    }
    var customIsDark by remember(settings.customThemeIsDark) {
        mutableStateOf(settings.customThemeIsDark)
    }

    // Wallpaper and Online Themes State
    val onlineThemes = remember { OnlineThemeRepository.onlineThemes }
    var selectedCategory by remember { mutableStateOf("All") }
    val wallpaperCategories = listOf("All", "Sri Lanka", "Cyberpunk", "Space & Nature", "Minimal")
    var customUrlInput by remember { mutableStateOf("") }
    var downloadingThemeId by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = OnlineThemeRepository.saveUriAsWallpaper(context, uri)
                result.onSuccess { localPath ->
                    prefs.updateCustomWallpaper(localPath)
                    Toast.makeText(context, "Gallery photo applied as keyboard background!", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Failed to load photo: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val filteredOnlineThemes = remember(selectedCategory) {
        if (selectedCategory == "All") onlineThemes
        else onlineThemes.filter { it.category == selectedCategory }
    }

    val activeTheme = prefs.getActiveTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("themes_screen")
    ) {
        // --- TOP NAVIGATION TABS (Light / Dark / Custom Color / Wallpapers) ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth().testTag("theme_mode_tabs")
        ) {
            ThemeTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    modifier = Modifier.testTag("theme_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- HEADER & LIVE KEYBOARD PREVIEW ---
            item {
                ActiveThemePreviewCard(
                    activeTheme = activeTheme,
                    settings = settings,
                    previewText = previewText,
                    onTextChange = { previewText = it },
                    showTuningPanel = showTuningPanel,
                    onToggleTuningPanel = { showTuningPanel = !showTuningPanel },
                    onResetWallpaper = {
                        prefs.updateCustomWallpaper(null)
                        Toast.makeText(context, "Solid theme background restored", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateRadius = { prefs.updateKeyCornerRadius(it) },
                    onUpdateKeyBgAlpha = { prefs.updateKeyBackgroundAlpha(it) },
                    onUpdateBorders = { prefs.updateShowKeyBorders(it) },
                    onUpdateBorderAlpha = { prefs.updateKeyBorderAlpha(it) },
                    onUpdateHeightScale = { prefs.updateHeightScale(it) }
                )
            }

            // --- TAB CONTENT BASED ON SELECTION ---
            when (selectedTab) {
                ThemeTab.LIGHT -> {
                    // Section Header
                    item {
                        ThemeSectionHeader(
                            title = "Curated Light Themes",
                            subtitle = "Clean, high-visibility light palettes optimized for bright environments and crisp readability."
                        )
                    }

                    // Light Themes List
                    items(KeyboardTheme.LightThemes, key = { it.id }) { theme ->
                        ThemePresetCard(
                            theme = theme,
                            isSelected = settings.themeId == theme.id && settings.customWallpaperPath == null,
                            onSelect = {
                                prefs.updateThemeId(theme.id)
                                prefs.updateCustomWallpaper(null)
                                Toast.makeText(context, "${theme.name} theme applied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                ThemeTab.DARK -> {
                    // Section Header
                    item {
                        ThemeSectionHeader(
                            title = "Curated Dark & AMOLED Themes",
                            subtitle = "Deep dark, AMOLED true black, and neon-accented glass palettes designed to save battery and reduce eye strain."
                        )
                    }

                    // Dark Themes List
                    items(KeyboardTheme.DarkThemes, key = { it.id }) { theme ->
                        ThemePresetCard(
                            theme = theme,
                            isSelected = settings.themeId == theme.id && settings.customWallpaperPath == null,
                            onSelect = {
                                prefs.updateThemeId(theme.id)
                                prefs.updateCustomWallpaper(null)
                                Toast.makeText(context, "${theme.name} theme applied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                ThemeTab.CUSTOM -> {
                    // Section Header
                    item {
                        ThemeSectionHeader(
                            title = "Custom Color Themes & Studio",
                            subtitle = "Pick vibrant custom color palettes or create and fine-tune your personalized keyboard color theme."
                        )
                    }

                    // Custom Theme Studio / Builder Card
                    item {
                        CustomThemeStudioCard(
                            customBgColor = customBgColor,
                            customKeyBgColor = customKeyBgColor,
                            customAccentColor = customAccentColor,
                            customTextColor = customTextColor,
                            customIsDark = customIsDark,
                            isCustomThemeActive = settings.themeId == "custom_user_theme" && settings.customWallpaperPath == null,
                            onBgChange = {
                                customBgColor = it
                                prefs.updateCustomTheme(it, customKeyBgColor, customAccentColor, customTextColor, customIsDark)
                            },
                            onKeyBgChange = {
                                customKeyBgColor = it
                                prefs.updateCustomTheme(customBgColor, it, customAccentColor, customTextColor, customIsDark)
                            },
                            onAccentChange = {
                                customAccentColor = it
                                prefs.updateCustomTheme(customBgColor, customKeyBgColor, it, customTextColor, customIsDark)
                            },
                            onTextColorChange = {
                                customTextColor = it
                                prefs.updateCustomTheme(customBgColor, customKeyBgColor, customAccentColor, it, customIsDark)
                            },
                            onIsDarkChange = {
                                customIsDark = it
                                prefs.updateCustomTheme(customBgColor, customKeyBgColor, customAccentColor, customTextColor, it)
                            },
                            onApply = {
                                prefs.updateCustomTheme(customBgColor, customKeyBgColor, customAccentColor, customTextColor, customIsDark)
                                prefs.updateCustomWallpaper(null)
                                Toast.makeText(context, "Custom theme saved & applied!", Toast.LENGTH_SHORT).show()
                            },
                            onReset = {
                                customBgColor = Color(0xFF111827)
                                customKeyBgColor = Color(0xFF1F2937)
                                customAccentColor = Color(0xFF38BDF8)
                                customTextColor = Color(0xFFF9FAFB)
                                customIsDark = true
                                prefs.updateCustomTheme(customBgColor, customKeyBgColor, customAccentColor, customTextColor, customIsDark)
                                Toast.makeText(context, "Reset custom theme to defaults", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Curated Custom Palette Section
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Curated Vivid Palettes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Special vibrant color harmonies and dual-tone themes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    items(KeyboardTheme.CustomColorThemes, key = { it.id }) { theme ->
                        ThemePresetCard(
                            theme = theme,
                            isSelected = settings.themeId == theme.id && settings.customWallpaperPath == null,
                            onSelect = {
                                prefs.updateThemeId(theme.id)
                                prefs.updateCustomWallpaper(null)
                                Toast.makeText(context, "${theme.name} theme applied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                ThemeTab.WALLPAPERS -> {
                    // Wallpaper Content
                    item {
                        ThemeSectionHeader(
                            title = "Wallpapers & Photo Backgrounds",
                            subtitle = "Apply personal gallery photos or curated high-definition Sri Lankan nature scenes behind your keyboard."
                        )
                    }

                    // Active Wallpaper Control & Dimness Slider
                    if (!settings.customWallpaperPath.isNullOrBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().testTag("active_wallpaper_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Active Wallpaper Background",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                prefs.updateCustomWallpaper(null)
                                                Toast.makeText(context, "Wallpaper removed", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("remove_wallpaper_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove", fontSize = 12.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Background Dim / Darkness",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${(settings.wallpaperDim * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = settings.wallpaperDim,
                                        onValueChange = { prefs.updateWallpaperDim(it) },
                                        valueRange = 0.05f..0.85f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("wallpaper_dim_slider")
                                    )
                                }
                            }
                        }
                    }

                    // Pick from Device Gallery Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable { galleryLauncher.launch("image/*") }
                                .testTag("pick_gallery_image_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Pick Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Choose Photo from Gallery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Select any personal photo or saved picture on your phone.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Custom URL Image Downloader Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().testTag("custom_url_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Add Wallpaper from Web Image URL",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                OutlinedTextField(
                                    value = customUrlInput,
                                    onValueChange = { customUrlInput = it },
                                    placeholder = { Text("https://images.unsplash.com/... or direct image link", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_image_url_input")
                                )

                                Button(
                                    onClick = {
                                        if (customUrlInput.isNotBlank()) {
                                            downloadingThemeId = "custom_url"
                                            scope.launch {
                                                val result = OnlineThemeRepository.downloadAndSaveWallpaper(
                                                    context = context,
                                                    imageUrl = customUrlInput.trim(),
                                                    themeId = "custom_${System.currentTimeMillis()}"
                                                )
                                                downloadingThemeId = null
                                                result.onSuccess { localPath ->
                                                    prefs.updateCustomWallpaper(localPath)
                                                    Toast.makeText(context, "Wallpaper downloaded & applied!", Toast.LENGTH_SHORT).show()
                                                    customUrlInput = ""
                                                }.onFailure { error ->
                                                    Toast.makeText(context, "Download error: ${error.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = customUrlInput.isNotBlank() && downloadingThemeId != "custom_url",
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("download_custom_url_btn")
                                ) {
                                    if (downloadingThemeId == "custom_url") {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Downloading...")
                                    } else {
                                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download & Apply Wallpaper")
                                    }
                                }
                            }
                        }
                    }

                    // Category Filter Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wallpaperCategories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Online Theme Gallery Items
                    items(filteredOnlineThemes, key = { it.id }) { themeItem ->
                        val isDownloading = downloadingThemeId == themeItem.id

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .testTag("online_theme_${themeItem.id}")
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    AsyncImage(
                                        model = themeItem.previewUrl,
                                        contentDescription = themeItem.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xCC000000))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = themeItem.category,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(themeItem.accentColorHex).copy(alpha = 0.9f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "HD Theme",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = themeItem.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = themeItem.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Button(
                                        onClick = {
                                            downloadingThemeId = themeItem.id
                                            scope.launch {
                                                val result = OnlineThemeRepository.downloadAndSaveWallpaper(
                                                    context = context,
                                                    imageUrl = themeItem.downloadUrl,
                                                    themeId = themeItem.id
                                                )
                                                downloadingThemeId = null
                                                result.onSuccess { localPath ->
                                                    prefs.updateCustomWallpaper(localPath)
                                                    Toast.makeText(context, "${themeItem.name} applied as keyboard background!", Toast.LENGTH_SHORT).show()
                                                }.onFailure { error ->
                                                    Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !isDownloading,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(themeItem.accentColorHex)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("apply_online_theme_${themeItem.id}")
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Applying...", fontSize = 13.sp)
                                        } else {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Apply as Keyboard Background", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun ThemeSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ThemePresetCard(
    theme: KeyboardTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x22FFFFFF),
        label = "border_color"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("theme_item_${theme.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Visual Swatch Preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.background)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.keyBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = theme.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = when (theme.category) {
                        ThemeCategory.LIGHT -> "Light Mode • High Contrast"
                        ThemeCategory.DARK -> "Dark Mode • Eye Safe"
                        ThemeCategory.CUSTOM -> "Custom Theme Palette"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveThemePreviewCard(
    activeTheme: KeyboardTheme,
    settings: com.slashboard.keyboard.data.repository.KeyboardSettings,
    previewText: String,
    onTextChange: (String) -> Unit,
    showTuningPanel: Boolean,
    onToggleTuningPanel: () -> Unit,
    onResetWallpaper: () -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onUpdateKeyBgAlpha: (Float) -> Unit,
    onUpdateBorders: (Boolean) -> Unit,
    onUpdateBorderAlpha: (Float) -> Unit,
    onUpdateHeightScale: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("active_theme_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(activeTheme.accentColor)
                    )
                    Column {
                        Text(
                            text = "Active: ${activeTheme.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.customWallpaperPath != null) "Photo wallpaper overlay"
                            else if (activeTheme.isDark) "Dark Theme Active" else "Light Theme Active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (settings.customWallpaperPath != null) {
                        OutlinedButton(
                            onClick = onResetWallpaper,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Use Solid Color", fontSize = 10.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleTuningPanel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("toggle_tuning_panel_btn")
                    ) {
                        Icon(
                            imageVector = if (showTuningPanel) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showTuningPanel) "Hide Sliders" else "Tune Style", fontSize = 11.sp)
                    }
                }
            }

            // Expandable Tuning Panel (Corner radius, opacity, borders, height scale)
            AnimatedVisibility(visible = showTuningPanel) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    // Key Corner Rounding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Key Corner Rounding", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("${settings.keyCornerRadius} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = settings.keyCornerRadius.toFloat(),
                        onValueChange = { onUpdateRadius(it.toInt()) },
                        valueRange = 0f..20f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("key_radius_slider")
                    )

                    // Key Background Opacity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Key Background Opacity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("${(settings.keyBackgroundAlpha * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = settings.keyBackgroundAlpha,
                        onValueChange = onUpdateKeyBgAlpha,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("theme_key_bg_opacity_slider")
                    )

                    // Key Borders Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Key Borders", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = settings.showKeyBorders,
                            onCheckedChange = onUpdateBorders,
                            modifier = Modifier.testTag("theme_key_borders_switch")
                        )
                    }

                    if (settings.showKeyBorders) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Border Opacity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Text("${(settings.keyBorderAlpha * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.keyBorderAlpha,
                            onValueChange = onUpdateBorderAlpha,
                            valueRange = 0.05f..1f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("theme_key_border_opacity_slider")
                        )
                    }

                    // Keyboard Height Scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keyboard Height Scale", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("${(settings.heightScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = settings.heightScale,
                        onValueChange = onUpdateHeightScale,
                        valueRange = 0.70f..1.40f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("theme_height_slider")
                    )
                }
            }

            // Interactive Live Keyboard Preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE KEYBOARD PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (previewText.isEmpty()) "Tap keys to test" else previewText,
                            fontSize = 12.sp,
                            color = if (previewText.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                VirtualKeyboard(
                    settings = settings,
                    currentWord = previewText,
                    fullText = previewText,
                    onInsertText = { text -> onTextChange(previewText + text) },
                    onDeleteCharacter = { if (previewText.isNotEmpty()) onTextChange(previewText.dropLast(1)) },
                    onEnter = { onTextChange(previewText + "\n") },
                    onClearText = { onTextChange("") },
                    onOpenSettings = {}
                )
            }
        }
    }
}

@Composable
private fun CustomThemeStudioCard(
    customBgColor: Color,
    customKeyBgColor: Color,
    customAccentColor: Color,
    customTextColor: Color,
    customIsDark: Boolean,
    isCustomThemeActive: Boolean,
    onBgChange: (Color) -> Unit,
    onKeyBgChange: (Color) -> Unit,
    onAccentChange: (Color) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onIsDarkChange: (Boolean) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCustomThemeActive) 2.dp else 1.dp,
                color = if (isCustomThemeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("custom_theme_studio_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Custom Theme Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Base Mode Segment (Light vs Dark Canvas)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!customIsDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                onIsDarkChange(false)
                                if (customBgColor.toArgb() == Color(0xFF111827).toArgb()) {
                                    onBgChange(Color(0xFFFAF7F2))
                                    onKeyBgChange(Color(0xFFFFFFFF))
                                    onTextColorChange(Color(0xFF1E293B))
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Light", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!customIsDark) Color.White else MaterialTheme.colorScheme.onSurface)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (customIsDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                onIsDarkChange(true)
                                if (customBgColor.toArgb() == Color(0xFFFAF7F2).toArgb()) {
                                    onBgChange(Color(0xFF111827))
                                    onKeyBgChange(Color(0xFF1F2937))
                                    onTextColorChange(Color(0xFFF9FAFB))
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Dark", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (customIsDark) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Quick Starter Palettes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Starter Color Schemes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val starterPalettes = listOf(
                        "Deep Purple" to Triple(Color(0xFF1F1A24), Color(0xFF2E2738), Color(0xFFBB86FC)),
                        "Midnight Blue" to Triple(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77)),
                        "Emerald" to Triple(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF34D399)),
                        "Crimson" to Triple(Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFFF87171)),
                        "Cyberpunk" to Triple(Color(0xFF120E2E), Color(0xFF241442), Color(0xFFF43F5E)),
                        "Matte Black" to Triple(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFFBB86FC)),
                        "Obsidian Amber" to Triple(Color(0xFF12100E), Color(0xFF2E2419), Color(0xFFF59E0B)),
                        "Pure Light Latte" to Triple(Color(0xFFFAF7F2), Color(0xFFFFFFFF), Color(0xFFD97706)),
                        "Ice Light Arctic" to Triple(Color(0xFFF0F9FF), Color(0xFFFFFFFF), Color(0xFF0284C7))
                    )

                    starterPalettes.forEach { (name, colors) ->
                        val (bg, keyBg, accent) = colors
                        val isLightStarter = bg.red > 0.8f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    onIsDarkChange(!isLightStarter)
                                    onBgChange(bg)
                                    onKeyBgChange(keyBg)
                                    onAccentChange(accent)
                                    onTextColorChange(if (isLightStarter) Color(0xFF1E293B) else Color(0xFFF9FAFB))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // 1. Background Color Selector
            ColorSelectorRow(
                title = "Keyboard Background",
                selectedColor = customBgColor,
                onColorSelect = onBgChange,
                options = listOf(
                    Color(0xFF111827), // Dark Gray
                    Color(0xFF000000), // Pure Black
                    Color(0xFF0F0B1E), // Deep Violet
                    Color(0xFF0A1128), // Deep Navy
                    Color(0xFF061A14), // Deep Forest
                    Color(0xFF1C0B18), // Deep Plum
                    Color(0xFF1C0D02), // Dark Rust
                    Color(0xFFFAF7F2), // Cream Latte (Light)
                    Color(0xFFF1F5F9), // Slate White (Light)
                    Color(0xFFFFF1F2), // Rose White (Light)
                    Color(0xFFECFDF5), // Mint White (Light)
                    Color(0xFFF0F9FF)  // Sky White (Light)
                ),
                testTag = "custom_color_bg_picker"
            )

            // 2. Key Surface Color Selector
            ColorSelectorRow(
                title = "Key Surface Background",
                selectedColor = customKeyBgColor,
                onColorSelect = onKeyBgChange,
                options = listOf(
                    Color(0xFF1F2937),
                    Color(0xFF2B2052),
                    Color(0xFF1C315E),
                    Color(0xFF134234),
                    Color(0xFF451952),
                    Color(0xFF4A2307),
                    Color(0xFF334155),
                    Color(0xFFFFFFFF),
                    Color(0xFFE2E8F0),
                    Color(0xFFFFE4E6),
                    Color(0xFFD1FAE5),
                    Color(0xFFE0F2FE)
                ),
                testTag = "custom_color_key_bg_picker"
            )

            // 3. Accent / Indicator Color Selector
            ColorSelectorRow(
                title = "Accent & Highlight Color",
                selectedColor = customAccentColor,
                onColorSelect = onAccentChange,
                options = listOf(
                    Color(0xFF38BDF8), // Sky Cyan
                    Color(0xFFA855F7), // Cyber Purple
                    Color(0xFF34D399), // Emerald
                    Color(0xFFF59E0B), // Amber Gold
                    Color(0xFFF43F5E), // Rose Pink
                    Color(0xFFD946EF), // Neon Magenta
                    Color(0xFFEA580C), // Flame Orange
                    Color(0xFF10B981), // Green Mint
                    Color(0xFF06B6D4), // Vivid Teal
                    Color(0xFF7C3AED), // Royal Violet
                    Color(0xFFEF4444), // Crimson
                    Color(0xFF22C55E)  // Vivid Lime
                ),
                testTag = "custom_color_accent_picker"
            )

            // 4. Text Color Selector
            ColorSelectorRow(
                title = "Key Text & Label Color",
                selectedColor = customTextColor,
                onColorSelect = onTextColorChange,
                options = listOf(
                    Color(0xFFFFFFFF), // Pure White
                    Color(0xFFF9FAFB), // Off White
                    Color(0xFFFEF3C7), // Warm Pale Gold
                    Color(0xFFECFEFF), // Pale Cyan
                    Color(0xFF0F172A), // Charcoal Navy (For Light themes)
                    Color(0xFF292524), // Dark Stone (For Light themes)
                    Color(0xFF064E3B), // Dark Green (For Light themes)
                    Color(0xFF00FF00)  // Hacker Matrix Neon
                ),
                testTag = "custom_color_text_picker"
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("reset_custom_theme_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp)
                }

                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(2f).testTag("apply_custom_theme_btn")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Custom Theme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ColorSelectorRow(
    title: String,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    options: List<Color>,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag(testTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Color Swatch indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Text(
                    text = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb())),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { color ->
                val isSelected = color.toArgb() == selectedColor.toArgb()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114 > 0.6) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
