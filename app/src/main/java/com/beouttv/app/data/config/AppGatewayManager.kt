package com.beouttv.app.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GatewayStatus {
    NORMAL,
    MAINTENANCE,
    SUSPENDED,
    UPDATE_REQUIRED
}

data class RemoteConfig(
    val status: GatewayStatus = GatewayStatus.NORMAL,
    val maintenanceMessage: String = "التطبيق قيد الصيانة والتطوير، سنعود للعمل قريباً.",
    val suspendedMessage: String = "تم إيقاف الخدمة مؤقتاً، يرجى المحاولة لاحقاً.",
    val updateMessage: String = "يوجد تحديث إجباري جديد متاح للتطبيق.",
    val updateUrl: String = "https://t.me/beouttvapp",
    val secondaryUpdateUrl: String = "https://t.me/oneplusnet",
    val minVersionCode: Int = 1,
    val minVersionName: String = "1.0",
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val currentAppVersionName: String = "1.0",
    val currentAppVersionCode: Int = 1,
    val activeApiUrl: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ACTIVE_API_URL),
    val cloudflareEndpoint: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_CLOUDFLARE_ENDPOINT),
    val backupPanelUrl: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_BACKUP_PANEL_URL),
    val channelsUrl: String = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_CHANNELS_URL)
)

object AppGatewayManager {
    private const val PREFS_NAME = "gateway_vault_prefs"
    private const val KEY_CACHED_API_URL = "k_api_url"
    private const val KEY_CACHED_STATUS = "k_status"
    private const val KEY_CACHED_MSG = "k_msg"
    private const val KEY_CACHED_UPDATE_MSG = "k_update_msg"
    private const val KEY_CACHED_UPDATE_URL = "k_update_url"
    private const val KEY_CACHED_CHANNELS_URL = "k_channels_url"
    private const val KEY_CACHED_MIN_VERSION_CODE = "k_min_version_code"
    private const val KEY_CACHED_MIN_VERSION_NAME = "k_min_version_name"
    private const val KEY_CACHED_LATEST_VERSION_NAME = "k_latest_version_name"

    private val FIREBASE_HOSTING_GATEWAY_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_HOSTING_GATEWAY)
    private val FIREBASE_RTDB_GATEWAY_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_RTDB_GATEWAY)
    private val FIREBASE_STORAGE_CONFIG_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_STORAGE_CONFIG_URL)
    private val PRIMARY_GATEWAY_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_PRIMARY_PANEL_URL)
    private val BOUNTYIFY_GATEWAY_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_BOUNTYIFY_PANEL_URL)
    private val FALLBACK_GATEWAY_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FALLBACK_GATEWAY_URL)

    private lateinit var prefs: SharedPreferences
    private val _configFlow = MutableStateFlow(RemoteConfig())
    val configFlow: StateFlow<RemoteConfig> = _configFlow.asStateFlow()

    var currentAppVersionCode: Int = 1
        private set
    var currentAppVersionName: String = "1.0"
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            currentAppVersionName = packageInfo.versionName ?: "3.0"
            currentAppVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: Throwable) {}

        val defaultApi = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_ACTIVE_API_URL)
        val defaultChannels = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_FIREBASE_CHANNELS_URL)
        val savedApi = prefs.getString(KEY_CACHED_API_URL, defaultApi) ?: defaultApi
        val savedMsg = prefs.getString(KEY_CACHED_MSG, "التطبيق قيد الصيانة والتطوير، سنعود للعمل قريباً.") ?: ""
        val savedUpdateMsg = prefs.getString(KEY_CACHED_UPDATE_MSG, "يوجد تحديث إجباري جديد متاح للتطبيق.") ?: "يوجد تحديث إجباري جديد متاح للتطبيق."
        val savedUpdateUrl = prefs.getString(KEY_CACHED_UPDATE_URL, "https://t.me/beouttvapp") ?: "https://t.me/beouttvapp"
        val savedChannelsUrl = prefs.getString(KEY_CACHED_CHANNELS_URL, defaultChannels) ?: defaultChannels
        val savedMinVersionCode = prefs.getInt(KEY_CACHED_MIN_VERSION_CODE, 1)
        val savedMinVersionName = prefs.getString(KEY_CACHED_MIN_VERSION_NAME, "1.0") ?: "1.0"
        val savedLatestVersionName = prefs.getString(KEY_CACHED_LATEST_VERSION_NAME, "1.0") ?: "1.0"
        val savedStatusStr = prefs.getString(KEY_CACHED_STATUS, GatewayStatus.NORMAL.name) ?: GatewayStatus.NORMAL.name

        val initialStatus = when {
            savedStatusStr.equals("MAINTENANCE", ignoreCase = true) -> GatewayStatus.MAINTENANCE
            savedStatusStr.equals("SUSPENDED", ignoreCase = true) -> GatewayStatus.SUSPENDED
            savedStatusStr.equals("UPDATE_REQUIRED", ignoreCase = true) -> GatewayStatus.UPDATE_REQUIRED
            savedMinVersionCode > currentAppVersionCode -> GatewayStatus.UPDATE_REQUIRED
            isVersionOlder(currentAppVersionName, savedMinVersionName) -> GatewayStatus.UPDATE_REQUIRED
            else -> GatewayStatus.NORMAL
        }

        _configFlow.value = RemoteConfig(
            status = initialStatus,
            activeApiUrl = savedApi,
            maintenanceMessage = savedMsg,
            updateMessage = savedUpdateMsg,
            updateUrl = savedUpdateUrl.ifBlank { "https://t.me/beouttvapp" },
            secondaryUpdateUrl = "https://t.me/oneplusnet",
            channelsUrl = savedChannelsUrl,
            minVersionCode = savedMinVersionCode,
            minVersionName = savedMinVersionName,
            latestVersionName = savedLatestVersionName,
            currentAppVersionName = currentAppVersionName,
            currentAppVersionCode = currentAppVersionCode
        )

        // Asynchronously check remote panel / Firebase on startup
        CoroutineScope(Dispatchers.IO).launch {
            syncRemoteConfig()
        }
    }

    fun isVersionOlder(current: String, minRequired: String): Boolean {
        if (minRequired.isBlank()) return false
        return try {
            val curParts = current.trim().split(".").mapNotNull { part ->
                part.filter { it.isDigit() }.toIntOrNull()
            }
            val reqParts = minRequired.trim().split(".").mapNotNull { part ->
                part.filter { it.isDigit() }.toIntOrNull()
            }
            if (curParts.isEmpty() || reqParts.isEmpty()) return false
            val maxLen = maxOf(curParts.size, reqParts.size)
            for (i in 0 until maxLen) {
                val c = curParts.getOrElse(i) { 0 }
                val r = reqParts.getOrElse(i) { 0 }
                if (c < r) return true
                if (c > r) return false
            }
            false
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun syncRemoteConfig(): Boolean = withContext(Dispatchers.IO) {
        val urlsToTry = listOf(
            FIREBASE_RTDB_GATEWAY_URL,
            FIREBASE_HOSTING_GATEWAY_URL,
            FIREBASE_STORAGE_CONFIG_URL,
            PRIMARY_GATEWAY_URL,
            BOUNTYIFY_GATEWAY_URL,
            FALLBACK_GATEWAY_URL
        )
        for (targetUrl in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "okhttp/4.12.0")
                    .header("X-App-Sig", "0de08ec6cd0526dcc699041ed8e6dfb3")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            applyRemoteJson(body)
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to next server or keep cached state
            }
        }
        false
    }

    private fun applyRemoteJson(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val statusStr = json.optString("status", json.optString("mode", GatewayStatus.NORMAL.name))
            val msg = json.optString("maintenance_message", json.optString("maintenanceMessage", json.optString("message", _configFlow.value.maintenanceMessage)))
            val updateMsg = json.optString("update_message", json.optString("updateMessage", "يوجد تحديث إجباري جديد متاح للتطبيق."))
            val updateUrl = json.optString("update_url", json.optString("updateUrl", _configFlow.value.updateUrl)).ifBlank { "https://t.me/beouttvapp" }
            val apiUrl = json.optString("api_url", json.optString("apiUrl", _configFlow.value.activeApiUrl))
            val channelsUrl = json.optString("channels_url", json.optString("channelsUrl", _configFlow.value.channelsUrl))

            val minVersionCode = json.optInt("min_version_code", json.optInt("minVersionCode", json.optInt("minSupportedVersion", 1)))
            val minVersionName = json.optString("min_version_name", json.optString("minVersionName", json.optString("minVersion", "1.0")))
            val latestVersionCode = json.optInt("latest_version_code", json.optInt("latestVersionCode", minVersionCode))
            val latestVersionName = json.optString("latest_version_name", json.optString("latestVersionName", json.optString("latest_version", minVersionName)))

            val effectiveStatus = when {
                statusStr.equals("maintenance", ignoreCase = true) || statusStr.equals("maint", ignoreCase = true) -> GatewayStatus.MAINTENANCE
                statusStr.equals("suspended", ignoreCase = true) || statusStr.equals("pause", ignoreCase = true) -> GatewayStatus.SUSPENDED
                statusStr.equals("update", ignoreCase = true) || statusStr.equals("update_required", ignoreCase = true) -> GatewayStatus.UPDATE_REQUIRED
                minVersionCode > currentAppVersionCode -> GatewayStatus.UPDATE_REQUIRED
                isVersionOlder(currentAppVersionName, minVersionName) -> GatewayStatus.UPDATE_REQUIRED
                else -> GatewayStatus.NORMAL
            }

            prefs.edit()
                .putString(KEY_CACHED_STATUS, effectiveStatus.name)
                .putString(KEY_CACHED_API_URL, apiUrl)
                .putString(KEY_CACHED_MSG, msg)
                .putString(KEY_CACHED_UPDATE_MSG, updateMsg)
                .putString(KEY_CACHED_UPDATE_URL, updateUrl)
                .putString(KEY_CACHED_CHANNELS_URL, channelsUrl)
                .putInt(KEY_CACHED_MIN_VERSION_CODE, minVersionCode)
                .putString(KEY_CACHED_MIN_VERSION_NAME, minVersionName)
                .putString(KEY_CACHED_LATEST_VERSION_NAME, latestVersionName)
                .apply()

            _configFlow.value = _configFlow.value.copy(
                status = effectiveStatus,
                activeApiUrl = apiUrl,
                maintenanceMessage = msg,
                updateMessage = updateMsg,
                updateUrl = updateUrl,
                secondaryUpdateUrl = "https://t.me/oneplusnet",
                channelsUrl = channelsUrl,
                minVersionCode = minVersionCode,
                minVersionName = minVersionName,
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                currentAppVersionName = currentAppVersionName,
                currentAppVersionCode = currentAppVersionCode
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getActiveApiUrl(): String = _configFlow.value.activeApiUrl
    fun getChannelsUrl(): String = _configFlow.value.channelsUrl
}
