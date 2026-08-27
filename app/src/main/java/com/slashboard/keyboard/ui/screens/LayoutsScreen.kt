package com.slashboard.keyboard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.SlashboardApp
import com.slashboard.keyboard.data.model.KeyCode
import com.slashboard.keyboard.data.model.KeyModel
import com.slashboard.keyboard.data.model.KeyboardLayout
import com.slashboard.keyboard.data.model.KeyboardTheme
import com.slashboard.keyboard.data.repository.HelakuruSinglishParser

@Composable
fun LayoutsScreen() {
    val prefs = SlashboardApp.instance.preferencesRepository
    val settings by prefs.settingsFlow.collectAsState()

    val allLayouts = remember { KeyboardLayout.AvailableLayouts }
    val activeTheme = remember(settings.themeId, settings.keyCornerRadius, settings.showKeyBorders) {
        prefs.getActiveTheme()
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var testInputText by remember { mutableStateOf("") }
    var composingBuffer by remember { mutableStateOf("") }
    var lastSelectedLayoutName by remember { mutableStateOf<String?>(null) }

    val categories = listOf("All", "English", "Sinhala")
    val filteredLayouts = remember(selectedCategory, allLayouts) {
        if (selectedCategory == "All") allLayouts
        else allLayouts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val currentActiveLayout = remember(settings.layoutId) {
        prefs.getActiveLayout()
    }
    val isSinglish = currentActiveLayout.id == "sinhala_singlish"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("layouts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Keyboard Layouts",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Switch between English (QWERTY) and Helakuru Singlish (සිංග්ලිෂ්) layouts.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(categories) { category ->
                        val isChipSelected = selectedCategory == category
                        FilterChip(
                            selected = isChipSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("layout_chip_$category")
                        )
                    }
                }
            }
        }

        // Layout Cards
        items(filteredLayouts, key = { it.id }) { layoutItem ->
            val isSelected = layoutItem.id == settings.layoutId

            LayoutSelectionCard(
                layoutItem = layoutItem,
                isSelected = isSelected,
                theme = activeTheme,
                onSelect = {
                    prefs.updateLayoutId(layoutItem.id)
                    lastSelectedLayoutName = layoutItem.name
                    composingBuffer = ""
                    testInputText = ""
                }
            )
        }

        // Interactive Live Tester Sandbox Card inside Layouts Screen
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("layout_tester_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Tester",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live Test (${currentActiveLayout.name})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (testInputText.isNotEmpty() || composingBuffer.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    testInputText = ""
                                    composingBuffer = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Test output box
                    val displayText = if (isSinglish && composingBuffer.isNotEmpty()) {
                        testInputText + HelakuruSinglishParser.parse(composingBuffer)
                    } else {
                        testInputText
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (displayText.isEmpty()) {
                            Text(
                                text = if (isSinglish) "Type Singlish (e.g. mama, oya, sinhala)..." else "Tap keys below to test layout...",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = displayText,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Mini preview of layout keys
                    MiniLayoutKeyPreview(
                        layout = currentActiveLayout,
                        theme = activeTheme,
                        onKeyTap = { keyChar ->
                            if (isSinglish) {
                                composingBuffer += keyChar
                            } else {
                                testInputText += keyChar
                            }
                        },
                        onBackspace = {
                            if (isSinglish && composingBuffer.isNotEmpty()) {
                                composingBuffer = composingBuffer.dropLast(1)
                            } else if (testInputText.isNotEmpty()) {
                                testInputText = testInputText.dropLast(1)
                            }
                        },
                        onSpace = {
                            if (isSinglish && composingBuffer.isNotEmpty()) {
                                testInputText += HelakuruSinglishParser.parse(composingBuffer) + " "
                                composingBuffer = ""
                            } else {
                                testInputText += " "
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutSelectionCard(
    layoutItem: KeyboardLayout,
    isSelected: Boolean,
    theme: KeyboardTheme,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("layout_card_${layoutItem.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = layoutItem.name,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = layoutItem.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = layoutItem.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = layoutItem.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                lineHeight = 16.sp
            )

            // Mini key visualizer for layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.background.copy(alpha = 0.5f))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sampleKeys = layoutItem.rows.firstOrNull()?.take(8) ?: emptyList()
                sampleKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.keyBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key.primary,
                            color = theme.keyTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLayoutKeyPreview(
    layout: KeyboardLayout,
    theme: KeyboardTheme,
    onKeyTap: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.background)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        layout.rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (rowIndex == layout.rows.size - 1) {
                    // Shift Key
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isShifted) theme.accentColor else theme.functionalKeyBackground)
                            .clickable { isShifted = !isShifted },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Shift",
                            tint = if (isShifted) Color.White else theme.keyTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                row.forEach { key ->
                    if (!key.isFunctional) {
                        val displayChar = if (isShifted) key.shifted else key.primary
                        Box(
                            modifier = Modifier
                                .weight(key.weight)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.keyBackground)
                                .clickable { onKeyTap(displayChar) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayChar,
                                color = theme.keyTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (rowIndex == layout.rows.size - 1) {
                    // Backspace Key
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.functionalKeyBackground)
                            .clickable { onBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = theme.keyTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Mini Spacebar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.keyBackground)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Space",
                    color = theme.keyTextColor.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
