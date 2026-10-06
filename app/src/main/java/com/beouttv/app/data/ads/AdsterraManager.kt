package com.beouttv.app.data.ads

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AdsterraConfig(
    val enabled: Boolean = true,
    val directLinkUrl: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ADSTERRA_DIRECT_LINK),
    val seeAllDirectLink: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ADSTERRA_DIRECT_LINK),
    val qualityAdEnabled: Boolean = true,
    val serverSwitchAdEnabled: Boolean = true,
    val streamLaunchAdEnabled: Boolean = true,
    val seeAllAdEnabled: Boolean = true,
    val bannerEnabled: Boolean = true,
    val bannerHtml: String = "",
    val bannerDomain: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_WEBAPP_DOMAIN),
    val socialBarEnabled: Boolean = true,
    val socialBarScriptUrl: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ADSTERRA_SOCIAL_BAR),
    val minIntervalSeconds: Long = 45L,
    val streamLaunchClickFrequency: Int = 3,
    val inAppWebView: Boolean = false
)

object AdsterraManager {
    private const val PREFS_NAME = "adsterra_vault_prefs"
    private const val KEY_ENABLED = "k_ad_enabled"
    private const val KEY_DIRECT_LINK = "k_ad_direct_link"
    private const val KEY_SEE_ALL_LINK = "k_ad_see_all_link"
    private const val KEY_QUALITY_AD = "k_ad_quality_enabled"
    private const val KEY_SERVER_AD = "k_ad_server_enabled"
    private const val KEY_STREAM_AD = "k_ad_stream_enabled"
    private const val KEY_SEE_ALL_AD = "k_ad_see_all_enabled"
    private const val KEY_BANNER_AD = "k_ad_banner_enabled"
    private const val KEY_BANNER_HTML = "k_ad_banner_html"
    private const val KEY_BANNER_DOMAIN = "k_ad_banner_domain"
    private const val KEY_SOCIAL_BAR_ENABLED = "k_ad_social_bar_enabled"
    private const val KEY_SOCIAL_BAR_SCRIPT = "k_ad_social_bar_script"
    private const val KEY_MIN_INTERVAL = "k_ad_min_interval"
    private const val KEY_CLICK_FREQ = "k_ad_click_freq"
    private const val KEY_IN_APP = "k_ad_in_app"
    private const val KEY_USER_DISABLED_ADS = "k_user_disabled_ads"

    private val FIREBASE_ADS_CONFIG_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_ADS_URL)

    val DEFAULT_DIRECT_LINK: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ADSTERRA_DIRECT_LINK)
    val DEFAULT_SOCIAL_BAR_SCRIPT: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ADSTERRA_SOCIAL_BAR)
    val REGISTERED_DOMAIN: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_WEBAPP_DOMAIN)
    val REGISTERED_FIREBASE_DOMAIN: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_WEBAPP_DOMAIN)

    private var prefs: SharedPreferences? = null
    private val _configFlow = MutableStateFlow(AdsterraConfig())
    val configFlow: StateFlow<AdsterraConfig> = _configFlow.asStateFlow()

    private val _activeAdUrl = MutableStateFlow<String?>(null)
    val activeAdUrl: StateFlow<String?> = _activeAdUrl.asStateFlow()

    private val _showSocialBar = MutableStateFlow(false)
    val showSocialBar: StateFlow<Boolean> = _showSocialBar.asStateFlow()

    fun dismissAd() {
        _activeAdUrl.value = null
    }

    fun dismissSocialBar() {
        _showSocialBar.value = false
    }

    private var lastAdShownTimestamp: Long = 0L
    private var lastQualityAdTimestamp: Long = 0L
    private var lastSeeAllAdTimestamp: Long = 0L
    private var lastTopBannerAdTimestamp: Long = 0L
    private var streamLaunchCounter: Int = 0
    private var serverSwitchCounter: Int = 0
    private var topBannerDetailsClickCount: Int = 0

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadCachedConfig()
        }
        syncRemoteConfig()
    }

    private fun loadCachedConfig() {
        val p = prefs ?: return
        val userDisabled = p.getBoolean(KEY_USER_DISABLED_ADS, false)
        val enabled = if (userDisabled) false else p.getBoolean(KEY_ENABLED, true)
        val directLink = p.getString(KEY_DIRECT_LINK, DEFAULT_DIRECT_LINK) ?: DEFAULT_DIRECT_LINK
        val seeAllLink = p.getString(KEY_SEE_ALL_LINK, directLink) ?: directLink
        val qualityEnabled = p.getBoolean(KEY_QUALITY_AD, true)
        val serverEnabled = p.getBoolean(KEY_SERVER_AD, true)
        val streamEnabled = p.getBoolean(KEY_STREAM_AD, true)
        val seeAllEnabled = p.getBoolean(KEY_SEE_ALL_AD, true)
        val bannerEnabled = p.getBoolean(KEY_BANNER_AD, true)
        val bannerHtml = p.getString(KEY_BANNER_HTML, "") ?: ""
        val bannerDomain = p.getString(KEY_BANNER_DOMAIN, REGISTERED_FIREBASE_DOMAIN) ?: REGISTERED_FIREBASE_DOMAIN
        val socialBarEnabled = p.getBoolean(KEY_SOCIAL_BAR_ENABLED, true)
        val socialBarScript = p.getString(KEY_SOCIAL_BAR_SCRIPT, DEFAULT_SOCIAL_BAR_SCRIPT) ?: DEFAULT_SOCIAL_BAR_SCRIPT
        val minInterval = p.getLong(KEY_MIN_INTERVAL, 45L)
        val clickFreq = p.getInt(KEY_CLICK_FREQ, 3)

        _configFlow.value = AdsterraConfig(
            enabled = enabled,
            directLinkUrl = directLink,
            seeAllDirectLink = seeAllLink,
            qualityAdEnabled = qualityEnabled,
            serverSwitchAdEnabled = serverEnabled,
            streamLaunchAdEnabled = streamEnabled,
            seeAllAdEnabled = seeAllEnabled,
            bannerEnabled = bannerEnabled,
            bannerHtml = bannerHtml,
            bannerDomain = bannerDomain,
            socialBarEnabled = socialBarEnabled,
            socialBarScriptUrl = socialBarScript,
            minIntervalSeconds = minInterval,
            streamLaunchClickFrequency = clickFreq,
            inAppWebView = false
        )
    }

    /**
     * Allows the user to temporarily enable or disable all ads from Settings
     */
    fun isAdsEnabled(): Boolean {
        val p = prefs ?: return true
        val userDisabled = p.getBoolean(KEY_USER_DISABLED_ADS, false)
        return !userDisabled
    }

    fun setAdsEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_USER_DISABLED_ADS, !enabled)?.apply()
        _configFlow.value = _configFlow.value.copy(enabled = enabled)
        if (!enabled) {
            dismissAd()
            dismissSocialBar()
        }
    }

    fun syncRemoteConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(FIREBASE_ADS_CONFIG_URL)
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful && body.isNotBlank() && body != "null") {
                    val json = JSONObject(body)
                    val remoteEnabled = json.optBoolean("enabled", true)
                    val userDisabled = prefs?.getBoolean(KEY_USER_DISABLED_ADS, false) ?: false
                    val enabled = if (userDisabled) false else remoteEnabled
                    val directLink = json.optString("directLinkUrl", json.optString("direct_link_url", DEFAULT_DIRECT_LINK)).ifBlank { DEFAULT_DIRECT_LINK }
                    val seeAllLink = json.optString("seeAllDirectLink", json.optString("see_all_direct_link", directLink)).ifBlank { directLink }
                    val qualityEnabled = json.optBoolean("qualityAdEnabled", json.optBoolean("quality_ad_enabled", true))
                    val serverEnabled = json.optBoolean("serverSwitchAdEnabled", json.optBoolean("server_ad_enabled", true))
                    val streamEnabled = json.optBoolean("streamLaunchAdEnabled", json.optBoolean("stream_ad_enabled", true))
                    val seeAllEnabled = json.optBoolean("seeAllAdEnabled", json.optBoolean("see_all_ad_enabled", true))
                    val bannerEnabled = json.optBoolean("bannerEnabled", json.optBoolean("banner_enabled", true))
                    val bannerHtml = json.optString("bannerHtml", json.optString("banner_html", ""))
                    val bannerDomain = json.optString("bannerDomain", json.optString("banner_domain", REGISTERED_FIREBASE_DOMAIN)).ifBlank { REGISTERED_FIREBASE_DOMAIN }
                    val socialBarEnabled = json.optBoolean("socialBarEnabled", json.optBoolean("social_bar_enabled", true))
                    val socialBarScript = json.optString("socialBarScriptUrl", json.optString("social_bar_script", DEFAULT_SOCIAL_BAR_SCRIPT)).ifBlank { DEFAULT_SOCIAL_BAR_SCRIPT }
                    val minInterval = json.optLong("minIntervalSeconds", json.optLong("min_interval_seconds", 45L))
                    val clickFreq = json.optInt("streamLaunchClickFrequency", json.optInt("click_frequency", 3))

                    val newConfig = AdsterraConfig(
                        enabled = enabled,
                        directLinkUrl = directLink,
                        seeAllDirectLink = seeAllLink,
                        qualityAdEnabled = qualityEnabled,
                        serverSwitchAdEnabled = serverEnabled,
                        streamLaunchAdEnabled = streamEnabled,
                        seeAllAdEnabled = seeAllEnabled,
                        bannerEnabled = bannerEnabled,
                        bannerHtml = bannerHtml,
                        bannerDomain = bannerDomain,
                        socialBarEnabled = socialBarEnabled,
                        socialBarScriptUrl = socialBarScript,
                        minIntervalSeconds = minInterval,
                        streamLaunchClickFrequency = clickFreq,
                        inAppWebView = false
                    )

                    _configFlow.value = newConfig
                    prefs?.edit()?.apply {
                        putBoolean(KEY_ENABLED, remoteEnabled)
                        putString(KEY_DIRECT_LINK, directLink)
                        putString(KEY_SEE_ALL_LINK, seeAllLink)
                        putBoolean(KEY_QUALITY_AD, qualityEnabled)
                        putBoolean(KEY_SERVER_AD, serverEnabled)
                        putBoolean(KEY_STREAM_AD, streamEnabled)
                        putBoolean(KEY_SEE_ALL_AD, seeAllEnabled)
                        putBoolean(KEY_BANNER_AD, bannerEnabled)
                        putString(KEY_BANNER_HTML, bannerHtml)
                        putString(KEY_BANNER_DOMAIN, bannerDomain)
                        putBoolean(KEY_SOCIAL_BAR_ENABLED, socialBarEnabled)
                        putString(KEY_SOCIAL_BAR_SCRIPT, socialBarScript)
                        putLong(KEY_MIN_INTERVAL, minInterval)
                        putInt(KEY_CLICK_FREQ, clickFreq)
                        putBoolean(KEY_IN_APP, false)
                        apply()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Checks if cooldown interval has passed since last general ad
     */
    fun isCooldownSatisfied(): Boolean {
        val now = System.currentTimeMillis()
        val minIntervalMs = _configFlow.value.minIntervalSeconds * 1000L
        return (now - lastAdShownTimestamp) >= minIntervalMs
    }

    /**
     * Triggers the In-App Floating Capsule Ad Overlay inside BeoutTV
     */
    fun showInAppAd(customUrl: String? = null): Boolean {
        if (!isAdsEnabled()) return false
        val config = _configFlow.value
        if (!config.enabled) return false

        val targetUrl = customUrl?.ifBlank { null } ?: config.directLinkUrl.ifBlank { DEFAULT_DIRECT_LINK }
        lastAdShownTimestamp = System.currentTimeMillis()
        _activeAdUrl.value = targetUrl
        return true
    }

    /**
     * Smart Quality Selection Ad:
     * Triggered when user selects a high-definition quality (1080p, 720p, 740p, FHD, HD)
     * ONLY if there are multiple qualities available to choose from!
     */
    fun onQualitySelected(context: Context, qualityLabel: String, totalQualitiesCount: Int): Boolean {
        val config = _configFlow.value
        if (!config.enabled || !config.qualityAdEnabled) return false

        // Only trigger if multiple qualities are available
        if (totalQualitiesCount <= 1) return false

        // Check for 1080, 720, 740 (HD / FHD)
        val isTargetQuality = qualityLabel.contains("1080") ||
                qualityLabel.contains("720") ||
                qualityLabel.contains("740") ||
                qualityLabel.contains("FHD", ignoreCase = true)

        if (isTargetQuality) {
            val now = System.currentTimeMillis()
            if (now - lastQualityAdTimestamp >= 20000L) {
                lastQualityAdTimestamp = now
                return showInAppAd()
            }
        }
        return false
    }

    /**
     * Smart Server Switch Ad:
     * Triggered when user switches to an alternative server
     */
    fun onServerSwitched(context: Context, serverName: String): Boolean {
        val config = _configFlow.value
        if (!config.enabled || !config.serverSwitchAdEnabled) return false

        serverSwitchCounter++
        if (isCooldownSatisfied() || serverSwitchCounter % 2 == 0) {
            return showInAppAd()
        }
        return false
    }

    /**
     * Smart Stream Launch Ad:
     * Always triggers the In-App Floating Capsule Ad Overlay on EVERY channel launch.
     */
    fun onStreamLaunch(context: Context): Boolean {
        val config = _configFlow.value
        if (!config.enabled || !config.streamLaunchAdEnabled) return false

        streamLaunchCounter++
        val freq = config.streamLaunchClickFrequency.coerceAtLeast(1)
        if (freq == 1 || streamLaunchCounter % freq == 0) {
            return showInAppAd()
        }
        return false
    }

    /**
     * Smart Episode Launch Ad (دراما ريلز):
     * Triggered when user opens episode 3 or higher.
     * Episodes 1 and 2 NEVER show ads.
     */
    fun onEpisodeLaunch(episodeNumber: Int): Boolean {
        if (episodeNumber < 3) return false
        return showInAppAd()
    }

    /**
     * Smart See-All (عرض الكل) Click Ad:
     * Triggered when user taps "عرض الكل" on category sections
     */
    fun onSeeAllClicked(context: Context): Boolean {
        if (com.beouttv.app.core.tv.DeviceTypeDetector.isTvModeFlow.value) return false
        val config = _configFlow.value
        if (!config.enabled || !config.seeAllAdEnabled) return false

        val now = System.currentTimeMillis()
        if (now - lastSeeAllAdTimestamp >= 20000L) {
            lastSeeAllAdTimestamp = now
            val targetUrl = config.seeAllDirectLink.ifBlank { config.directLinkUrl }
            return openDirectLink(context, targetUrl)
        }
        return false
    }

    /**
     * Smart Floating Popunder Trigger for Movie / Series Details & Hero Banner Details:
     * - Triggered floatingly when opening details or entering ANY movie or series.
     * - Dynamic smart pacing: triggers every 2nd click (counter % 2 == 1).
     * - Minimum interval: 30 seconds cooldown between triggers to ensure non-intrusive presentation.
     */
    fun onMediaDetailsOpened(context: Context): Boolean {
        val config = _configFlow.value
        if (!config.enabled || !config.socialBarEnabled) return false

        topBannerDetailsClickCount++
        val now = System.currentTimeMillis()

        if (now - lastTopBannerAdTimestamp >= 30000L && (topBannerDetailsClickCount % 2 == 1)) {
            lastTopBannerAdTimestamp = now
            _showSocialBar.value = true
            return true
        }
        return false
    }

    fun onTopBannerDetailsClicked(context: Context): Boolean {
        return onMediaDetailsOpened(context)
    }

    /**
     * Opens the Direct Link via modern Chrome Custom Tabs (CCT)
     * Real Google Chrome engine guarantees 100% impression and click attribution
     * without net::ERR_HTTP_RESPONSE_CODE_FAILURE
     */
    fun openDirectLink(context: Context, customUrl: String? = null): Boolean {
        if (!isAdsEnabled()) return false
        val config = _configFlow.value
        if (!config.enabled) return false

        val targetUrl = customUrl?.ifBlank { null } ?: config.directLinkUrl.ifBlank { DEFAULT_DIRECT_LINK }
        lastAdShownTimestamp = System.currentTimeMillis()

        // Clear any active overlay so failing WebViews are NEVER triggered for direct links
        _activeAdUrl.value = null

        try {
            val extras = android.os.Bundle()
            extras.putBinder("android.support.customtabs.extra.SESSION", null)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtras(extras)
                putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", 0xFF08090B.toInt())
                putExtra("android.support.customtabs.extra.TITLE_VISIBILITY", 1)
                putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true)

                // Bind to Chrome Custom Tab if Chrome is installed
                val chromePackage = "com.android.chrome"
                try {
                    context.packageManager.getPackageInfo(chromePackage, 0)
                    setPackage(chromePackage)
                } catch (_: Exception) {}
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
