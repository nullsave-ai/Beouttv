package com.beouttv.app.data.api

import com.beouttv.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SportsApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val FIREBASE_PROTOBUF_MATCHES = "https://digite-bda04.web.app/matches.pb.gz"
        const val FIREBASE_HOSTING_MATCHES = "https://digite-bda04.web.app/matches.json"
        const val PRIMARY_HOST = "https://atared.serv00.net/365.php"
        const val FALLBACK_365_WEBWS = "https://webws.365scores.com/web"
    }

    /**
     * Fetches real-time smart matches for any given date from Protobuf CDN / Firebase Cloud / 365Scores.
     * dateParam: "today", "tomorrow", "yesterday", or "YYYY-MM-DD"
     */
    suspend fun getMatchesByDate(dateParam: String): List<Match> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Match>()

        // 1. Primary: ALWAYS query Official 365Scores Live WebWS FIRST for accurate real-time schedules
        try {
            val formattedDate = convertDateTo365Format(dateParam)
            val webwsUrl = if (dateParam == "live") {
                "$FALLBACK_365_WEBWS/games/current/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&sports=1"
            } else {
                "$FALLBACK_365_WEBWS/games/allscores/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&sports=1&startDate=$formattedDate&endDate=$formattedDate"
            }

            val request = Request.Builder()
                .url(webwsUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val compsMap = mutableMapOf<Int, String>()
                        val compsArr = json.optJSONArray("competitions") ?: JSONArray()
                        for (i in 0 until compsArr.length()) {
                            val cObj = compsArr.getJSONObject(i)
                            compsMap[cObj.optInt("id")] = cObj.optString("name", "كرة القدم")
                        }

                        val gamesArr = json.optJSONArray("games") ?: JSONArray()
                        for (i in 0 until gamesArr.length()) {
                            val gObj = gamesArr.getJSONObject(i)
                            val compId = gObj.optInt("competitionId", 0)
                            val compName = compsMap[compId] ?: gObj.optString("competitionDisplayName", "كرة القدم")
                            parseWebwsAllscoresGame(gObj, compName, compId, dateParam)?.let { result.add(it) }
                        }
                        if (result.isNotEmpty()) return@withContext result
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: High-Speed Protobuf Binary CDN Stream (Only if 365Scores network is unreachable)
        try {
            val req = Request.Builder()
                .url("$FIREBASE_PROTOBUF_MATCHES?t=${System.currentTimeMillis()}")
                .header("User-Agent", "okhttp/4.12.0")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val rawStream = response.body?.byteStream()
                    if (rawStream != null) {
                        val uncompressed = java.util.zip.GZIPInputStream(rawStream).readBytes()
                        val pbMatches = com.beouttv.app.data.proto.MatchProtoCodec.decodeMatchesList(uncompressed)
                        if (pbMatches.isNotEmpty()) {
                            val filtered = if (dateParam == "today" || dateParam == "tomorrow" || dateParam == "yesterday") {
                                pbMatches.filter { it.dateCategory == dateParam || (dateParam == "today" && it.status == MatchStatus.LIVE) }
                            } else pbMatches
                            if (filtered.isNotEmpty()) return@withContext filtered
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Firebase Cloud Hosting JSON Feed
        try {
            val req = Request.Builder()
                .url("$FIREBASE_HOSTING_MATCHES?t=${System.currentTimeMillis()}")
                .header("User-Agent", "okhttp/4.12.0")
                .build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val m = parseFirebaseMatch(obj, dateParam)
                            if (m != null) result.add(m)
                        }
                        if (result.isNotEmpty()) return@withContext result
                    }
                }
            }
        } catch (_: Exception) {}

        result
    }

    /**
     * Fetches clubs list for a specific league directly from 365Scores WebWS.
     */
    suspend fun getTeamsForLeague(leagueId: String): List<ClubInfo> = withContext(Dispatchers.IO) {
        val teamsList = mutableListOf<ClubInfo>()

        try {
            val webwsUrl = "$FALLBACK_365_WEBWS/standings/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&competitions=$leagueId&type=1&live=false"
            val request = Request.Builder().url(webwsUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val standingsArr = json.optJSONArray("standings") ?: JSONArray()
                        for (s in 0 until standingsArr.length()) {
                            val stObj = standingsArr.getJSONObject(s)
                            val leagueName = stObj.optString("competitionDisplayName", "الدوري")
                            val rowsArr = stObj.optJSONArray("rows") ?: JSONArray()
                            for (i in 0 until rowsArr.length()) {
                                val rObj = rowsArr.getJSONObject(i)
                                val comp = rObj.optJSONObject("competitor") ?: continue
                                val tid = comp.optString("id", "$i")
                                val tname = comp.optString("name", "النادي")
                                if (teamsList.none { it.id == tid }) {
                                    teamsList.add(
                                        ClubInfo(
                                            id = tid,
                                            name = tname,
                                            logoUrl = if (tid.isNotBlank() && tid != "0") "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$tid" else "",
                                            rank = rObj.optInt("position", teamsList.size + 1),
                                            played = rObj.optInt("gamePlayed", 0),
                                            points = rObj.optDouble("points", 0.0).toInt(),
                                            won = rObj.optInt("gamesWon", 0),
                                            draw = rObj.optInt("gamesEven", 0),
                                            lost = rObj.optInt("gamesLost", 0),
                                            goalDiff = rObj.optDouble("ratio", 0.0).toInt(),
                                            leagueId = leagueId,
                                            leagueName = leagueName
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        teamsList
    }

    /**
     * Fetches championship standings table directly from 365Scores WebWS (100% accurate, zero offset).
     */
    suspend fun getLeagueStandings(competitionId: String, competitionName: String): LeagueStanding? = withContext(Dispatchers.IO) {
        try {
            val webwsUrl = "$FALLBACK_365_WEBWS/standings/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&competitions=$competitionId&type=1&live=false"
            val request = Request.Builder()
                .url(webwsUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val standingsArr = json.optJSONArray("standings") ?: JSONArray()
                        if (standingsArr.length() > 0) {
                            var compTitle = competitionName
                            val rows = mutableListOf<StandingRow>()

                            for (s in 0 until standingsArr.length()) {
                                val stObj = standingsArr.getJSONObject(s)
                                if (compTitle.isBlank()) {
                                    compTitle = stObj.optString("competitionDisplayName", competitionName)
                                }
                                val rowsArr = stObj.optJSONArray("rows") ?: JSONArray()
                                for (i in 0 until rowsArr.length()) {
                                    val rObj = rowsArr.getJSONObject(i)
                                    val comp = rObj.optJSONObject("competitor") ?: continue
                                    val teamId = comp.optInt("id", 0)
                                    val teamName = comp.optString("name", "الفريق")
                                    val teamLogo = if (teamId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$teamId" else ""

                                    val rank = rObj.optInt("position", rows.size + 1)
                                    val played = rObj.optInt("gamePlayed", 0)
                                    val won = rObj.optInt("gamesWon", 0)
                                    val draw = rObj.optInt("gamesEven", 0)
                                    val lost = rObj.optInt("gamesLost", 0)
                                    val goalDiff = rObj.optDouble("ratio", 0.0).toInt()
                                    val points = rObj.optDouble("points", 0.0).toInt()

                                    if (rows.none { it.teamName == teamName }) {
                                        rows.add(
                                            StandingRow(
                                                rank = rank,
                                                teamName = teamName,
                                                teamLogoUrl = teamLogo,
                                                played = played,
                                                won = won,
                                                draw = draw,
                                                lost = lost,
                                                goalDiff = goalDiff,
                                                points = points
                                            )
                                        )
                                    }
                                }
                            }
                            if (rows.isNotEmpty()) {
                                return@withContext LeagueStanding(
                                    leagueId = competitionId,
                                    leagueName = compTitle.ifBlank { "الدوري" },
                                    rows = rows
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct Fallback to 365Scores WebWS Standings
        try {
            val webwsUrl = "$FALLBACK_365_WEBWS/standings/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&competitions=$competitionId&type=1&live=false"
            val request = Request.Builder()
                .url(webwsUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val standingsArr = json.optJSONArray("standings") ?: JSONArray()
                        if (standingsArr.length() > 0) {
                            val stObj = standingsArr.getJSONObject(0)
                            val compTitle = competitionName.ifBlank { stObj.optString("competitionDisplayName", competitionName) }
                            val rowsArr = stObj.optJSONArray("rows") ?: JSONArray()
                            val rows = mutableListOf<StandingRow>()
                            for (i in 0 until rowsArr.length()) {
                                val rObj = rowsArr.getJSONObject(i)
                                val comp = rObj.optJSONObject("competitor")
                                val teamId = comp?.optInt("id", 0) ?: 0
                                val teamName = comp?.optString("name", "الفريق") ?: "الفريق"
                                val teamLogo = if (teamId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$teamId" else ""

                                val rank = rObj.optInt("position", i + 1)
                                val played = rObj.optInt("gamePlayed", 0)
                                val won = rObj.optInt("gamesWon", 0)
                                val draw = rObj.optInt("gamesEven", 0)
                                val lost = rObj.optInt("gamesLost", 0)
                                val goalDiff = rObj.optDouble("ratio", 0.0).toInt()
                                val points = rObj.optDouble("points", 0.0).toInt()

                                rows.add(
                                    StandingRow(
                                        rank = rank,
                                        teamName = teamName,
                                        teamLogoUrl = teamLogo,
                                        played = played,
                                        won = won,
                                        draw = draw,
                                        lost = lost,
                                        goalDiff = goalDiff,
                                        points = points
                                    )
                                )
                            }
                            if (rows.isNotEmpty()) {
                                return@withContext LeagueStanding(
                                    leagueId = competitionId,
                                    leagueName = compTitle,
                                    rows = rows
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    /**
     * Fetches fixtures and upcoming/recent matches for a specific club.
     */
    suspend fun getTeamFixtures(teamId: String, teamName: String): List<Match> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<Match>()

        // 1. Fetch from webws competitor fixtures
        try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val today = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 30)
            val future = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -60)
            val past = sdf.format(cal.time)

            val url = "$FALLBACK_365_WEBWS/games/fixtures/?appTypeId=5&langId=27&timezoneName=Asia/Baghdad&userCountryId=114&competitors=$teamId&startDate=$past&endDate=$future"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val gamesArr = json.optJSONArray("games") ?: JSONArray()
                        val targetTid = teamId.toIntOrNull() ?: 0

                        for (i in 0 until gamesArr.length()) {
                            val gObj = gamesArr.getJSONObject(i)

                            // 1. Sport Type Filter (Must be 1 = Men's Football)
                            val sportId = gObj.optInt("sportId", 1).takeIf { it > 0 } ?: gObj.optInt("sportTypeId", 1)
                            if (sportId != 1) continue

                            // 2. Strict Competitor ID Matching (Prevent any other club with similar name like Al-Nassr Kuwait vs Al-Nassr Saudi)
                            val homeComp = gObj.optJSONObject("homeCompetitor")
                            val awayComp = gObj.optJSONObject("awayCompetitor")
                            val hid = homeComp?.optInt("id", 0) ?: 0
                            val aid = awayComp?.optInt("id", 0) ?: 0
                            val compsArr = gObj.optJSONArray("comps")

                            var isExactCompetitor = false
                            if (targetTid > 0) {
                                if (hid == targetTid || aid == targetTid) {
                                    isExactCompetitor = true
                                } else if (compsArr != null) {
                                    for (c in 0 until compsArr.length()) {
                                        val cid = compsArr.getJSONObject(c).optInt("id", 0)
                                        if (cid == targetTid) {
                                            isExactCompetitor = true
                                            break
                                        }
                                    }
                                }
                            }
                            if (targetTid > 0 && !isExactCompetitor) continue

                            val parsed = parseWebwsGame(gObj, "team") ?: continue

                            // 3. Strict Filter for Women's, Youth, Reserves, and Secondary tournaments
                            val compLower = parsed.competition.lowercase()
                            val homeLower = parsed.homeTeam.lowercase()
                            val awayLower = parsed.awayTeam.lowercase()

                            val isWomens = compLower.contains("سيدات") || compLower.contains("women") || compLower.contains("femenin") || compLower.contains("femenino") || compLower.contains("frauen") || compLower.contains("dames") ||
                                           homeLower.contains("سيدات") || homeLower.contains("women") || homeLower.contains("femeni") || homeLower.contains("(w)") ||
                                           awayLower.contains("سيدات") || awayLower.contains("women") || awayLower.contains("femeni") || awayLower.contains("(w)")

                            val isYouthOrReserves = compLower.contains("تحت 21") || compLower.contains("تحت 19") || compLower.contains("تحت 23") || compLower.contains("تحت 17") ||
                                                    compLower.contains("u21") || compLower.contains("u19") || compLower.contains("u23") || compLower.contains("u17") ||
                                                    compLower.contains("شباب") || compLower.contains("ناشئين") || compLower.contains("رديف") || compLower.contains("reserves") ||
                                                    homeLower.contains("تحت 21") || homeLower.contains("u21") || homeLower.contains("u19") || homeLower.contains(" u23") ||
                                                    awayLower.contains("تحت 21") || awayLower.contains("u21") || awayLower.contains("u19") || awayLower.contains(" u23") ||
                                                    (homeLower.endsWith(" ب") || (homeLower.endsWith(" b") && !homeLower.contains("أتلتيك") && !homeLower.contains("athletic"))) ||
                                                    (awayLower.endsWith(" ب") || (awayLower.endsWith(" b") && !awayLower.contains("أتلتيك") && !awayLower.contains("athletic")))

                            val isOtherSport = compLower.contains("صالات") || compLower.contains("futsal") || compLower.contains("سلة") || compLower.contains("basketball") || compLower.contains("يد") || compLower.contains("handball")

                            if (!isWomens && !isYouthOrReserves && !isOtherSport) {
                                matches.add(parsed)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        matches
    }

    /**
     * Fetches squad & players for a specific club if genuinely available.
     */
    suspend fun getTeamSquad(teamId: String, teamName: String): List<PlayerInfo> = withContext(Dispatchers.IO) {
        val squad = mutableListOf<PlayerInfo>()

        // Try 365Scores Athletes / Squad API
        try {
            val url = "$FALLBACK_365_WEBWS/athletes/?appTypeId=5&langId=27&competitorId=$teamId"
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val athletesArr = json.optJSONArray("athletes") ?: JSONArray()
                        for (i in 0 until athletesArr.length()) {
                            val aObj = athletesArr.getJSONObject(i)
                            val pid = aObj.optString("id", "").trim()
                            val pname = aObj.optString("name", "").trim()
                            if (pname.isNotBlank() && !pname.equals("null", true) && !pname.equals("لاعب", true)) {
                                val num = aObj.optString("jerseyNumber", "").takeIf { it.isNotBlank() } ?: aObj.optInt("shirtNumber", 0).takeIf { it > 0 }?.toString() ?: ""
                                val pos = aObj.optJSONObject("position")?.optString("name", "لاعب") ?: aObj.optString("positionName", "لاعب")
                                val photo = "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Athletes:default.png/Athletes/$pid"
                                squad.add(
                                    PlayerInfo(
                                        id = pid.ifBlank { "$i" },
                                        name = pname,
                                        jerseyNumber = num,
                                        position = pos,
                                        photoUrl = photo
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        squad
    }

    private fun parseFirebaseMatch(obj: JSONObject, dateParam: String): Match? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val comp = obj.optString("competition", "كرة القدم")
        val home = obj.optString("homeTeam", "الفريق")
        val homeLogo = obj.optString("homeTeamLogoUrl", "")
        val away = obj.optString("awayTeam", "الفريق")
        val awayLogo = obj.optString("awayTeamLogoUrl", "")
        val homeScore = if (!obj.isNull("homeScore")) obj.optInt("homeScore") else null
        val awayScore = if (!obj.isNull("awayScore")) obj.optInt("awayScore") else null
        val statusStr = obj.optString("status", "UPCOMING")
        val status = try { MatchStatus.valueOf(statusStr) } catch (_: Exception) { MatchStatus.UPCOMING }
        val timeLabel = obj.optString("startTimeLabel", "اليوم")
        val channel = obj.optString("channel", "").takeIf { it.isNotBlank() && !it.equals("null", true) }
        val comm = obj.optString("commentator", "").takeIf { it.isNotBlank() && !it.equals("null", true) }
        val roundOrStadium = obj.optString("roundOrStadium", "").takeIf { it.isNotBlank() && !it.equals("null", true) }
        val dateCat = obj.optString("dateCategory", dateParam)
        val isUcl = obj.optBoolean("isUcl", comp.contains("أبطال أوروبا") || comp.contains("Champions League"))
        val compLogo = obj.optString("competitionLogoUrl", "")
        val streamUrl = obj.optString("streamUrl", "").takeIf { it.isNotBlank() && !it.equals("null", true) }

        return Match(
            id = id,
            competition = comp,
            homeTeam = home,
            homeTeamLogoUrl = homeLogo,
            awayTeam = away,
            awayTeamLogoUrl = awayLogo,
            homeScore = homeScore,
            awayScore = awayScore,
            status = status,
            startTimeLabel = timeLabel,
            sport = "Football",
            channel = channel,
            commentator = comm,
            roundOrStadium = roundOrStadium,
            dateCategory = dateCat,
            isUcl = isUcl,
            competitionLogoUrl = compLogo,
            streamUrl = streamUrl
        )
    }

    private fun parse365Match(obj: JSONObject, dateParam: String): Match? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val champObj = obj.optJSONObject("competition")
        val compName = champObj?.optString("name", "كرة القدم") ?: "كرة القدم"
        val compId = champObj?.optInt("id", 0) ?: 0
        val compLogo = if (compId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_64,h_64,c_limit/Competitions/$compId" else ""

        val homeObj = obj.optJSONObject("home_team")
        val homeName = homeObj?.optString("name", "الفريق المضيف") ?: "الفريق المضيف"
        val homeId = homeObj?.optInt("id", 0) ?: 0
        val homeLogo = if (homeId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$homeId" else ""

        val awayObj = obj.optJSONObject("away_team")
        val awayName = awayObj?.optString("name", "الفريق الضيف") ?: "الفريق الضيف"
        val awayId = awayObj?.optInt("id", 0) ?: 0
        val awayLogo = if (awayId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$awayId" else ""

        val scoreObj = obj.optJSONObject("score")
        val homeScore = if (scoreObj != null && !scoreObj.isNull("home")) scoreObj.optInt("home", -1).takeIf { it >= 0 } else null
        val awayScore = if (scoreObj != null && !scoreObj.isNull("away")) scoreObj.optInt("away", -1).takeIf { it >= 0 } else null

        val statusObj = obj.optJSONObject("status")
        val statusCode = statusObj?.optString("code", "upcoming") ?: "upcoming"
        val statusText = statusObj?.optString("text", "اليوم") ?: "اليوم"
        val isLive = statusObj?.optBoolean("is_live", false) ?: (statusCode == "live")
        val isFinished = statusObj?.optBoolean("is_finished", false) ?: (statusCode == "finished")

        val status = when {
            isLive -> MatchStatus.LIVE
            isFinished && homeScore != null && awayScore != null -> MatchStatus.FINISHED
            else -> MatchStatus.UPCOMING
        }

        // Clean Channel text: Never allow "null", "None", or blanks
        val rawChannel = obj.optString("channel", "").trim()
        val channel = if (rawChannel.isNotBlank() && !rawChannel.equals("null", true) && !rawChannel.equals("none", true)) rawChannel else null

        val rawCommentator = obj.optString("commentator", "").trim()
        val commentator = if (rawCommentator.isNotBlank() && !rawCommentator.equals("null", true) && !rawCommentator.equals("none", true)) rawCommentator else null

        val rawRound = obj.optString("round", "").trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
        val rawStadium = obj.optString("stadium", "").trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
        val roundOrStadium = rawRound ?: rawStadium

        val dtObj = obj.optJSONObject("datetime")
        val formattedDate = dtObj?.optString("formatted_date", "").takeIf { !it.isNullOrBlank() && !it.equals("null", true) }
        val isUcl = champObj?.optBoolean("is_ucl", false) ?: (compName.contains("أبطال أوروبا") || compName.contains("Champions League"))

        return Match(
            id = id,
            competition = compName,
            homeTeam = homeName,
            homeTeamLogoUrl = homeLogo,
            awayTeam = awayName,
            awayTeamLogoUrl = awayLogo,
            homeScore = if (status == MatchStatus.UPCOMING) null else homeScore,
            awayScore = if (status == MatchStatus.UPCOMING) null else awayScore,
            status = status,
            startTimeLabel = statusText,
            sport = "Football",
            channel = channel,
            commentator = commentator,
            roundOrStadium = roundOrStadium,
            roundNumber = rawRound,
            stadium = rawStadium,
            dateCategory = dateParam,
            kickoffDateFormatted = formattedDate,
            isUcl = isUcl,
            competitionLogoUrl = compLogo
        )
    }

    private fun parseWebwsGame(obj: JSONObject, dateParam: String): Match? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val compName = obj.optString("competitionDisplayName", "كرة القدم")
        val compId = obj.optInt("competitionId", 0)

        // Explicitly reject Youth League (6297) and FA Cup Preliminary (7856)
        if (compId == 6297 || compId == 7856) return null
        val compLower = compName.lowercase()
        if (compLower.contains("للشباب") || compLower.contains("الجولات التمهيدية") ||
            compLower.contains("التصفيات الأولية") || compLower.contains("التصفيات التمهيدية") ||
            compLower.contains("youth league") || compLower.contains("تحت 19") ||
            compLower.contains("u19") || compLower.contains("تحت 21") || compLower.contains("u21")) {
            return null
        }

        val compLogo = if (compId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_64,h_64,c_limit/Competitions/$compId" else ""

        // Check for homeCompetitor / awayCompetitor objects (365Scores fixtures standard format)
        val homeCompObj = obj.optJSONObject("homeCompetitor")
        val awayCompObj = obj.optJSONObject("awayCompetitor")

        val homeId: Int
        val homeName: String
        val awayId: Int
        val awayName: String
        var homeScoreVal: Int? = null
        var awayScoreVal: Int? = null

        if (homeCompObj != null && awayCompObj != null) {
            homeId = homeCompObj.optInt("id", 0)
            homeName = homeCompObj.optString("name", "الفريق المضيف")
            val hScore = homeCompObj.optDouble("score", -1.0)
            if (hScore >= 0.0) homeScoreVal = hScore.toInt()

            awayId = awayCompObj.optInt("id", 0)
            awayName = awayCompObj.optString("name", "الفريق الضيف")
            val aScore = awayCompObj.optDouble("score", -1.0)
            if (aScore >= 0.0) awayScoreVal = aScore.toInt()
        } else {
            val compsArr = obj.optJSONArray("comps") ?: JSONArray()
            if (compsArr.length() < 2) return null
            val homeObj = compsArr.getJSONObject(0)
            homeId = homeObj.optInt("id", 0)
            homeName = homeObj.optString("name", "الفريق المضيف")

            val awayObj = compsArr.getJSONObject(1)
            awayId = awayObj.optInt("id", 0)
            awayName = awayObj.optString("name", "الفريق الضيف")

            val scrsArr = obj.optJSONArray("scores")
            if (scrsArr != null && scrsArr.length() > 0) homeScoreVal = scrsArr.optDouble(0, -1.0).toInt().takeIf { it >= 0 }
            if (scrsArr != null && scrsArr.length() > 1) awayScoreVal = scrsArr.optDouble(1, -1.0).toInt().takeIf { it >= 0 }
        }

        val homeLower = homeName.lowercase()
        val awayLower = awayName.lowercase()
        if (homeLower.contains("تحت 19") || awayLower.contains("تحت 19") ||
            homeLower.contains("u19") || awayLower.contains("u19") ||
            homeLower.contains("تحت 21") || awayLower.contains("تحت 21")) {
            return null
        }

        val homeLogo = if (homeId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$homeId" else ""
        val awayLogo = if (awayId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$awayId" else ""

        val statusGroup = obj.optInt("statusGroup", 0)
        val isActive = obj.optBoolean("active", false) || statusGroup == 3
        val isFinished = obj.optBoolean("isFinished", false) || statusGroup == 4

        val status = when {
            isActive -> MatchStatus.LIVE
            isFinished -> MatchStatus.FINISHED
            else -> MatchStatus.UPCOMING
        }

        val startTimeStr = obj.optString("startTime", "")
        var formattedDate: String? = null
        val startTimeLabel = when (status) {
            MatchStatus.LIVE -> {
                val gt = obj.optDouble("gameTime", 0.0).toInt()
                if (gt > 0) "مباشر • $gt'" else "مباشر • جارية"
            }
            MatchStatus.FINISHED -> "انتهت • FT"
            MatchStatus.UPCOMING -> {
                if (startTimeStr.isNotBlank()) {
                    try {
                        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        val date = inFormat.parse(startTimeStr.substringBefore("+"))
                        if (date != null) {
                            val timePart = SimpleDateFormat("HH:mm", Locale.US).format(date)
                            formattedDate = SimpleDateFormat("d MMMM", Locale("ar")).format(date)
                            timePart
                        } else obj.optString("statusText", "قريباً")
                    } catch (e: Exception) {
                        obj.optString("statusText", "قريباً")
                    }
                } else obj.optString("statusText", "قريباً")
            }
        }

        val roundNum = obj.optInt("roundNum", 0)
        val roundName = if (roundNum > 0) "الجولة $roundNum" else obj.optJSONObject("stage")?.optString("name", "")?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        val venueObj = obj.optJSONObject("venue")
        val stadium = venueObj?.optString("name", "")?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        val roundOrStadium = roundName ?: stadium

        val tvArr = obj.optJSONArray("tvNetworks") ?: obj.optJSONArray("broadcasts")
        val channel = if (tvArr != null && tvArr.length() > 0) {
            tvArr.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        } else null

        val isUcl = (compId == 572 || compName.contains("أبطال أوروبا") || compName.contains("Champions League")) &&
                    !compLower.contains("شباب") && !compLower.contains("19")

        return Match(
            id = id,
            competition = compName,
            homeTeam = homeName,
            homeTeamLogoUrl = homeLogo,
            awayTeam = awayName,
            awayTeamLogoUrl = awayLogo,
            homeScore = if (status == MatchStatus.UPCOMING) null else homeScoreVal,
            awayScore = if (status == MatchStatus.UPCOMING) null else awayScoreVal,
            status = status,
            startTimeLabel = startTimeLabel,
            sport = "Football",
            channel = channel,
            roundOrStadium = roundOrStadium,
            roundNumber = roundName,
            stadium = stadium,
            dateCategory = dateParam,
            kickoffDateFormatted = formattedDate,
            isUcl = isUcl,
            competitionLogoUrl = compLogo
        )
    }

    private fun parseWebwsAllscoresGame(obj: JSONObject, compName: String, compId: Int, dateParam: String): Match? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val sportId = obj.optInt("sportId", 1)
        if (sportId != 1) return null // Strict Football Only: Reject Basketball, Tennis, Handball, etc.

        // Explicitly reject Youth League (6297) and FA Cup Preliminary (7856)
        if (compId == 6297 || compId == 7856) return null

        val compLower = compName.lowercase()
        if (compLower.contains("للشباب") || compLower.contains("الجولات التمهيدية") ||
            compLower.contains("التصفيات الأولية") || compLower.contains("التصفيات التمهيدية") ||
            compLower.contains("youth league") || compLower.contains("تحت 19") ||
            compLower.contains("u19") || compLower.contains("تحت 21") || compLower.contains("u21")) {
            return null
        }

        val homeObj = obj.optJSONObject("homeCompetitor") ?: return null
        val awayObj = obj.optJSONObject("awayCompetitor") ?: return null

        val homeName = homeObj.optString("name", "الفريق المضيف")
        val awayName = awayObj.optString("name", "الفريق الضيف")

        val homeLower = homeName.lowercase()
        val awayLower = awayName.lowercase()
        if (homeLower.contains("تحت 19") || awayLower.contains("تحت 19") ||
            homeLower.contains("u19") || awayLower.contains("u19") ||
            homeLower.contains("تحت 21") || awayLower.contains("تحت 21")) {
            return null
        }

        val homeId = homeObj.optInt("id", 0)
        val homeLogo = if (homeId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$homeId" else ""

        val awayId = awayObj.optInt("id", 0)
        val awayLogo = if (awayId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitors/$awayId" else ""

        val homeScoreRaw = homeObj.optDouble("score", -1.0)
        val awayScoreRaw = awayObj.optDouble("score", -1.0)
        val homeScore = if (homeScoreRaw >= 0) homeScoreRaw.toInt() else null
        val awayScore = if (awayScoreRaw >= 0) awayScoreRaw.toInt() else null

        val statusGroup = obj.optInt("statusGroup", 2)
        val statusText = obj.optString("statusText", "")
        val gameTimeDisplay = obj.optString("gameTimeDisplay", "").trim()
        val gameTime = obj.optDouble("gameTime", -1.0)

        val isHalftime = statusText.contains("استراحة") || statusText.contains("بين الشوطين") || statusText == "شوط" || statusText.contains("Half", true) || statusText.contains("HT", true)
        val isExtraTime = statusText.contains("إضافي") || statusText.contains("ET", true)
        val isPenalties = statusText.contains("ترجيح") || statusText.contains("Penalties", true) || statusText.contains("PEN", true)

        val status = when {
            statusGroup == 3 || gameTimeDisplay.contains("'") || statusText.contains("الشوط") || isHalftime || isExtraTime || isPenalties -> MatchStatus.LIVE
            statusGroup == 4 || statusText.contains("انتهت") || statusText.contains("FT", true) -> MatchStatus.FINISHED
            else -> MatchStatus.UPCOMING
        }

        // Real-time dynamic game minute / status label
        val formattedMinute = when {
            isHalftime -> "استراحة"
            isPenalties -> "ركلات ترجيح"
            isExtraTime -> if (gameTimeDisplay.isNotBlank()) "إضافي $gameTimeDisplay" else "شوط إضافي"
            gameTimeDisplay.isNotBlank() -> gameTimeDisplay
            gameTime > 0 -> "${gameTime.toInt()}'"
            status == MatchStatus.LIVE -> if (statusText.isNotBlank()) statusText else "مباشر"
            else -> null
        }

        val startTimeStr = obj.optString("startTime", "")
        var timeLabel = statusText
        if (startTimeStr.isNotBlank()) {
            try {
                val cleanDateStr = if (startTimeStr.contains("+")) startTimeStr.substringBefore("+") else startTimeStr.substringBefore("Z")
                val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Baghdad")
                }
                val date = inFormat.parse(cleanDateStr)
                if (date != null) {
                    val outFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
                        timeZone = TimeZone.getDefault()
                    }
                    timeLabel = outFormat.format(date)
                }
            } catch (_: Exception) {}
        }

        val compLogo = if (compId > 0) "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit/Competitions/$compId" else ""
        val isUcl = (compId == 572 || compName.contains("أبطال أوروبا") || compName.contains("Champions League")) &&
                    !compLower.contains("شباب") && !compLower.contains("19")
        val roundNum = obj.optInt("roundNum", 0)
        val rawRoundName = obj.optString("roundName", "").takeIf { it.isNotBlank() && !it.equals("null", true) }
        val stageName = obj.optString("stageName", "").takeIf { it.isNotBlank() && !it.equals("null", true) }

        val formattedRound = when {
            roundNum > 0 -> {
                val prefix = if (!rawRoundName.isNullOrBlank() && rawRoundName != "null") rawRoundName.trim() else "الجولة"
                "$prefix $roundNum"
            }
            !stageName.isNullOrBlank() && stageName != "null" -> stageName.trim()
            !rawRoundName.isNullOrBlank() && rawRoundName != "الجولة" && rawRoundName != "جولة" && rawRoundName != "null" -> rawRoundName.trim()
            else -> null
        }

        return Match(
            id = "m_$id",
            competition = compName,
            homeTeam = homeName,
            homeTeamLogoUrl = homeLogo,
            awayTeam = awayName,
            awayTeamLogoUrl = awayLogo,
            homeScore = if (status == MatchStatus.UPCOMING) null else homeScore,
            awayScore = if (status == MatchStatus.UPCOMING) null else awayScore,
            status = status,
            startTimeLabel = timeLabel,
            sport = "Football",
            channel = if (isUcl) "beIN Sports 1 HD" else null,
            roundOrStadium = formattedRound,
            roundNumber = formattedRound,
            dateCategory = dateParam,
            isUcl = isUcl,
            competitionLogoUrl = compLogo,
            gameMinute = formattedMinute
        )
    }

    private fun convertDateTo365Format(param: String): String {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return when (param) {
            "yesterday" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                sdf.format(cal.time)
            }
            "tomorrow" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                sdf.format(cal.time)
            }
            "today" -> sdf.format(cal.time)
            else -> {
                if (param.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                    try {
                        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(param)
                        if (d != null) sdf.format(d) else sdf.format(cal.time)
                    } catch (_: Exception) {
                        sdf.format(cal.time)
                    }
                } else {
                    sdf.format(cal.time)
                }
            }
        }
    }
}
