package com.beouttv.app.data.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(
    val code: String,
    val title: String,
    val nativeName: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "العربية", true),
    ENGLISH("en", "English", "English", false),
    TURKISH("tr", "التركية", "Türkçe", false),
    PERSIAN("fa", "الفارسية", "فارسی", true),
    FRENCH("fr", "الفرنسية", "Français", false)
}

object LanguageManager {

    private const val PREFS_LANG = "beout_lang_prefs"
    private const val KEY_LANG = "selected_app_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_LANG, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANG, "ar") ?: "ar"
        _currentLanguage.value = AppLanguage.entries.find { it.code == code } ?: AppLanguage.ARABIC
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        val prefs = context.getSharedPreferences(PREFS_LANG, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.code).apply()
    }
}

/**
 * Smart, minimal reactive translation helper.
 * Re-evaluates instantly when language changes.
 */
fun tr(
    ar: String,
    en: String,
    tr: String = en,
    fa: String = ar,
    fr: String = en
): String {
    return when (LanguageManager.currentLanguage.value) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
        AppLanguage.TURKISH -> tr
        AppLanguage.PERSIAN -> fa
        AppLanguage.FRENCH -> fr
    }
}

/**
 * Global App Translation Keys Dictionary across all screens
 */
object S {
    // Navigation & Primary Tabs
    fun home(): String = tr("الرئيسية", "Home", "Ana Sayfa", "خانه", "Accueil")
    fun liveTv(): String = tr("البث المباشر", "Live TV", "Canlı TV", "پخش زنده", "TV en direct")
    fun movies(): String = tr("السينما والأفلام", "Cinema & Movies", "Filmler", "فیلم‌ها", "Films & Cinéma")
    fun search(): String = tr("البحث الذكي", "Smart Search", "Akıllı Arama", "جستجوی هوشمند", "Recherche")
    fun more(): String = tr("المزيد", "More", "Daha Fazla", "بیشتر", "Plus")
    fun sports(): String = tr("المباريات", "Sports", "Maçlar", "مسابقات", "Sports")
    fun settings(): String = tr("الإعدادات", "Settings", "Ayarlar", "تنظیمات", "Paramètres")
    fun favorites(): String = tr("المفضلة وقائمتي", "Favorites & Watchlist", "Favorilerim", "علاقه‌مندی‌ها", "Favoris")

    // Common Controls & Headers
    fun seeAll(): String = tr("عرض الكل", "See All", "Tümünü Gör", "مشاهده همه", "Voir tout")
    fun back(): String = tr("رجوع", "Back", "Geri", "بازگشت", "Retour")
    fun play(): String = tr("تشغيل الآن", "Play Now", "Şimdi Oynat", "پخش اکنون", "Regarder")
    fun quality(): String = tr("الجودة", "Quality", "Kalite", "کیفیت", "Qualité")
    fun all(): String = tr("الكل", "All", "Tümü", "همه", "Tous")
    fun clear(): String = tr("مسح", "Clear", "Temizle", "پاک کردن", "Effacer")
    fun searchPlaceholder(): String = tr("ابحث عن فيلم، ممثل، قسم، أو قناة...", "Search movies, actors, channels...", "Film, oyuncu veya kanal ara...", "جستجوی فیلم، بازیگر، شبکه...", "Rechercher films, acteurs, chaînes...")

    // Home & Cinema Headers
    fun featuredMovies(): String = tr("أفلام مميزة وشائعة", "Featured Movies", "Öne Çıkan Filmler", "فیلم‌های ویژه", "Films en vedette")
    fun latestCinema(): String = tr("أحدث السينما والأفلام", "Latest Cinema", "Son Filmler", "جدیدترین فیلم‌ها", "Derniers films")
    fun topChannels(): String = tr("أبرز قنوات البث المباشر", "Top Live Channels", "Popüler Canlı Kanallar", "شبکه‌های برتر زنده", "Principales chaînes")
    fun todayMatches(): String = tr("جدول مباريات اليوم", "Today's Matches", "Günün Maçları", "مسابقات امروز", "Matchs du jour")

    // Movie Categories
    fun foreignMovies(): String = tr("أفلام أجنبي", "Foreign Movies", "Yabancı Filmler", "فیلم‌های خارجی", "Films étrangers")
    fun arabicMovies(): String = tr("أفلام عربي", "Arabic Movies", "Arap Filmleri", "فیلم‌های عربی", "Films arabes")
    fun animeMovies(): String = tr("أفلام أنمي وكرتون", "Anime & Cartoons", "Anime ve Çizgi Film", "انیمه و کارتون", "Animés et dessins animés")
    fun indianMovies(): String = tr("أفلام هندية", "Indian Movies", "Hint Filmleri", "فیلم‌های هندی", "Films indiens")
    fun asianMovies(): String = tr("أفلام آسيوية", "Asian Movies", "Asya Filmleri", "فیلم‌های آسیایی", "Films asiatiques")
    fun turkishMovies(): String = tr("أفلام تركية", "Turkish Movies", "Türk Filmleri", "فیلم‌های ترکی", "Films turcs")
    fun action(): String = tr("أكشن", "Action", "Aksiyon", "اکشن", "Action")
    fun comedy(): String = tr("كوميدي", "Comedy", "Komedi", "کمدی", "Comédie")
    fun horror(): String = tr("رعب وغموض", "Horror & Mystery", "Korku ve Gizem", "ترسناک و رازآلود", "Horreur et mystère")
    fun drama(): String = tr("دراما", "Drama", "Dram", "درام", "Drame")
    fun plays(): String = tr("مسرحيات عربية", "Arabic Plays", "Arap Tiyatrosu", "نمایش‌های عربی", "Pièces de théâtre")

    // Movie Details
    fun story(): String = tr("قصة العمل", "Story & Plot", "Hikaye", "داستان فیلم", "Synopsis")
    fun director(): String = tr("الإخراج", "Director", "Yönetmen", "کارگردان", "Réalisateur")
    fun cast(): String = tr("طاقم العمل والبطولة", "Cast & Stars", "Oyuncular", "بازیگران", "Acteurs")
    fun similarMovies(): String = tr("أفلام مقترحة مشابهة", "Similar Movies", "Benzer Filmler", "فیلم‌های مشابه", "Films similaires")
    fun details(): String = tr("التفاصيل", "Details", "Detaylar", "جزئیات", "Détails")

    // Settings & Account & Support
    fun language(): String = tr("لغة التطبيق", "App Language", "Uygulama Dili", "زبان برنامه", "Langue de l'application")
    fun ghostAccount(): String = tr("الملف الشخصي والحساب", "Account & Profile", "Hesap ve Profil", "حساب و نمایه", "Compte et profil")
    fun notifications(): String = tr("تنبيهات المباريات المحلية", "Match Notifications", "Maç Bildirimleri", "اعلان مسابقات", "Notifications de Match")
    fun support(): String = tr("قناة الدعم والمجتمع", "Support & Community", "Destek ve Topluluk", "پشتیبانی و تلگرام", "Support & Communauté")
    fun storage(): String = tr("إدارة الذاكرة والتخزين", "Storage & Cache", "Depolama ve Önbellek", "مدیریت حافظه", "Stockage & Cache")
    fun clearCache(): String = tr("مسح الذاكرة", "Clear Cache", "Önbelleği Temizle", "پاک کردن حافظه", "Vider le cache")
    fun safeMode(): String = tr("الحماية الأسرية والأخلاقية", "Family & Content Safety", "Aile Koruması", "محافظت از خانواده", "Sécurité Familiale")
    fun about(): String = tr("عن التطبيق", "About App", "Uygulama Hakkında", "درباره برنامه", "À propos")
    fun playerSettings(): String = tr("إعدادات المشغل والبث", "Player & Stream Settings", "Oynatıcı Ayarları", "تنظیمات پخش‌کننده", "Paramètres du lecteur")

    // Sports & Matches Schedule & Standings
    fun matches(): String = tr("المباريات", "Matches", "Maçlar", "مسابقات", "Matchs")
    fun matchesSchedule(): String = tr("جدول المباريات", "Match Schedule", "Maç Programı", "برنامه مسابقات", "Calendrier des matchs")
    fun standingsTable(): String = tr("جدول الترتيب", "League Standings", "Puan Durumu", "جدول رده‌بندی", "Classement")
    fun yesterday(): String = tr("أمس", "Yesterday", "Dün", "دیروز", "Hier")
    fun today(): String = tr("اليوم", "Today", "Bugün", "امروز", "Aujourd'hui")
    fun tomorrow(): String = tr("غداً", "Tomorrow", "Yarın", "فردا", "Demain")
    fun liveNow(): String = tr("مباشر الآن", "Live Now", "Canlı", "هم‌اکنون زنده", "En direct")
    fun upcoming(): String = tr("القادمة", "Upcoming", "Yaklaşan", "آینده", "À venir")
    fun finished(): String = tr("المنتهية", "Finished", "Biten", "پایان‌یافته", "Terminé")
    fun channel(): String = tr("القناة", "Channel", "Kanal", "شبکه", "Chaîne")
    fun commentator(): String = tr("المعلق", "Commentator", "Spiker", "گزارشگر", "Commentateur")
    fun watchLive(): String = tr("مشاهدة البث", "Watch Live", "Canlı İzle", "تماشای زنده", "Regarder en direct")
    fun setReminder(): String = tr("تنبيه", "Remind", "Hatırlat", "یادآوری", "Rappeler")
    fun reminderSet(): String = tr("تم ضبط التنبيه للمباراة بنجاح", "Match reminder set successfully", "Maç hatırlatıcısı ayarlandı", "یادآوری مسابقه با موفقیت تنظیم شد", "Rappel de match défini")
    fun rank(): String = tr("الترتيب", "Rank", "Sıra", "رتبه", "Rang")
    fun team(): String = tr("الفريق", "Team", "Takım", "تیم", "Équipe")
    fun played(): String = tr("لعب", "P", "O", "بازی", "J")
    fun won(): String = tr("ف", "W", "G", "برد", "V")
    fun draw(): String = tr("ت", "D", "B", "مساوی", "N")
    fun lost(): String = tr("خ", "L", "M", "باخت", "D")
    fun goalDiff(): String = tr("فارق", "GD", "AV", "تفاضل", "Diff")
    fun points(): String = tr("نقاط", "PTS", "P", "امتیاز", "PTS")
    fun championsLeagueZone(): String = tr("منطقة دوري الأبطال", "Champions League Zone", "Şampiyonlar Ligi Bölgesi", "منطقه لیگ قهرمانان", "Zone Ligue des Champions")

    // Empty States
    fun noResults(): String = tr("لا توجد نتائج", "No Results", "Sonuç Bulunamadı", "نتیجه‌ای یافت نشد", "Aucun résultat")
    fun emptyFavorites(): String = tr("قائمتك المفضلة فارغة حالياً", "Your favorites list is empty", "Favori listeniz boş", "لیست علاقه‌مندی‌ها خالی است", "Votre liste de favoris est vide")
    fun explore(): String = tr("استكشف الأفلام والقنوات", "Explore Movies & Channels", "Filmleri ve Kanalları Keşfet", "کاوش فیلم‌ها و شبکه‌ها", "Explorer les films et chaînes")
}
