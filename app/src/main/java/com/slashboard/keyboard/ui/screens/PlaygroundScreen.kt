package com.slashboard.keyboard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.slashboard.keyboard.R
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.model.KeyboardLayout
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.ui.components.VirtualKeyboard
import com.slashboard.keyboard.util.ImeHelper

@Composable
fun PlaygroundScreen(
    onNavigateToThemes: () -> Unit,
    onNavigateToLayouts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val prefs = SlashboardApp.instance.preferencesRepository
    val db = SlashboardApp.instance.database
    val settings by prefs.settingsFlow.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var textInput by remember { mutableStateOf("") }
    var keystrokeCount by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isImeEnabled by remember { mutableStateOf(ImeHelper.isImeEnabled(context)) }
    var isImeSelected by remember { mutableStateOf(ImeHelper.isImeSelected(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isImeEnabled = ImeHelper.isImeEnabled(context)
                isImeSelected = ImeHelper.isImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isFullyActive = isImeEnabled && isImeSelected
    val activeTheme = remember(settings.themeId, settings.keyCornerRadius, settings.showKeyBorders) {
        prefs.getActiveTheme()
    }
    val activeLayout = remember(settings.layoutId) {
        prefs.getActiveLayout()
    }

    // Extract current typing word for suggestions
    val currentWord = remember(textInput) {
        val trimmed = textInput
        if (trimmed.isEmpty() || trimmed.endsWith(" ")) ""
        else trimmed.substringAfterLast(" ")
    }

    // Calculate approximate Words Per Minute (WPM)
    val wpm = remember(textInput, keystrokeCount, startTime) {
        if (startTime == 0L || textInput.length < 5) 0
        else {
            val minutes = (System.currentTimeMillis() - startTime) / 60000.0
            if (minutes > 0.05) {
                val words = textInput.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                (words / minutes).toInt().coerceAtMost(200)
            } else 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper interactive dashboard
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Banner & Quick Setup Reminder
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("hero_banner_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.keyboard_hero_banner),
                        contentDescription = "Keyboard Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0F0B1E))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "xSlashboardx Sandbox",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeLayout.name} • ${activeTheme.name}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7C3AED))
                                .clickable { onNavigateToSetup() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("setup_wizard_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Setup IME ⚡",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Setup Alert Banner if keyboard is not fully activated
            if (!isFullyActive) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToSetup() }
                        .testTag("playground_setup_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Setup Needed",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup System Keyboard",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (!isImeEnabled) "Step 1: Enable in Settings • Tap to activate"
                                else "Step 2: Select as active input method • Tap to select",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Setup Now",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stats & Mode Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = "$wpm WPM",
                    accent = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    icon = Icons.Default.PlayArrow,
                    label = "Keystrokes",
                    value = "$keystrokeCount keys",
                    accent = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    icon = Icons.Default.Language,
                    label = "Chars",
                    value = "${textInput.length} chars",
                    accent = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
            }

            // Interactive Text Canvas Box
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("typing_canvas_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Typing Canvas",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Copy text
                            IconButton(
                                onClick = {
                                    if (textInput.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(textInput))
                                        db.addClipboardItem(textInput)
                                    }
                                },
                                modifier = Modifier.size(28.dp).testTag("copy_canvas_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Reset text
                            IconButton(
                                onClick = {
                                    textInput = ""
                                    keystrokeCount = 0
                                    startTime = 0L
                                },
                                modifier = Modifier.size(28.dp).testTag("reset_canvas_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Canvas",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        if (textInput.isEmpty()) {
                            Text(
                                text = "Tap the keyboard keys below to test typing, emoji picker, and clipboard gestures...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = textInput,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Quick Toolbar customizers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Customizers",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch Theme shortcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigateToThemes() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("quick_theme_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Themes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Switch Layout shortcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigateToLayouts() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("quick_layout_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Layouts",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Layouts",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Settings shortcut
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigateToSettings() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("quick_settings_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Settings",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Interactive Virtual Keyboard docked at bottom
        VirtualKeyboard(
            settings = settings,
            currentWord = currentWord,
            fullText = textInput,
            onInsertText = { char ->
                if (startTime == 0L) startTime = System.currentTimeMillis()
                keystrokeCount++
                textInput += char
            },
            onDeleteCharacter = {
                if (textInput.isNotEmpty()) {
                    textInput = textInput.dropLast(1)
                    keystrokeCount++
                }
            },
            onReplaceWord = { oldW, newW ->
                if (startTime == 0L) startTime = System.currentTimeMillis()
                keystrokeCount++
                if (textInput.endsWith(oldW)) {
                    textInput = textInput.dropLast(oldW.length) + newW
                } else {
                    textInput += newW
                }
            },
            onEnter = {
                textInput += "\n"
                keystrokeCount++
            },
            onCursorMove = { /* Cursor glide */ },
            onClearText = {
                textInput = ""
            },
            onOpenSettings = onNavigateToSettings
        )
    }
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
