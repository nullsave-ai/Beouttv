package com.beouttv.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.config.PlayerPreferences
import com.beouttv.app.data.config.SubtitleSizePreset
import com.beouttv.app.data.config.SubtitleStylePreset
import com.beouttv.app.data.ghost.UserAccountManager
import com.beouttv.app.data.ghost.UserProfile
import com.beouttv.app.data.ghost.GhostAccountManager
import com.beouttv.app.data.ghost.GhostProfile
import com.beouttv.app.data.i18n.AppLanguage
import com.beouttv.app.data.i18n.LanguageManager
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.notification.BatteryOptimizationHelper
import com.beouttv.app.data.notification.LocalNotificationManager
import com.beouttv.app.ui.components.BatteryOptimizationDialog
import com.beouttv.app.ui.components.BeoutIconButton
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.core.tv.DeviceTypeDetector
import java.io.File

private const val PREFS_SETTINGS = "beout_user_settings_prefs"
private const val KEY_MATCH_ALERTS = "match_alerts_enabled"
private const val KEY_ALERT_TIME = "alert_time_minutes"
private const val KEY_SOUND_VIBRATE = "alert_sound_vibrate"
private const val KEY_DEFAULT_QUALITY = PlayerPreferences.KEY_DEFAULT_QUALITY
private const val KEY_DEFAULT_SERVER = PlayerPreferences.KEY_DEFAULT_SERVER
private const val KEY_HW_ACCEL = PlayerPreferences.KEY_HW_ACCEL
private const val KEY_BUFFER_SIZE = PlayerPreferences.KEY_BUFFER_SIZE
private const val KEY_ASPECT_RATIO = PlayerPreferences.KEY_ASPECT_RATIO

private const val TELEGRAM_SUPPORT_URL = "https://t.me/beouttvapp"

private enum class TvMoreCategory(
    val icon: ImageVector,
    val title: String
) {
    ACCOUNT(Icons.Rounded.AccountCircle, "الملف الشخصي والحساب"),
    DISPLAY(Icons.Rounded.Tv, "توافق الشاشات ووضع العرض"),
    PLAYER(Icons.Rounded.PlayCircle, "مشغل الفيديو والجودة"),
    LANGUAGE(Icons.Rounded.Language, "لغة التطبيق والواجهة"),
    ALERTS(Icons.Rounded.Notifications, "تنبيهات المباريات المباشرة"),
    SUBTITLES(Icons.Rounded.Subtitles, "الترجمة والخطوط"),
    XTREAM(Icons.Rounded.CloudDownload, "اشتراك شخصي (Xtream / M3U)"),
    LIBRARY(Icons.Rounded.VideoLibrary, "المكتبة والمحتوى"),
    SUPPORT(Icons.Rounded.Security, "الأمان وتخزين المؤقت والدعم")
}

@Composable
private fun TvSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun TvYouTubeStyleMoreScreen(
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
    onMoviesClick: () -> Unit = {},
    onSeriesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE) }
    val userProfile by UserAccountManager.profileState.collectAsState()
    val isTvCurrent by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val (appVerName, appVerCode) = remember { getAppVersionInfo(context) }
    var cacheSizeText by remember { mutableStateOf(calculateCacheSize(context)) }

    var selectedCategory by remember { mutableStateOf(TvMoreCategory.ACCOUNT) }

    var matchAlertsEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_MATCH_ALERTS, true)) }
    var alertTimeMinutes by remember { mutableStateOf(prefs.getInt(KEY_ALERT_TIME, 15)) }
    var soundVibrateEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_SOUND_VIBRATE, true)) }

    var defaultQuality by remember { mutableStateOf(PlayerPreferences.getDefaultQuality(context)) }
    var hwAccelEnabled by remember { mutableStateOf(PlayerPreferences.isHwAccelerationEnabled(context)) }
    var audioSoftDecode by remember { mutableStateOf(PlayerPreferences.isAudioSoftDecodeEnabled(context)) }
    var autoSwitchServerOnError by remember { mutableStateOf(PlayerPreferences.isAutoSwitchServerOnErrorEnabled(context)) }
    var bufferSeconds by remember { mutableStateOf(PlayerPreferences.getBufferSizeSeconds(context)) }
    var showPlayerStats by remember { mutableStateOf(PlayerPreferences.isShowPlayerStatsEnabled(context)) }
    var subtitleSizePreset by remember { mutableStateOf(PlayerPreferences.getSubtitleSizePreset(context)) }
    var subtitleStylePreset by remember { mutableStateOf(PlayerPreferences.getSubtitleStylePreset(context)) }
    var tvModePref by remember { mutableIntStateOf(PlayerPreferences.getTvModePreference(context)) }
    var adultFilterEnabled by remember { mutableStateOf(PlayerPreferences.isAdultFilterEnabled(context)) }
    var showXtreamTvDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 10.dp, start = 76.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column (YouTube TV Categories Rail)
        Surface(
            modifier = Modifier
                .width(190.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF20B0F19),
            border = BorderStroke(1.dp, BeoutColors.Divider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                Text(
                    text = S.more(),
                    style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp),
                    color = BeoutColors.TextPrimary,
                    modifier = Modifier.padding(start = 10.dp, bottom = 12.dp, top = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(TvMoreCategory.entries) { cat ->
                        val isSelected = selectedCategory == cat
                        var isItemFocused by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .onFocusChanged {
                                    isItemFocused = it.isFocused
                                    if (it.isFocused) {
                                        selectedCategory = cat
                                    }
                                }
                                .tvFocusable(
                                    focusedScale = 1.03f,
                                    focusedBorderColor = BeoutColors.BrightBlue,
                                    focusedBorderWidth = 1.5.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    onEnter = { selectedCategory = cat }
                                )
                                .background(
                                    when {
                                        isItemFocused -> BeoutColors.BrightBlue.copy(alpha = 0.28f)
                                        isSelected -> BeoutColors.BrightBlue.copy(alpha = 0.16f)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (isSelected || isItemFocused) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = cat.title,
                                style = BeoutTextStyles.BodyBold.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected || isItemFocused) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected || isItemFocused) Color.White else Color.White.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Right Content Pane
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF20B0F19),
            border = BorderStroke(1.dp, BeoutColors.Divider)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedCategory) {
                    TvMoreCategory.ACCOUNT -> {
                        item {
                            val avatar = com.beouttv.app.ui.screens.profile.BEOUT_AVATARS.firstOrNull { it.id == userProfile.avatarId }
                                ?: com.beouttv.app.ui.screens.profile.BEOUT_AVATARS.first()

                            TvSettingsCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                            .border(2.dp, BeoutColors.BrightBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = avatar.icon,
                                            contentDescription = null,
                                            tint = BeoutColors.BrightBlue,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = userProfile.username,
                                                style = BeoutTextStyles.CardTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                                color = BeoutColors.TextPrimary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(BeoutShapes.Small)
                                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tr("عضو VIP", "VIP Member", "VIP Üye", "عضو VIP", "Membre VIP"),
                                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = BeoutColors.BrightBlue
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr("حساب محلي مشفر 100% بدون خوادم خارجية", "100% Encrypted Local Account", "Şifreli Yerel Hesap", "حساب کاربری محلی رمزگذاری‌شده", "Compte local chiffré"),
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                            color = BeoutColors.TextSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.PrimaryBlue)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onAccountClick)
                                            .clickable(onClick = onAccountClick)
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tr("إدارة الحساب", "Manage Account", "Hesabı Yönet", "مدیریت حساب", "Gérer le compte"),
                                            style = BeoutTextStyles.Button.copy(fontSize = 12.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("اختر صورتك الرمزية المفضلة (تتغير فورياً):", "Choose Avatar (Updates instantly):", "Avatar Seçin:", "انتخاب آواتار:", "Choisir un avatar :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(com.beouttv.app.ui.screens.profile.BEOUT_AVATARS) { av ->
                                        val isChosen = userProfile.avatarId == av.id
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(if (isChosen) BeoutColors.BrightBlue.copy(alpha = 0.3f) else BeoutColors.ElevatedSurface)
                                                .border(
                                                    if (isChosen) 2.dp else 1.dp,
                                                    if (isChosen) BeoutColors.BrightBlue else BeoutColors.Divider,
                                                    CircleShape
                                                )
                                                .tvFocusable(shape = CircleShape, onEnter = {
                                                    UserAccountManager.updateAvatar(context, av.id)
                                                    Toast.makeText(context, av.titleAr, Toast.LENGTH_SHORT).show()
                                                })
                                                .clickable {
                                                    UserAccountManager.updateAvatar(context, av.id)
                                                    Toast.makeText(context, av.titleAr, Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = av.icon,
                                                contentDescription = av.titleAr,
                                                tint = if (isChosen) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.DISPLAY -> {
                        item {
                            TvSettingsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr("نمط واجهة التطبيق وتوافق الأجهزة", "UI Mode & Hardware Compatibility", "Ekran Modu", "حالت رابط کاربری", "Mode d'interface"),
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                            color = BeoutColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = tr("التبديل بين وضع التحكم بالريموت لشاشات التلفاز أو اللمس للهاتف", "Switch between TV Remote and Phone Touch modes", "Kumanda ve dokunmatik modları arasında geçiş yap", "تغییر بین ریموت تلویزیون و لمس گوشی", "Basculer entre modes TV et tactile"),
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.5.sp),
                                            color = BeoutColors.TextSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isTvCurrent) "شاشات ذكية نشط" else "وضع الهاتف نشط",
                                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = BeoutColors.BrightBlue
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                val modes = listOf(
                                    Triple(0, Icons.Rounded.AutoAwesome, tr("كشف تلقائي", "Auto Detect", "Otomatik", "خودکار", "Auto") to tr("تحديد نوع الجهاز تلقائياً", "Auto hardware detection", "Otomatik cihaz algılama", "تشخیص خودکار سخت‌افزار", "Détection automatique")),
                                    Triple(1, Icons.Rounded.Tv, tr("وضع الشاشات", "Smart TV", "Akıllı TV", "تلویزیون هوشمند", "Smart TV") to tr("تحكم D-Pad وقائمة جانبية عائمة", "D-Pad remote & Floating Rail", "Kumanda ve yüzen yan menü", "ریموت D-Pad و نوار شناور", "Télécommande & Rail flottant")),
                                    Triple(2, Icons.Rounded.Smartphone, tr("وضع الهاتف", "Phone Mode", "Telefon", "حالت گوشی", "Téléphone") to tr("شاشة لمسية وشريط سفلي", "Touch & Bottom Bar", "Dokunmatik ve alt çubuk", "لمسی و نوار پایین", "Tactile & Barre du bas"))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    modes.forEach { (modeId, icon, labelPair) ->
                                        val isSelected = tvModePref == modeId
                                        val (title, subtitle) = labelPair

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(BeoutShapes.Medium)
                                                .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.15f) else BeoutColors.ElevatedSurface)
                                                .border(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider,
                                                    BeoutShapes.Medium
                                                )
                                                .tvFocusable(shape = BeoutShapes.Medium, onEnter = {
                                                    tvModePref = modeId
                                                    PlayerPreferences.setTvModePreference(context, modeId)
                                                    DeviceTypeDetector.syncUserPreference(context)
                                                    Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
                                                })
                                                .clickable {
                                                    tvModePref = modeId
                                                    PlayerPreferences.setTvModePreference(context, modeId)
                                                    DeviceTypeDetector.syncUserPreference(context)
                                                    Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = title,
                                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) BeoutColors.BrightBlue else Color.White
                                                )
                                                Text(
                                                    text = subtitle,
                                                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.5.sp),
                                                    color = BeoutColors.TextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.PLAYER -> {
                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("إعدادات محرك وتشغيل الفيديو", "Video Player Engine & Hardware", "Video Oynatıcı Motoru", "موتور پخش ویدیو", "Moteur de lecture"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(10.dp))

                                SettingsSwitchRow(
                                    title = tr("تسريع العتاد البرمجي (HW Acceleration)", "Hardware Acceleration", "Donanım Hızlandırma", "شتاب‌دهنده سخت‌افزاری", "Accélération matérielle"),
                                    subtitle = tr("استخدام معالج الرسومات لفك تشفير البث بسلاسة فائقة", "Uses GPU for smooth high-framerate decoding", "Akıcı kod çözme için GPU kullanır", "استفاده از پردازنده گرافیکی", "Utilise le GPU pour décoder"),
                                    checked = hwAccelEnabled,
                                    onCheckedChange = {
                                        hwAccelEnabled = it
                                        PlayerPreferences.setHwAccelerationEnabled(context, it)
                                    }
                                )

                                Spacer(Modifier.height(8.dp))

                                SettingsSwitchRow(
                                    title = tr("فك ترميز الصوت برمجياً (Audio Software Decode)", "Audio Software Decode", "Yazılımsal Ses Kod Çözme", "رمزگشایی نرم‌افزاری صدا", "Décodage audio logiciel"),
                                    subtitle = tr("حل مشكلة انقطاع الصوت أو القنوات الصامتة", "Fixes missing audio or unsupported codecs", "Eksik ses sorunlarını çözer", "رفع مشکل قطع صدا", "Résout les problèmes de son"),
                                    checked = audioSoftDecode,
                                    onCheckedChange = {
                                        audioSoftDecode = it
                                        PlayerPreferences.setAudioSoftDecodeEnabled(context, it)
                                    }
                                )

                                Spacer(Modifier.height(8.dp))

                                SettingsSwitchRow(
                                    title = tr("التبديل التلقائي عند تعذر البث", "Auto Switch Server on Error", "Hata Durumunda Otomatik Geçiş", "تغییر خودکار سرور در صورت خطا", "Basculement automatique"),
                                    subtitle = tr("الانتقال الفوري لسيرفر بديل في حال توقف البث", "Switches to backup stream seamlessly on failure", "Yayın durduğunda yedek sunucuya geçer", "انتقال فوری به سرور پشتیبان", "Passe au flux de secours"),
                                    checked = autoSwitchServerOnError,
                                    onCheckedChange = {
                                        autoSwitchServerOnError = it
                                        PlayerPreferences.setAutoSwitchServerOnErrorEnabled(context, it)
                                    }
                                )

                                Spacer(Modifier.height(8.dp))

                                SettingsSwitchRow(
                                    title = tr("عرض إحصائيات البث ومعدل البت المباشر", "Show Player Stats & Bitrate", "Canlı İstatistikleri Göster", "نمایش آمار زنده پخش", "Statistiques du lecteur"),
                                    subtitle = tr("إظهار الدقة ومعدل الإطارات وسرعة الاستجابة على الشاشة", "Displays resolution, framerate and bitrate overlay", "Çözünürlük ve kare hızını gösterir", "نمایش وضوح و نرخ فریم", "Affiche résolution et débit"),
                                    checked = showPlayerStats,
                                    onCheckedChange = {
                                        showPlayerStats = it
                                        PlayerPreferences.setShowPlayerStatsEnabled(context, it)
                                    }
                                )

                                Spacer(Modifier.height(8.dp))

                                SettingsSwitchRow(
                                    title = tr("تفعيل الفلتر الأخلاقي (+18)", "Enable Adult Content Filter (+18)", "Yetişkin İçerik Filtresi (+18)", "فیلتر محتوای بزرگسال (+18)", "Filtre de contenu adulte (+18)"),
                                    subtitle = tr("حجب وتصفية القنوات والأفلام ذات التصنيف الإباحي أو غير اللائق", "Blocks and filters adult (+18) and inappropriate content", "Yetişkin (+18) ve uygunsuz içerikleri engeller", "مسدودسازی محتوای نامناسب و بزرگسال", "Bloque le contenu adulte et inapproprié"),
                                    checked = adultFilterEnabled,
                                    onCheckedChange = {
                                        adultFilterEnabled = it
                                        PlayerPreferences.setAdultFilterEnabled(context, it)
                                    }
                                )
                            }
                        }

                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("جودة البث الافتراضية:", "Default Stream Quality:", "Varsayılan Yayın Kalitesi:", "کیفیت پیش‌فرض پخش:", "Qualité de diffusion par défaut :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                val qualities = listOf("Auto", "1080p", "720p", "480p", "360p")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(qualities) { q ->
                                        ChoiceChip(
                                            text = if (q == "Auto") tr("تلقائي (Auto)", "Auto", "Otomatik", "خودکار", "Auto") else q,
                                            selected = defaultQuality == q,
                                            onClick = {
                                                defaultQuality = q
                                                PlayerPreferences.setDefaultQuality(context, q)
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = tr("حجم التخزين المؤقت للثبات (Buffer Size):", "Buffer Size for Stability:", "Kararlılık İçin Arabellek:", "اندازه حافظه بافر:", "Taille du tampon :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                val buffers = listOf(15, 30, 60, 120)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(buffers) { b ->
                                        ChoiceChip(
                                            text = "$b ${tr("ثانية", "sec", "sn", "ثانیه", "sec")}",
                                            selected = bufferSeconds == b,
                                            onClick = {
                                                bufferSeconds = b
                                                PlayerPreferences.setBufferSizeSeconds(context, b)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.LANGUAGE -> {
                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("اختر لغة الواجهة (تتغير فورياً وتتكيف تلقائياً مع اتجاه الشاشة):", "Select App Language (updates instantly):", "Uygulama Dilini Seçin:", "زبان برنامه را انتخاب کنید:", "Choisissez la langue :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(12.dp))
                                val currentLang by LanguageManager.currentLanguage.collectAsState()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppLanguage.entries.forEach { lang ->
                                        ChoiceChip(
                                            text = "${lang.nativeName} (${lang.title})",
                                            selected = currentLang == lang,
                                            onClick = {
                                                LanguageManager.setLanguage(context, lang)
                                                Toast.makeText(context, lang.nativeName, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.ALERTS -> {
                        item {
                            TvSettingsCard {
                                SettingsSwitchRow(
                                    title = tr("تفعيل تنبيهات المباريات المباشرة", "Enable Live Match Alerts", "Canlı Maç Bildirimleri", "فعال‌سازی اعلان مسابقات", "Activer les alertes"),
                                    subtitle = tr("إشعارك قبل انطلاق مباريات ناديك المفضل أو المباريات الكبرى", "Notifies before matches start", "Maçlar başlamadan önce bildirim", "اعلان قبل از شروع مسابقه", "Notifie avant le coup d'envoi"),
                                    checked = matchAlertsEnabled,
                                    onCheckedChange = {
                                        matchAlertsEnabled = it
                                        prefs.edit().putBoolean(KEY_MATCH_ALERTS, it).apply()
                                    }
                                )

                                Spacer(Modifier.height(8.dp))

                                SettingsSwitchRow(
                                    title = tr("الصوت والاهتزاز للتنبيهات", "Alert Sound & Vibration", "Ses ve Titreşim", "صدا و لرزش اعلان", "Son et vibration"),
                                    subtitle = tr("تشغيل نغمة تنبيه مميزة عند بدء المباراة", "Plays notification sound when match kicks off", "Maç başladığında ses çalar", "پخش صدا هنگام شروع مسابقه", "Sonnerie au début du match"),
                                    checked = soundVibrateEnabled,
                                    onCheckedChange = {
                                        soundVibrateEnabled = it
                                        prefs.edit().putBoolean(KEY_SOUND_VIBRATE, it).apply()
                                    }
                                )

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = tr("وقت التنبيه قبل المباراة:", "Alert Lead Time:", "Maç Öncesi Bildirim Zamanı:", "زمان اعلان قبل از مسابقه:", "Délai de notification :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                val times = listOf(5, 10, 15, 30)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(times) { t ->
                                        ChoiceChip(
                                            text = "${tr("قبل", "Before", "Önce", "قبل از", "Avant")} $t ${tr("دقيقة", "min", "dk", "دقیقه", "min")}",
                                            selected = alertTimeMinutes == t,
                                            onClick = {
                                                alertTimeMinutes = t
                                                prefs.edit().putInt(KEY_ALERT_TIME, t).apply()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.SUBTITLES -> {
                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("حجم خط الترجمة:", "Subtitle Font Size:", "Altyazı Yazı Boyutu:", "اندازه قلم زیرنویس:", "Taille de sous-titres :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(SubtitleSizePreset.entries) { sz ->
                                        ChoiceChip(
                                            text = sz.displayNameAr,
                                            selected = subtitleSizePreset == sz,
                                            onClick = {
                                                subtitleSizePreset = sz
                                                PlayerPreferences.setSubtitleSizePreset(context, sz)
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = tr("نمط ولون الترجمة:", "Subtitle Style & Color:", "Altyazı Stili:", "سبک و رنگ زیرنویس:", "Style de sous-titres :"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(SubtitleStylePreset.entries) { st ->
                                        ChoiceChip(
                                            text = st.displayNameAr,
                                            selected = subtitleStylePreset == st,
                                            onClick = {
                                                subtitleStylePreset = st
                                                PlayerPreferences.setSubtitleStylePreset(context, st)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.XTREAM -> {
                        item {
                            val accounts by com.beouttv.app.data.xtream.XtreamAccountManager.accounts.collectAsState()
                            TvSettingsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr("اشتراكات الاكستريم الشخصية و M3U", "Personal IPTV & M3U Accounts", "Kişisel IPTV ve M3U", "اشتراک‌های شخصی اکستریم و M3U", "Abonnements personnels IPTV et M3U"),
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                            color = BeoutColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "يدعم إضافة حتى 3 حسابات، مع فحص تلقائي لانتهاء الصلاحية وتخزين فائق السرعة عبر نظام Protobuf الثنائي",
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                            color = BeoutColors.TextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { showXtreamTvDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                                        shape = BeoutShapes.Small,
                                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                                    ) {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("إدارة الحسابات (${accounts.size}/${com.beouttv.app.data.xtream.XtreamAccountManager.MAX_ACCOUNTS})", style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp))
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                if (accounts.isEmpty()) {
                                    Text(
                                        text = "لا توجد أي اشتراكات مضافة حالياً. اضغط على 'إدارة الحسابات' لإدخال بيانات اشتراكك.",
                                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                        color = BeoutColors.TextSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        accounts.forEach { acc ->
                                            val isExp = acc.isExpired()
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(BeoutShapes.Small)
                                                    .background(BeoutColors.ElevatedSurface)
                                                    .border(1.dp, if (isExp) Color(0xFFEF5350) else BeoutColors.Divider, BeoutShapes.Small)
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(acc.name, style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = BeoutColors.TextPrimary)
                                                        Text(acc.getFormattedExpiry(), style = BeoutTextStyles.Caption.copy(fontSize = 10.sp), color = if (isExp) Color(0xFFEF5350) else Color(0xFF81C784))
                                                    }
                                                    Text(
                                                        text = "${acc.channelsCount} قناة • ${acc.moviesCount} فيلم • ${acc.seriesCount} مسلسل",
                                                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                                        color = BeoutColors.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.LIBRARY -> {
                        item {
                            TvSettingsCard {
                                Text(
                                    text = tr("المكتبة والمحتوى السريع", "Quick Content Access", "Hızlı İçerik Erişimi", "دسترسی سریع به محتوا", "Accès rapide au contenu"),
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Card 1: Favorites
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.ElevatedSurface)
                                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onFavoritesClick)
                                            .clickable(onClick = onFavoritesClick)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Rounded.FavoriteBorder,
                                                contentDescription = null,
                                                tint = BeoutColors.BrightBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = S.favorites(),
                                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = tr("القنوات والأفلام المحفوظة", "Saved channels & media", "Kaydedilen içerikler", "محتوای ذخیره شده", "Contenus enregistrés"),
                                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                                color = BeoutColors.TextSecondary
                                            )
                                        }
                                    }

                                    // Card 2: Movies
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.ElevatedSurface)
                                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onMoviesClick)
                                            .clickable(onClick = onMoviesClick)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = null,
                                                tint = BeoutColors.BrightBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = S.latestCinema(),
                                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = tr("أحدث الأفلام العالمية والعربية", "Latest world & Arabic movies", "En yeni filmler", "جدیدترین فیلم‌ها", "Derniers films"),
                                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                                color = BeoutColors.TextSecondary
                                            )
                                        }
                                    }

                                    // Card 3: Series
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.ElevatedSurface)
                                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onSeriesClick)
                                            .clickable(onClick = onSeriesClick)
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tv,
                                                contentDescription = null,
                                                tint = BeoutColors.BrightBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = tr("مكتبة المسلسلات", "TV Series", "Özel Diziler", "سریال‌ها", "Séries"),
                                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = tr("جميع المسلسلات والمواسم والحلقات", "All shows, seasons & episodes", "Tüm diziler ve sezonlar", "تمام سریال‌ها و فصل‌ها", "Toutes les séries"),
                                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                                color = BeoutColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TvMoreCategory.SUPPORT -> {
                        item {
                            TvSettingsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr("الذاكرة المؤقتة والتخزين", "Cache & Storage Management", "Önbellek ve Depolama", "حافظه و فضای ذخیره‌سازی", "Gestion du cache"),
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                            color = BeoutColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${tr("الحجم الحالي:", "Current size:", "Mevcut boyut:", "حجم فعلی:", "Taille actuelle :")} $cacheSizeText",
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.5.sp),
                                            color = BeoutColors.TextSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.ElevatedSurface)
                                            .border(1.dp, BeoutColors.BrightBlue, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = {
                                                clearAppCache(context)
                                                cacheSizeText = calculateCacheSize(context)
                                                Toast.makeText(context, tr("تم تنظيف الذاكرة المؤقتة بنجاح", "Cache cleared successfully", "Önbellek temizlendi", "حافظه پنهان پاک شد", "Cache vidé avec succès"), Toast.LENGTH_SHORT).show()
                                            })
                                            .clickable {
                                                clearAppCache(context)
                                                cacheSizeText = calculateCacheSize(context)
                                                Toast.makeText(context, tr("تم تنظيف الذاكرة المؤقتة بنجاح", "Cache cleared successfully", "Önbellek temizlendi", "حافظه پنهان پاک شد", "Cache vidé avec succès"), Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tr("مسح الذاكرة المؤقتة", "Clear Cache", "Önbelleği Temizle", "پاک کردن حافظه پنهان", "Vider le cache"),
                                            style = BeoutTextStyles.Button.copy(fontSize = 11.5.sp),
                                            color = BeoutColors.BrightBlue
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TvSettingsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr("قناة الدعم والتحديثات الرسمية", "Official Support & Updates", "Resmi Destek ve Güncellemeler", "پشتیبانی و به‌روزرسانی‌ها", "Support et mises à jour"),
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                            color = BeoutColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "@beouttvapp",
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.5.sp),
                                            color = BeoutColors.BrightBlue
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(BeoutColors.PrimaryBlue)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = { openTelegramChannel(context) })
                                            .clickable { openTelegramChannel(context) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tr("فتح التلجرام", "Open Telegram", "Telegram'ı Aç", "باز کردن تلگرام", "Ouvrir Telegram"),
                                            style = BeoutTextStyles.Button.copy(fontSize = 11.5.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TvSettingsCard {
                                Text(
                                    text = "BeoutTV v$appVerName (Build $appVerCode)",
                                    style = BeoutTextStyles.BodyBold.copy(fontSize = 13.sp),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "IQonic Software Engineering • t.me/lion4kapp",
                                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                    color = BeoutColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showXtreamTvDialog) {
        XtreamManagerDialog(onDismiss = { showXtreamTvDialog = false })
    }
}

@Composable
fun MoreScreen(
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
    onMoviesClick: () -> Unit = {},
    onSeriesClick: () -> Unit = {}
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    if (isTvMode) {
        TvYouTubeStyleMoreScreen(
            onFavoritesClick = onFavoritesClick,
            onSettingsClick = onSettingsClick,
            onAccountClick = onAccountClick,
            onMoviesClick = onMoviesClick,
            onSeriesClick = onSeriesClick
        )
        return
    }

    val context = LocalContext.current
    val userProfile by UserAccountManager.profileState.collectAsState()
    val (appVerName, appVerCode) = remember { getAppVersionInfo(context) }
    var showXtreamDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserAccountManager.loadProfile(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            text = S.more(),
            style = BeoutTextStyles.ScreenTitle,
            color = BeoutColors.TextPrimary,
            modifier = Modifier.padding(BeoutDimensions.ScreenPadding)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Account Profile Banner
            item {
                val avatar = com.beouttv.app.ui.screens.profile.BEOUT_AVATARS.firstOrNull { it.id == userProfile.avatarId }
                    ?: com.beouttv.app.ui.screens.profile.BEOUT_AVATARS.first()

                Box(
                    modifier = Modifier
                        .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS)
                        .fillMaxWidth()
                        .clip(BeoutShapes.Large)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Large)
                        .clickable(onClick = onAccountClick)
                        .padding(BeoutDimensions.SpaceM)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                .border(1.5.dp, BeoutColors.BrightBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = avatar.icon,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(BeoutDimensions.SpaceM))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.username,
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tr("عضو", "VIP", "Üye", "عضو", "Membre"),
                                        style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = BeoutColors.BrightBlue
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tr("حساب محلي مشفر (خصوصية 100% بدون خوادم)", "100% Encrypted Local Account", "Şifreli Yerel Hesap", "حساب کاربری محلی رمزگذاری‌شده", "Compte local chiffré"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary
                        )
                    }
                }
            }

            // 2. Direct Content Libraries (Series & Movies)
            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Tv,
                    title = tr("مكتبة المسلسلات الحصرية", "Exclusive TV Series", "Özel Diziler", "سریال‌های اختصاصی", "Séries exclusives"),
                    subtitle = tr("تصفح جميع المسلسلات والمواسم والحلقات", "Browse all TV shows, seasons & episodes", "Tüm dizileri, sezonları ve bölümleri keşfet", "مرور تمام سریال‌ها، فصل‌ها و قسمت‌ها", "Parcourir toutes les séries et saisons"),
                    onClick = onSeriesClick
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Movie,
                    title = S.latestCinema(),
                    subtitle = tr("أحدث الأفلام العالمية والعربية بجودة عالية", "Latest international & Arabic movies in high quality", "En yeni yerli ve yabancı filmler", "جدیدترین فیلم‌های جهانی و عربی با کیفیت بالا", "Derniers films en haute qualité"),
                    onClick = onMoviesClick
                )
            }

            // 3. Menu Items
            item {
                MoreMenuRow(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = S.favorites(),
                    subtitle = tr("القنوات والأفلام والمسلسلات المحفوظة", "Locally saved channels, movies and series", "Yerel olarak kaydedilen içerikler", "شبکه‌ها، فیلم‌ها و سریال‌های ذخیره شده", "Chaînes, films et séries enregistrés"),
                    onClick = onFavoritesClick
                )
            }

            item {
                val xtreamAccounts by com.beouttv.app.data.xtream.XtreamAccountManager.accounts.collectAsState()
                MoreMenuRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = tr("اشتراك شخصي (Xtream / M3U)", "Personal IPTV & M3U", "Kişisel IPTV / M3U", "اشتراک شخصی (Xtream / M3U)", "Abonnement personnel (Xtream / M3U)"),
                    subtitle = if (xtreamAccounts.isEmpty()) {
                        tr("إضافة سيرفر اكستريم أو رابط M3U خاص بك", "Add your own Xtream server or M3U playlist", "Kendi Xtream sunucunuzu veya M3U bağlantınızı ekleyin", "افزودن سرور اکستریم یا لینک M3U شخصی", "Ajouter votre serveur Xtream ou lien M3U")
                    } else {
                        "تمت إضافة ${xtreamAccounts.size} حسابات • سرعة فائقة بنظام Protobuf"
                    },
                    badge = if (xtreamAccounts.isNotEmpty()) "${xtreamAccounts.size}/${com.beouttv.app.data.xtream.XtreamAccountManager.MAX_ACCOUNTS}" else "Xtream",
                    onClick = { showXtreamDialog = true }
                )
            }

            item {
                val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
                MoreMenuRow(
                    icon = if (isTvMode) Icons.Rounded.Tv else Icons.Rounded.Smartphone,
                    title = tr("نمط العرض والتحكم (هاتف / تلفاز)", "Display & UI Mode (Phone / TV)", "Ekran Modu (Telefon / TV)", "حالت نمایش (گوشی / تلویزیون)", "Mode d'affichage (Téléphone / TV)"),
                    subtitle = if (isTvMode) "الوضع الحالي: شاشات ذكية (Android TV) • اضغط للتبديل الفوري" else "الوضع الحالي: هاتف محمول (Touch) • اضغط للتبديل الفوري",
                    badge = if (isTvMode) "TV Mode" else "Mobile",
                    onClick = {
                        DeviceTypeDetector.toggleTvMode(context)
                        val newIsTv = DeviceTypeDetector.isTvDevice(context)
                        val name = if (newIsTv) "وضع الشاشات (Android TV)" else "وضع الهاتف المحمول"
                        Toast.makeText(context, "تم التبديل إلى: $name", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Tune,
                    title = S.settings(),
                    subtitle = tr("تخصيص المشغل، التنبيهات، والذاكرة", "Customize player, alerts, and storage", "Oynatıcıyı, bildirimleri ve belleği özelleştir", "شخصی‌سازی پخش‌کننده، اعلان‌ها و حافظه", "Personnaliser le lecteur, alertes et stockage"),
                    onClick = onSettingsClick
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = S.support(),
                    subtitle = tr("الانضمام لقناة التلجرام الرسمية @beouttvapp", "Join official Telegram channel @beouttvapp", "Resmi Telegram kanalına katılın @beouttvapp", "عضویت در کانال رسمی تلگرام @beouttvapp", "Rejoindre la chaîne Telegram officielle @beouttvapp"),
                    badge = "Telegram",
                    onClick = { openTelegramChannel(context) }
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Security,
                    title = S.safeMode(),
                    subtitle = tr("نظام الفلترة الشامل لمنع المحتوى المخل مفعّل دائماً", "Active family filter preventing inappropriate content", "Aile koruma filtresi her zaman etkin", "فیلتر خانواده برای جلوگیری از محتوای نامناسب همیشه فعال است", "Filtre familial toujours actif"),
                    onClick = onSettingsClick
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Code,
                    title = "IQonic",
                    subtitle = tr("شركة برمجية متخصصة في تطوير التطبيقات • t.me/lion4kapp", "Software engineering company • t.me/lion4kapp", "Yazılım geliştirme şirketi • t.me/lion4kapp", "شرکت توسعه نرم‌افزار • t.me/lion4kapp", "Entreprise de logiciels • t.me/lion4kapp"),
                    badge = "Software",
                    onClick = { openTelegramLink(context, "https://t.me/lion4kapp") }
                )
            }

            item {
                MoreMenuRow(
                    icon = Icons.Rounded.Info,
                    title = S.about(),
                    subtitle = tr("الإصدار $appVerName (Build $appVerCode) السينمائي", "Version $appVerName (Build $appVerCode) Cinema", "Sürüm $appVerName (Build $appVerCode)", "نسخه $appVerName سینمایی", "Version $appVerName Cinéma"),
                    onClick = onSettingsClick
                )
            }
        }
    }

    if (showXtreamDialog) {
        XtreamManagerDialog(onDismiss = { showXtreamDialog = false })
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(BeoutShapes.Medium)
                .background(BeoutColors.ElevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(BeoutDimensions.SpaceM))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = BeoutTextStyles.CardTitle, color = BeoutColors.TextPrimary)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.BrightBlue)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp), color = BeoutColors.TextSecondary)
        }

        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = BeoutColors.TextDisabled)
    }
}

// ---------------- Settings Screen Clean Categorized Implementation ----------------

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToShortDrama: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE) }

    var matchAlertsEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_MATCH_ALERTS, true)) }
    var alertTimeMinutes by remember { mutableStateOf(prefs.getInt(KEY_ALERT_TIME, 15)) }
    var soundVibrateEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_SOUND_VIBRATE, true)) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showXtreamSettingsDialog by remember { mutableStateOf(false) }
    var showDownloadedDramaDialog by remember { mutableStateOf(false) }

    var defaultQuality by remember { mutableStateOf(PlayerPreferences.getDefaultQuality(context)) }
    var defaultServer by remember { mutableStateOf(PlayerPreferences.getDefaultServer(context)) }
    var slowInternetMode by remember { mutableStateOf(PlayerPreferences.isSlowInternetModeEnabled(context)) }
    var hwAccelEnabled by remember { mutableStateOf(PlayerPreferences.isHwAccelerationEnabled(context)) }
    var audioSoftDecode by remember { mutableStateOf(PlayerPreferences.isAudioSoftDecodeEnabled(context)) }
    var skipFramesOnVsync by remember { mutableStateOf(PlayerPreferences.isSkipFramesOnSameVSyncEnabled(context)) }
    var loadTimeoutSecs by remember { mutableStateOf(PlayerPreferences.getLoadTimeoutSeconds(context)) }
    var autoSwitchServerOnError by remember { mutableStateOf(PlayerPreferences.isAutoSwitchServerOnErrorEnabled(context)) }
    var bufferSeconds by remember { mutableStateOf(PlayerPreferences.getBufferSizeSeconds(context)) }
    var aspectRatioMode by remember { mutableStateOf(PlayerPreferences.getAspectRatioMode(context)) }
    var renderMode by remember { mutableStateOf(PlayerPreferences.getRenderMode(context)) }
    var showPlayerStats by remember { mutableStateOf(PlayerPreferences.isShowPlayerStatsEnabled(context)) }
    var subtitleSizePreset by remember { mutableStateOf(PlayerPreferences.getSubtitleSizePreset(context)) }
    var subtitleStylePreset by remember { mutableStateOf(PlayerPreferences.getSubtitleStylePreset(context)) }
    var tvModePref by remember { mutableIntStateOf(PlayerPreferences.getTvModePreference(context)) }
    var adultFilterEnabled by remember { mutableStateOf(PlayerPreferences.isAdultFilterEnabled(context)) }
    val isTvCurrent by DeviceTypeDetector.isTvModeFlow.collectAsState()

    var cacheSizeText by remember { mutableStateOf(calculateCacheSize(context)) }
    val (appVerName, appVerCode) = remember { getAppVersionInfo(context) }

    if (isTvCurrent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.Background)
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeoutIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = S.back(),
                    onClick = onBack,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(S.settings(), style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp), color = BeoutColors.TextPrimary)
            }
            TvYouTubeStyleMoreScreen(
                onFavoritesClick = {},
                onSettingsClick = {},
                onAccountClick = {},
                onMoviesClick = {},
                onSeriesClick = {}
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeoutIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = S.back(), onClick = onBack)
            Spacer(Modifier.width(BeoutDimensions.SpaceS))
            Text(S.settings(), style = BeoutTextStyles.ScreenTitle, color = BeoutColors.TextPrimary)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ================= 1. MULTILINGUAL LANGUAGE SELECTOR =================
            item {
                val currentLang by LanguageManager.currentLanguage.collectAsState()
                SettingsSectionTitle(S.language(), Icons.Rounded.Language)

                SettingsCard {
                    Text(
                        text = tr(
                            ar = "اختر لغة الواجهة (تتغير فورياً وتتكيف تلقائياً مع اتجاه الشاشة):",
                            en = "Select App Language (updates instantly with RTL/LTR support):",
                            tr = "Uygulama Dilini Seçin (anında güncellenir):",
                            fa = "زبان برنامه را انتخاب کنید (تغییر آنی و تطبیق راست به چپ):",
                            fr = "Choisissez la langue (mise à jour instantanée) :"
                        ),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                        color = BeoutColors.TextSecondary
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(com.beouttv.app.data.i18n.AppLanguage.entries) { lang ->
                            val isSelected = currentLang == lang
                            ChoiceChip(
                                text = "${lang.nativeName} (${lang.title})",
                                selected = isSelected,
                                onClick = {
                                    LanguageManager.setLanguage(context, lang)
                                    Toast.makeText(context, lang.nativeName, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // ================= 1.5 ADULT CONTENT FILTER (+18) =================
            item {
                SettingsSectionTitle(
                    title = tr(
                        "الحماية الأسرية والفلتر الأخلاقي",
                        "Family Protection & Content Filter",
                        "Aile Koruması ve İçerik Filtresi",
                        "محافظت از خانواده و فیلتر محتوا",
                        "Protection familiale et filtre de contenu"
                    ),
                    icon = Icons.Rounded.Security
                )

                SettingsCard {
                    SettingsSwitchRow(
                        title = tr(
                            "حظر المحتوى غير اللائق والإباحي (+18)",
                            "Block Adult Content (+18)",
                            "+18 Yetişkin İçeriğini Engelle",
                            "مسدودسازی محتوای بزرگسالان (+18)",
                            "Bloquer le contenu adulte (+18)"
                        ),
                        subtitle = tr(
                            "إخفاء وتصفية كافة الأفلام والمسلسلات والقنوات الإباحية والمخلة تلقائياً",
                            "Filter and hide adult movies, series and channels automatically",
                            "Yetişkin filmlerini, dizilerini ve kanallarını otomatik olarak gizle",
                            "پنهان‌سازی و فیلتر خودکار فیلم‌ها، سریال‌ها و شبکه‌های نامناسب",
                            "Masquer automatiquement les films, séries et chaînes réservés aux adultes"
                        ),
                        checked = adultFilterEnabled,
                        onCheckedChange = { enabled ->
                            adultFilterEnabled = enabled
                            PlayerPreferences.setAdultFilterEnabled(context, enabled)
                            com.beouttv.app.data.filter.ContentFilterEngine.isAdultFilterEnabled = enabled
                        }
                    )
                }
            }

            // ================= 2. APP UI & TV/PHONE DISPLAY MODE =================
            item {
                SettingsSectionTitle(
                    title = tr(
                        "نمط واجهة التطبيق وتوافق الأجهزة",
                        "UI Mode & Device Compatibility",
                        "Arayüz Modu ve Cihaz Uyumluluğu",
                        "حالت رابط کاربری و سازگاری دستگاه",
                        "Mode d'interface et compatibilité"
                    ),
                    icon = Icons.Rounded.Devices
                )

                SettingsCard {
                    // Status Badge & Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("التبديل بين وضع الهاتف ووضع الشاشات الذكية:", "Switch between Phone & Smart TV mode:", "Telefon ve Akıllı TV modu arasında geçiş yap:", "تغییر بین حالت گوشی و تلویزیون هوشمند:", "Basculer entre mode Téléphone et Smart TV :"),
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tr(
                                    "يمكنك التبديل يدوياً أو ترك النظام يكتشف الشاشة أو الهاتف تلقائياً",
                                    "Switch manually or let the app automatically detect your screen type",
                                    "Manuel geçiş yapın veya uygulamanın ekranı otomatik algılamasına izin verin",
                                    "به صورت دستی جابه‌جا شوید یا اجازه دهید برنامه خودکار نوع صفحه را تشخیص دهد",
                                    "Basculez manuellement ou laissez l'application détecter l'écran"
                                ),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }

                        // Active Mode Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(BeoutColors.BrightBlue)
                                )
                                Text(
                                    text = if (isTvCurrent) "شاشات ذكية نشط" else "وضع الهاتف نشط",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(BeoutDimensions.SpaceM))

                    // 3 Interactive Mode Cards
                    val modes = listOf(
                        Triple(
                            0,
                            Icons.Rounded.AutoAwesome,
                            tr("كشف تلقائي", "Auto Detect", "Otomatik", "خودکار", "Automatique") to tr("تحديد نوع الجهاز تلقائياً", "Auto detects hardware", "Otomatik cihaz algılama", "تشخیص خودکار سخت‌افزار", "Détection automatique")
                        ),
                        Triple(
                            1,
                            Icons.Rounded.Tv,
                            tr("وضع الشاشات", "Smart TV", "Akıllı TV", "تلویزیون هوشمند", "Smart TV") to tr("تحكم D-Pad وقائمة جانبية", "D-Pad remote & Nav Rail", "Kumanda ve yan menü", "ریموت D-Pad و نوار کناری", "Télécommande & Rail")
                        ),
                        Triple(
                            2,
                            Icons.Rounded.Smartphone,
                            tr("وضع الهاتف", "Phone Mode", "Telefon", "حالت گوشی", "Téléphone") to tr("شاشة لمسية وشريط سفلي", "Touch & Bottom Bar", "Dokunmatik ve alt çubuk", "لمسی و نوار پایین", "Tactile & Barre du bas")
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { (modeId, icon, labelPair) ->
                            val isSelected = tvModePref == modeId
                            val (title, subtitle) = labelPair

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.2f)
                                        else BeoutColors.ElevatedSurface
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        tvModePref = modeId
                                        DeviceTypeDetector.setTvMode(context, modeId)
                                        Toast.makeText(
                                            context,
                                            "تم تفعيل: $title",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.08f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (isSelected) Color.White else BeoutColors.TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = title,
                                        style = BeoutTextStyles.CardTitle.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextPrimary,
                                        maxLines = 1
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = subtitle,
                                        style = BeoutTextStyles.Caption.copy(fontSize = 9.sp),
                                        color = BeoutColors.TextSecondary,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(BeoutDimensions.SpaceM))

                    // Quick Toggle Button (زر بنمط وهوية التطبيق)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .background(BeoutColors.ElevatedSurface)
                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Medium)
                            .clickable {
                                DeviceTypeDetector.toggleTvMode(context)
                                tvModePref = PlayerPreferences.getTvModePreference(context)
                                val newIsTv = DeviceTypeDetector.isTvDevice(context)
                                val modeName = if (newIsTv) "وضع الشاشات (Android TV)" else "وضع الهاتف المحمول"
                                Toast.makeText(context, "تم التبديل إلى: $modeName", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isTvCurrent) "التبديل إلى وضع الهاتف المحمول (Touch)" else "التبديل إلى وضع الشاشات والتلفاز (Android TV)",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // ================= PERSONAL XTREAM & M3U SUBSCRIPTIONS =================
            item {
                val xtreamAccounts by com.beouttv.app.data.xtream.XtreamAccountManager.accounts.collectAsState()
                SettingsSectionTitle(
                    title = tr(
                        "اشتراكات الاكستريم الشخصية و M3U",
                        "Personal IPTV & M3U Accounts",
                        "Kişisel IPTV ve M3U",
                        "اشتراک‌های شخصی اکستریم و M3U",
                        "Abonnements IPTV et M3U"
                    ),
                    icon = Icons.Rounded.CloudDownload
                )

                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("إدارة اشتراكات الاكستريم وقوائم M3U:", "Manage Xtream & M3U Accounts:", "Xtream ve M3U Yönetimi:", "مدیریت اکستریم و M3U:", "Gérer Xtream et M3U :"),
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "يمكنك إضافة حتى 3 حسابات شخصية لمشاهدة القنوات والأفلام والمسلسلات داخل التطبيق مع فحص الصلاحية والتخزين الثنائي الفائق السرعة.",
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }

                        Button(
                            onClick = { showXtreamSettingsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                            shape = BeoutShapes.Small
                        ) {
                            Text("إدارة (${xtreamAccounts.size}/${com.beouttv.app.data.xtream.XtreamAccountManager.MAX_ACCOUNTS})", style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp))
                        }
                    }
                }
            }

            // ================= 3. TELEGRAM SUPPORT & COMMUNITY =================
            item {
                SettingsSectionTitle(S.support(), Icons.AutoMirrored.Rounded.HelpOutline)

                Box(
                    modifier = Modifier
                        .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS)
                        .fillMaxWidth()
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.ElevatedSurface)
                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), BeoutShapes.Medium)
                        .clickable { openTelegramChannel(context) }
                        .padding(BeoutDimensions.SpaceM)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(BeoutDimensions.SpaceM))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("قناة التلجرام الرسمية والدعم المباشر", "Official Telegram Channel & Direct Support", "Resmi Telegram Kanalı ve Destek", "کانال تلگرام رسمی و پشتیبانی مستقیم", "Canal Telegram officiel et support"),
                                style = BeoutTextStyles.CardTitle.copy(fontWeight = FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                            Text(
                                text = "t.me/beouttvapp • " + tr("تحديثات وروابط وسيرفرات مباشرة", "Updates, direct streams & community", "Güncellemeler ve yayınlar", "به‌روزرسانی‌ها و سرورها", "Mises à jour et streams"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = BeoutColors.BrightBlue)
                    }
                }
            }

            // ================= 4. IN-APP LOCAL NOTIFICATIONS & REMINDERS =================
            item {
                SettingsSectionTitle(S.notifications(), Icons.Rounded.NotificationsActive)

                SettingsCard {
                    // Match Alerts Switch
                    SettingsSwitchRow(
                        title = tr("تفعيل تنبيهات المباريات المباشرة", "Enable Live Match Notifications", "Canlı Maç Bildirimlerini Etkinleştir", "فعال‌سازی اعلان‌های مسابقات زنده", "Activer les notifications de match"),
                        subtitle = tr("إشعار محلي من داخل التطبيق يذكرك بوقت المباراة", "In-app local notification reminding you before kick-off", "Maç başlamadan önce yerel bildirim", "اعلان محلی برای یادآوری شروع مسابقه", "Notification locale vous rappelant le début du match"),
                        checked = matchAlertsEnabled,
                        onCheckedChange = { enabled ->
                            matchAlertsEnabled = enabled
                            prefs.edit().putBoolean(KEY_MATCH_ALERTS, enabled).apply()
                            if (enabled) {
                                val notifManager = androidx.core.app.NotificationManagerCompat.from(context)
                                if (!notifManager.areNotificationsEnabled()) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        (context as? android.app.Activity)?.requestPermissions(
                                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                            101
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (matchAlertsEnabled) {
                        Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                        // Alert Time Choice
                        Text(
                            text = tr("وقت التنبيه قبل المباراة:", "Alert Time Before Match:", "Maçtan Önce Bildirim Zamanı:", "زمان یادآوری قبل از شروع مسابقه:", "Délai de notification avant le match :"),
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp),
                            color = BeoutColors.TextPrimary
                        )
                        Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val options = listOf(
                                0 to tr("عند البداية", "At Kick-off", "Başlama Anında", "زمان شروع", "Au coup d'envoi"),
                                15 to tr("قبل 15 دقيقة", "15 mins before", "15 dk önce", "۱۵ دقیقه قبل", "15 min avant"),
                                30 to tr("قبل 30 دقيقة", "30 mins before", "30 dk önce", "۳۰ دقیقه قبل", "30 min avant"),
                                60 to tr("قبل ساعة", "1 hour before", "1 saat önce", "۱ ساعت قبل", "1 heure avant")
                            )
                            items(options) { (mins, label) ->
                                val selected = alertTimeMinutes == mins
                                ChoiceChip(
                                    text = label,
                                    selected = selected,
                                    onClick = {
                                        alertTimeMinutes = mins
                                        prefs.edit().putInt(KEY_ALERT_TIME, mins).apply()
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        // Sound & Vibration Switch
                        SettingsSwitchRow(
                            title = tr("صوت التنبيه والاهتزاز", "Alert Sound & Vibration", "Bildirim Sesi ve Titreşim", "صدا و لرزش اعلان", "Son et vibration"),
                            subtitle = tr("تشغيل صوت التنبيه والاهتزاز عند وصول الإشعار", "Play alert sound and vibrate on notification", "Bildirim geldiğinde ses çal ve titre", "پخش صدا و لرزش هنگام دریافت اعلان", "Jouer un son et vibrer lors de la notification"),
                            checked = soundVibrateEnabled,
                            onCheckedChange = {
                                soundVibrateEnabled = it
                                prefs.edit().putBoolean(KEY_SOUND_VIBRATE, it).apply()
                            }
                        )

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        // Battery & Background Reliability Card
                        val isOptimized = BatteryOptimizationHelper.isEverythingOptimized(context)
                        Surface(
                            onClick = { showBatteryDialog = true },
                            shape = BeoutShapes.Small,
                            color = if (isOptimized) Color(0xFF132A1C) else Color(0xFF2A1C13),
                            border = BorderStroke(1.dp, if (isOptimized) Color(0xFF238636) else Color(0xFF9E4A1A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOptimized) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (isOptimized) Color(0xFF3FB950) else Color(0xFFF0883E),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr("حالة وصول التنبيهات في الخلفية", "Background Alert Reliability", "Arka Plan Bildirim Durumu", "وضعیت اعلان‌ها در پس‌زمینه", "Fiabilité des alertes en arrière-plan"),
                                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                        color = BeoutColors.TextPrimary
                                    )
                                    Text(
                                        text = if (isOptimized) tr("مضمونة 100% (تم استثناء توفير البطارية)", "100% Guaranteed (Battery Exempted)", "Garantili (Pil Kısıtlaması Yok)", "تضمین‌شده (بدون محدودیت باتری)", "Garanti à 100% (Batterie exemptée)") else tr("انقر لضبط إذن البطارية وضمان التنبيه فوراً", "Tap to allow background running", "Arka planda çalışmaya izin vermek için tıklayın", "برای فعال‌سازی بدون محدودیت کلیک کنید", "Cliquez pour autoriser l'arrière-plan"),
                                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                        color = if (isOptimized) Color(0xFF3FB950) else Color(0xFFF0883E)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = null,
                                    tint = BeoutColors.TextDisabled,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showBatteryDialog) {
                item {
                    BatteryOptimizationDialog(onDismiss = { showBatteryDialog = false })
                }
            }

            // ================= 5. PLAYER & STREAMING SETTINGS =================
            item {
                SettingsSectionTitle(S.playerSettings(), Icons.Rounded.PlayCircleOutline)

                SettingsCard {
                    // Default Quality
                    Text(tr("جودة البث الافتراضية:", "Default Stream Quality:", "Varsayılan Yayın Kalitesi:", "کیفیت پیش‌فرض پخش:", "Qualité de diffusion par défaut :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val qualities = listOf(
                            "Auto (FHD)" to tr("تلقائي (FHD)", "Auto (FHD)", "Otomatik (FHD)", "خودکار (FHD)", "Auto (FHD)"),
                            "1080p FHD" to "1080p FHD",
                            "720p HD" to "720p HD",
                            "480p SD" to "480p SD"
                        )
                        items(qualities) { (qKey, qLabel) ->
                            ChoiceChip(
                                text = qLabel,
                                selected = defaultQuality == qKey,
                                onClick = {
                                    defaultQuality = qKey
                                    prefs.edit().putString(KEY_DEFAULT_QUALITY, qKey).apply()
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Default Server & Source
                    Text(tr("سيرفر ومصدر البث الافتراضي:", "Default Streaming Server:", "Varsayılan Yayın Sunucusu:", "سرور پیش‌فرض پخش:", "Serveur de streaming par défaut :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val servers = listOf(
                            "srv_direct" to "Beout Direct (HLS مباشر)",
                            "srv_vidlink" to "VidLink Pro (4K/1080p)",
                            "srv_videasy" to "Videasy (Multi-Sub)",
                            "srv_zxc" to "ZxcStream Fast",
                            "srv_2embed" to "CineSrc Backup"
                        )
                        items(servers) { (sKey, sLabel) ->
                            ChoiceChip(
                                text = sLabel,
                                selected = defaultServer == sKey,
                                onClick = {
                                    defaultServer = sKey
                                    prefs.edit().putString(KEY_DEFAULT_SERVER, sKey).apply()
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Ultra Slow Internet Booster Mode
                    SettingsSwitchRow(
                        title = tr("وضع النت شديد الضعف (Ultra Low-Speed Booster)", "Ultra Slow Internet Mode", "Ultra Düşük Hız Modu", "حالت اینترنت بسیار ضعیف", "Mode connexion très faible"),
                        subtitle = tr("تخزين مسبق عميق ومؤقت 90 ثانية، مع تقييد البث تلقائياً لأدنى دقة مكتشفة لمنع تقطيع البث نهائياً على باقات الهاتف والشبكات الضعيفة", "Aggressive pre-buffering (90s) and dynamic lowest-resolution discovery to eliminate stuttering on weak mobile networks", "Yavaş ağlarda takılmayı önlemek için 90s derin önbellek ve en düşük çözünürlük seçimi", "بافر عمیق ۹۰ ثانیه‌ای و انتخاب خودکار کمترین کیفیت موجود برای جلوگیری از وقفه در اینترنت ضعیف", "Pré-mise en mémoire tampon (90s) et sélection dynamique de la résolution minimale pour éliminer les coupures"),
                        checked = slowInternetMode,
                        onCheckedChange = {
                            slowInternetMode = it
                            PlayerPreferences.setSlowInternetModeEnabled(context, it)
                            if (it) {
                                bufferSeconds = 90
                                prefs.edit().putInt(KEY_BUFFER_SIZE, 90).apply()
                            }
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // HW Acceleration
                    SettingsSwitchRow(
                        title = tr("فك التشفير العتادي (HW+ Acceleration)", "Hardware Decoding (HW+)", "Donanım Hızlandırma (HW+)", "شتاب‌دهنده سخت‌افزاری (HW+)", "Décodage matériel (HW+)"),
                        subtitle = tr("تشغيل معالج الرسومات لتقليل الحرارة واستهلاك البطارية", "Uses GPU for smooth playback and low battery usage", "GPU kullanarak pil tasarrufu sağlar", "استفاده از پردازنده گرافیکی برای کاهش مصرف باتری", "Utilise le GPU pour économiser la batterie"),
                        checked = hwAccelEnabled,
                        onCheckedChange = {
                            hwAccelEnabled = it
                            prefs.edit().putBoolean(KEY_HW_ACCEL, it).apply()
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Audio Soft Decode (mytv engine)
                    SettingsSwitchRow(
                        title = tr("معالجة الصوت البرمجية (Audio Soft Decode)", "Audio Software Decoding", "Yazılımsal Ses Kod Çözme", "رمزگشایی نرم‌افزاری صدا", "Décodage audio logiciel"),
                        subtitle = tr("حل مشكلة القنوات التي تعمل بدون صوت والتوافق مع ترميزات AAC-LATM و AC3", "Fixes channels with no audio and improves AAC-LATM / AC3 compatibility", "Sessiz kanalları düzeltir ve AAC-LATM / AC3 uyumluluğunu artırır", "مشکل کانال‌های بدون صدا را رفع کرده و سازگاری صوتی را بهبود می‌بخشد", "Corrige les flux muets et améliore la compatibilité audio"),
                        checked = audioSoftDecode,
                        onCheckedChange = {
                            audioSoftDecode = it
                            PlayerPreferences.setAudioSoftDecodeEnabled(context, it)
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Skip Multiple Frames on Same VSync (mytv engine)
                    SettingsSwitchRow(
                        title = tr("تجاوز الإطارات الزائدة المتزامنة (VSync Frame Skip)", "Skip Redundant VSync Frames", "Aynı VSync'te Kare Atla", "رد کردن فریم‌های تکراری در همان VSync", "Ignorer les images VSync redondantes"),
                        subtitle = tr("إسقاط الإطارات المكررة في نفس تردد الشاشة لمنع التقطيع والاهتزاز", "Drops redundant frames on display refresh to eliminate stutter and jitter", "Titreşimi ve takılmayı önlemek için gereksiz kareleri atlar", "برای از بین بردن لرزش تصویر، فریم‌های اضافی را رد می‌کند", "Élimine les saccades et synchronise l'affichage"),
                        checked = skipFramesOnVsync,
                        onCheckedChange = {
                            skipFramesOnVsync = it
                            PlayerPreferences.setSkipFramesOnSameVSyncEnabled(context, it)
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Auto-Switch Server on Stream Error
                    SettingsSwitchRow(
                        title = tr("التبديل التلقائي عند انقطاع البث (Auto Server Failover)", "Auto-Switch Server on Error", "Hata Durumunda Otomatik Sunucu Değiştir", "تغییر خودکار سرور هنگام قطع جریان", "Changement automatique de serveur en cas d'erreur"),
                        subtitle = tr("التحويل التلقائي الفوري إلى سيرفر بديل للقناة في حال توقف أو انقطاع البث", "Instantly switches to a backup server if the current stream drops or stalls", "Yayın durursa veya takılırsa anında yedek sunucuya geçer", "در صورت قطعی یا ایستادن پخش، سریعاً به سرور پشتیبان متصل می‌شود", "Bascule instantanément sur un serveur de secours en cas de coupure"),
                        checked = autoSwitchServerOnError,
                        onCheckedChange = {
                            autoSwitchServerOnError = it
                            PlayerPreferences.setAutoSwitchServerOnErrorEnabled(context, it)
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Buffer Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr("حجم التخزين المؤقت (مانع التقطيع):", "Buffer Size (Anti-Buffering):", "Önbellek Boyutu:", "اندازه بافر:", "Taille de mise en mémoire tampon :"),
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp),
                            color = BeoutColors.TextPrimary
                        )
                        if (slowInternetMode) {
                            Text(
                                text = tr("(مثبت تلقائياً على 90 ثانية مع وضع النت الضعيف)", "(Auto-locked to 90s by Booster)", "(Booster ile 90s kilitli)", "(قفل روی ۹۰ ثانیه با بوستر)", "(Fixé à 90s via Booster)"),
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }
                    }
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val buffers = listOf(
                            15 to tr("15 ثانية (سريع)", "15s (Fast)", "15 sn (Hızlı)", "۱۵ ثانیه (سریع)", "15s (Rapide)"),
                            30 to tr("30 ثانية (قياسي)", "30s (Standard)", "30 sn (Standart)", "۳۰ ثانیه (استاندارد)", "30s (Standard)"),
                            60 to tr("60 ثانية (للنت الضعيف)", "60s (Slow Internet)", "60 sn (Yavaş İnternet)", "۶۰ ثانیه (اینترنت ضعیف)", "60s (Connexion lente)"),
                            90 to tr("90 ثانية (للنت شديد الضعف)", "90s (Ultra Slow Net)", "90 sn (Çok Yavaş Net)", "۹۰ ثانیه (اینترنت بسیار ضعیف)", "90s (Très bas débit)")
                        )
                        items(buffers) { (secs, label) ->
                            ChoiceChip(
                                text = label,
                                selected = bufferSeconds == secs,
                                onClick = {
                                    bufferSeconds = secs
                                    prefs.edit().putInt(KEY_BUFFER_SIZE, secs).apply()
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Load Timeout Seconds (mytv engine)
                    Text(tr("مهلة انتظار استجابة البث (Load Timeout):", "Stream Load Timeout:", "Yayın Yükleme Zaman Aşımı:", "مهلت بارگذاری جریان:", "Délai de chargement du flux :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val timeouts = listOf(
                            10 to tr("10 ثوانٍ (سريع)", "10s (Fast)", "10 sn (Hızlı)", "۱۰ ثانیه (سریع)", "10s (Rapide)"),
                            15 to tr("15 ثانية (مستحسن)", "15s (Recommended)", "15 sn (Önerilen)", "۱۵ ثانیه (پیشنهادی)", "15s (Recommandé)"),
                            25 to tr("25 ثانية (للشبكات البطيئة)", "25s (Slow Networks)", "25 sn (Yavaş Ağlar)", "۲۵ ثانیه (شبکه‌های ضعیف)", "25s (Réseaux lents)"),
                            40 to tr("40 ثانية (أقصى انتظار)", "40s (Max Timeout)", "40 sn (Maksimum Bekleme)", "۴۰ ثانیه (حداکثر انتظار)", "40s (Délai maximal)")
                        )
                        items(timeouts) { (secs, label) ->
                            ChoiceChip(
                                text = label,
                                selected = loadTimeoutSecs == secs,
                                onClick = {
                                    loadTimeoutSecs = secs
                                    PlayerPreferences.setLoadTimeoutSeconds(context, secs)
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Render Engine Mode (SurfaceView vs TextureView from mytv)
                    Text(tr("محرك معالجة ورسم الفيديو (Render Engine):", "Video Rendering Engine:", "Video İşleme Motoru:", "موتور رندر ویدیو:", "Moteur de rendu vidéo :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val engines = listOf(
                            "SurfaceView" to tr("SurfaceView (أعلى كفاءة وسلاسة)", "SurfaceView (Best Performance)", "SurfaceView (En İyi Performans)", "SurfaceView (بیشترین کارایی)", "SurfaceView (Haute performance)"),
                            "TextureView" to tr("TextureView (توافقية قصوى)", "TextureView (Max Compatibility)", "TextureView (Maksimum Uyumluluk)", "TextureView (حداکثر سازگاری)", "TextureView (Compatibilité max)")
                        )
                        items(engines) { (eKey, eLabel) ->
                            ChoiceChip(
                                text = eLabel,
                                selected = renderMode == eKey,
                                onClick = {
                                    renderMode = eKey
                                    PlayerPreferences.setRenderMode(context, eKey)
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Show Stream Technical Stats (Stats for Nerds from mytv)
                    SettingsSwitchRow(
                        title = tr("عرض إحصائيات البث التقنية (Stats for Nerds)", "Stream Diagnostics (Stats for Nerds)", "Yayın Teknik Bilgileri (Stats for Nerds)", "اطلاعات فنی پخش (Stats for Nerds)", "Statistiques techniques détaillées"),
                        subtitle = tr("إظهار لوحة شفافة داخل المشغل تعرض معدل البت الفعلي (Bitrate) والدقة والفريمات وترميزات الصوت والفيديو", "Displays a live overlay showing real-time bitrate, resolution, framerate, codecs, and buffer health", "Gerçek zamanlı bit hızı, çözünürlük, kare hızı ve codec bilgilerini gösterir", "نمایش پنل شفاف اطلاعات زنده شامل بیت‌ریت، رزولوشن، فریم‌ریت و کدک‌ها", "Affiche le débit binaire, la résolution, les codecs et l'état du tampon en direct"),
                        checked = showPlayerStats,
                        onCheckedChange = {
                            showPlayerStats = it
                            PlayerPreferences.setShowPlayerStatsEnabled(context, it)
                        }
                    )

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Aspect Ratio
                    Text(tr("أبعاد الشاشة الافتراضية:", "Default Aspect Ratio:", "Varsayılan En-Boy Oranı:", "نسبت ابعاد صفحه:", "Format d'image par défaut :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val ratios = listOf(
                            "Fit" to tr("ملاءمة الشاشة (16:9)", "Fit (16:9)", "Ekrana Uydur (16:9)", "تطبیق (16:9)", "Ajuster (16:9)"),
                            "Fill" to tr("ملء الشاشة", "Fill Screen", "Ekranı Doldur", "تمام‌صفحه", "Plein écran"),
                            "Zoom" to tr("تكبير وقص", "Zoom / Crop", "Yakınlaştır ve Kırp", "بزرگ‌نمایی و برش", "Zoom / Rognage"),
                            "4:3" to tr("النسبة الكلاسيكية (4:3)", "Classic (4:3)", "Klasik (4:3)", "کلاسیک (4:3)", "Classique (4:3)"),
                            "Wide" to tr("الوضع السينمائي (2.35:1)", "Cinematic (2.35:1)", "Sinematik (2.35:1)", "سینمایی (2.35:1)", "Cinéma (2.35:1)")
                        )
                        items(ratios) { (rKey, rLabel) ->
                            ChoiceChip(
                                text = rLabel,
                                selected = aspectRatioMode == rKey,
                                onClick = {
                                    aspectRatioMode = rKey
                                    PlayerPreferences.setAspectRatioMode(context, rKey)
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Subtitle Size
                    Text(tr("حجم خط الترجمة:", "Subtitle Font Size:", "Altyazı Boyutu:", "اندازه قلم زیرنویس:", "Taille des sous-titres :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubtitleSizePreset.entries) { preset ->
                            ChoiceChip(
                                text = preset.displayNameAr,
                                selected = subtitleSizePreset == preset,
                                onClick = {
                                    subtitleSizePreset = preset
                                    PlayerPreferences.setSubtitleSizePreset(context, preset)
                                }
                            )
                        }
                    }

                    Divider(color = BeoutColors.Divider, modifier = Modifier.padding(vertical = BeoutDimensions.SpaceS))

                    // Subtitle Style & Appearance
                    Text(tr("مظهر ونمط الترجمة السينمائية:", "Subtitle Appearance & Style:", "Altyazı Görünümü:", "سبک و ظاهر زیرنویس:", "Style des sous-titres :"), style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp), color = BeoutColors.TextPrimary)
                    Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubtitleStylePreset.entries) { preset ->
                            ChoiceChip(
                                text = preset.displayNameAr,
                                selected = subtitleStylePreset == preset,
                                onClick = {
                                    subtitleStylePreset = preset
                                    PlayerPreferences.setSubtitleStylePreset(context, preset)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    // Subtitle Live Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .background(Color(0xFF0A0E14))
                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewColor = when (subtitleStylePreset) {
                            SubtitleStylePreset.YELLOW_SHADOW -> Color(0xFFFFD700)
                            SubtitleStylePreset.AMBER_GOLD -> Color(0xFFE5A93C)
                            else -> Color.White
                        }
                        val previewBg = if (subtitleStylePreset == SubtitleStylePreset.OBSIDIAN_BOX) Color(0xB3101721) else Color.Transparent
                        Text(
                            text = "معاينة حية: مرحباً بكم في BeoutTV",
                            color = previewColor,
                            fontSize = subtitleSizePreset.sizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(previewBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ================= 6. DOWNLOADED SHORT DRAMAS =================
            item {
                SettingsSectionTitle("دراما ريلز المحملة", Icons.Rounded.DownloadDone)

                val downloadedSeriesList by com.beouttv.app.data.repository.ShortDramaDownloadManager.downloadedSeriesList.collectAsState()
                val totalDownloadedEps = downloadedSeriesList.sumOf { it.downloadedCount }
                val totalBytes = downloadedSeriesList.sumOf { it.totalBytes }
                val totalMb = totalBytes / (1024f * 1024f)
                val sizeText = if (totalMb > 1024) String.format("%.2f GB", totalMb / 1024f) else String.format("%.0f MB", totalMb)

                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDownloadedDramaDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadForOffline,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مكتبة المسلسلات المحملة (أوفلاين)",
                                    style = BeoutTextStyles.CardTitle.copy(fontWeight = FontWeight.Bold),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (downloadedSeriesList.isEmpty()) "لا توجد مسلسلات محملة حالياً (انقر للتفاصيل)"
                                           else "${downloadedSeriesList.size} مسلسلات • $totalDownloadedEps حلقة • $sizeText",
                                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                                    color = if (downloadedSeriesList.isEmpty()) BeoutColors.TextSecondary else BeoutColors.BrightBlue
                                )
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = BeoutColors.TextDisabled)
                    }
                }
            }

            // ================= 7. STORAGE & CACHE MANAGER =================
            item {
                SettingsSectionTitle(S.storage(), Icons.Rounded.Storage)

                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("الذاكرة المؤقتة للبوسترات والبيانات", "Posters & Data Cache", "Görseller ve Veri Önbelleği", "حافظه پنهان پوسترها و داده‌ها", "Cache des affiches et données"), style = BeoutTextStyles.CardTitle, color = BeoutColors.TextPrimary)
                            Text(
                                text = tr("الحجم الحالي: $cacheSizeText", "Current Size: $cacheSizeText", "Mevcut Boyut: $cacheSizeText", "حجم فعلی: $cacheSizeText", "Taille actuelle : $cacheSizeText"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }

                        Button(
                            onClick = {
                                clearAppCache(context)
                                cacheSizeText = calculateCacheSize(context)
                                Toast.makeText(context, tr("تم تنظيف الذاكرة المؤقتة بنجاح", "Cache cleared successfully", "Önbellek temizlendi", "حافظه پنهان پاک شد", "Cache vidé avec succès"), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.ElevatedSurface),
                            shape = BeoutShapes.Small,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.Divider)
                        ) {
                            Text(S.clearCache(), style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = BeoutColors.TextPrimary)
                        }
                    }
                }
            }


            // ================= 7. ABOUT APP & PRIVACY =================
            item {
                SettingsSectionTitle(S.about(), Icons.Rounded.Info)

                SettingsCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(BeoutShapes.Medium)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(BeoutColors.PrimaryBlue, Color(0xFF0D47A1))
                                        )
                                    )
                                    .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LiveTv,
                                    contentDescription = "BeoutTV Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("BEOUT TV", style = BeoutTextStyles.CardTitle.copy(fontWeight = FontWeight.Bold), color = BeoutColors.TextPrimary)
                                Text(tr("الإصدار $appVerName (Build $appVerCode) السينمائي", "Version $appVerName (Build $appVerCode) Cinema", "Sürüm $appVerName (Build $appVerCode) Sinema", "نسخه $appVerName سینمایی", "Version $appVerName Cinéma"), style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp), color = BeoutColors.TextSecondary)
                            }
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))
                        Divider(color = BeoutColors.Divider)
                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        Text(
                            text = tr(
                                ar = "تطبيق بدون خوادم تتبع خارجية وبدون إعلانات. جميع ملفات التفضيلات وسجل البحث تُحفظ محلياً على جهازك لضمان أعلى درجات الخصوصية والأمان.",
                                en = "Ad-free, zero tracking servers. All preferences and search history are saved locally on your device for absolute privacy.",
                                tr = "Reklamsız ve takipsiz. Tüm tercihleriniz ve geçmişiniz gizliliğiniz için cihazınızda saklanır.",
                                fa = "بدون تبلیغات و بدون ردیابی. تمام تنظیمات و سوابق شما برای حفظ حریم خصوصی روی دستگاه شما ذخیره می‌شود.",
                                fr = "Sans publicité, zéro serveur de suivi. Vos préférences sont stockées localement pour une confidentialité totale."
                            ),
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = BeoutColors.TextSecondary
                        )
                    }
                }
            }

            // ================= 7. DEVELOPER & SOFTWARE COMPANY (IQonic) =================
            item {
                SettingsSectionTitle(tr("الشركة المطورة", "Software Developer", "Geliştirici Şirket", "شرکت توسعه‌دهنده", "Société de développement"), Icons.Rounded.Code)

                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IQonic",
                                    style = BeoutTextStyles.CardTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.BrightBlue)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Software",
                                        style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tr(
                                    ar = "شركة برمجية متخصصة في تطوير الأنظمة الرقمية، تطبيقات البث المباشر، وتقنيات الذكاء الاصطناعي وهندسة البرمجيات المتقدمة.",
                                    en = "Software engineering company specializing in digital systems, media streaming apps, AI technologies, and high-performance software architecture.",
                                    tr = "Dijital sistemler, gelişmiş yayın uygulamaları ve teknoloji çözümleri geliştiren yazılım şirketi.",
                                    fa = "شرکت نرم‌افزاری متخصص در توسعه سیستم‌های دیجیتال و برنامه‌های پخش زنده.",
                                    fr = "Entreprise de logiciels spécialisée dans les solutions numériques et de streaming."
                                ),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(BeoutDimensions.SpaceM))
                    Divider(color = BeoutColors.Divider)
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    // Telegram Channel Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.25f), BeoutShapes.Small)
                            .clickable { openTelegramLink(context, "https://t.me/lion4kapp") }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "t.me/lion4kapp",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = BeoutColors.TextPrimary
                            )
                            Text(
                                text = tr("قناة شركة IQonic البرمجية الرسمية على التلجرام", "Official IQonic Software Telegram channel", "Resmi IQonic Telegram kanalı", "کانال رسمی تلگرام IQonic", "Chaîne Telegram officielle IQonic"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.5.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showXtreamSettingsDialog) {
        XtreamManagerDialog(onDismiss = { showXtreamSettingsDialog = false })
    }

    if (showDownloadedDramaDialog) {
        DownloadedDramaDialog(
            onDismiss = { showDownloadedDramaDialog = false },
            onPlaySeries = { series, epNum ->
                showDownloadedDramaDialog = false
                com.beouttv.app.ui.screens.shortdrama.ShortDramaViewModel.pendingOfflineSeries = Pair(series, epNum)
                onNavigateToShortDrama()
            }
        )
    }
}

// ---------------- Helper Components & Functions ----------------

@Composable
private fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 4.dp)
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .padding(BeoutDimensions.SpaceM)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Small)
            .tvFocusable(shape = BeoutShapes.Small, onEnter = { onCheckedChange(!checked) })
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp), color = BeoutColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp), color = BeoutColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BeoutColors.BrightBlue,
                uncheckedThumbColor = BeoutColors.TextSecondary,
                uncheckedTrackColor = BeoutColors.ElevatedSurface
            )
        )
    }
}

@Composable
private fun GhostPrivilegeItem(title: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = BeoutColors.BrightBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
            color = BeoutColors.TextPrimary
        )
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(BeoutShapes.Small)
            .tvFocusable(shape = BeoutShapes.Small, onEnter = onClick)
            .background(if (selected) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface)
            .border(1.dp, if (selected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = BeoutTextStyles.SecondaryInfo.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) Color.White else BeoutColors.TextSecondary
        )
    }
}

private fun openTelegramLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
    }
}

private fun getAppVersionInfo(context: Context): Pair<String, Int> {
    return try {
        val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val vName = pInfo.versionName ?: "1.0"
        val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode
        }
        Pair(vName, vCode)
    } catch (_: Throwable) {
        Pair("1.0", 1)
    }
}

private fun openTelegramChannel(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_SUPPORT_URL))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "يرجى فتح الرابط: $TELEGRAM_SUPPORT_URL", Toast.LENGTH_LONG).show()
    }
}

private fun calculateCacheSize(context: Context): String {
    return try {
        var size: Long = 0
        context.cacheDir?.let { size += getFolderSize(it) }
        context.externalCacheDir?.let { size += getFolderSize(it) }
        val mb = size.toDouble() / (1024 * 1024)
        String.format(java.util.Locale.US, "%.1f MB", mb)
    } catch (e: Exception) {
        "0.0 MB"
    }
}

private fun getFolderSize(dir: File): Long {
    var size: Long = 0
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getFolderSize(file) else file.length()
    }
    return size
}

private fun clearAppCache(context: Context) {
    try {
        com.beouttv.app.data.proto.ProtobufDiskCache.clear()
        context.cacheDir?.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
