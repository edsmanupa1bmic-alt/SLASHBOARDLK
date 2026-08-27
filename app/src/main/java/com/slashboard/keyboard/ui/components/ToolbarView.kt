package com.slashboard.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.KeyboardTheme

/**
 * Compose ToolbarView: 7 Circular Action Chips from Left to Right:
 * 1. En (Language Toggle)
 * 2. Emoji (☺)
 * 3. Settings / Grid (⚙)
 * 4. Aa (Text Styler & Case Selector)
 * 5. Voice Mic (🎤)
 * 6. Clipboard (📋)
 * 7. Collapse (˅)
 */
@Composable
fun ToolbarView(
    theme: KeyboardTheme,
    activeLayoutName: String,
    modifier: Modifier = Modifier,
    onCycleLayout: () -> Unit = {},
    onOpenEmoji: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onTextStyler: () -> Unit = {},
    onVoiceMic: () -> Unit = {},
    onOpenClipboard: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onCursorLeft: () -> Unit = {},
    onCursorRight: () -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    val isEnglish = activeLayoutName.contains("English", ignoreCase = true) || activeLayoutName.contains("Qwerty", ignoreCase = true)
    val langText = if (isEnglish) "En" else "Si"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color.Transparent)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Language Toggle Chip (En / Si)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) { onCycleLayout() }
                .testTag("toolbar_layout"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = langText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 2. Emoji Icon (☺)
        ToolbarActionChip(
            icon = Icons.Default.Mood,
            contentDescription = "Emoji / Sticker Panel",
            testTag = "toolbar_emoji",
            onClick = onOpenEmoji
        )

        // 3. Settings / Grid Icon (⚙)
        ToolbarActionChip(
            icon = Icons.Default.Settings,
            contentDescription = "Settings Hub",
            testTag = "toolbar_settings",
            onClick = onOpenSettings
        )

        // 4. Aa Icon (Text Styler & Case Selector)
        ToolbarActionChip(
            icon = Icons.Default.TextFields,
            contentDescription = "Text Styler & Case Selector",
            testTag = "toolbar_text_styler",
            onClick = onTextStyler
        )

        // 5. Voice Mic Icon (🎤)
        ToolbarActionChip(
            icon = Icons.Default.Mic,
            contentDescription = "Voice Typing",
            testTag = "toolbar_voice_mic",
            onClick = onVoiceMic
        )

        // 6. Clipboard Icon (📋)
        ToolbarActionChip(
            icon = Icons.AutoMirrored.Filled.Assignment,
            contentDescription = "Clipboard History",
            testTag = "toolbar_clipboard",
            onClick = onOpenClipboard
        )

        // 7. Collapse Arrow (˅)
        ToolbarActionChip(
            icon = Icons.Default.KeyboardHide,
            contentDescription = "Collapse Keyboard",
            testTag = "toolbar_collapse",
            onClick = onCollapse
        )
    }
}

@Composable
private fun ToolbarActionChip(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White)
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}
