package com.beouttv.app.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.beouttv.app.data.ads.AdsterraManager
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.core.tv.DeviceTypeDetector
import kotlinx.coroutines.delay

@Composable
fun InAppAdOverlay(
    adUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isTvMode = remember { DeviceTypeDetector.isTvDevice(context) }
    val skipFocusRequester = remember { FocusRequester() }
    var countdownSeconds by remember { mutableIntStateOf(7) }
    var canDismiss by remember { mutableStateOf(false) }

    // Enforce mandatory viewing: Back button is blocked until the countdown completes
    BackHandler(enabled = true) {
        if (canDismiss) {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        for (i in 7 downTo 1) {
            countdownSeconds = i
            delay(1000L)
        }
        countdownSeconds = 0
        canDismiss = true
        // Automatic ad skip after 7 seconds!
        delay(200L)
        onDismiss()
    }

    LaunchedEffect(canDismiss) {
        if (canDismiss && isTvMode) {
            delay(150)
            try {
                skipFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9999f)
            .background(Color(0xFF08090B))
    ) {
        // 1. Fullscreen In-App WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable cookies (both 1st and 3rd party) required for Adsterra impression attribution
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                        // Genuine Chrome Mobile UserAgent so Adsterra recognizes it as real Chrome
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onCreateWindow(
                            view: android.webkit.WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            val href = view?.handler?.obtainMessage()
                            view?.requestFocusNodeHref(href)
                            val targetUrl = href?.data?.getString("url")
                            if (!targetUrl.isNullOrBlank()) {
                                AdsterraManager.openDirectLink(ctx, targetUrl)
                                return true
                            }
                            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                        }
                    }

                    fun renderFallbackScreen(view: android.webkit.WebView?) {
                        val fallbackHtml = """
                            <!DOCTYPE html>
                            <html dir="rtl" lang="ar">
                            <head>
                                <meta charset="utf-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    * { margin:0; padding:0; box-sizing:border-box; }
                                    body {
                                        background-color: #08090B;
                                        color: #FFFFFF;
                                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                        display: flex;
                                        flex-direction: column;
                                        align-items: center;
                                        justify-content: center;
                                        min-height: 100vh;
                                        padding: 28px;
                                        text-align: center;
                                    }
                                    .badge {
                                        background: rgba(0, 180, 216, 0.15);
                                        color: #00B4D8;
                                        border: 1px solid rgba(0, 180, 216, 0.4);
                                        padding: 6px 16px;
                                        border-radius: 20px;
                                        font-size: 12px;
                                        font-weight: 700;
                                        margin-bottom: 18px;
                                    }
                                    h2 {
                                        font-size: 20px;
                                        font-weight: 800;
                                        margin-bottom: 12px;
                                        color: #F8FAFC;
                                    }
                                    p {
                                        color: #94A3B8;
                                        font-size: 13.5px;
                                        line-height: 1.6;
                                        max-width: 320px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="badge">شكراً لدعمك • BEOUT TV</div>
                                <h2>مشاهدة الإعلانات تدعم استمرار البث</h2>
                                <p>تساعدنا هذه الثواني البسيطة على تغطية تكاليف الخوادم وتوفير البث بجودة مجانية للجميع.<br><br>سيبدأ تشغيل البث تلقائياً فور انتهاء العداد في الأعلى.</p>
                            </body>
                            </html>
                        """.trimIndent()
                        try {
                            view?.loadDataWithBaseURL("https://digite-bda04.web.app/", fallbackHtml, "text/html", "UTF-8", null)
                        } catch (_: Exception) {}
                    }

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            val uri = request.url
                            val scheme = uri.scheme?.lowercase() ?: ""

                            // Open Play Store or external app stores directly to count conversion clicks
                            if (url.contains("play.google.com/store") || scheme == "market") {
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }

                            // External custom schemes (tel, mailto, intent, etc.)
                            if (scheme != "http" && scheme != "https") {
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }

                            return false
                        }

                        override fun onReceivedError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                renderFallbackScreen(view)
                            }
                        }

                        override fun onReceivedHttpError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            if (request?.isForMainFrame == true) {
                                val status = errorResponse?.statusCode ?: 0
                                if (status >= 400) {
                                    renderFallbackScreen(view)
                                }
                            }
                        }
                    }

                    loadUrl(adUrl)
                }
            },
            update = { _ -> },
            onRelease = { webView ->
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                } catch (_: Exception) {}
            }
        )

        // 2. Hairline Blue Progress Line at the very top
        val timerProgress = if (canDismiss) 1f else ((7 - countdownSeconds) / 7f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(2.5.dp),
            color = BeoutColors.BrightBlue,
            trackColor = Color.Transparent
        )

        // 3. Floating Glassmorphic Pill Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xD90D1117))
                    .border(
                        1.dp,
                        if (canDismiss) BeoutColors.BrightBlue.copy(alpha = 0.8f) else BeoutColors.Divider.copy(alpha = 0.8f),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Sponsor Badge & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ADS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BeoutColors.BrightBlue
                        )
                    }
                    Text(
                        text = if (!canDismiss) "مشاهدة مطلوبة (${countdownSeconds}s)" else "شكراً لدعمك BeoutTV",
                        style = BeoutTextStyles.Caption.copy(
                            color = if (!canDismiss) Color.White else BeoutColors.BrightBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Right: Floating Skip Pill Button
                Button(
                    onClick = { if (canDismiss) onDismiss() },
                    enabled = canDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canDismiss) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.08f),
                        contentColor = if (canDismiss) Color.White else BeoutColors.TextDisabled,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = BeoutColors.TextDisabled
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canDismiss) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.12f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .focusRequester(skipFocusRequester)
                        .then(
                            if (isTvMode && canDismiss) {
                                Modifier.tvFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    onEnter = { if (canDismiss) onDismiss() }
                                )
                            } else Modifier
                        )
                ) {
                    if (canDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "تخطي ومتابعة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "تخطي (${countdownSeconds}s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeoutColors.TextDisabled
                        )
                    }
                }
            }
        }
    }
}
