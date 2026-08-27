package com.slashboard.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.EmojiData
import com.slashboard.keyboard.data.model.KeyboardTheme

@Composable
fun EmojiPickerView(
    theme: KeyboardTheme,
    height: Dp,
    modifier: Modifier = Modifier,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onCloseEmoji: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf("smileys") }
    var searchQuery by remember { mutableStateOf("") }

    val currentEmojis = remember(selectedCategoryId, searchQuery) {
        if (searchQuery.isNotBlank()) {
            EmojiData.search(searchQuery)
        } else {
            EmojiData.Categories.find { it.id == selectedCategoryId }?.emojis ?: emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(theme.background)
    ) {
        // Search & control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(theme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(theme.keyBackground)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Emojis",
                    tint = theme.secondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search emojis...",
                            color = theme.secondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = theme.keyTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(theme.accentColor),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("emoji_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = theme.secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Back to ABC Keyboard button
            IconButton(
                onClick = onCloseEmoji,
                modifier = Modifier.size(36.dp).testTag("emoji_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Back to Keyboard",
                    tint = theme.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Backspace button
            IconButton(
                onClick = onBackspace,
                modifier = Modifier.size(36.dp).testTag("emoji_backspace_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Category Tabs (scrollable row)
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(theme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiData.Categories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) theme.keyPressedBackground else theme.keyBackground)
                            .clickable { selectedCategoryId = category.id }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("emoji_cat_${category.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Emoji Grid
        val isKaomoji = selectedCategoryId == "kaomoji" && searchQuery.isBlank()
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isKaomoji) 3 else 7),
            contentPadding = PaddingValues(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("emoji_grid")
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.keyBackground)
                        .clickable { onEmojiSelected(emoji) }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = if (isKaomoji) 12.sp else 22.sp,
                        color = theme.keyTextColor,
                        fontWeight = if (isKaomoji) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
