package com.beouttv.app.ui.theme.tv

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.beouttv.app.ui.theme.BeoutColors

/**
 * 10-Foot UI Focus Engine for Smart TV / Android TV / Fire TV:
 * Provides immediate visual feedback (Scale, Halo Glow, Border Stroke) when the user
 * navigates with D-Pad arrow keys on a remote control from across the room.
 */
@Composable
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusedScale: Float = 1.06f,
    focusedBorderColor: Color = BeoutColors.BrightBlue,
    focusedBorderWidth: Dp = 2.5.dp,
    unfocusedBorderColor: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 0.dp,
    focusedElevation: Dp = 8.dp,
    onEnter: (() -> Unit)? = null
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "tvFocusScale"
    )

    val currentElevation by animateFloatAsState(
        targetValue = if (isFocused) focusedElevation.value else elevation.value,
        animationSpec = tween(150),
        label = "tvElevation"
    )

    return this
        .scale(scale)
        .zIndex(if (isFocused) 10f else 1f)
        .shadow(currentElevation.dp, shape = shape)
        .border(
            width = if (isFocused) focusedBorderWidth else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .clip(shape)
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (onEnter != null) {
                            onEnter.invoke()
                            true
                        } else false
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        .focusable(interactionSource = interactionSource)
        .then(
            if (onEnter != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onEnter
                )
            } else Modifier
        )
}
