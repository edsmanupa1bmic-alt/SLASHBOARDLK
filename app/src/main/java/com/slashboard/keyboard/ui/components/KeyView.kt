package com.slashboard.keyboard.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slashboard.keyboard.data.model.KeyCode
import com.slashboard.keyboard.data.model.KeyModel
import com.slashboard.keyboard.data.model.KeyboardTheme
import kotlin.math.abs

enum class ShiftState {
    OFF,
    SHIFT,
    CAPS_LOCK
}

@Composable
fun KeyView(
    key: KeyModel,
    theme: KeyboardTheme,
    shiftState: ShiftState,
    height: Dp,
    showSecondary: Boolean,
    showPopup: Boolean,
    hapticEnabled: Boolean,
    spaceLabel: String = "space",
    modifier: Modifier = Modifier,
    onKeyPress: (KeyModel) -> Unit,
    onLongPress: ((KeyModel) -> Unit)? = null,
    onSpaceSlide: ((Int) -> Unit)? = null
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val currentOnKeyPress by rememberUpdatedState(onKeyPress)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnSpaceSlide by rememberUpdatedState(onSpaceSlide)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "key_scale"
    )

    val labelText = when {
        key.code == KeyCode.CHARACTER -> {
            if (shiftState != ShiftState.OFF) key.shifted else key.primary
        }
        else -> key.primary
    }

    val keyBg = when {
        isPressed -> theme.keyPressedBackground
        key.isFunctional -> theme.functionalKeyBackground
        else -> theme.keyBackground
    }

    val shape = RoundedCornerShape(theme.keyRadiusDp.dp)

    val borderModifier = if (theme.keyBorderColor != Color.Transparent && theme.keyBorderColor.alpha > 0.01f) {
        Modifier.border(width = 1.3.dp, color = theme.keyBorderColor, shape = shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(height)
            .padding(horizontal = 2.dp, vertical = 2.5.dp)
            .scale(scale)
            .clip(shape)
            .background(keyBg)
            .then(borderModifier)
            .then(
                if (key.code == KeyCode.SPACE) {
                    // Custom robust gesture detector for spacebar: handles instant taps AND drag slides
                    Modifier.pointerInput(key, hapticEnabled) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            if (hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            var totalDragX = 0f
                            var hasSlid = false
                            val stepThreshold = 35f // px threshold for cursor step

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    // Pointer released (UP)
                                    change.consume()
                                    isPressed = false
                                    if (!hasSlid) {
                                        // Tap confirmed: trigger space press immediately!
                                        currentOnKeyPress(key)
                                    }
                                    break
                                } else {
                                    // Pointer moving
                                    val deltaX = change.position.x - change.previousPosition.x
                                    totalDragX += deltaX
                                    if (abs(totalDragX) > 20f) {
                                        hasSlid = true
                                    }

                                    if (totalDragX > stepThreshold) {
                                        change.consume()
                                        currentOnSpaceSlide?.invoke(1)
                                        totalDragX = 0f
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                    } else if (totalDragX < -stepThreshold) {
                                        change.consume()
                                        currentOnSpaceSlide?.invoke(-1)
                                        totalDragX = 0f
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                    }
                                }
                            }
                            isPressed = false
                        }
                    }
                } else {
                    Modifier.pointerInput(key, shiftState, hapticEnabled) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = {
                                currentOnKeyPress(key)
                            },
                            onLongPress = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                if (currentOnLongPress != null) {
                                    currentOnLongPress?.invoke(key)
                                } else if (key.secondary.isNotEmpty()) {
                                    currentOnKeyPress(key.copy(primary = key.secondary))
                                }
                            }
                        )
                    }
                }
            )
            .testTag("key_${key.primary}"),
        contentAlignment = Alignment.Center
    ) {
        // Popup Preview on press
        if (isPressed && showPopup && key.code == KeyCode.CHARACTER) {
            KeyPopupPreview(
                text = labelText,
                theme = theme,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Secondary character label in top-right
        if (showSecondary && key.secondary.isNotEmpty() && !key.isFunctional) {
            Text(
                text = key.secondary,
                color = theme.secondaryTextColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 3.dp)
            )
        }

        // Main key content
        when (key.code) {
            KeyCode.SHIFT -> {
                val shiftIconColor = when (shiftState) {
                    ShiftState.CAPS_LOCK -> theme.accentColor
                    ShiftState.SHIFT -> theme.accentColor
                    ShiftState.OFF -> theme.keyTextColor
                }
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Shift",
                    tint = shiftIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.BACKSPACE -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.ENTER -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = theme.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.EMOJI -> {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = "Emoji",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.CLIPBOARD -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = "Clipboard",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.SETTINGS -> {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyCode.CURSOR_LEFT -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Left Arrow",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            KeyCode.CURSOR_RIGHT -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Right Arrow",
                    tint = theme.keyTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            KeyCode.CLEAR_ALL -> {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = theme.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            KeyCode.SPACE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = spaceLabel,
                        color = theme.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            else -> {
                Text(
                    text = labelText,
                    color = if (key.isFunctional) theme.accentColor else theme.keyTextColor,
                    fontSize = if (labelText.length > 2) 13.sp else 18.sp,
                    fontWeight = if (key.isFunctional) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun KeyPopupPreview(
    text: String,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset { IntOffset(0, -110) }
            .size(50.dp, 56.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surface)
            .border(1.5.dp, theme.accentColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = theme.keyTextColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
