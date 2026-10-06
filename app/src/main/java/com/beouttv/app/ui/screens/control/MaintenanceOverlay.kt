package com.beouttv.app.ui.screens.control

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.config.AppGatewayManager
import com.beouttv.app.data.config.GatewayStatus
import com.beouttv.app.data.config.RemoteConfig
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.launch

@Composable
fun MaintenanceOverlay(config: RemoteConfig) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }

    // Enforce app lock: user cannot back-navigate into the app
    BackHandler {
        (context as? android.app.Activity)?.finishAffinity()
    }

    val (title, icon, message, actionText) = when (config.status) {
        GatewayStatus.MAINTENANCE -> Quad(
            "وضع الصيانة والتطوير",
            Icons.Rounded.Build,
            config.maintenanceMessage,
            if (isChecking) "جاري التحقق من السيرفر..." else "إعادة المحاولة"
        )
        GatewayStatus.SUSPENDED -> Quad(
            "الخدمة متوقفة مؤقتاً",
            Icons.Rounded.PauseCircle,
            config.suspendedMessage,
            if (isChecking) "جاري التحقق..." else "تحديث الحالة"
        )
        GatewayStatus.UPDATE_REQUIRED -> Quad(
            "تحديث جديد متاح للتطبيق",
            Icons.Rounded.SystemUpdate,
            config.updateMessage.ifBlank { "يتوفر إصدار جديد من تطبيق Beout TV يتضمن قنوات وسيرفرات جديدة. يرجى التحديث لمتابعة المشاهدة." },
            "تحميل التحديث عبر التلجرام"
        )
        GatewayStatus.NORMAL -> Quad("", Icons.Rounded.Build, "", "")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .padding(BeoutDimensions.ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(BeoutColors.ElevatedSurface, shape = RoundedCornerShape(26.dp))
                    .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.3f), shape = RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BeoutColors.BrightBlue,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(Modifier.height(BeoutDimensions.SpaceL))

            Text(
                text = title,
                style = BeoutTextStyles.ScreenTitle,
                color = BeoutColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            // Version comparison badge if update required
            if (config.status == GatewayStatus.UPDATE_REQUIRED) {
                Spacer(Modifier.height(BeoutDimensions.SpaceS))
                Surface(
                    color = BeoutColors.ElevatedSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إصدارك: ${config.currentAppVersionName}",
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp),
                            color = BeoutColors.TextSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "المطلوب: ${if (config.minVersionName.isNotBlank() && config.minVersionName != "1.0") config.minVersionName else config.latestVersionName}",
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(BeoutDimensions.SpaceM))

            Text(
                text = message,
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 14.sp, lineHeight = 22.sp),
                color = BeoutColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(BeoutDimensions.SpaceXL))

            // Primary Action Button (Telegram / Update)
            Button(
                onClick = {
                    if (config.status == GatewayStatus.UPDATE_REQUIRED) {
                        val targetUrl = config.updateUrl.ifBlank { "https://t.me/beouttvapp" }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        scope.launch {
                            isChecking = true
                            AppGatewayManager.syncRemoteConfig()
                            isChecking = false
                        }
                    }
                },
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (config.status == GatewayStatus.UPDATE_REQUIRED) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = actionText,
                    color = Color.White,
                    style = BeoutTextStyles.Button
                )
            }

            // Secondary Option for Alternate Telegram Channel when update required
            if (config.status == GatewayStatus.UPDATE_REQUIRED) {
                Spacer(Modifier.height(BeoutDimensions.SpaceS))
                OutlinedButton(
                    onClick = {
                        val backupUrl = config.secondaryUpdateUrl.ifBlank { "https://t.me/oneplusnet" }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(backupUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    border = BorderStroke(1.dp, BeoutColors.Divider),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        text = "قناة التحديثات البديلة @oneplusnet",
                        color = BeoutColors.TextSecondary,
                        style = BeoutTextStyles.Button.copy(fontSize = 13.sp)
                    )
                }

                Spacer(Modifier.height(BeoutDimensions.SpaceS))
                TextButton(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            AppGatewayManager.syncRemoteConfig()
                            isChecking = false
                        }
                    },
                    enabled = !isChecking
                ) {
                    Text(
                        text = if (isChecking) "جاري التحقق..." else "تحققت وقمت بالتحديث؟ اضغط هنا لإعادة الفحص",
                        color = BeoutColors.BrightBlue,
                        style = BeoutTextStyles.Caption.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

