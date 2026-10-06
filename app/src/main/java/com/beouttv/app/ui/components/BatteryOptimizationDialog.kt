package com.beouttv.app.ui.components

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.notification.BatteryOptimizationHelper
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isNotifGranted by remember { mutableStateOf(BatteryOptimizationHelper.isNotificationsEnabled(context)) }
    var isBatteryIgnored by remember { mutableStateOf(BatteryOptimizationHelper.isBatteryOptimizationIgnored(context)) }
    var isExactAlarmGranted by remember { mutableStateOf(BatteryOptimizationHelper.isExactAlarmAllowed(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BeoutColors.Card,
        shape = BeoutShapes.Medium,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A5F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = BeoutColors.BrightBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "ضمان وصول التنبيهات في موعدها",
                style = BeoutTextStyles.SectionTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = BeoutColors.TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تقوم بعض واجهات الهواتف (مثل شاومي، سامسونج، هواوي) بتقييد عمل التطبيقات في الخلفية لتوفير البطارية، مما قد يؤخر تنبيهات المباريات. يرجى تفعيل الخيارات التالية لضمان رنين المنبه فوراً:",
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.5.sp),
                    color = BeoutColors.TextSecondary
                )

                Spacer(Modifier.height(14.dp))

                // 1. Notification Permission Status
                OptimizationStatusRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "إذن الإشعارات",
                    subtitle = "لاستلام الإشعارات المباشرة",
                    isGranted = isNotifGranted,
                    onFix = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            (context as? Activity)?.requestPermissions(
                                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                101
                            )
                        } else {
                            BatteryOptimizationHelper.openAppInfoSettings(context)
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                // 2. Battery Optimization Status
                OptimizationStatusRow(
                    icon = Icons.Rounded.Bolt,
                    title = "استثناء توفير البطارية",
                    subtitle = "تشغيل المنبه حتى لو كان التطبيق مغلقاً",
                    isGranted = isBatteryIgnored,
                    onFix = {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                    }
                )

                Spacer(Modifier.height(10.dp))

                // 3. Exact Alarm Status
                OptimizationStatusRow(
                    icon = Icons.Rounded.AlarmOn,
                    title = "المنبهات الدقيقة (Exact Alarm)",
                    subtitle = "التنبيه بالدقيقة والثانية المحددة",
                    isGranted = isExactAlarmGranted,
                    onFix = {
                        BatteryOptimizationHelper.requestExactAlarmPermission(context)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isBatteryIgnored) {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                    } else if (!isExactAlarmGranted) {
                        BatteryOptimizationHelper.requestExactAlarmPermission(context)
                    } else if (!isNotifGranted) {
                        BatteryOptimizationHelper.openAppInfoSettings(context)
                    } else {
                        BatteryOptimizationHelper.openOemAutoStartSettings(context)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                shape = BeoutShapes.Small
            ) {
                Icon(Icons.Rounded.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isBatteryIgnored && isExactAlarmGranted && isNotifGranted) "إعدادات التشغيل التلقائي للمصنّع" else "ضبط وضمان التنبيهات الآن",
                    style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, BeoutColors.Divider),
                shape = BeoutShapes.Small
            ) {
                Text(
                    text = "تم / إغلاق",
                    style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, color = BeoutColors.TextPrimary)
                )
            }
        }
    )
}

@Composable
private fun OptimizationStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Small)
            .background(if (isGranted) Color(0xFF132A1C) else Color(0xFF2A1C13))
            .border(0.5.dp, if (isGranted) Color(0xFF238636) else Color(0xFF9E4A1A), BeoutShapes.Small)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF3FB950) else Color(0xFFF0883E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                    color = BeoutColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                    color = BeoutColors.TextDisabled
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "مفعل",
                    tint = Color(0xFF3FB950),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Surface(
                    onClick = onFix,
                    shape = BeoutShapes.Small,
                    color = Color(0xFFF0883E),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = "تفعيل",
                        style = BeoutTextStyles.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
