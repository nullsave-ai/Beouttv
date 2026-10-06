package com.beouttv.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.beouttv.app.data.ads.AdsterraManager
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutShapes

/**
 * Adsterra Social Bar Composable
 *
 * Runs the dynamic Adsterra Social Bar widget (Unit ID: 31134482 / digite-bda04.web.app).
 * Loads with base URL "https://digite-bda04.web.app/" to guarantee domain validation.
 * Intercepts all click-out redirects and routes them directly to Chrome Custom Tabs
 * for 100% impression and conversion attribution without net::ERR_HTTP_RESPONSE_CODE_FAILURE.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraSocialBar(
    modifier: Modifier = Modifier
) {
    val config by AdsterraManager.configFlow.collectAsState()
    val context = LocalContext.current

    if (!config.enabled || !config.socialBarEnabled || config.socialBarScriptUrl.isBlank()) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(Color.Transparent)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0) // Completely transparent
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        loadsImagesAutomatically = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
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

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            return handleUrl(ctx, url)
                        }

                        @Suppress("DEPRECATION")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return handleUrl(ctx, url)
                        }

                        private fun handleUrl(context: Context, url: String?): Boolean {
                            if (url.isNullOrBlank()) return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                val lower = url.lowercase()
                                // If user clicked an ad offer, route to Chrome Custom Tabs
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

                    val htmlContent = """
                        <!DOCTYPE html>
                        <html lang="ar">
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
                            </style>
                        </head>
                        <body>
                            <script src="${config.socialBarScriptUrl}"></script>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL(
                        AdsterraManager.REGISTERED_DOMAIN,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }
}

/**
 * Dynamic Floating Social Bar Capsule
 * Sleek overlay that slides up seamlessly when triggered (e.g. top banner details click).
 * Auto-dismisses after 14 seconds, with user close button.
 */
@Composable
fun FloatingSocialBarCapsule(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(14000L)
        onDismiss()
    }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(78.dp)
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.ElevatedSurface)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
    ) {
        AdsterraSocialBar(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
