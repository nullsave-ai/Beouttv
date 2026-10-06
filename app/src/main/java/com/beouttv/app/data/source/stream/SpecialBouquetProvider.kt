package com.beouttv.app.data.source.stream

import android.content.Context
import com.beouttv.app.data.favorites.FavoritesManager
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.proto.ChannelProtoCodec

object SpecialBouquetProvider {

    const val CATEGORY_NAME = "قنوات بي ان سبورت"

    /**
     * Returns the curated list of live channels.
     * Tries the local binary protobuf catalog first, and falls back to guaranteed offline definitions.
     */
    fun getChannels(context: Context): List<Channel> {
        val channelMap = linkedMapOf<String, Channel>()

        // 1. Try reading clean binary catalog first and populate base map
        try {
            val datBytes = com.beouttv.app.data.storage.StreamsCatalogStorage.loadCatalogBytes(context)
            if (datBytes != null && datBytes.isNotEmpty()) {
                val protoChannels = ChannelProtoCodec.decodeChannelList(datBytes)
                for (ch in protoChannels) {
                    channelMap[ch.id] = ch.copy(isFavorite = FavoritesManager.isChannelFavorite(context, ch.id))
                }
            }
        } catch (_: Exception) {}

        // 2. Add/merge curated live channels
        fun addCh(
            id: String,
            name: String,
            logoUrl: String,
            streamPath: String,
            category: String,
            groupId: String? = null,
            drmKeyId: String? = null,
            drmKey: String? = null
        ) {
            val isFav = FavoritesManager.isChannelFavorite(context, id)
            channelMap[id] = Channel(
                id = id,
                name = name,
                logoUrl = logoUrl,
                category = category,
                isLive = true,
                currentProgram = "بث مباشر فائق الدقة",
                isFavorite = isFav,
                streamUrl = streamPath,
                userAgent = OstoraApiResolver.DEFAULT_USER_AGENT,
                referer = OstoraApiResolver.DEFAULT_REFERER,
                groupId = groupId,
                drmKeyId = drmKeyId,
                drmKey = drmKey
            )
        }

        // 1. beIN Sports HD
        for (i in 1..9) {
            addCh("sp_bein_$i", "beIN SPORTS $i HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", "https://www2.maziikaaaaa.shop/x1/${i}_32.json", "قنوات بي ان سبورت", "332")
        }
        addCh("sp_bein_news", "beIN SPORTS News HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", "https://www2.maziikaaaaa.shop/x1/10_32.json", "قنوات بي ان سبورت", "332")
        addCh("sp_bein_xtra1", "beIN SPORTS Xtra 1 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", "https://www2.maziikaaaaa.shop/x1/11_32.json", "قنوات بي ان سبورت", "332")
        addCh("sp_bein_xtra2", "beIN SPORTS Xtra 2 HD", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", "https://www2.maziikaaaaa.shop/x1/12_32.json", "قنوات بي ان سبورت", "332")

        // 2. beIN Sports SD
        for (i in 1..8) {
            addCh("sp_bein_${i}_sd", "beIN SPORTS $i (SD)", "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", "https://www2.maziikaaaaa.shop/x1/${i}_22.json", "قنوات بي ان سبورت SD", "283")
        }

        // 3. Thamanya Sports
        addCh("sp_thamanya_1", "ثمانية 1 HD", "https://lh3.googleusercontent.com/-h6Y-0y_WZY4/aKKMTRqb_mI/AAAAAAAATuI/k-L8T24_q54l7sC0P8E12mDsm903gH_gACNcBGAsYHQ/w250/0.jpg=w250", "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT1/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", "قنوات ثمانية", "6979", "HgcZ2mU8K0ZS8LC/hNlsdA", "R/CrWPWoHCC1tp3ElMvhAg")
        addCh("sp_thamanya_2", "ثمانية 2 HD", "https://lh3.googleusercontent.com/-MB-A_TtVjjk/aKKMUAzUZxI/AAAAAAAATuM/yV_Z7hXjR0c9w9w3-Uo6Vv8Ym2l3N6P_wCNcBGAsYHQ/w250/0.jpg=w250", "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT2/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", "قنوات ثمانية", "6979", "HgcZ2mU8K0ZS8LC/hNlsdA", "R/CrWPWoHCC1tp3ElMvhAg")
        addCh("sp_thamanya_3", "ثمانية 3 HD", "https://lh3.googleusercontent.com/-8QZm0xALoIw/aKKMUqC3mMI/AAAAAAAATuQ/z16g9N3R-s49N5l1lW9C2d2xX_vB9r81gCNcBGAsYHQ/w250/0.jpg=w250", "https://scccrwb-live.cdn.intigral-ott.net/bpk-tv/STCT3/wv-pr/manifest.mpd###HgcZ2mU8K0ZS8LC/hNlsdA:R/CrWPWoHCC1tp3ElMvhAg", "قنوات ثمانية", "6979", "HgcZ2mU8K0ZS8LC/hNlsdA", "R/CrWPWoHCC1tp3ElMvhAg")

        // 4. Alwan Sports (Group 7193)
        val alwanLogo = "https://lh3.googleusercontent.com/-2j2pA9PBQiQ/aaiQ7Y3oI4I/AAAAAAAABQ4/qZ-F0w2R9Gcv7uW9d8-gCNcBGAsYHQ/w250/0.jpg=w250"
        for (i in 1..6) {
            addCh("sp_alwan_$i", "ألوان $i HD", alwanLogo, "https://www2.maziikaaaaa.shop/x1/${40 + i}.json", "قنوات ألوان الرياضية", "7193")
        }

        // 13. Dax Curated Sports Multi-Quality Streams (Only verified sports)
        try {
            val daxChannels = DaxChannelsProvider.getChannels(context)
            for (dCh in daxChannels) {
                channelMap[dCh.id] = dCh
            }
        } catch (_: Exception) {}

        return channelMap.values.toList()
    }
}
