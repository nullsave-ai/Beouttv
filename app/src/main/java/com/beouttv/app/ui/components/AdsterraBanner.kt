package com.beouttv.app.ui.components

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.beouttv.app.data.ads.AdsterraManager
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutTextStyles

/**
 * Pure Adsterra Banner Slot (320x50 / Mobile Banner)
 * Dedicated ad space container without any fake text or buttons.
 * Renders the active Adsterra Banner HTML or Social Bar script.
 */
@Composable
fun AdsterraBanner(
    modifier: Modifier = Modifier
) {
    val config by AdsterraManager.configFlow.collectAsState()
    val context = LocalContext.current

    if (!config.enabled || !config.bannerEnabled) return

    val adContent = if (config.bannerHtml.isNotBlank()) {
        config.bannerHtml
    } else if (config.socialBarEnabled && config.socialBarScriptUrl.isNotBlank()) {
        """<script src="${config.socialBarScriptUrl}"></script>"""
    } else {
        ""
    }

    if (adContent.isBlank()) return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 324.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0) // Transparent background
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        loadsImagesAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            return handleUrl(url)
                        }

                        @Suppress("DEPRECATION")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return handleUrl(url)
                        }

                        private fun handleUrl(url: String?): Boolean {
                            if (url.isNullOrBlank()) return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                val lower = url.lowercase()
                                if (!lower.contains("digite-bda04.web.app") &&
                                    !lower.contains("digite-bda04.firebaseapp.com") &&
                                    !lower.contains("highperformanceformat.com") &&
                                    !lower.contains("highrevenueformat.com") &&
                                    !lower.contains("profitableratecpmnetwork.com")) {
                                    AdsterraManager.openDirectLink(context, url)
                                    return true
                                }
                            }
                            return false
                        }
                    }

                    tag = adContent
                    val htmlPage = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body {
                                    width: 100%;
                                    height: 100%;
                                    background: transparent !important;
                                    overflow: hidden;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                }
                                iframe {
                                    border: none !important;
                                    border-radius: 8px !important;
                                    max-width: 100% !important;
                                }
                            </style>
                        </head>
                        <body>
                            $adContent
                        </body>
                        </html>
                    """.trimIndent()

                    val baseDomain = if (config.bannerDomain.isNotBlank()) config.bannerDomain else AdsterraManager.REGISTERED_FIREBASE_DOMAIN
                    loadDataWithBaseURL(
                        baseDomain,
                        htmlPage,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                if (webView.tag != adContent && adContent.isNotBlank()) {
                    webView.tag = adContent
                    val htmlPage = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body {
                                    width: 100%;
                                    height: 100%;
                                    background: transparent !important;
                                    overflow: hidden;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                }
                                iframe {
                                    border: none !important;
                                    border-radius: 8px !important;
                                    max-width: 100% !important;
                                }
                            </style>
                        </head>
                        <body>
                            $adContent
                        </body>
                        </html>
                    """.trimIndent()

                    val baseDomain = if (config.bannerDomain.isNotBlank()) config.bannerDomain else AdsterraManager.REGISTERED_FIREBASE_DOMAIN
                    webView.loadDataWithBaseURL(
                        baseDomain,
                        htmlPage,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
        }
    }
}

/**
 * Click-To-Show Sponsored Card (بطاقة رعاية تفاعلية تفتح الإعلان عند النقر)
 * Designed with the subtle dark gradient, no borders, and dignified, respectable copy.
 */
@Composable
fun AdsterraSponsoredCard(
    modifier: Modifier = Modifier
) {
    val config by AdsterraManager.configFlow.collectAsState()
    val context = LocalContext.current

    if (!config.enabled) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .clickable {
                AdsterraManager.openDirectLink(context)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "رعاية البث",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeoutColors.BrightBlue
                    )
                }

                Column {
                    Text(
                        text = "مشاهدة إعلان لرعاية التطبيق",
                        style = BeoutTextStyles.Caption.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = "اضغط لعرض إعلان والمساهمة في استقرار الخوادم مجاناً",
                        style = BeoutTextStyles.Caption.copy(
                            color = BeoutColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BeoutColors.BrightBlue)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "عرض الإعلان",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = "Open",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
