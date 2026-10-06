package com.beouttv.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.xtream.XtreamAccount
import com.beouttv.app.data.xtream.XtreamAccountManager
import com.beouttv.app.data.xtream.XtreamAccountType
import com.beouttv.app.ui.components.BeoutIconButton
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable
import kotlinx.coroutines.launch

@Composable
fun XtreamManagerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val accounts by XtreamAccountManager.accounts.collectAsState()
    val isSyncing by XtreamAccountManager.isSyncing.collectAsState()
    val syncMessage by XtreamAccountManager.syncMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<XtreamAccount?>(null) }
    var accountToEdit by remember { mutableStateOf<XtreamAccount?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isTvMode) 0.85f else 0.95f)
                .fillMaxHeight(if (isTvMode) 0.88f else 0.92f)
                .clip(BeoutShapes.Large)
                .background(BeoutColors.Background)
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), BeoutShapes.Large)
                .padding(if (isTvMode) 20.dp else 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إدارة اشتراكات الاكستريم و M3U",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                            Text(
                                text = "حتى ${XtreamAccountManager.MAX_ACCOUNTS} حسابات شخصية • حفظ وتخزين فائق السرعة",
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }
                    }

                    BeoutIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                        onClick = onDismiss,
                        modifier = Modifier.tvFocusable(shape = CircleShape)
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BeoutColors.Divider, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))

                // Sync status banner
                if (isSyncing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Medium)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BeoutColors.BrightBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (syncMessage.isNotBlank()) syncMessage else "جارٍ مزامنة المحتوى...",
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                color = BeoutColors.TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Account count indicator & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحسابات المضافة (${accounts.size} / ${XtreamAccountManager.MAX_ACCOUNTS})",
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = BeoutColors.TextSecondary
                    )

                    if (accounts.size < XtreamAccountManager.MAX_ACCOUNTS) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BeoutColors.BrightBlue,
                                contentColor = Color.White
                            ),
                            shape = BeoutShapes.Medium,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.tvFocusable(shape = BeoutShapes.Medium)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "إضافة اشتراك جديد",
                                style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Small)
                                .background(Color(0xFFE65100).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFFF9800), BeoutShapes.Small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "الحد الأقصى مستخدم (3/3)",
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // List of accounts
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TvOff,
                                contentDescription = null,
                                tint = BeoutColors.TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لم تقم بإضافة أي اشتراك اكستريم أو M3U بعد",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp),
                                color = BeoutColors.TextSecondary
                            )
                            Text(
                                text = "اضغط على زر 'إضافة اشتراك جديد' لإدخال بيانات خادمك الخاص",
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            XtreamAccountCard(
                                account = acc,
                                onToggle = { XtreamAccountManager.toggleAccount(acc.id) },
                                onEdit = { accountToEdit = acc },
                                onSync = {
                                    coroutineScope.launch {
                                        val res = XtreamAccountManager.syncAccount(acc.id)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "تم تحديث محتوى الحساب بنجاح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشل التحديث: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onDelete = { accountToDelete = acc }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AddXtreamAccountDialog(
            onDismiss = { showAddDialog = false },
            onSuccess = {
                showAddDialog = false
                Toast.makeText(context, "تمت إضافة الاشتراك وتخزين المحتوى بنجاح", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Edit Account Dialog
    if (accountToEdit != null) {
        EditXtreamAccountDialog(
            account = accountToEdit!!,
            onDismiss = { accountToEdit = null },
            onSuccess = {
                accountToEdit = null
                Toast.makeText(context, "تم حفظ تعديلات الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = BeoutColors.Card,
            title = {
                Text(
                    text = "حذف الاشتراك الشخصي",
                    style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الحساب '${accountToDelete?.name}'؟ سيتم حذف جميع القنوات والأفلام والمسلسلات المخزنة الخاصة به فورياً.",
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp),
                    color = BeoutColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.id?.let { XtreamAccountManager.deleteAccount(it) }
                        accountToDelete = null
                        Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = BeoutShapes.Small,
                    modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                ) {
                    Text("نعم، حذف الحساب", color = Color.White, style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { accountToDelete = null },
                    modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                ) {
                    Text("إلغاء", color = BeoutColors.TextSecondary, style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp))
                }
            }
        )
    }
}

@Composable
private fun XtreamAccountCard(
    account: XtreamAccount,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpired = account.isExpired()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(
                1.dp,
                if (isExpired) Color(0xFFE53935) else if (account.isActive) BeoutColors.BrightBlue.copy(alpha = 0.4f) else BeoutColors.Divider,
                BeoutShapes.Medium
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isExpired) Color(0xFFE53935).copy(alpha = 0.2f)
                                else BeoutColors.BrightBlue.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (account.type == XtreamAccountType.XTREAM) Icons.Rounded.Dns else Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = if (isExpired) Color(0xFFEF5350) else BeoutColors.BrightBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.ElevatedSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (account.type == XtreamAccountType.XTREAM) "Xtream Codes" else "M3U",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }
                        Text(
                            text = maskHostUrl(if (account.type == XtreamAccountType.XTREAM) account.serverUrl else account.m3uUrl),
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                            color = BeoutColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Active Switch
                Switch(
                    checked = account.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BeoutColors.BrightBlue,
                        uncheckedThumbColor = BeoutColors.TextSecondary,
                        uncheckedTrackColor = BeoutColors.ElevatedSurface
                    ),
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                )
            }

            // Expiration Banner & Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Expiry Badge
                Box(
                    modifier = Modifier
                        .clip(BeoutShapes.Small)
                        .background(
                            if (isExpired) Color(0xFFB71C1C).copy(alpha = 0.3f)
                            else Color(0xFF1B5E20).copy(alpha = 0.25f)
                        )
                        .border(
                            1.dp,
                            if (isExpired) Color(0xFFEF5350) else Color(0xFF4CAF50),
                            BeoutShapes.Small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isExpired) Icons.Rounded.Warning else Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = if (isExpired) Color(0xFFEF5350) else Color(0xFF81C784),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = account.getFormattedExpiry(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (isExpired) Color(0xFFFF8A80) else Color(0xFFA5D6A7)
                        )
                    }
                }

                // Item counts
                Text(
                    text = "${account.channelsCount} قناة • ${account.moviesCount} فيلم • ${account.seriesCount} مسلسل",
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                    color = BeoutColors.TextSecondary
                )
            }

            // Category visibility tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الأقسام المفعلة:",
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                    color = BeoutColors.TextSecondary
                )
                AccountCategoryBadge("قنوات", account.showChannels)
                AccountCategoryBadge("أفلام", account.showMovies)
                AccountCategoryBadge("مسلسلات", account.showSeries)
            }

            // Action Buttons (Edit / Sync / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = BeoutColors.BrightBlue)
                    Spacer(Modifier.width(4.dp))
                    Text("تعديل", color = BeoutColors.BrightBlue, style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp))
                }

                Spacer(Modifier.width(4.dp))

                TextButton(
                    onClick = onSync,
                    modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = BeoutColors.TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("تحديث المحتوى", color = BeoutColors.TextSecondary, style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp))
                }

                Spacer(Modifier.width(4.dp))

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF5350))
                    Spacer(Modifier.width(4.dp))
                    Text("حذف", color = Color(0xFFEF5350), style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp))
                }
            }
        }
    }
}

@Composable
private fun AccountCategoryBadge(name: String, enabled: Boolean) {
    Box(
        modifier = Modifier
            .clip(BeoutShapes.Small)
            .background(if (enabled) BeoutColors.BrightBlue.copy(alpha = 0.15f) else BeoutColors.ElevatedSurface.copy(alpha = 0.5f))
            .border(
                0.5.dp,
                if (enabled) BeoutColors.BrightBlue.copy(alpha = 0.6f) else BeoutColors.Divider,
                BeoutShapes.Small
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = name,
            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal),
            color = if (enabled) BeoutColors.BrightBlue else BeoutColors.TextSecondary.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SectionToggleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(BeoutShapes.Small)
            .background(if (selected) BeoutColors.BrightBlue.copy(alpha = 0.2f) else BeoutColors.ElevatedSurface)
            .border(
                1.dp,
                if (selected) BeoutColors.BrightBlue else BeoutColors.Divider,
                BeoutShapes.Small
            )
            .clickable { onToggle() }
            .tvFocusable(shape = BeoutShapes.Small, onEnter = onToggle)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                color = if (selected) BeoutColors.TextPrimary else BeoutColors.TextSecondary
            )
        }
    }
}

private fun maskHostUrl(url: String): String {
    if (url.isBlank()) return ""
    return try {
        val scheme = if (url.startsWith("https://")) "https://" else if (url.startsWith("http://")) "http://" else ""
        val withoutScheme = url.removePrefix(scheme)
        val hostPort = withoutScheme.substringBefore('/')
        val path = if (withoutScheme.contains('/')) "/" + withoutScheme.substringAfter('/') else ""
        val host = hostPort.substringBefore(':')
        val port = if (hostPort.contains(':')) ":" + hostPort.substringAfter(':') else ""
        val maskedHost = if (host.length > 4) {
            host.take(2) + "••••••••" + host.takeLast(2)
        } else {
            "••••••••"
        }
        "$scheme$maskedHost$port"
    } catch (_: Exception) {
        "••••••••••••"
    }
}

@Composable
private fun AddXtreamAccountDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Xtream, 1: M3U
    var accountName by remember { mutableStateOf("") }

    // Xtream fields
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // M3U fields
    var m3uUrl by remember { mutableStateOf("") }

    // Category visibility
    var showChannels by remember { mutableStateOf(true) }
    var showMovies by remember { mutableStateOf(true) }
    var showSeries by remember { mutableStateOf(true) }

    var isSubmitting by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isTvMode) 0.8f else 0.95f)
                .clip(BeoutShapes.Large)
                .background(BeoutColors.Card)
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Large)
                .padding(if (isTvMode) 22.dp else 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = "إضافة اشتراك شخصي جديد",
                    style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextPrimary
                )

                // Tabs: Xtream vs M3U
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.ElevatedSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(BeoutShapes.Small)
                            .background(if (selectedTab == 0) BeoutColors.BrightBlue else Color.Transparent)
                            .clickable { if (!isSubmitting) selectedTab = 0 }
                            .tvFocusable(shape = BeoutShapes.Small, onEnter = { if (!isSubmitting) selectedTab = 0 })
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بيانات اكستريم (Xtream)",
                            style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = if (selectedTab == 0) Color.White else BeoutColors.TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(BeoutShapes.Small)
                            .background(if (selectedTab == 1) BeoutColors.BrightBlue else Color.Transparent)
                            .clickable { if (!isSubmitting) selectedTab = 1 }
                            .tvFocusable(shape = BeoutShapes.Small, onEnter = { if (!isSubmitting) selectedTab = 1 })
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "رابط قائمة (M3U URL)",
                            style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = if (selectedTab == 1) Color.White else BeoutColors.TextSecondary
                        )
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("اسم الحساب (اختياري مثلاً: اشتراكي الشخصي)") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedLabelColor = BeoutColors.BrightBlue,
                        unfocusedLabelColor = BeoutColors.TextSecondary
                    )
                )

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("رابط الخادم (مثال: http://line.pro:8080)") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeoutColors.BrightBlue,
                            unfocusedBorderColor = BeoutColors.Divider,
                            focusedLabelColor = BeoutColors.BrightBlue,
                            unfocusedLabelColor = BeoutColors.TextSecondary
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("اسم المستخدم") },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).tvFocusable(shape = BeoutShapes.Small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeoutColors.BrightBlue,
                                unfocusedBorderColor = BeoutColors.Divider,
                                focusedLabelColor = BeoutColors.BrightBlue,
                                unfocusedLabelColor = BeoutColors.TextSecondary
                            )
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).tvFocusable(shape = BeoutShapes.Small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeoutColors.BrightBlue,
                                unfocusedBorderColor = BeoutColors.Divider,
                                focusedLabelColor = BeoutColors.BrightBlue,
                                unfocusedLabelColor = BeoutColors.TextSecondary
                            )
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("رابط ملف M3U / M3U8 المباشر") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeoutColors.BrightBlue,
                            unfocusedBorderColor = BeoutColors.Divider,
                            focusedLabelColor = BeoutColors.BrightBlue,
                            unfocusedLabelColor = BeoutColors.TextSecondary
                        )
                    )
                }

                // Section customization toggles
                Text(
                    text = "تخصيص الأقسام المعروضة من هذا الاشتراك:",
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = BeoutColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionToggleChip(
                        label = "قنوات مباشرة",
                        icon = Icons.Rounded.LiveTv,
                        selected = showChannels,
                        onToggle = { if (!isSubmitting) showChannels = !showChannels },
                        modifier = Modifier.weight(1f)
                    )
                    SectionToggleChip(
                        label = "مكتبة الأفلام",
                        icon = Icons.Rounded.Movie,
                        selected = showMovies,
                        onToggle = { if (!isSubmitting) showMovies = !showMovies },
                        modifier = Modifier.weight(1f)
                    )
                    SectionToggleChip(
                        label = "المسلسلات",
                        icon = Icons.Rounded.Tv,
                        selected = showSeries,
                        onToggle = { if (!isSubmitting) showSeries = !showSeries },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF5350),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp)
                    )
                }

                // Progress status
                if (isSubmitting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BeoutColors.BrightBlue, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = progressStatus,
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                            color = BeoutColors.TextPrimary
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                    ) {
                        Text("إلغاء", color = BeoutColors.TextSecondary)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            if (selectedTab == 0) {
                                if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                                    errorMessage = "يرجى تعبئة كافة الحقول المطلوبة"
                                    return@Button
                                }
                                isSubmitting = true
                                coroutineScope.launch {
                                    val res = XtreamAccountManager.addXtreamAccount(
                                        name = accountName,
                                        serverUrl = serverUrl,
                                        user = username,
                                        pass = password,
                                        showChannels = showChannels,
                                        showMovies = showMovies,
                                        showSeries = showSeries,
                                        onProgress = { progressStatus = it }
                                    )
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        onSuccess()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "حدث خطأ غير متوقع"
                                    }
                                }
                            } else {
                                if (m3uUrl.isBlank()) {
                                    errorMessage = "يرجى إدخال رابط M3U"
                                    return@Button
                                }
                                isSubmitting = true
                                coroutineScope.launch {
                                    val res = XtreamAccountManager.addM3uAccount(
                                        name = accountName,
                                        m3uUrl = m3uUrl,
                                        showChannels = showChannels,
                                        showMovies = showMovies,
                                        showSeries = showSeries,
                                        onProgress = { progressStatus = it }
                                    )
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        onSuccess()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "حدث خطأ غير متوقع"
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BeoutColors.BrightBlue,
                            contentColor = Color.White
                        ),
                        shape = BeoutShapes.Small,
                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                    ) {
                        Text(if (isSubmitting) "جارٍ الحفظ..." else "إضافة الحساب وحفظ المحتوى")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditXtreamAccountDialog(
    account: XtreamAccount,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var accountName by remember { mutableStateOf(account.name) }
    var serverUrl by remember { mutableStateOf(account.serverUrl) }
    var username by remember { mutableStateOf(account.username) }
    var password by remember { mutableStateOf(account.password) }
    var m3uUrl by remember { mutableStateOf(account.m3uUrl) }

    var showChannels by remember { mutableStateOf(account.showChannels) }
    var showMovies by remember { mutableStateOf(account.showMovies) }
    var showSeries by remember { mutableStateOf(account.showSeries) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isTvMode) 0.8f else 0.95f)
                .clip(BeoutShapes.Large)
                .background(BeoutColors.Card)
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Large)
                .padding(if (isTvMode) 22.dp else 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل بيانات الاشتراك",
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = BeoutColors.TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (account.type == XtreamAccountType.XTREAM) "Xtream Codes" else "M3U",
                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue
                        )
                    }
                }

                // Account Name
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("اسم الحساب") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedLabelColor = BeoutColors.BrightBlue,
                        unfocusedLabelColor = BeoutColors.TextSecondary
                    )
                )

                if (account.type == XtreamAccountType.XTREAM) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("رابط الخادم (Server URL)") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeoutColors.BrightBlue,
                            unfocusedBorderColor = BeoutColors.Divider,
                            focusedLabelColor = BeoutColors.BrightBlue,
                            unfocusedLabelColor = BeoutColors.TextSecondary
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("اسم المستخدم") },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).tvFocusable(shape = BeoutShapes.Small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeoutColors.BrightBlue,
                                unfocusedBorderColor = BeoutColors.Divider,
                                focusedLabelColor = BeoutColors.BrightBlue,
                                unfocusedLabelColor = BeoutColors.TextSecondary
                            )
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).tvFocusable(shape = BeoutShapes.Small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BeoutColors.BrightBlue,
                                unfocusedBorderColor = BeoutColors.Divider,
                                focusedLabelColor = BeoutColors.BrightBlue,
                                unfocusedLabelColor = BeoutColors.TextSecondary
                            )
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("رابط قائمة التشغيل M3U") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeoutColors.BrightBlue,
                            unfocusedBorderColor = BeoutColors.Divider,
                            focusedLabelColor = BeoutColors.BrightBlue,
                            unfocusedLabelColor = BeoutColors.TextSecondary
                        )
                    )
                }

                // Section customization toggles
                Text(
                    text = "تخصيص الأقسام المعروضة من هذا الاشتراك:",
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = BeoutColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionToggleChip(
                        label = "قنوات مباشرة",
                        icon = Icons.Rounded.LiveTv,
                        selected = showChannels,
                        onToggle = { if (!isSubmitting) showChannels = !showChannels },
                        modifier = Modifier.weight(1f)
                    )
                    SectionToggleChip(
                        label = "مكتبة الأفلام",
                        icon = Icons.Rounded.Movie,
                        selected = showMovies,
                        onToggle = { if (!isSubmitting) showMovies = !showMovies },
                        modifier = Modifier.weight(1f)
                    )
                    SectionToggleChip(
                        label = "المسلسلات",
                        icon = Icons.Rounded.Tv,
                        selected = showSeries,
                        onToggle = { if (!isSubmitting) showSeries = !showSeries },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF5350),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp)
                    )
                }

                if (isSubmitting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BeoutColors.BrightBlue, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "جارٍ حفظ التعديلات وتحديث المحتوى...",
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                            color = BeoutColors.TextPrimary
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                    ) {
                        Text("إلغاء", color = BeoutColors.TextSecondary)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            val credentialsChanged = if (account.type == XtreamAccountType.XTREAM) {
                                serverUrl.trim() != account.serverUrl || username.trim() != account.username || password.trim() != account.password
                            } else {
                                m3uUrl.trim() != account.m3uUrl
                            }

                            val updatedAccount = account.copy(
                                name = if (accountName.isNotBlank()) accountName.trim() else account.name,
                                serverUrl = serverUrl.trim(),
                                username = username.trim(),
                                password = password.trim(),
                                m3uUrl = m3uUrl.trim(),
                                showChannels = showChannels,
                                showMovies = showMovies,
                                showSeries = showSeries
                            )

                            isSubmitting = true
                            coroutineScope.launch {
                                val res = XtreamAccountManager.updateAccount(updatedAccount, credentialsChanged)
                                isSubmitting = false
                                if (res.isSuccess) {
                                    onSuccess()
                                } else {
                                    errorMessage = res.exceptionOrNull()?.message ?: "حدث خطأ أثناء حفظ التعديلات"
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BeoutColors.BrightBlue,
                            contentColor = Color.White
                        ),
                        shape = BeoutShapes.Small,
                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                    ) {
                        Text(if (isSubmitting) "جارٍ الحفظ..." else "حفظ التعديلات")
                    }
                }
            }
        }
    }
}
