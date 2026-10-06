package com.beouttv.app.core.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.beouttv.app.data.config.PlayerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Intelligent device form-factor detector:
 * Accurately detects whether the device is a Smart TV, Android TV, Google TV, Fire TV,
 * TV Box, or an Android Phone/Tablet.
 * Supports manual user override in Settings for hybrid setups (e.g. tablet connected to TV monitor).
 */
object DeviceTypeDetector {

    private val _isTvModeFlow = MutableStateFlow(false)
    val isTvModeFlow: StateFlow<Boolean> = _isTvModeFlow.asStateFlow()

    private var cachedResult: Boolean? = null

    /**
     * Checks if the current operating environment is a TV / Smart Screen.
     */
    fun isTvDevice(context: Context): Boolean {
        // 1. Check user preference override
        val pref = PlayerPreferences.getTvModePreference(context)
        if (pref == 1) { // Forced TV
            _isTvModeFlow.value = true
            return true
        } else if (pref == 2) { // Forced Phone
            _isTvModeFlow.value = false
            return false
        }

        cachedResult?.let { return it }

        val detected = detectHardwareIsTv(context)
        cachedResult = detected
        _isTvModeFlow.value = detected
        return detected
    }

    private fun detectHardwareIsTv(context: Context): Boolean {
        try {
            // Check Android UiModeManager
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                return true
            }

            val pm = context.packageManager

            // Check Leanback / Television system features
            val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            val hasTvType = pm.hasSystemFeature("android.hardware.type.television")
            val hasLiveTv = pm.hasSystemFeature("android.software.live_tv")

            if (hasLeanback || hasTvType || hasLiveTv) {
                return true
            }

            // Check if touchscreen is absent (typical for TV Boxes / HDMI dongles)
            val lacksTouchscreen = !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (lacksTouchscreen) {
                return true
            }

            // Check Android TV system property hints (FireOS, Google TV, MiTV, Android TV)
            val brand = android.os.Build.BRAND.lowercase()
            val model = android.os.Build.MODEL.lowercase()
            val device = android.os.Build.DEVICE.lowercase()

            val isTvDeviceModel = brand.contains("aft") || // Amazon Fire TV
                    model.contains("fire tv") ||
                    model.contains("firetv") ||
                    model.contains("android tv") ||
                    model.contains("google tv") ||
                    model.contains("mitv") ||
                    model.contains("mibox") ||
                    device.contains("atv")

            if (isTvDeviceModel) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    /**
     * Initializes detector and warms up state flow.
     */
    fun init(context: Context) {
        refreshState(context)
    }

    /**
     * Refresh detection state (e.g. after user changes settings).
     */
    fun refreshState(context: Context) {
        cachedResult = null
        isTvDevice(context)
    }

    /**
     * Sets explicit TV mode preference: 0=Auto, 1=Force TV, 2=Force Phone.
     */
    fun setTvMode(context: Context, mode: Int) {
        PlayerPreferences.setTvModePreference(context, mode)
        refreshState(context)
    }

    /**
     * Toggles between Phone and TV mode.
     */
    fun toggleTvMode(context: Context) {
        val currentIsTv = isTvDevice(context)
        val newMode = if (currentIsTv) 2 else 1
        setTvMode(context, newMode)
    }

    /**
     * Synchronizes user preference and refreshes TV mode state.
     */
    fun syncUserPreference(context: Context) {
        refreshState(context)
    }
}
