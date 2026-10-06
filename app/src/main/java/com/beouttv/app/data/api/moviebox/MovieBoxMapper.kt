package com.beouttv.app.data.api.moviebox

import com.beouttv.app.data.model.*

object MovieBoxMapper {

    fun extractStillsUrl(stills: com.google.gson.JsonElement?): String? {
        if (stills == null || stills.isJsonNull) return null
        if (stills.isJsonObject) {
            return stills.asJsonObject.get("url")?.asString
        }
        if (stills.isJsonArray && stills.asJsonArray.size() > 0) {
            val first = stills.asJsonArray.get(0)
            if (first != null && first.isJsonObject) {
                return first.asJsonObject.get("url")?.asString
            }
        }
        return null
    }

    fun toMovie(item: MovieBoxItem): Movie {
        val id = "mb_${item.subjectId}"
        val poster = item.cover?.url.orEmpty()
        val year = extractYear(item.releaseDate)
        val rating = extractRating(item.imdbRatingValue ?: item.score)
        val duration = if (item.duration.isNotBlank()) item.duration else "115 دقيقة"
        val genres = if (item.genre.isNotBlank()) {
            item.genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else listOf("أفلام")
        val isSeries = item.subjectType == 2

        val servers = mutableListOf<StreamServer>()
        val subtitles = mutableListOf<SubtitleTrack>()

        val safeDetectors = item.resourceDetectors ?: emptyList()
        safeDetectors.forEach { detector ->
            val downloadUrl = detector.downloadUrl?.takeIf { it.isNotBlank() }
            val resourceLink = detector.resourceLink?.takeIf { it.isNotBlank() }
            if (downloadUrl != null && servers.none { it.url == downloadUrl }) {
                servers.add(
                    StreamServer(
                        id = "mb_srv_direct_${servers.size}",
                        name = "سيرفر MovieBox المباشر (FHD)",
                        quality = "1080p FHD",
                        url = downloadUrl,
                        isDirect = true
                    )
                )
            } else if (resourceLink != null && (resourceLink.contains(".mp4") || resourceLink.contains(".m3u8") || resourceLink.startsWith("http")) && servers.none { it.url == resourceLink }) {
                servers.add(
                    StreamServer(
                        id = "mb_srv_link_${servers.size}",
                        name = "سيرفر MovieBox (FHD)",
                        quality = "1080p FHD",
                        url = resourceLink,
                        isDirect = true
                    )
                )
            }
            val resList = detector.resolutionList ?: emptyList()
            resList.forEach { res ->
                val link = res.resourceLink?.takeIf { it.isNotBlank() }
                    ?: res.sourceUrl?.takeIf { it.isNotBlank() }
                if (link != null && servers.none { it.url == link }) {
                    val qName = "${res.resolution}p"
                    servers.add(
                        StreamServer(
                            id = "mb_res_${res.resolution}_${servers.size}",
                            name = "سيرفر MovieBox $qName",
                            quality = qName,
                            url = link,
                            isDirect = true
                        )
                    )
                }

                val capList = res.extCaptions ?: emptyList()
                capList.forEach { cap ->
                    if (cap.url.isNotBlank() && subtitles.none { it.url == cap.url }) {
                        val isAr = cap.lan.equals("ar", true) || cap.lanName.contains("عرب", true)
                        val labelName = if (isAr) "العربية [MovieBox الأصلي]" else "${cap.lanName.ifBlank { cap.lan.uppercase() }} [MovieBox]"
                        subtitles.add(
                            SubtitleTrack(
                                id = "mb_sub_${cap.id.ifBlank { subtitles.size.toString() }}",
                                language = if (isAr) "ar" else cap.lan,
                                label = labelName,
                                url = cap.url,
                                format = "srt"
                            )
                        )
                    }
                }
            }
            val detectorCaptions = detector.extCaptions ?: emptyList()
            detectorCaptions.forEach { cap ->
                if (cap.url.isNotBlank() && subtitles.none { it.url == cap.url }) {
                    val isAr = cap.lan.equals("ar", true) || cap.lanName.contains("عرب", true)
                    val labelName = if (isAr) "العربية [MovieBox الأصلي]" else "${cap.lanName.ifBlank { cap.lan.uppercase() }} [MovieBox]"
                    subtitles.add(
                        SubtitleTrack(
                            id = "mb_sub_${cap.id.ifBlank { subtitles.size.toString() }}",
                            language = if (isAr) "ar" else cap.lan,
                            label = labelName,
                            url = cap.url,
                            format = "srt"
                        )
                    )
                }
            }
        }
        subtitles.sortWith(
            compareByDescending<SubtitleTrack> { it.language == "ar" && it.label.contains("MovieBox") }
                .thenByDescending { it.language == "ar" }
                .thenByDescending { it.label.contains("MovieBox") }
        )

        val defaultStreamUrl = servers.maxByOrNull {
            it.quality.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
        }?.url ?: servers.firstOrNull()?.url ?: ""

        return Movie(
            id = id,
            title = item.title,
            originalTitle = item.title,
            posterUrl = poster,
            backdropUrl = poster,
            year = year,
            rating = rating,
            imdbRatingFormatted = if (rating > 0f) "$rating/10" else "7.5/10",
            quality = servers.firstOrNull()?.quality ?: "1080p FHD",
            duration = duration,
            genre = if (genres.isNotEmpty()) genres.joinToString(" • ") else "سينما عالمية",
            category = if (isSeries) "مسلسلات" else if (item.genre.contains("Anime", ignoreCase = true)) "أنمي" else "أفلام MovieBox",
            genresList = genres,
            description = item.description.ifBlank { "مشاهدة وتحميل ${item.title} بجودة عالية على BeoutTV." },
            streamUrl = defaultStreamUrl,
            servers = servers,
            subtitles = subtitles,
            country = item.countryName.orEmpty(),
            language = item.language ?: "مترجم بالعربية (FHD)",
            isSeries = isSeries
        )
    }

    fun toSeries(item: MovieBoxItem): Series {
        val id = "mb_${item.subjectId}"
        val poster = item.cover?.url.orEmpty()
        val year = extractYear(item.releaseDate)
        val rating = extractRating(item.imdbRatingValue ?: item.score)
        val genre = if (item.genre.isNotBlank()) item.genre.replace(",", " •") else "مسلسلات عالمية"

        return Series(
            id = id,
            title = item.title,
            originalTitle = item.title,
            posterUrl = poster,
            backdropUrl = poster,
            year = year,
            rating = rating,
            genre = if (item.genre.contains("Anime", ignoreCase = true)) "أنمي مترجم" else genre,
            description = item.description.ifBlank { "مشاهدة وتحميل مسلسل ${item.title} بجودة عالية وبكافة المواسم والحلقات على BeoutTV." },
            seasons = emptyList()
        )
    }

    fun toDetailedMovie(detail: MovieBoxSubjectDetail, fallbackItem: Movie? = null): Movie {
        val id = "mb_${detail.subjectId}"
        val poster = detail.cover?.url.orEmpty().ifBlank { fallbackItem?.posterUrl.orEmpty() }
        val backdrop = extractStillsUrl(detail.stills) ?: poster
        val year = extractYear(detail.releaseDate).takeIf { it > 0 } ?: fallbackItem?.year ?: 2024
        val rating = extractRating(detail.imdbRatingValue).takeIf { it > 0f } ?: fallbackItem?.rating ?: 7.5f
        val duration = if (detail.duration.isNotBlank()) detail.duration else fallbackItem?.duration ?: "115 دقيقة"
        val isSeries = detail.subjectType == 2
        val genres = if (detail.genre.isNotBlank()) {
            detail.genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else fallbackItem?.genresList ?: listOf("أفلام")

        val safeStaff = detail.staffList ?: emptyList()
        val castMembers = safeStaff.map { staff ->
            CastMember(
                name = staff.name,
                character = staff.character.ifBlank { if (staff.staffType == 2) "المخرج" else "ممثل" },
                profileUrl = staff.avatarUrl
            )
        }

        val directorName = safeStaff.firstOrNull { it.staffType == 2 }?.name
            ?: fallbackItem?.director.orEmpty()

        // Extract direct video stream resolutions from resourceDetectors
        val servers = mutableListOf<StreamServer>()
        val subtitles = mutableListOf<SubtitleTrack>()

        val safeDetectors = detail.resourceDetectors ?: emptyList()
        safeDetectors.forEach { detector ->
            val downloadUrl = detector.downloadUrl?.takeIf { it.isNotBlank() }
            val resourceLink = detector.resourceLink?.takeIf { it.isNotBlank() }

            // 1. Direct high-speed downloadUrl from MovieBox CDN (FHD 1080p MP4)
            if (downloadUrl != null && servers.none { it.url == downloadUrl }) {
                servers.add(
                    StreamServer(
                        id = "mb_srv_direct_${servers.size}",
                        name = "سيرفر MovieBox المباشر (FHD)",
                        quality = "1080p FHD",
                        url = downloadUrl,
                        isDirect = true
                    )
                )
            } else if (resourceLink != null && (resourceLink.contains(".mp4") || resourceLink.contains(".m3u8") || resourceLink.startsWith("http")) && servers.none { it.url == resourceLink }) {
                servers.add(
                    StreamServer(
                        id = "mb_srv_link_${servers.size}",
                        name = "سيرفر MovieBox (FHD)",
                        quality = "1080p FHD",
                        url = resourceLink,
                        isDirect = true
                    )
                )
            }

            // 2. Resolution list streams
            val resList = detector.resolutionList ?: emptyList()
            resList.forEach { res ->
                val link = res.resourceLink?.takeIf { it.isNotBlank() }
                    ?: res.sourceUrl?.takeIf { it.isNotBlank() }
                if (link != null && servers.none { it.url == link }) {
                    val qName = "${res.resolution}p"
                    servers.add(
                        StreamServer(
                            id = "mb_res_${res.resolution}_${servers.size}",
                            name = "سيرفر MovieBox $qName",
                            quality = qName,
                            url = link,
                            isDirect = true
                        )
                    )
                }

                // Subtitles attached to resolution
                val capList = res.extCaptions ?: emptyList()
                capList.forEach { cap ->
                    if (cap.url.isNotBlank() && subtitles.none { it.url == cap.url }) {
                        val isAr = cap.lan.equals("ar", true) || cap.lanName.contains("عرب", true)
                        val labelName = if (isAr) "العربية [MovieBox الأصلي]" else "${cap.lanName.ifBlank { cap.lan.uppercase() }} [MovieBox]"
                        subtitles.add(
                            SubtitleTrack(
                                id = "mb_sub_${cap.id.ifBlank { subtitles.size.toString() }}",
                                language = if (isAr) "ar" else cap.lan,
                                label = labelName,
                                url = cap.url,
                                format = "srt"
                            )
                        )
                    }
                }
            }

            // 3. Top-level detector captions (Essential for Odyssey, Blades of the Guardians, Mad Max, Mutiny)
            val detectorCaptions = detector.extCaptions ?: emptyList()
            detectorCaptions.forEach { cap ->
                if (cap.url.isNotBlank() && subtitles.none { it.url == cap.url }) {
                    val isAr = cap.lan.equals("ar", true) || cap.lanName.contains("عرب", true)
                    val labelName = if (isAr) "العربية [MovieBox الأصلي]" else "${cap.lanName.ifBlank { cap.lan.uppercase() }} [MovieBox]"
                    subtitles.add(
                        SubtitleTrack(
                            id = "mb_sub_${cap.id.ifBlank { subtitles.size.toString() }}",
                            language = if (isAr) "ar" else cap.lan,
                            label = labelName,
                            url = cap.url,
                            format = "srt"
                        )
                    )
                }
            }
        }

        // Always place MovieBox Arabic subtitles at the very top (#1)
        subtitles.sortWith(
            compareByDescending<SubtitleTrack> { it.language == "ar" && it.label.contains("MovieBox") }
                .thenByDescending { it.language == "ar" }
                .thenByDescending { it.label.contains("MovieBox") }
        )

        // Pick highest resolution direct stream as default streamUrl (e.g. 1080p or 720p)
        val defaultStreamUrl = servers.maxByOrNull {
            it.quality.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
        }?.url ?: servers.firstOrNull()?.url ?: ""

        return Movie(
            id = id,
            title = detail.title.ifBlank { fallbackItem?.title.orEmpty() },
            originalTitle = detail.title,
            posterUrl = poster,
            backdropUrl = backdrop,
            year = year,
            rating = rating,
            imdbRatingFormatted = "$rating/10",
            quality = servers.firstOrNull()?.quality ?: "1080p FHD",
            duration = duration,
            genre = if (genres.isNotEmpty()) genres.joinToString(" • ") else "سينما عالمية",
            category = if (isSeries) "مسلسلات" else if (detail.genre.contains("Anime", ignoreCase = true)) "أنمي" else "أفلام MovieBox",
            genresList = genres,
            description = detail.description.ifBlank { fallbackItem?.description.orEmpty() },
            streamUrl = defaultStreamUrl,
            servers = servers,
            subtitles = subtitles,
            director = directorName,
            cast = castMembers.map { it.name },
            castMembers = castMembers,
            country = detail.countryName.orEmpty(),
            language = detail.language ?: "مترجم بالعربية (FHD)",
            isSeries = isSeries
        )
    }

    fun toDetailedSeries(detail: MovieBoxSubjectDetail, fallbackItem: Series? = null): Series {
        val id = "mb_${detail.subjectId}"
        val poster = detail.cover?.url.orEmpty().ifBlank { fallbackItem?.posterUrl.orEmpty() }
        val backdrop = extractStillsUrl(detail.stills) ?: poster
        val year = extractYear(detail.releaseDate).takeIf { it > 0 } ?: fallbackItem?.year ?: 2024
        val rating = extractRating(detail.imdbRatingValue).takeIf { it > 0f } ?: fallbackItem?.rating ?: 7.5f
        val genre = if (detail.genre.isNotBlank()) detail.genre.replace(",", " •") else fallbackItem?.genre ?: "مسلسلات"

        val episodesList = mutableListOf<Episode>()
        val primaryDetector = detail.resourceDetectors?.firstOrNull()
        val totalEps = primaryDetector?.totalEpisode ?: 1

        val resolutions = primaryDetector?.resolutionList ?: emptyList()
        if (resolutions.isNotEmpty()) {
            // Group by episode
            val epGroups = resolutions.groupBy { it.ep.coerceAtLeast(1) }
            epGroups.forEach { (epNum, resList) ->
                val epServers = resList.mapNotNull { res ->
                    res.resourceLink?.takeIf { it.isNotBlank() }?.let { link ->
                        StreamServer(
                            id = "mb_ep_${epNum}_${res.resolution}",
                            name = "سيرفر ${res.resolution}p",
                            quality = "${res.resolution}p",
                            url = link,
                            isDirect = true
                        )
                    }
                }
                val epSubtitles = mutableListOf<SubtitleTrack>()
                resList.flatMap { it.extCaptions ?: emptyList<MovieBoxExtCaption>() }.forEach { cap: MovieBoxExtCaption ->
                    if (cap.url.isNotBlank() && epSubtitles.none { it.url == cap.url }) {
                        val isAr = cap.lan.equals("ar", true) || cap.lanName.contains("عرب", true)
                        val labelName = if (isAr) "العربية [MovieBox الأصلي]" else "${cap.lanName.ifBlank { cap.lan.uppercase() }} [MovieBox]"
                        epSubtitles.add(
                            SubtitleTrack(
                                id = "mb_sub_${cap.id}",
                                language = if (isAr) "ar" else cap.lan,
                                label = labelName,
                                url = cap.url,
                                format = "srt"
                            )
                        )
                    }
                }
                epSubtitles.sortByDescending { it.language == "ar" }

                episodesList.add(
                    Episode(
                        id = "${id}_s1_e$epNum",
                        episodeNumber = epNum,
                        title = "الحلقة $epNum",
                        durationMinutes = 45,
                        thumbnailUrl = backdrop,
                        streamUrl = epServers.firstOrNull()?.url.orEmpty(),
                        seriesId = id,
                        seasonNumber = 1,
                        servers = epServers,
                        subtitles = epSubtitles
                    )
                )
            }
        } else {
            // Create placeholders for the episodes so user can see and select each episode
            for (i in 1..totalEps) {
                episodesList.add(
                    Episode(
                        id = "${id}_s1_e$i",
                        episodeNumber = i,
                        title = "الحلقة $i",
                        durationMinutes = 45,
                        thumbnailUrl = backdrop,
                        streamUrl = "",
                        seriesId = id,
                        seasonNumber = 1,
                        servers = emptyList(),
                        subtitles = emptyList()
                    )
                )
            }
        }

        val seasons = listOf(Season(seasonNumber = 1, episodes = episodesList))

        return Series(
            id = id,
            title = detail.title.ifBlank { fallbackItem?.title.orEmpty() },
            originalTitle = detail.title,
            posterUrl = poster,
            backdropUrl = backdrop,
            year = year,
            rating = rating,
            genre = genre,
            description = detail.description.ifBlank { fallbackItem?.description.orEmpty() },
            seasons = seasons
        )
    }

    private fun extractYear(dateStr: String): Int {
        if (dateStr.isBlank()) return 2024
        val match = Regex("\\b(19\\d\\d|20\\d\\d)\\b").find(dateStr)
        return match?.value?.toIntOrNull() ?: 2024
    }

    private fun extractRating(ratingStr: String?): Float {
        if (ratingStr.isNullOrBlank()) return 7.5f
        return ratingStr.toFloatOrNull() ?: 7.5f
    }
}
