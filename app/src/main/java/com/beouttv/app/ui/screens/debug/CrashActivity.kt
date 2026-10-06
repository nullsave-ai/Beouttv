package com.beouttv.app.ui.screens.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.MainActivity
import com.beouttv.app.data.debug.GlobalCrashHandler

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorMessage = intent.getStringExtra(GlobalCrashHandler.EXTRA_ERROR_MESSAGE) ?: "حدث تعطل غير متوقع في النظام"
        val stackTrace = intent.getStringExtra(GlobalCrashHandler.EXTRA_STACK_TRACE) ?: "لا يوجد سجل أخطاء متاح"
        val deviceInfo = intent.getStringExtra(GlobalCrashHandler.EXTRA_DEVICE_INFO) ?: ""
        val timestamp = intent.getStringExtra(GlobalCrashHandler.EXTRA_TIMESTAMP) ?: ""

        setContent {
            CrashScreen(
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                deviceInfo = deviceInfo,
                timestamp = timestamp,
                onCopy = { fullReport ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("BEOUT_TV_CRASH_LOG", fullReport)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "تم نسخ تقرير الخطأ بالكامل إلى الحافظة", Toast.LENGTH_LONG).show()
                },
                onRestart = {
                    val restartIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(restartIntent)
                    finish()
                },
                onClearDataAndRestart = {
                    try {
                        cacheDir?.deleteRecursively()
                        filesDir?.deleteRecursively()
                    } catch (_: Exception) {}
                    val restartIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(restartIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun CrashScreen(
    errorMessage: String,
    stackTrace: String,
    deviceInfo: String,
    timestamp: String,
    onCopy: (String) -> Unit,
    onRestart: () -> Unit,
    onClearDataAndRestart: () -> Unit
) {
    val fullReport = buildString {
        appendLine("=========================================")
        appendLine("       BEOUT TV - CRASH DEBUG REPORT     ")
        appendLine("=========================================")
        appendLine(deviceInfo)
        appendLine("-----------------------------------------")
        appendLine("Error Message:")
        appendLine(errorMessage)
        appendLine("-----------------------------------------")
        appendLine("Full Stack Trace:")
        appendLine(stackTrace)
        appendLine("=========================================")
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0E12)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181F))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE50914).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = Color(0xFFFF4B55),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تقرير استكشاف الأخطاء | Crash Debugger",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "BEOUT TV • $timestamp",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = { onCopy(fullReport) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF00E5FF)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(verticalScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Warning Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221114)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "توقف التطبيق بشكل غير متوقع",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Device Info Box
                if (deviceInfo.isNotBlank()) {
                    Column {
                        Text(
                            text = "معلومات النظام والجهاز",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14161D))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = deviceInfo,
                                color = Color(0xFF88A0BF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Stack Trace Monospace Box
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل التتبع البرمجي (Stack Trace)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "قابلة للنسخ والتمرير الأفقي",
                            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 380.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF08090C))
                            .border(1.dp, Color(0xFF262A36), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                            Text(
                                text = stackTrace,
                                color = Color(0xFFFFB4A2),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = Color(0xFF14161D),
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Copy & Restart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCopy(fullReport) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0B1118)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ السجل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onRestart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF232734),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة تشغيل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Secondary Action: Clear Data & Restart
                    OutlinedButton(
                        onClick = onClearDataAndRestart,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7575)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF7575))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح بيانات التطبيق المؤقتة وإعادة التشغيل", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
