package com.beouttv.app.ui.screens.notifications

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beouttv.app.data.notification.AppNotificationItem
import com.beouttv.app.data.notification.NotificationsManager
import com.beouttv.app.ui.components.BeoutIconButton
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by NotificationsManager.notificationsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        NotificationsManager.markAllAsRead()
    }

    val matchReminders = notifications.filter { it.type == "MATCH_REMINDER" }
    val broadcasts = notifications.filter { it.type == "BROADCAST" && !it.isAd }
    val ads = notifications.filter { it.type == "AD" || it.isAd }

    val filteredList = when (selectedFilter) {
        "MATCHES" -> matchReminders
        "BROADCASTS" -> broadcasts
        "ADS" -> ads
        else -> notifications
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = BeoutColors.Card,
            shape = BeoutShapes.Medium,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = BeoutColors.LiveRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "مسح جميع التنبيهات",
                    style = BeoutTextStyles.SectionTitle.copy(fontSize = 16.sp),
                    color = BeoutColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من مسح جميع الإشعارات السابقة وإلغاء المنبهات المجدولة للمباريات؟",
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp),
                    color = BeoutColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationsManager.clearAll(context)
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.LiveRed),
                    shape = BeoutShapes.Small
                ) {
                    Text(
                        text = "تأكيد المسح",
                        style = BeoutTextStyles.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    border = BorderStroke(1.dp, BeoutColors.Divider),
                    shape = BeoutShapes.Small
                ) {
                    Text(
                        text = "إلغاء",
                        style = BeoutTextStyles.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            color = BeoutColors.TextPrimary
                        )
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BeoutColors.Background)
                .height(BeoutDimensions.TopBarHeight)
                .padding(horizontal = BeoutDimensions.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeoutIconButton(
                icon = Icons.Rounded.ArrowForward,
                contentDescription = "رجوع",
                onClick = onBack
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "التنبيهات والإشعارات",
                style = BeoutTextStyles.SectionTitle,
                color = BeoutColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            if (notifications.isNotEmpty()) {
                Surface(
                    onClick = { showClearDialog = true },
                    shape = BeoutShapes.Small,
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "مسح الكل",
                            tint = BeoutColors.LiveRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "مسح الكل",
                            style = BeoutTextStyles.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = BeoutColors.LiveRed
                        )
                    }
                }
            }
        }

        // Sub Filter Tabs with Vector Icons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters: List<Triple<String, String, ImageVector>> = listOf(
                Triple("ALL", "الكل (${notifications.size})", Icons.Rounded.ListAlt),
                Triple("MATCHES", "تذكير المباريات (${matchReminders.size})", Icons.Rounded.SportsSoccer),
                Triple("BROADCASTS", "إشعارات البث (${broadcasts.size})", Icons.Rounded.LiveTv),
                Triple("ADS", "العروض والإعلانات (${ads.size})", Icons.Rounded.Campaign)
            )
            items(filters) { (key, label, icon) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(BeoutShapes.Small)
                        .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                        .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else BeoutColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = BeoutTextStyles.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) Color.White else BeoutColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.Card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = BeoutColors.TextDisabled,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "لا توجد تنبيهات حالياً",
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 16.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "عند ضبط تذكير لمباراة قادمة أو وصول إشعار بث مباشر من السيرفر، ستظهر هنا فوراً.",
                        style = BeoutTextStyles.Caption,
                        color = BeoutColors.TextDisabled,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = BeoutDimensions.ScreenPadding,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onDelete = {
                            if (item.matchId != null) {
                                NotificationsManager.cancelMatchReminder(context, item.matchId)
                            } else {
                                NotificationsManager.deleteNotification(item.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: AppNotificationItem,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isMatch = item.type == "MATCH_REMINDER"
    val isAd = item.type == "AD" || item.isAd
    val timeFormatted = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd/MM • HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val cardBorder = when {
        isAd -> BorderStroke(1.dp, Color(0xFFE5A93C).copy(alpha = 0.5f))
        isMatch -> BorderStroke(1.dp, BeoutColors.Divider)
        else -> BorderStroke(1.dp, BeoutColors.Divider)
    }

    val cardBg = when {
        isAd -> Color(0xFF131A24)
        else -> BeoutColors.Card
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .border(cardBorder.width, cardBorder.brush, BeoutShapes.Medium)
            .background(cardBg)
            .clickable(enabled = !item.actionUrl.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.actionUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Type Badge + Time + Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Badge Type with Vector Icon
                val badgeBg = when {
                    isAd -> Color(0xFF2E220D)
                    isMatch -> Color(0xFF1E293B)
                    else -> Color(0xFF1E3A5F)
                }
                val badgeBorder = when {
                    isAd -> Color(0xFFE5A93C).copy(alpha = 0.6f)
                    isMatch -> Color(0xFF334155)
                    else -> Color(0xFF2563EB)
                }
                val badgeTint = when {
                    isAd -> Color(0xFFE5A93C)
                    isMatch -> Color(0xFF94A3B8)
                    else -> Color(0xFF60A5FA)
                }
                val badgeIcon = when {
                    isAd -> Icons.Rounded.Campaign
                    isMatch -> Icons.Rounded.SportsSoccer
                    else -> Icons.Rounded.LiveTv
                }
                val badgeText = when {
                    isAd -> "إعلان ترويجي"
                    isMatch -> "تذكير مباراة"
                    else -> "إشعار بث مباشر"
                }

                Box(
                    modifier = Modifier
                        .clip(BeoutShapes.Small)
                        .background(badgeBg)
                        .border(0.5.dp, badgeBorder, BeoutShapes.Small)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = badgeText,
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeTint
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = timeFormatted,
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "حذف",
                        tint = BeoutColors.TextDisabled,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Optional Image Banner
            if (!item.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(BeoutShapes.Small)
                        .border(0.5.dp, BeoutColors.Divider, BeoutShapes.Small)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                style = BeoutTextStyles.CardTitle.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isAd) Color(0xFFFDE68A) else BeoutColors.TextPrimary
            )

            Spacer(Modifier.height(4.dp))

            // Message / Details
            Text(
                text = item.message,
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = BeoutColors.TextSecondary
            )

            if (isMatch && item.matchCompetition != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = BeoutColors.TextDisabled,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.matchCompetition,
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextDisabled
                    )
                }
            }

            // Action URL Button
            if (!item.actionUrl.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                val btnBg = if (isAd) Color(0xFFE5A93C) else BeoutColors.BrightBlue
                val btnTextCol = if (isAd) Color(0xFF0F141C) else Color.White
                val btnLabel = item.actionTitle ?: if (isAd) "عرض الإعلان / زيارة الرابط" else "فتح الرابط"

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.actionUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                    shape = BeoutShapes.Small,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = btnTextCol,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = btnLabel,
                        style = BeoutTextStyles.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = btnTextCol
                    )
                }
            }
        }
    }
}
