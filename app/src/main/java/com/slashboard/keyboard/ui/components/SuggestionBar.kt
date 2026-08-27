package com.slashboard.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.KeyboardTheme

data class SuggestionItem(
    val display: String,
    val replacement: String,
    val isPrimary: Boolean = false,
    val isShortcut: Boolean = false,
    val isCorrection: Boolean = false
)

@Composable
fun SuggestionBar(
    suggestions: List<SuggestionItem>,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier,
    onSelectSuggestion: (SuggestionItem) -> Unit
) {
    if (suggestions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.surface)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.take(3).forEachIndexed { index, item ->
            val isCenter = item.isPrimary || index == 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCenter) theme.keyPressedBackground else theme.keyBackground)
                    .clickable { onSelectSuggestion(item) }
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .testTag("suggestion_$index"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (item.isShortcut) {
                        Text(
                            text = "⚡ ",
                            fontSize = 11.sp,
                            color = theme.accentColor
                        )
                    } else if (item.isCorrection) {
                        Text(
                            text = "✨ ",
                            fontSize = 11.sp,
                            color = theme.accentColor
                        )
                    }
                    Text(
                        text = item.display,
                        color = if (isCenter) theme.accentColor else theme.keyTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (index < suggestions.size - 1 && index < 2) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}
