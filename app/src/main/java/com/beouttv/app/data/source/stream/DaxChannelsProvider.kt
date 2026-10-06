package com.beouttv.app.data.source.stream

import android.content.Context
import com.beouttv.app.data.favorites.FavoritesManager
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.StreamServer
import org.json.JSONObject
import java.io.File

/**
 * High-performance provider for Dax TV channels and multi-quality stream servers.
 * Loads dynamically from device storage (/storage/emulated/0/Dax/) if present,
 * with a resilient zero-lag precompiled offline catalog as fallback.
 */
object DaxChannelsProvider {

    private const val DAX_DIR = "/storage/emulated/0/Dax"
    private const val CHANNELS_FILE = "$DAX_DIR/decrypted_channels.json"
    private const val URLS_FILE = "$DAX_DIR/decrypted_urls.json"

    // Default High-Res Branding Logos
    private const val LOGO_BEIN = "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572"
    private const val LOGO_ALWAN = "https://lh3.googleusercontent.com/-2j2pA9PBQiQ/aaiQ7Y3oI4I/AAAAAAAABQ4/qZ-F0w2R9Gcv7uW9d8-gCNcBGAsYHQ/w250/0.jpg=w250"
    private const val LOGO_DUBAI_SPORTS = "https://upload.wikimedia.org/wikipedia/ar/c/c0/Dubai_Sports_logo.png"
    private const val LOGO_MBC = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/MBC_Group_Logo.svg/512px-MBC_Group_Logo.svg.png"
    private const val LOGO_SHAHID = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Shahid_VIP_Logo.svg/512px-Shahid_VIP_Logo.svg.png"
    private const val LOGO_REAL = "https://upload.wikimedia.org/wikipedia/en/thumb/5/56/Real_Madrid_CF.svg/512px-Real_Madrid_CF.svg.png"
    private const val LOGO_BARCA = "https://upload.wikimedia.org/wikipedia/en/thumb/4/47/FC_Barcelona_%28crest%29.svg/512px-FC_Barcelona_%28crest%29.svg.png"
    private const val LOGO_MANU = "https://upload.wikimedia.org/wikipedia/en/thumb/7/7a/Manchester_United_FC_crest.svg/512px-Manchester_United_FC_crest.svg.png"

    fun getChannels(context: Context): List<Channel> {
        val chFile = File(CHANNELS_FILE)
        val urlFile = File(URLS_FILE)

        if (chFile.exists() && urlFile.exists()) {
            try {
                val parsed = parseDaxJsonFiles(context, chFile, urlFile)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {}
        }

        return getPrecompiledFallbackChannels(context)
    }

    private fun parseDaxJsonFiles(context: Context, chFile: File, urlFile: File): List<Channel> {
        val urlsJsonStr = urlFile.readText(Charsets.UTF_8)
        val chJsonStr = chFile.readText(Charsets.UTF_8)

        val urlsRoot = JSONObject(urlsJsonStr)
        val urlsArr = urlsRoot.optJSONArray("urls") ?: return emptyList()

        // Map stream_id -> List<StreamServer>
        val streamMap = mutableMapOf<String, List<StreamServer>>()
        for (i in 0 until urlsArr.length()) {
            val uObj = urlsArr.getJSONObject(i)
            val sid = uObj.optString("stream_id", "")
            val srvArr = uObj.optJSONArray("servers")
            val srvList = mutableListOf<StreamServer>()
            if (srvArr != null) {
                for (j in 0 until srvArr.length()) {
                    val s = srvArr.getJSONObject(j)
                    val rawUrl = s.optString("url", "").trim()
                    if (rawUrl.isBlank()) continue
                    val cleanUrl = sanitizeStreamUrl(rawUrl)
                    val sName = s.optString("name", "سيرفر ${j + 1}").trim()
                    val qual = when {
                        sName.contains("1080") || sName.contains("FHD", true) -> "1080p"
                        sName.contains("720") || sName.contains("HD", true) -> "720p"
                        sName.contains("480") || sName.contains("SD", true) -> "480p"
                        sName.contains("360") || sName.contains("Low", true) -> "360p"
                        sName.contains("240") -> "240p"
                        else -> "1080p"
                    }
                    srvList.add(
                        StreamServer(
                            id = "${sid}_srv_$j",
                            name = formatServerLabel(sName, j),
                            quality = qual,
                            url = cleanUrl,
                            isDirect = true
                        )
                    )
                }
            }
            if (sid.isNotBlank() && srvList.isNotEmpty()) {
                streamMap[sid] = srvList
            }
        }

        val chRoot = JSONObject(chJsonStr)
        val chArr = chRoot.optJSONArray("channels") ?: return emptyList()
        val result = mutableListOf<Channel>()

        for (i in 0 until chArr.length()) {
            val chObj = chArr.getJSONObject(i)
            val rawName = chObj.optString("name", "").trim()
            val pkg = chObj.optString("package_id", "").trim()
            val sid = chObj.optString("stream_id", "").trim()
            val srvs = streamMap[sid] ?: emptyList()

            // Completely skip all purged packages and channels taken from DAX
            val lowerRaw = rawName.lowercase()
            val lowerPkg = pkg.lowercase()
            if (pkg == "Beoutqmultiidax" || pkg == "Beinentermenmentetyacine" ||
                pkg == "BeinNMMULTIQUALITYDAXTV" || lowerPkg.contains("nm") ||
                lowerRaw.contains(" nm") || lowerRaw.contains("nm ") || lowerRaw.contains("bein sport nm") ||
                lowerRaw.contains("ابو ظبي") || lowerRaw.contains("أبوظبي") || lowerRaw.contains("abu dhabi") ||
                lowerRaw.contains("beout") || lowerRaw.contains("osn")
            ) {
                continue
            }

            if (srvs.isEmpty()) continue

            // Clean Arabic categorization algorithm
            val category = resolveArabicCategory(pkg, rawName)
            val displayName = formatChannelName(rawName, pkg)
            val logo = resolveLogo(rawName, pkg)
            val chId = "dax_${pkg}_${sid}_$i"

            result.add(
                Channel(
                    id = chId,
                    name = displayName,
                    logoUrl = logo,
                    category = category,
                    isLive = true,
                    currentProgram = "بث مباشر فائق الجودة",
                    isFavorite = FavoritesManager.isChannelFavorite(context, chId),
                    streamUrl = srvs.first().url,
                    userAgent = OstoraApiResolver.DEFAULT_USER_AGENT,
                    referer = OstoraApiResolver.DEFAULT_REFERER,
                    servers = srvs
                )
            )
        }

        return result
    }

    private fun resolveArabicCategory(pkg: String, name: String): String {
        return when {
            pkg == "ARABSPORTTTT" -> "قنوات رياضية عربية"
            pkg == "Mbcpackagemulti" -> "باقة قنوات MBC"
            pkg == "Bundesliagueshahid" -> "قنوات الدوري الألماني وشاهد"
            pkg == "StarzPlayitalianleaguemulti" -> "قنوات STARZPLAY الرياضية"
            pkg == "Thamanyasaudileaguemulti" -> "قنوات ثمانية"
            pkg == "BEINSPORTLOWINTERNETYACINETV" -> {
                if (name.contains("TOD", true)) "قنوات المباريات والبث المباشر"
                else "قنوات beIN Sports المتعددة"
            }
            pkg.contains("Beinmax", ignoreCase = true) || pkg == "Jei2y2f1h9" -> "قنوات beIN MAX"
            pkg in listOf("Uwueyye", "Privatestreamcat", "Daxtvsports2") -> "قنوات البث الخاص"
            else -> "قنوات البث المباشر"
        }
    }

    private fun formatChannelName(name: String, pkg: String): String {
        return when {
            name.equals("RealMadridTV", true) -> "قناة ريال مدريد HD"
            name.equals("BarcelonaTV", true) -> "قناة برشلونة HD"
            name.equals("ManUnitedTV", true) -> "قناة مانشستر يونايتد HD"
            name.equals("العراقية الرياضية", true) -> "العراقية الرياضية HD"
            name.equals("العراقية السريانية", true) -> "العراقية السريانية HD"
            name.equals("DUBAISPORTS1", true) -> "دبي الرياضية 1 HD"
            name.equals("DUBAISPORTS2", true) -> "دبي الرياضية 2 HD"
            name.startsWith("ShahidSport", true) -> {
                val num = name.replace(Regex("[^0-9]"), "")
                "شاهد رياضة $num HD"
            }
            name.startsWith("STARZPLAY", true) -> {
                val num = name.replace(Regex("[^0-9]"), "")
                "STARZPLAY $num HD"
            }
            name.startsWith("THAMANYA SPORT", true) -> {
                val num = name.replace(Regex("[^0-9]"), "")
                "ثمانية $num HD"
            }
            else -> name
        }
    }

    private fun formatServerLabel(name: String, index: Int): String {
        return when {
            name.contains("1080") -> "جودة فائقة 1080p (FHD)"
            name.contains("720") -> "جودة عالية 720p (HD)"
            name.contains("480") -> "جودة متوسطة 480p (SD)"
            name.contains("360") -> "جودة ضعيفة 360p (Low)"
            name.contains("240") -> "جودة منخفضة جداً 240p"
            name.contains("HD", true) -> "سيرفر HD عالي الدقة"
            name.contains("SD", true) -> "سيرفر SD موفر للبيانات"
            name.contains("الاسبانية", true) -> "تعليق إسباني"
            name.contains("الانجليزية", true) -> "تعليق إنجليزي"
            else -> "سيرفر ${index + 1} ($name)"
        }
    }

    private fun resolveLogo(name: String, pkg: String): String {
        return when {
            name.contains("bein", true) -> LOGO_BEIN
            name.contains("alwan", true) || name.contains("ألوان") -> LOGO_ALWAN
            name.contains("mbc", true) -> LOGO_MBC
            name.contains("shahid", true) -> LOGO_SHAHID
            name.contains("real", true) -> LOGO_REAL
            name.contains("barca", true) -> LOGO_BARCA
            name.contains("man", true) -> LOGO_MANU
            name.contains("dubai", true) || name.contains("دبي") -> LOGO_DUBAI_SPORTS
            else -> LOGO_BEIN
        }
    }

    private fun sanitizeStreamUrl(url: String): String {
        var clean = url.trim()
        if (clean.contains("1201157m3u8")) {
            clean = clean.replace("1201157m3u8", "1201157.m3u8")
        }
        return clean
    }

    /**
     * Fallback precompiled channels guaranteed to always work even if device files are absent.
     */
    private fun getPrecompiledFallbackChannels(context: Context): List<Channel> {
        val list = mutableListOf<Channel>()

        // Alwan Sports 1 to 5 (HD & SD)
        for (i in 1..5) {
            val hd = "http://82.39.115.19:24652/H$i.m3u8"
            val sd = "http://82.39.115.19:24652/S$i.m3u8"
            val servers = listOf(
                StreamServer("alwan_${i}_hd", "سيرفر 1 - Full HD (1080p)", "1080p", hd),
                StreamServer("alwan_${i}_sd", "سيرفر 2 - SD موفر للبيانات", "576p", sd)
            )
            val chId = "dax_alwan_$i"
            list.add(
                Channel(
                    id = chId,
                    name = "ألوان سبورت $i HD",
                    logoUrl = LOGO_ALWAN,
                    category = "قنوات ألوان الرياضية",
                    isLive = true,
                    currentProgram = "بث مباشر فائق الجودة",
                    isFavorite = FavoritesManager.isChannelFavorite(context, chId),
                    streamUrl = hd,
                    servers = servers
                )
            )
        }

        return list
    }
}
