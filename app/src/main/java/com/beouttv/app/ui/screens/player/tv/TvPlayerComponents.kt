package com.beouttv.app.ui.screens.player.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.beouttv.app.data.model.Channel
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable

/**
 * TV Remote Color Keys Shortcut Legend:
 * Displays clear visual guide for smart TV remote buttons.
 */
@Composable
fun TvRemoteShortcutsLegend(
    isMultiViewActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BeoutColors.Overlay,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BeoutColors.Divider),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvKeyBadge(keyLabel = "OK", actionLabel = "تشغيل / إيقاف")
            TvKeyBadge(keyLabel = "◀ ▶", actionLabel = "تقديم / تأخير 10ث")
            TvKeyBadge(keyLabel = "▲ ▼", actionLabel = "دليل القنوات")
            TvKeyBadge(keyLabel = "الأصفر / F3", actionLabel = "الدقة والجودة")
            TvKeyBadge(keyLabel = "الأزرق / F4", actionLabel = "الصوت والترجمة")

            if (isMultiViewActive) {
                TvKeyBadge(keyLabel = "الأخضر / +", actionLabel = "إضافة قناة")
                TvKeyBadge(keyLabel = "1..4", actionLabel = "تحديد نافذة")
                TvKeyBadge(keyLabel = "رجوع", actionLabel = "إغلاق نافذة")
            } else {
                TvKeyBadge(keyLabel = "رجوع", actionLabel = "خروج")
            }
        }
    }
}

@Composable
private fun TvKeyBadge(keyLabel: String, actionLabel: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(BeoutColors.ElevatedSurface)
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = keyLabel,
                color = BeoutColors.BrightBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = actionLabel,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * TV Quick Channel Drawer:
 * Horizontal channel switcher overlay activated via D-Pad UP/DOWN on smart TV remote.
 * Allows effortless channel flipping without interrupting current playback.
 */
@Composable
fun TvQuickChannelDrawer(
    channels: List<Channel>,
    currentChannelId: String,
    onSelectChannel: (Channel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to currently playing channel
    LaunchedEffect(currentChannelId) {
        val idx = channels.indexOfFirst { it.id == currentChannelId }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xF0070B12), Color(0xFF070B12))
                )
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.LiveTv,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "دليل القنوات السريع • تنقل بالأسهم واختر بـ OK",
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = "${channels.size} قناة متاحة",
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                    color = BeoutColors.TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { _, channel ->
                    val isPlaying = channel.id == currentChannelId

                    Surface(
                        modifier = Modifier
                            .width(130.dp)
                            .height(84.dp)
                            .tvFocusable(
                                focusedScale = 1.08f,
                                focusedBorderColor = BeoutColors.BrightBlue,
                                focusedBorderWidth = 2.dp,
                                shape = RoundedCornerShape(10.dp),
                                onEnter = { onSelectChannel(channel) }
                            ),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPlaying) BeoutColors.BrightBlue.copy(alpha = 0.25f) else Color(0xFF131A26),
                        border = BorderStroke(
                            1.dp,
                            if (isPlaying) BeoutColors.BrightBlue else Color(0x22FFFFFF)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (channel.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = channel.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            Text(
                                text = channel.name,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = if (isPlaying) BeoutColors.BrightBlue else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
