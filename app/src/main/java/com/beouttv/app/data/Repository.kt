package com.beouttv.app.data

import com.beouttv.app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer

/**
 * مستودع البيانات الموحد لتطبيق BEOUT TV.
 * يتصل مباشرة بسيرفر ولوحة تحكم Firebase السحابية (apklive) ومرايا CDN الخاصة بالمشروع،
 * مع كاش محلي للعمل دون اتصال.
 */
object Repo {
    private val CATALOG_ENDPOINTS = listOf(
        "https://apklive-default-rtdb.firebaseio.com/catalog.json",
        "https://apklive.web.app/catalog.json",
        "https://oneplus-bot.nullsave-ai.workers.dev/catalog.json",
        "https://oneplus-admin.web.app/catalog.json"
    )

    private val CONFIG_ENDPOINTS = listOf(
        "https://apklive-default-rtdb.firebaseio.com/app_config.json",
        "https://apklive.web.app/app_config.json"
    )

    @Volatile
    private var cachedCatalog: CloudCatalog? = null
    private var lastFetchTime = 0L

    private suspend fun getOrFetchCatalog(force: Boolean = false): CloudCatalog {
        val now = System.currentTimeMillis()
        if (!force && cachedCatalog != null && (now - lastFetchTime < 120_000L)) {
            return cachedCatalog!!
        }

        for (endpoint in CATALOG_ENDPOINTS) {
            try {
                val raw = Api.get(endpoint, dedupe = true)
                if (raw.isNotBlank()) {
                    val parsed = parseCatalog(raw)
                    if (parsed.channels.isNotEmpty() || parsed.movies.isNotEmpty() || parsed.matches.isNotEmpty()) {
                        cachedCatalog = parsed
                        lastFetchTime = now
                        return parsed
                    }
                }
            } catch (_: Exception) {
                // تجربة الرابط البديل تلقائياً
            }
        }

        return cachedCatalog ?: CloudCatalog()
    }

    private fun parseCatalog(raw: String): CloudCatalog {
        return try {
            Api.json.decodeFromString<CloudCatalog>(raw)
        } catch (_: Exception) {
            runCatching {
                val obj = org.json.JSONObject(raw)
                val channels = mutableListOf<CloudChannel>()
                val chArr = obj.optJSONArray("channels")
                if (chArr != null) {
                    for (i in 0 until chArr.length()) {
                        val c = chArr.optJSONObject(i) ?: continue
                        channels.add(
                            CloudChannel(
                                id = c.optLong("id", i.toLong() + 1),
                                name = c.optString("name", "قناة"),
                                group = c.optString("group", "عام"),
                                logo = c.optString("logo", ""),
                                stream_url = c.optString("stream_url", c.optString("url", "")),
                                url = c.optString("url", c.optString("stream_url", "")),
                                number = c.optInt("number", i + 1)
                            )
                        )
                    }
                } else {
                    val chObj = obj.optJSONObject("channels")
                    if (chObj != null) {
                        val keys = chObj.keys()
                        var idx = 1
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val c = chObj.optJSONObject(k) ?: continue
                            channels.add(
                                CloudChannel(
                                    id = c.optLong("id", idx.toLong()),
                                    name = c.optString("name", "قناة"),
                                    group = c.optString("group", "عام"),
                                    logo = c.optString("logo", ""),
                                    stream_url = c.optString("stream_url", c.optString("url", "")),
                                    url = c.optString("url", c.optString("stream_url", "")),
                                    number = c.optInt("number", idx++)
                                )
                            )
                        }
                    }
                }

                val matches = mutableListOf<CloudMatch>()
                val mArr = obj.optJSONArray("matches")
                if (mArr != null) {
                    for (i in 0 until mArr.length()) {
                        val m = mArr.optJSONObject(i) ?: continue
                        matches.add(
                            CloudMatch(
                                home = m.optString("home"),
                                away = m.optString("away"),
                                time = m.optString("time", "20:00"),
                                competition = m.optString("competition", ""),
                                channel = m.optString("channel", ""),
                                day = m.optInt("day", 0),
                                live = m.optBoolean("live", false),
                                score = m.optString("score", ""),
                                stream_url = m.optString("stream_url", "")
                            )
                        )
                    }
                }

                val movies = mutableListOf<CloudMovie>()
                val movArr = obj.optJSONArray("movies")
                if (movArr != null) {
                    for (i in 0 until movArr.length()) {
                        val mv = movArr.optJSONObject(i) ?: continue
                        movies.add(
                            CloudMovie(
                                id = mv.optLong("id", i.toLong() + 1),
                                title = mv.optString("title"),
                                kind = mv.optString("kind", "movie"),
                                year = mv.optInt("year", 2024),
                                rating = mv.optDouble("rating", 7.0),
                                duration = mv.optInt("duration", 110),
                                genres = runCatching {
                                    val gArr = mv.optJSONArray("genres")
                                    if (gArr != null) (0 until gArr.length()).map { gArr.getString(it) } else emptyList()
                                }.getOrDefault(emptyList()),
                                poster = mv.optString("poster"),
                                backdrop = mv.optString("backdrop"),
                                url = mv.optString("url"),
                                synopsis = mv.optString("synopsis")
                            )
                        )
                    }
                }

                CloudCatalog(
                    channels = channels,
                    matches = matches,
                    movies = movies,
                    last_updated = obj.optLong("last_updated", System.currentTimeMillis())
                )
            }.getOrDefault(CloudCatalog())
        }
    }

    private fun toItem(ch: CloudChannel): Item = Item(
        id = "ch_${ch.id}",
        kind = "channel",
        title = ch.name,
        logo = ch.logo.ifBlank { null },
        poster = ch.logo.ifBlank { null },
        category = ch.group,
        live = true,
        online = true,
        streamUrl = ch.stream_url ?: ch.url
    )

    private fun toItem(m: CloudMatch, index: Int, allChannels: List<CloudChannel>): Item {
        val targetCh = allChannels.find { it.name.trim().equals(m.channel.trim(), ignoreCase = true) }
        return Item(
            id = "match_$index",
            kind = "channel",
            title = "${m.home} ✕ ${m.away}",
            description = "${m.competition} • ${m.time} • ${m.channel}",
            quality = if (m.live) "مباشر" else m.time,
            category = "مباريات اليوم",
            live = m.live,
            online = true,
            streamUrl = m.stream_url?.ifBlank { null } ?: targetCh?.stream_url ?: targetCh?.url
        )
    }

    private fun toItem(mv: CloudMovie): Item = Item(
        id = "m_${mv.id}",
        kind = mv.kind,
        title = mv.title,
        poster = mv.poster.ifBlank { null },
        backdrop = mv.backdrop.ifBlank { mv.poster }.ifBlank { null },
        year = mv.year,
        description = mv.synopsis.ifBlank { null },
        durationMin = mv.duration,
        quality = String.format(java.util.Locale.US, "%.1f", mv.rating),
        category = mv.genres.firstOrNull(),
        live = false,
        online = true,
        streamUrl = mv.url.ifBlank { null }
    )

    suspend fun home(): HomeResponse {
        val cat = getOrFetchCatalog()
        val sections = mutableListOf<Section>()
        val chItems = cat.channels.map { toItem(it) }
        val matchItems = cat.matches.mapIndexed { idx, m -> toItem(m, idx, cat.channels) }
        val movieItems = cat.movies.map { toItem(it) }

        // 1. Hero Carousel: مباشر + قنوات مميزة
        val heroCandidates = mutableListOf<Item>()
        heroCandidates.addAll(matchItems.filter { it.live })
        heroCandidates.addAll(chItems.take(6))
        if (heroCandidates.isNotEmpty()) {
            sections.add(Section(id = "sec_hero", type = "hero", title = "البث المباشر والمميز", items = heroCandidates.take(7)))
        }

        // 2. مباريات اليوم المباشرة
        if (matchItems.isNotEmpty()) {
            sections.add(Section(id = "sec_matches", type = "channels", title = "مباريات اليوم المباشرة ⚽", items = matchItems))
        }

        // 3. متابعة المشاهدة
        sections.add(Section(id = "sec_continue", type = "continue", title = "متابعة المشاهدة ⏱️"))

        // 4. أبرز القنوات التلفزيونية
        if (chItems.isNotEmpty()) {
            sections.add(Section(id = "sec_top_channels", type = "channels", title = "أبرز القنوات التلفزيونية 📺", items = chItems.take(15)))
        }

        // 5. التصنيفات والباقات
        val groups = cat.channels.map { it.group }.filter { it.isNotBlank() }.distinct()
        if (groups.isNotEmpty()) {
            val catItems = groups.map { Item(id = it, kind = "category", title = it) }
            sections.add(Section(id = "sec_cats", type = "categories", title = "باقات القنوات", items = catItems))
        }

        // 6. تقسيم القنوات حسب الباقات من لوحة التحكم
        for (g in groups) {
            val groupChannels = chItems.filter { it.category == g }
            if (groupChannels.isNotEmpty()) {
                sections.add(Section(id = "sec_grp_${g.hashCode()}", type = "channels", title = g, items = groupChannels))
            }
        }

        // 7. الأفلام والمسلسلات إذا وُجدت في اللوحة
        val moviesOnly = movieItems.filter { it.kind == "movie" }
        if (moviesOnly.isNotEmpty()) {
            sections.add(Section(id = "sec_movies", type = "row", title = "أحدث الأفلام 🎬", items = moviesOnly))
        }
        val seriesOnly = movieItems.filter { it.kind == "series" }
        if (seriesOnly.isNotEmpty()) {
            sections.add(Section(id = "sec_series", type = "row", title = "مسلسلات مميزة 🍿", items = seriesOnly))
        }

        return HomeResponse(sections = sections)
    }

    suspend fun config(): AppConfig {
        for (endpoint in CONFIG_ENDPOINTS) {
            try {
                val txt = Api.get(endpoint, dedupe = false)
                val c = Api.json.decodeFromString<CloudAppConfig>(txt)
                val appVer = 3 // الإصدار الحالي للتطبيق
                return AppConfig(
                    minVersion = c.min_version_code,
                    latestVersion = c.latest_version_code,
                    updateUrl = c.update_url.ifBlank { "https://github.com/nullsave-ai/Beouttv/releases/latest/download/BeoutTV-Universal-v3.0.apk" },
                    updateTitle = c.update_title.ifBlank { "تحديث جديد متوفر لتطبيق BEOUT TV" },
                    updateMessage = c.update_message.ifBlank { "يرجى تنزيل الإصدار الأحدث من تطبيق BEOUT TV لمتابعة البث بأعلى ثبات وجودة." },
                    forceUpdate = c.force_update,
                    maintenance = if (c.force_update && c.min_version_code > appVer) c.update_message else null
                )
            } catch (_: Exception) {}
        }
        return AppConfig()
    }

    suspend fun channelCategories(): List<Category> {
        val cat = getOrFetchCatalog()
        val groups = cat.channels.map { it.group }.filter { it.isNotBlank() }.distinct()
        return groups.map { Category(id = it, name = it) }
    }

    suspend fun catalogCategories(kind: String): List<Category> {
        val cat = getOrFetchCatalog()
        val genres = cat.movies.filter { it.kind == kind }.flatMap { it.genres }.distinct()
        return genres.map { Category(id = it, name = it) }
    }

    suspend fun channels(category: String?, cursor: String?): Page {
        val cat = getOrFetchCatalog()
        val chItems = cat.channels.map { toItem(it) }
        val filtered = if (category.isNullOrBlank()) chItems else chItems.filter { it.category == category }
        return Page(items = filtered, next = null)
    }

    suspend fun catalog(kind: String, category: String?, cursor: String?): Page {
        val cat = getOrFetchCatalog()
        val movieItems = cat.movies.filter { it.kind == kind }.map { toItem(it) }
        val filtered = if (category.isNullOrBlank()) movieItems else movieItems.filter { it.category == category }
        return Page(items = filtered, next = null)
    }

    suspend fun detail(id: String): Detail {
        val cat = getOrFetchCatalog()
        val mv = cat.movies.find { "m_${it.id}" == id }
        if (mv != null) {
            val item = toItem(mv)
            val seasons = if (mv.episodes.isNotEmpty()) {
                listOf(
                    Season(
                        id = "s1",
                        number = 1,
                        title = "الموسم 1",
                        episodes = mv.episodes.mapIndexed { idx, ep ->
                            Episode(
                                id = "${id}_ep_${idx + 1}",
                                number = idx + 1,
                                title = ep.title.ifBlank { "الحلقة ${idx + 1}" },
                                poster = mv.poster.ifBlank { null }
                            )
                        }
                    )
                )
            } else emptyList()
            val related = cat.movies.filter { it.id != mv.id && it.kind == mv.kind }.take(6).map { toItem(it) }
            return Detail(item = item, seasons = seasons, related = related)
        }
        val ch = cat.channels.find { "ch_${it.id}" == id }
        if (ch != null) {
            val item = toItem(ch)
            val related = cat.channels.filter { it.id != ch.id && it.group == ch.group }.take(6).map { toItem(it) }
            return Detail(item = item, seasons = emptyList(), related = related)
        }
        return Detail(item = Item(id = id, kind = "channel", title = "محتوى BEOUT TV", online = true), seasons = emptyList(), related = emptyList())
    }

    suspend fun search(query: String, type: String?, cursor: String?): Page {
        val cat = getOrFetchCatalog()
        val q = query.trim().lowercase()
        val all = mutableListOf<Item>()
        if (type == null || type == "channel") {
            all.addAll(cat.channels.map { toItem(it) }.filter { it.title.lowercase().contains(q) })
            all.addAll(cat.matches.mapIndexed { idx, m -> toItem(m, idx, cat.channels) }.filter {
                it.title.lowercase().contains(q) || it.description?.lowercase()?.contains(q) == true
            })
        }
        if (type == null || type != "channel") {
            all.addAll(cat.movies.map { toItem(it) }.filter {
                it.title.lowercase().contains(q) || it.category?.lowercase()?.contains(q) == true
            })
        }
        return Page(items = all, next = null)
    }

    suspend fun suggest(query: String): List<String> {
        val cat = getOrFetchCatalog()
        val q = query.trim().lowercase()
        val chSuggestions = cat.channels.map { it.name }.filter { it.lowercase().contains(q) }
        val movSuggestions = cat.movies.map { it.title }.filter { it.lowercase().contains(q) }
        return (chSuggestions + movSuggestions).take(6)
    }

    suspend fun play(kind: String, id: String): PlayInfo {
        val cat = getOrFetchCatalog()
        val ch = cat.channels.find { "ch_${it.id}" == id || it.name == id }
        if (ch != null) {
            val u = ch.stream_url?.ifBlank { null } ?: ch.url ?: ""
            return PlayInfo(url = u, live = true)
        }
        if (id.startsWith("match_")) {
            val idx = id.removePrefix("match_").toIntOrNull() ?: 0
            val m = cat.matches.getOrNull(idx)
            if (m != null) {
                val targetCh = cat.channels.find { it.name.trim().equals(m.channel.trim(), ignoreCase = true) }
                val u = m.stream_url?.ifBlank { null } ?: targetCh?.stream_url ?: targetCh?.url ?: ""
                return PlayInfo(url = u, live = true)
            }
        }
        val mv = cat.movies.find { "m_${it.id}" == id }
        if (mv != null) {
            return PlayInfo(url = mv.url, live = false)
        }
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return PlayInfo(url = id, live = kind == "channel")
        }
        return PlayInfo(url = "", live = kind == "channel")
    }

    // ---- محلي + مزامنة best-effort ----
    suspend fun toggleFavorite(item: Item) {
        val wasFav = Prefs.favorites.first().any { it.id == item.id }
        Prefs.toggleFavorite(item)
        Api.post("sync/favorites", """{"itemId":${Api.json.encodeToString(String.serializer(), item.id)},"on":${!wasFav}}""")
    }

    suspend fun saveProgress(e: HistoryEntry) {
        Prefs.saveProgress(e)
        if (e.item.kind != "channel")
            Api.post("sync/history", Api.json.encodeToString(HistoryEntry.serializer(), e))
    }
}
