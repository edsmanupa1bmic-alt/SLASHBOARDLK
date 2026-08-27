package com.slashboard.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.KeyboardTheme

/**
 * A horizontal scrollable bar displaying frequently used emojis.
 * Positioned directly above the number row for immediate 1-tap typing.
 */
@Composable
fun FrequentEmojiBar(
    theme: KeyboardTheme,
    emojis: List<String>,
    height: Dp = 36.dp,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("frequent_emoji_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(42.dp)
                        .clip(RoundedCornerShape(theme.keyRadiusDp.dp))
                        .background(theme.keyBackground.copy(alpha = (theme.keyBackground.alpha * 0.75f).coerceIn(0.1f, 1f)))
                        .clickable {
                            onEmojiSelected(emoji)
                        }
                        .testTag("freq_emoji_$emoji"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 19.sp,
                        color = Color.Unspecified
                    )
                }
            }
        }
    }
}
