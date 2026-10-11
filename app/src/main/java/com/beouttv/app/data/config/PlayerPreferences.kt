package com.beouttv.app.data.config

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.beouttv.app.data.filter.ContentFilterEngine

enum class SubtitleStylePreset(
    val id: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val foregroundColor: Int,
    val backgroundColor: Int,
    val edgeType: Int,
    val edgeColor: Int
) {
    WHITE_OUTLINE(
        id = "white_outline",
        displayNameAr = "أبيض مع حواف سوداء (سينمائي كلاسيكي)",
        displayNameEn = "White with Black Outline",
        foregroundColor = Color.WHITE,
        backgroundColor = Color.TRANSPARENT,
        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        edgeColor = Color.BLACK
    ),
    YELLOW_SHADOW(
        id = "yellow_shadow",
        displayNameAr = "أصفر مع ظل ناعم (عالي الوضوح)",
        displayNameEn = "Yellow with Shadow",
        foregroundColor = 0xFFFFD700.toInt(),
        backgroundColor = Color.TRANSPARENT,
        edgeType = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
        edgeColor = Color.BLACK
    ),
    OBSIDIAN_BOX(
        id = "obsidian_box",
        displayNameAr = "أبيض مع صندوق أسود أنيق (Modern Box)",
        displayNameEn = "White on Obsidian Box",
        foregroundColor = Color.WHITE,
        backgroundColor = 0xB3101721.toInt(),
        edgeType = CaptionStyleCompat.EDGE_TYPE_NONE,
        edgeColor = Color.TRANSPARENT
    ),
    AMBER_GOLD(
        id = "amber_gold",
        displayNameAr = "ذهبي كهرماني فاخر (Luxury Amber)",
        displayNameEn = "Luxury Amber Gold",
        foregroundColor = 0xFFE5A93C.toInt(),
        backgroundColor = Color.TRANSPARENT,
        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        edgeColor = Color.BLACK
    )
}

enum class SubtitleSizePreset(
    val id: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val sizeSp: Float
) {
    SMALL("small", "صغير (14sp)", "Small (14sp)", 14f),
    MEDIUM("medium", "قياسي (18sp)", "Medium (18sp)", 18f),
    LARGE("large", "كبير (22sp)", "Large (22sp)", 22f),
    EXTRA_LARGE("xlarge", "ضخم (26sp)", "Extra Large (26sp)", 26f)
}

object PlayerPreferences {
    private const val PREFS_SETTINGS = "beout_user_settings_prefs"

    const val KEY_HW_ACCEL = "hw_acceleration"
    const val KEY_BUFFER_SIZE = "buffer_size_seconds"
    const val KEY_ASPECT_RATIO = "aspect_ratio_mode"
    const val KEY_DEFAULT_QUALITY = "default_stream_quality"
    const val KEY_DEFAULT_SERVER = "default_stream_server"
    const val KEY_SUBTITLE_SIZE = "subtitle_size_preset"
    const val KEY_SUBTITLE_STYLE = "subtitle_style_preset"
    const val KEY_AUDIO_SOFT_DECODE = "audio_soft_decode"
    const val KEY_SKIP_FRAMES_SAME_VSYNC = "skip_frames_same_vsync"
    const val KEY_LOAD_TIMEOUT = "player_load_timeout_seconds"
    const val KEY_AUTO_SWITCH_SERVER_ON_ERROR = "auto_switch_server_on_error"
    const val KEY_SLOW_INTERNET_MODE = "player_slow_internet_mode"
    const val KEY_RENDER_MODE = "player_render_mode"
    const val KEY_SHOW_PLAYER_STATS = "player_show_stats"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
    }

    // --- Audio Soft Decoding (mytv engine) ---
    fun isAudioSoftDecodeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUDIO_SOFT_DECODE, false)
    }

    fun setAudioSoftDecodeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUDIO_SOFT_DECODE, enabled).apply()
    }

    // --- Skip Multiple Frames on Same VSync (mytv engine) ---
    fun isSkipFramesOnSameVSyncEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SKIP_FRAMES_SAME_VSYNC, true)
    }

    fun setSkipFramesOnSameVSyncEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SKIP_FRAMES_SAME_VSYNC, enabled).apply()
    }

    // --- Player Load Timeout Seconds (mytv engine) ---
    fun getLoadTimeoutSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOAD_TIMEOUT, 15)
    }

    fun setLoadTimeoutSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_LOAD_TIMEOUT, seconds).apply()
    }

    // --- Auto Switch Server on Error ---
    fun isAutoSwitchServerOnErrorEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_SWITCH_SERVER_ON_ERROR, false)
    }

    fun setAutoSwitchServerOnErrorEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_SWITCH_SERVER_ON_ERROR, enabled).apply()
    }

    // --- Ultra Slow Internet Booster Mode ---
    fun isSlowInternetModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SLOW_INTERNET_MODE, false)
    }

    fun setSlowInternetModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SLOW_INTERNET_MODE, enabled).apply()
    }

    // --- Render Engine Mode (SurfaceView vs TextureView from mytv) ---
    fun getRenderMode(context: Context): String {
        return getPrefs(context).getString(KEY_RENDER_MODE, "SurfaceView") ?: "SurfaceView"
    }

    fun setRenderMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_RENDER_MODE, mode).apply()
    }

    // --- Stream Diagnostics & Technical Stats (mytv metadata) ---
    fun isShowPlayerStatsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_PLAYER_STATS, false)
    }

    fun setShowPlayerStatsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_PLAYER_STATS, enabled).apply()
    }

    // --- Hardware Acceleration ---
    fun isHwAccelerationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HW_ACCEL, true)
    }

    fun setHwAccelerationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HW_ACCEL, enabled).apply()
    }

    // --- Buffer Size (Seconds) ---
    fun getBufferSizeSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_BUFFER_SIZE, 30)
    }

    fun setBufferSizeSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_BUFFER_SIZE, seconds).apply()
    }

    // --- Aspect Ratio Mode ("Fit", "Fill", "Zoom") ---
    fun getAspectRatioMode(context: Context): String {
        return getPrefs(context).getString(KEY_ASPECT_RATIO, "Fit") ?: "Fit"
    }

    fun setAspectRatioMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_ASPECT_RATIO, mode).apply()
    }

    // --- Default Quality ---
    fun getDefaultQuality(context: Context): String {
        return getPrefs(context).getString(KEY_DEFAULT_QUALITY, "Auto (FHD)") ?: "Auto (FHD)"
    }

    fun setDefaultQuality(context: Context, quality: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_QUALITY, quality).apply()
    }

    // --- Default Server ---
    fun getDefaultServer(context: Context): String {
        return getPrefs(context).getString(KEY_DEFAULT_SERVER, "srv_wecima") ?: "srv_wecima"
    }

    fun setDefaultServer(context: Context, serverKey: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_SERVER, serverKey).apply()
    }

    // --- Subtitle Size Preset ---
    fun getSubtitleSizePreset(context: Context): SubtitleSizePreset {
        val id = getPrefs(context).getString(KEY_SUBTITLE_SIZE, SubtitleSizePreset.MEDIUM.id)
        return SubtitleSizePreset.entries.firstOrNull { it.id == id } ?: SubtitleSizePreset.MEDIUM
    }

    fun setSubtitleSizePreset(context: Context, preset: SubtitleSizePreset) {
        getPrefs(context).edit().putString(KEY_SUBTITLE_SIZE, preset.id).apply()
    }

    // --- Subtitle Style Preset ---
    fun getSubtitleStylePreset(context: Context): SubtitleStylePreset {
        val id = getPrefs(context).getString(KEY_SUBTITLE_STYLE, SubtitleStylePreset.WHITE_OUTLINE.id)
        return SubtitleStylePreset.entries.firstOrNull { it.id == id } ?: SubtitleStylePreset.WHITE_OUTLINE
    }

    fun setSubtitleStylePreset(context: Context, preset: SubtitleStylePreset) {
        getPrefs(context).edit().putString(KEY_SUBTITLE_STYLE, preset.id).apply()
    }

    // --- Build CaptionStyleCompat for ExoPlayer SubtitleView ---
    fun buildCaptionStyle(context: Context): CaptionStyleCompat {
        val preset = getSubtitleStylePreset(context)
        return CaptionStyleCompat(
            preset.foregroundColor,
            preset.backgroundColor,
            Color.TRANSPARENT,
            preset.edgeType,
            preset.edgeColor,
            Typeface.SANS_SERIF
        )
    }

    // --- Apply Subtitle Settings to PlayerView SubtitleView ---
    fun applySubtitleSettings(subtitleView: SubtitleView?, context: Context) {
        subtitleView?.let { view ->
            val style = buildCaptionStyle(context)
            val sizePreset = getSubtitleSizePreset(context)
            view.setStyle(style)
            view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, sizePreset.sizeSp)
            view.setBottomPaddingFraction(0.07f)
        }
    }

    // --- TV Mode Preference ---
    // 0 = Auto detect, 1 = Force TV Mode, 2 = Force Phone Mode
    private const val KEY_TV_MODE_PREF = "tv_mode_preference"

    fun getTvModePreference(context: Context): Int {
        return getPrefs(context).getInt(KEY_TV_MODE_PREF, 0)
    }

    fun setTvModePreference(context: Context, pref: Int) {
        getPrefs(context).edit().putInt(KEY_TV_MODE_PREF, pref).apply()
    }

    // --- Adult Content Filter (+18) - Permanently forced active ---
    private const val KEY_ADULT_FILTER = "adult_content_filter_enabled"

    fun isAdultFilterEnabled(context: Context): Boolean {
        return true
    }

    fun setAdultFilterEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADULT_FILTER, true).apply()
        ContentFilterEngine.isAdultFilterEnabled = true
    }
}
