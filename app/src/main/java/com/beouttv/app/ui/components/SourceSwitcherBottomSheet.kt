package com.beouttv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.MediaSource
import com.beouttv.app.data.source.MediaSourceManager
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSwitcherBottomSheet(
    onDismissRequest: () -> Unit
) {
    val sources by MediaSourceManager.sourcesFlow.collectAsState()
    val activeSource by MediaSourceManager.activeSourceFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isCompactLandscape = screenHeight <= 450.dp
    val bottomPad = if (isCompactLandscape) 12.dp else 32.dp
    val listMaxHeight = if (isCompactLandscape) (screenHeight * 0.55f) else 380.dp

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF101721),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = BeoutDimensions.ScreenPadding)
                .padding(bottom = bottomPad)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tr("مصادر الأفلام والمسلسلات", "Content Sources & APIs", "İçerik Kaynakları", "منابع فیلم و سریال", "Sources de contenu"),
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 17.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tr("تغيير المصدر يحدث محتوى الشاشة الرئيسية والمكتبة فورياً", "Switching source immediately updates Home and Library content", "Kaynak değişimi tüm içeriği anında günceller", "تغییر منبع کل محتوا را فوراً به‌روزرسانی می‌کند", "Le changement de source met à jour tout le contenu"),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary
                    )
                }

                // Add Custom Source Button
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Source",
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(BeoutDimensions.SpaceM))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listMaxHeight)
            ) {
                items(sources, key = { it.id }) { source ->
                    val isSelected = source.id == activeSource.id

                    val bg = if (isSelected) Color(0xFF16253B) else BeoutColors.Card
                    val border = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .background(bg)
                            .border(1.dp, border, BeoutShapes.Medium)
                            .clickable {
                                MediaSourceManager.selectSource(source.id)
                                onDismissRequest()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio / Check Indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary, CircleShape)
                                .background(if (isSelected) BeoutColors.BrightBlue else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Source Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.name,
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.2f) else Color(0xFF202E3F))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = source.speedLabel,
                                        style = BeoutTextStyles.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                        color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary
                                    )
                                }
                            }

                            if (source.description.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = source.description,
                                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                    color = BeoutColors.TextSecondary
                                )
                            }
                        }

                        // Delete button for custom sources
                        if (source.isCustom) {
                            IconButton(
                                onClick = { MediaSourceManager.removeCustomSource(source.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = BeoutColors.LiveRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Source Dialog
    if (showAddDialog) {
        AddCustomSourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url, desc ->
                MediaSourceManager.addCustomSource(name, url, desc)
                showAddDialog = false
                onDismissRequest()
            }
        )
    }
}

@Composable
private fun AddCustomSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151F2C),
        title = {
            Text(
                text = tr("إضافة مصدر محتوى جديد", "Add New Content Source", "Yeni İçerik Kaynağı Ekle", "افزودن منبع جدید", "Ajouter une source"),
                style = BeoutTextStyles.SectionTitle,
                color = BeoutColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr("أدخل رابط الـ API الخاص بسيرفر الأفلام والمسلسلات:", "Enter Movie/Series API Endpoint URL:", "API URL adresini girin:", "آدرس API منبع را وارد کنید:", "Entrez l'URL de l'API :"),
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                    color = BeoutColors.TextSecondary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = "" },
                    label = { Text("اسم المصدر (مثال: سيرفر خاص)", color = BeoutColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; errorMsg = "" },
                    label = { Text("رابط الـ API (مثال: https://.../api.php)", color = BeoutColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف (اختياري)", color = BeoutColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = BeoutColors.LiveRed, style = BeoutTextStyles.Caption)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        errorMsg = "يرجى كتابة اسم المصدر ورابط الـ API كاملاً"
                        return@Button
                    }
                    onAdd(name, url, desc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue)
            ) {
                Text("حفظ وتفعيل", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("إلغاء", "Cancel", "İptal", "لغو", "Annuler"), color = BeoutColors.TextSecondary)
            }
        }
    )
}
