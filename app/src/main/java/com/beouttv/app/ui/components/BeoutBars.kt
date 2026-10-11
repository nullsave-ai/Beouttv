package com.beouttv.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.beouttv.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.ui.navigation.BeoutDestination
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

/**
 * Compact top bar used on primary screens (Home, Live TV, etc).
 */
@Composable
fun BeoutTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BeoutColors.Background)
            .height(BeoutDimensions.TopBarHeight)
            .padding(horizontal = BeoutDimensions.ScreenPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        onMenuClick?.let {
            BeoutIconButton(
                icon = Icons.Rounded.Menu,
                contentDescription = "القائمة الرئيسية",
                tint = BeoutColors.TextPrimary,
                onClick = it
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = title,
            style = BeoutTextStyles.SectionTitle,
            color = BeoutColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        onSearchClick?.let {
            BeoutIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = S.search(),
                tint = BeoutColors.BrightBlue,
                onClick = it
            )
        }

        onNotificationsClick?.let {
            Spacer(Modifier.width(BeoutDimensions.SpaceXS))
            val unreadCount by com.beouttv.app.data.notification.NotificationsManager.unreadCountFlow.collectAsState()
            Box(contentAlignment = Alignment.Center) {
                BeoutIconButton(
                    icon = if (unreadCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                    contentDescription = S.notifications(),
                    tint = BeoutColors.BrightBlue,
                    onClick = it
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.LiveRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else "$unreadCount",
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                }
            }
        }

        onProfileClick?.let {
            Spacer(Modifier.width(BeoutDimensions.SpaceXS))
            BeoutIconButton(
                icon = Icons.Rounded.AccountCircle,
                contentDescription = S.ghostAccount(),
                tint = BeoutColors.BrightBlue,
                onClick = it
            )
        }
    }
}

/**
 * Matte Obsidian Floating Deck Navigation Bar
 * Adheres strictly to the BeoutTV Matte Obsidian Palette:
 * - Solid Matte Obsidian Dark container (#10141B) with crisp divider border.
 * - Solid Matte Card active highlight (#18202C) with subtle crisp border.
 * - Clean solid brand blue (#2196F3) for selected state, muted slate (#8B949E) for unselected.
 * - Zero glowing gradients, 100% full text visibility, and instant iOS-style responsiveness.
 */
@Composable
fun BeoutBottomBar(
    currentDestination: BeoutDestination,
    modifier: Modifier = Modifier,
    onDestinationSelected: (BeoutDestination) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val items = listOf(
        Triple(BeoutDestination.Home, Icons.Rounded.Home, S.home()),
        Triple(BeoutDestination.LiveTv, Icons.Rounded.Tv, S.liveTv()),
        Triple(BeoutDestination.Sports, Icons.Rounded.SportsSoccer, tr("المباريات", "Sports", "Maçlar", "مسابقات", "Sports")),
        Triple(BeoutDestination.More, Icons.Rounded.MoreHoriz, S.more())
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF210141B))
                .border(
                    width = 1.dp,
                    color = Color(0xFF232B38),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (destination, icon, label) ->
                val selected = currentDestination == destination

                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale"
                )

                val itemColor by animateColorAsState(
                    targetValue = if (selected) BeoutColors.BrightBlue else Color(0xFF8B949E),
                    animationSpec = tween(durationMillis = 160, easing = LinearEasing),
                    label = "itemColor"
                )

                val indicatorWidth by animateDpAsState(
                    targetValue = if (selected) 14.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "indicatorWidth"
                )

                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(150),
                    label = "indicatorAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDestinationSelected(destination)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = itemColor,
                            modifier = Modifier
                                .size(21.dp)
                                .scale(iconScale)
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = label,
                            style = BeoutTextStyles.Caption.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = if (selected) 10.5.sp else 10.sp,
                                color = itemColor
                            ),
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(Modifier.height(2.dp))

                        // Modern Sleek Dot / Pill Indicator (Hardware Accelerated)
                        Box(
                            modifier = Modifier
                                .height(3.5.dp)
                                .width(indicatorWidth)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue.copy(alpha = indicatorAlpha))
                        )
                    }
                }
            }
        }
    }
}
