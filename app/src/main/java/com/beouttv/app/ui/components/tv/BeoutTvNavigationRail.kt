package com.beouttv.app.ui.components.tv

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.beouttv.app.R
import com.beouttv.app.ui.navigation.BeoutDestination
import com.beouttv.app.ui.navigation.bottomNavDestinations
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.core.tv.DeviceTypeDetector

/**
 * Modern Collapsing / Expanding Navigation Rail for Android TV & Leanback:
 * Provides smooth D-Pad navigation between primary sections from a couch distance.
 * Supports horizontal D-Pad transitions into content and horizontal tab cycling.
 */
@Composable
fun BeoutTvNavigationRail(
    currentDestination: BeoutDestination,
    onNavigateToDestination: (BeoutDestination) -> Unit,
    modifier: Modifier = Modifier,
    selectedTabFocusRequester: FocusRequester? = null,
    onMoveToContent: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var isRailFocused by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) 150.dp else 52.dp,
        animationSpec = tween(durationMillis = 200),
        label = "tvRailWidth"
    )

    Surface(
        modifier = modifier
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
            .wrapContentHeight()
            .width(railWidth)
            .zIndex(100f)
            .clip(RoundedCornerShape(18.dp))
            .onFocusChanged { 
                isRailFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xEB0E1422),
        border = BorderStroke(1.dp, Color(0x333875FF)),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val railDestinations = bottomNavDestinations
            railDestinations.forEachIndexed { index, dest ->
                val isSelected = dest.route == currentDestination.route
                val icon = when (dest) {
                    BeoutDestination.Home -> Icons.Rounded.Home
                    BeoutDestination.LiveTv -> Icons.Rounded.LiveTv
                    BeoutDestination.Sports -> Icons.Rounded.SportsSoccer
                    BeoutDestination.Search -> Icons.Rounded.Search
                    BeoutDestination.More -> Icons.Rounded.Menu
                    else -> Icons.Rounded.Circle
                }
                val label = when (dest) {
                    BeoutDestination.Home -> "الرئيسية"
                    BeoutDestination.LiveTv -> "البث المباشر"
                    BeoutDestination.Sports -> "المباريات"
                    BeoutDestination.Search -> "البحث"
                    BeoutDestination.More -> "المزيد"
                    else -> dest.route
                }

                TvRailItem(
                    icon = icon,
                    label = label,
                    isSelected = isSelected,
                    isExpanded = isRailFocused,
                    focusRequester = if (isSelected) selectedTabFocusRequester else null,
                    onSelect = { onNavigateToDestination(dest) },
                    onMoveToContent = onMoveToContent,
                    onNavigateHorizontal = { forward ->
                        val nextIndex = if (forward) {
                            (index + 1) % railDestinations.size
                        } else {
                            if (index - 1 < 0) railDestinations.size - 1 else index - 1
                        }
                        onNavigateToDestination(railDestinations[nextIndex])
                    },
                    isRtl = isRtl
                )
            }
        }
    }
}

@Composable
private fun TvRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onMoveToContent: () -> Unit,
    onNavigateHorizontal: (Boolean) -> Unit,
    isRtl: Boolean
) {
    var isItemFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isItemFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    val isTowardsContent = if (isRtl) {
                        keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    } else {
                        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    }
                    val isAwayFromContent = if (isRtl) {
                        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    } else {
                        keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    }

                    when {
                        isTowardsContent -> {
                            onMoveToContent()
                            true
                        }
                        isAwayFromContent -> {
                            onNavigateHorizontal(false)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .tvFocusable(
                focusedScale = 1.04f,
                focusedBorderColor = BeoutColors.BrightBlue,
                focusedBorderWidth = 1.5.dp,
                shape = RoundedCornerShape(10.dp),
                onEnter = onSelect
            )
            .background(
                when {
                    isItemFocused -> BeoutColors.BrightBlue.copy(alpha = 0.28f)
                    isSelected -> BeoutColors.BrightBlue.copy(alpha = 0.18f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = if (isExpanded) 10.dp else 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected || isItemFocused) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )

        if (isExpanded) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isSelected || isItemFocused) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = if (isSelected || isItemFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
