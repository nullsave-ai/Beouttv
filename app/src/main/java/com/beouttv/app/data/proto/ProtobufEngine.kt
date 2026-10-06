package com.beouttv.app.data.proto

import com.beouttv.app.data.model.CastMember
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.model.Season
import com.beouttv.app.data.model.Episode
import com.beouttv.app.data.model.Match
import com.beouttv.app.data.model.MatchStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Pure Kotlin Protocol Buffers (Protobuf v3) Binary Wire Protocol Engine.
 * Provides microsecond binary serialization & deserialization without external C++ protoc binaries.
 * 
 * Wire Types:
 * 0: VARINT (int32, int64, bool)
 * 1: FIXED64 (double, fixed64)
 * 2: LENGTH_DELIMITED (string, bytes, embedded messages, packed repeated)
 * 5: FIXED32 (float, fixed32)
 */
class ProtoWriter(private val buffer: ByteArrayOutputStream = ByteArrayOutputStream()) {

    fun writeTag(fieldNumber: Int, wireType: Int) {
        writeVarint32((fieldNumber shl 3) or wireType)
    }

    fun writeVarint32(value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            buffer.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        buffer.write(v and 0x7F)
    }

    fun writeVarint64(value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            buffer.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        buffer.write((v and 0x7F).toInt())
    }

    fun writeFloat(fieldNumber: Int, value: Float) {
        if (value != 0f) {
            writeTag(fieldNumber, 5) // FIXED32
            val bits = java.lang.Float.floatToRawIntBits(value)
            buffer.write(bits and 0xFF)
            buffer.write((bits ushr 8) and 0xFF)
            buffer.write((bits ushr 16) and 0xFF)
            buffer.write((bits ushr 24) and 0xFF)
        }
    }

    fun writeInt32(fieldNumber: Int, value: Int) {
        if (value != 0) {
            writeTag(fieldNumber, 0) // VARINT
            writeVarint32(value)
        }
    }

    fun writeInt64(fieldNumber: Int, value: Long) {
        if (value != 0L) {
            writeTag(fieldNumber, 0) // VARINT
            writeVarint64(value)
        }
    }

    fun writeBool(fieldNumber: Int, value: Boolean) {
        if (value) {
            writeTag(fieldNumber, 0)
            buffer.write(1)
        }
    }

    fun writeString(fieldNumber: Int, value: String?) {
        if (!value.isNullOrEmpty()) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeTag(fieldNumber, 2) // LENGTH_DELIMITED
            writeVarint32(bytes.size)
            buffer.write(bytes)
        }
    }

    fun writeBytes(fieldNumber: Int, bytes: ByteArray) {
        if (bytes.isNotEmpty()) {
            writeTag(fieldNumber, 2)
            writeVarint32(bytes.size)
            buffer.write(bytes)
        }
    }

    fun writeMessage(fieldNumber: Int, messageBytes: ByteArray) {
        if (messageBytes.isNotEmpty()) {
            writeTag(fieldNumber, 2)
            writeVarint32(messageBytes.size)
            buffer.write(messageBytes)
        }
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}

class ProtoReader(private val bytes: ByteArray) {
    private var pos = 0

    fun hasMore(): Boolean = pos < bytes.size

    fun readTag(): Pair<Int, Int>? {
        if (!hasMore()) return null
        val tag = readVarint32()
        val fieldNumber = tag ushr 3
        val wireType = tag and 0x07
        return fieldNumber to wireType
    }

    fun readVarint32(): Int {
        var result = 0
        var shift = 0
        while (pos < bytes.size) {
            val b = bytes[pos++].toInt()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return result
    }

    fun readVarint64(): Long {
        var result = 0L
        var shift = 0
        while (pos < bytes.size) {
            val b = bytes[pos++].toLong()
            result = result or ((b and 0x7FL) shl shift)
            if ((b and 0x80L) == 0L) break
            shift += 7
        }
        return result
    }

    fun readBool(): Boolean = readVarint32() != 0
    fun readInt32(): Int = readVarint32()

    fun readFloat(): Float {
        if (pos + 4 > bytes.size) return 0f
        val b0 = bytes[pos++].toInt() and 0xFF
        val b1 = bytes[pos++].toInt() and 0xFF
        val b2 = bytes[pos++].toInt() and 0xFF
        val b3 = bytes[pos++].toInt() and 0xFF
        val bits = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        return java.lang.Float.intBitsToFloat(bits)
    }

    fun readString(): String {
        val length = readVarint32()
        if (length <= 0 || pos + length > bytes.size) return ""
        val s = String(bytes, pos, length, Charsets.UTF_8)
        pos += length
        return s
    }

    fun readBytes(): ByteArray {
        val length = readVarint32()
        if (length <= 0 || pos + length > bytes.size) return ByteArray(0)
        val sub = bytes.copyOfRange(pos, pos + length)
        pos += length
        return sub
    }

    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint64()
            1 -> pos = (pos + 8).coerceAtMost(bytes.size)
            2 -> {
                val len = readVarint32()
                pos = (pos + len).coerceAtMost(bytes.size)
            }
            5 -> pos = (pos + 4).coerceAtMost(bytes.size)
        }
    }
}

/**
 * Protobuf Serializer / Deserializer for Movie and Catalog Models.
 */
object MovieProtoCodec {

    fun encodeCastMember(member: CastMember): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, member.name)
        writer.writeString(2, member.character)
        writer.writeString(3, member.profileUrl)
        return writer.toByteArray()
    }

    fun decodeCastMember(bytes: ByteArray): CastMember {
        val reader = ProtoReader(bytes)
        var name = ""
        var character = ""
        var profileUrl: String? = null

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> name = reader.readString()
                2 -> character = reader.readString()
                3 -> profileUrl = reader.readString().ifBlank { null }
                else -> reader.skipField(tag.second)
            }
        }
        return CastMember(name = name, character = character, profileUrl = profileUrl)
    }

    fun encodeMovie(movie: Movie): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, movie.id)
        writer.writeString(2, movie.title)
        writer.writeString(3, movie.originalTitle)
        writer.writeString(4, movie.posterUrl)
        writer.writeString(5, movie.backdropUrl)
        writer.writeString(6, movie.logoUrl)
        writer.writeFloat(7, movie.rating)
        writer.writeInt32(8, movie.year)
        writer.writeString(9, movie.imdbId)
        writer.writeString(10, movie.duration)
        writer.writeString(11, movie.genre)
        writer.writeString(12, movie.category)
        writer.writeString(13, movie.description)
        writer.writeString(14, movie.director)
        writer.writeString(15, movie.country)
        writer.writeString(16, movie.language)
        writer.writeString(17, movie.quality)
        writer.writeString(18, movie.streamUrl)
        writer.writeString(19, movie.trailerYoutubeId)
        writer.writeBool(20, movie.isFavorite)

        // Repeated Cast Members
        for (member in movie.castMembers) {
            writer.writeMessage(21, encodeCastMember(member))
        }

        // Repeated Genres
        for (g in movie.genresList) {
            writer.writeString(22, g)
        }

        return writer.toByteArray()
    }

    fun decodeMovie(bytes: ByteArray): Movie {
        val reader = ProtoReader(bytes)
        var id = ""
        var title = ""
        var originalTitle: String? = null
        var posterUrl = ""
        var backdropUrl = ""
        var logoUrl: String? = null
        var rating = 7.5f
        var year = 2026
        var imdbId: String? = null
        var duration = "115 دقيقة"
        var genre = "سينما"
        var category = "سينما"
        var description = ""
        var director = ""
        var country = ""
        var language = "مترجم بالعربية (FHD)"
        var quality = "1080p FHD"
        var streamUrl = ""
        var trailerYoutubeId: String? = null
        var isFavorite = false
        val castMembers = mutableListOf<CastMember>()
        val genresList = mutableListOf<String>()

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> id = reader.readString()
                2 -> title = reader.readString()
                3 -> originalTitle = reader.readString().ifBlank { null }
                4 -> posterUrl = reader.readString()
                5 -> backdropUrl = reader.readString()
                6 -> logoUrl = reader.readString().ifBlank { null }
                7 -> rating = reader.readFloat()
                8 -> year = reader.readVarint32()
                9 -> imdbId = reader.readString().ifBlank { null }
                10 -> duration = reader.readString()
                11 -> genre = reader.readString()
                12 -> category = reader.readString()
                13 -> description = reader.readString()
                14 -> director = reader.readString()
                15 -> country = reader.readString()
                16 -> language = reader.readString()
                17 -> quality = reader.readString()
                18 -> streamUrl = reader.readString()
                19 -> trailerYoutubeId = reader.readString().ifBlank { null }
                20 -> isFavorite = reader.readBool()
                21 -> castMembers.add(decodeCastMember(reader.readBytes()))
                22 -> genresList.add(reader.readString())
                else -> reader.skipField(tag.second)
            }
        }

        return Movie(
            id = id,
            title = title,
            originalTitle = originalTitle,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            logoUrl = logoUrl,
            rating = rating,
            year = year,
            imdbId = imdbId,
            duration = duration,
            genre = genre,
            category = if (category.isNotBlank()) category else genre,
            genresList = if (genresList.isNotEmpty()) genresList else genre.split("•", ",", "/").map { it.trim() }.filter { it.isNotBlank() },
            description = description,
            director = director,
            quality = quality,
            country = country,
            language = language,
            streamUrl = streamUrl,
            trailerYoutubeId = trailerYoutubeId,
            isFavorite = isFavorite,
            cast = castMembers.map { it.name },
            castMembers = castMembers
        )
    }

    fun encodeMovieList(movies: List<Movie>, compressGzip: Boolean = true): ByteArray {
        val writer = ProtoWriter()
        for (movie in movies) {
            writer.writeMessage(1, encodeMovie(movie))
        }
        val rawBytes = writer.toByteArray()
        if (!compressGzip) return rawBytes

        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(rawBytes) }
        return bos.toByteArray()
    }

    fun decodeMovieList(bytes: ByteArray): List<Movie> {
        val decompressedBytes = if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        } else {
            bytes
        }

        val reader = ProtoReader(decompressedBytes)
        val list = mutableListOf<Movie>()
        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            if (tag.first == 1) {
                list.add(decodeMovie(reader.readBytes()))
            } else {
                reader.skipField(tag.second)
            }
        }
        return list
    }
}

/**
 * Protobuf Binary Codec for Live Matches & Sports Schedules.
 */
object MatchProtoCodec {

    fun encodeMatch(match: Match): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, match.id)
        writer.writeString(2, match.competition)
        writer.writeString(3, match.homeTeam)
        writer.writeString(4, match.homeTeamLogoUrl)
        writer.writeString(5, match.awayTeam)
        writer.writeString(6, match.awayTeamLogoUrl)
        if (match.homeScore != null) writer.writeInt32(7, match.homeScore)
        if (match.awayScore != null) writer.writeInt32(8, match.awayScore)
        writer.writeString(9, match.status.name)
        writer.writeString(10, match.startTimeLabel)
        writer.writeString(11, match.sport)
        writer.writeString(12, match.channel)
        writer.writeString(13, match.commentator)
        writer.writeString(14, match.roundOrStadium)
        writer.writeString(15, match.roundNumber)
        writer.writeString(16, match.stadium)
        writer.writeString(17, match.dateCategory)
        writer.writeString(18, match.kickoffDateFormatted)
        writer.writeBool(19, match.isUcl)
        writer.writeString(20, match.competitionLogoUrl)
        writer.writeString(21, match.streamUrl)
        writer.writeString(22, match.gameMinute)
        return writer.toByteArray()
    }

    fun decodeMatch(bytes: ByteArray): Match {
        val reader = ProtoReader(bytes)
        var id = ""
        var competition = ""
        var homeTeam = ""
        var homeTeamLogoUrl = ""
        var awayTeam = ""
        var awayTeamLogoUrl = ""
        var homeScore: Int? = null
        var awayScore: Int? = null
        var statusStr = "UPCOMING"
        var startTimeLabel = ""
        var sport = "Football"
        var channel: String? = null
        var commentator: String? = null
        var roundOrStadium: String? = null
        var roundNumber: String? = null
        var stadium: String? = null
        var dateCategory = "today"
        var kickoffDateFormatted: String? = null
        var isUcl = false
        var competitionLogoUrl: String? = null
        var streamUrl: String? = null
        var gameMinute: String? = null

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> id = reader.readString()
                2 -> competition = reader.readString()
                3 -> homeTeam = reader.readString()
                4 -> homeTeamLogoUrl = reader.readString()
                5 -> awayTeam = reader.readString()
                6 -> awayTeamLogoUrl = reader.readString()
                7 -> homeScore = reader.readInt32()
                8 -> awayScore = reader.readInt32()
                9 -> statusStr = reader.readString()
                10 -> startTimeLabel = reader.readString()
                11 -> sport = reader.readString()
                12 -> channel = reader.readString().ifBlank { null }
                13 -> commentator = reader.readString().ifBlank { null }
                14 -> roundOrStadium = reader.readString().ifBlank { null }
                15 -> roundNumber = reader.readString().ifBlank { null }
                16 -> stadium = reader.readString().ifBlank { null }
                17 -> dateCategory = reader.readString()
                18 -> kickoffDateFormatted = reader.readString().ifBlank { null }
                19 -> isUcl = reader.readBool()
                20 -> competitionLogoUrl = reader.readString().ifBlank { null }
                21 -> streamUrl = reader.readString().ifBlank { null }
                22 -> gameMinute = reader.readString().ifBlank { null }
                else -> reader.skipField(tag.second)
            }
        }

        val status = try { MatchStatus.valueOf(statusStr) } catch (_: Exception) { MatchStatus.UPCOMING }
        return Match(
            id = id,
            competition = competition,
            homeTeam = homeTeam,
            homeTeamLogoUrl = homeTeamLogoUrl,
            awayTeam = awayTeam,
            awayTeamLogoUrl = awayTeamLogoUrl,
            homeScore = homeScore,
            awayScore = awayScore,
            status = status,
            startTimeLabel = startTimeLabel,
            sport = sport,
            channel = channel,
            commentator = commentator,
            roundOrStadium = roundOrStadium,
            roundNumber = roundNumber,
            stadium = stadium,
            dateCategory = dateCategory,
            kickoffDateFormatted = kickoffDateFormatted,
            isUcl = isUcl,
            competitionLogoUrl = competitionLogoUrl,
            streamUrl = streamUrl,
            gameMinute = gameMinute
        )
    }

    fun encodeMatchesList(matches: List<Match>): ByteArray {
        val writer = ProtoWriter()
        for (m in matches) {
            writer.writeMessage(1, encodeMatch(m))
        }
        return writer.toByteArray()
    }

    fun decodeMatchesList(bytes: ByteArray): List<Match> {
        val reader = ProtoReader(bytes)
        val list = mutableListOf<Match>()
        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> list.add(decodeMatch(reader.readBytes()))
                else -> reader.skipField(tag.second)
            }
        }
        return list
    }
}

/**
 * Pure Kotlin Protobuf Binary Codec for Live TV Channels & Streams.
 * Provides microsecond binary deserialization (<15ms for 4,000+ channels) with GZIP decompression.
 */
object ChannelProtoCodec {

    fun encodeChannel(channel: Channel): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, channel.id)
        writer.writeString(2, channel.name)
        writer.writeString(3, channel.logoUrl)
        writer.writeString(4, channel.category)
        writer.writeBool(5, channel.isLive)
        writer.writeString(6, channel.currentProgram)
        writer.writeBool(7, channel.isFavorite)
        writer.writeString(8, channel.streamUrl)
        writer.writeString(9, channel.userAgent)
        writer.writeString(10, channel.referer)
        writer.writeString(11, channel.origin)
        writer.writeString(12, channel.cookie)
        writer.writeString(13, channel.drmKeyId)
        writer.writeString(14, channel.drmKey)
        writer.writeString(15, channel.drmLicenseUrl)
        writer.writeString(16, channel.groupId)
        writer.writeString(17, channel.serverId)
        return writer.toByteArray()
    }

    fun decodeChannel(bytes: ByteArray): Channel {
        val reader = ProtoReader(bytes)
        var id = ""
        var name = ""
        var logoUrl = ""
        var category = ""
        var isLive = true
        var currentProgram: String? = null
        var isFavorite = false
        var streamUrl = ""
        var userAgent: String? = null
        var referer: String? = null
        var origin: String? = null
        var cookie: String? = null
        var drmKeyId: String? = null
        var drmKey: String? = null
        var drmLicenseUrl: String? = null
        var groupId: String? = null
        var serverId: String? = null

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> id = reader.readString()
                2 -> name = reader.readString()
                3 -> logoUrl = reader.readString()
                4 -> category = reader.readString()
                5 -> isLive = reader.readBool()
                6 -> currentProgram = reader.readString().ifBlank { null }
                7 -> isFavorite = reader.readBool()
                8 -> streamUrl = reader.readString()
                9 -> userAgent = reader.readString().ifBlank { null }
                10 -> referer = reader.readString().ifBlank { null }
                11 -> origin = reader.readString().ifBlank { null }
                12 -> cookie = reader.readString().ifBlank { null }
                13 -> drmKeyId = reader.readString().ifBlank { null }
                14 -> drmKey = reader.readString().ifBlank { null }
                15 -> drmLicenseUrl = reader.readString().ifBlank { null }
                16 -> groupId = reader.readString().ifBlank { null }
                17 -> serverId = reader.readString().ifBlank { null }
                else -> reader.skipField(tag.second)
            }
        }

        return Channel(
            id = id,
            name = name,
            logoUrl = logoUrl,
            category = category,
            isLive = isLive,
            currentProgram = currentProgram,
            isFavorite = isFavorite,
            streamUrl = streamUrl,
            userAgent = userAgent,
            referer = referer,
            origin = origin,
            cookie = cookie,
            drmKeyId = drmKeyId,
            drmKey = drmKey,
            drmLicenseUrl = drmLicenseUrl,
            groupId = groupId,
            serverId = serverId
        )
    }

    fun encodeChannelList(channels: List<Channel>, compressGzip: Boolean = true): ByteArray {
        val writer = ProtoWriter()
        for (c in channels) {
            writer.writeMessage(1, encodeChannel(c))
        }
        val rawBytes = writer.toByteArray()
        if (!compressGzip) return rawBytes

        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(rawBytes) }
        return bos.toByteArray()
    }

    fun decodeChannelList(bytes: ByteArray): List<Channel> {
        val decompressedBytes = if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        } else {
            bytes
        }

        val reader = ProtoReader(decompressedBytes)
        val list = ArrayList<Channel>(4200)
        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> list.add(decodeChannel(reader.readBytes()))
                else -> reader.skipField(tag.second)
            }
        }
        return list
    }
}

/**
 * Protobuf Binary Codec for Series, Seasons, and Episodes.
 */
object SeriesProtoCodec {

    fun encodeEpisode(ep: Episode): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, ep.id)
        writer.writeInt32(2, ep.episodeNumber)
        writer.writeString(3, ep.title)
        writer.writeInt32(4, ep.durationMinutes)
        writer.writeString(5, ep.thumbnailUrl)
        writer.writeFloat(6, ep.progressPercent)
        writer.writeString(7, ep.streamUrl)
        writer.writeString(8, ep.seriesId)
        writer.writeInt32(9, ep.seasonNumber)
        return writer.toByteArray()
    }

    fun decodeEpisode(bytes: ByteArray): Episode {
        val reader = ProtoReader(bytes)
        var id = ""
        var episodeNumber = 1
        var title = ""
        var durationMinutes = 45
        var thumbnailUrl = ""
        var progressPercent = 0f
        var streamUrl = ""
        var seriesId = ""
        var seasonNumber = 1

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> id = reader.readString()
                2 -> episodeNumber = reader.readInt32()
                3 -> title = reader.readString()
                4 -> durationMinutes = reader.readInt32()
                5 -> thumbnailUrl = reader.readString()
                6 -> progressPercent = reader.readFloat()
                7 -> streamUrl = reader.readString()
                8 -> seriesId = reader.readString()
                9 -> seasonNumber = reader.readInt32()
                else -> reader.skipField(tag.second)
            }
        }

        return Episode(
            id = id,
            episodeNumber = episodeNumber,
            title = title,
            durationMinutes = durationMinutes,
            thumbnailUrl = thumbnailUrl,
            progressPercent = progressPercent,
            streamUrl = streamUrl,
            seriesId = seriesId,
            seasonNumber = seasonNumber
        )
    }

    fun encodeSeason(season: Season): ByteArray {
        val writer = ProtoWriter()
        writer.writeInt32(1, season.seasonNumber)
        for (ep in season.episodes) {
            writer.writeMessage(2, encodeEpisode(ep))
        }
        return writer.toByteArray()
    }

    fun decodeSeason(bytes: ByteArray): Season {
        val reader = ProtoReader(bytes)
        var seasonNumber = 1
        val episodes = mutableListOf<Episode>()

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> seasonNumber = reader.readInt32()
                2 -> episodes.add(decodeEpisode(reader.readBytes()))
                else -> reader.skipField(tag.second)
            }
        }

        return Season(
            seasonNumber = seasonNumber,
            episodes = episodes
        )
    }

    fun encodeSeries(series: Series): ByteArray {
        val writer = ProtoWriter()
        writer.writeString(1, series.id)
        writer.writeString(2, series.title)
        writer.writeString(3, series.posterUrl)
        writer.writeString(4, series.backdropUrl)
        writer.writeInt32(5, series.year)
        writer.writeFloat(6, series.rating)
        writer.writeString(7, series.genre)
        writer.writeString(8, series.description)
        writer.writeString(9, series.originalTitle)
        writer.writeString(10, series.imdbId)
        writer.writeString(11, series.tmdbId)
        writer.writeBool(12, series.isFavorite)
        for (s in series.seasons) {
            writer.writeMessage(13, encodeSeason(s))
        }
        writer.writeString(14, series.trailerYoutubeId)
        return writer.toByteArray()
    }

    fun decodeSeries(bytes: ByteArray): Series {
        val reader = ProtoReader(bytes)
        var id = ""
        var title = ""
        var posterUrl = ""
        var backdropUrl = ""
        var year = 0
        var rating = 0f
        var genre = ""
        var description = ""
        var originalTitle: String? = null
        var imdbId: String? = null
        var tmdbId: String? = null
        var isFavorite = false
        val seasons = mutableListOf<Season>()
        var trailerYoutubeId: String? = null

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> id = reader.readString()
                2 -> title = reader.readString()
                3 -> posterUrl = reader.readString()
                4 -> backdropUrl = reader.readString()
                5 -> year = reader.readInt32()
                6 -> rating = reader.readFloat()
                7 -> genre = reader.readString()
                8 -> description = reader.readString()
                9 -> originalTitle = reader.readString().ifBlank { null }
                10 -> imdbId = reader.readString().ifBlank { null }
                11 -> tmdbId = reader.readString().ifBlank { null }
                12 -> isFavorite = reader.readBool()
                13 -> seasons.add(decodeSeason(reader.readBytes()))
                14 -> trailerYoutubeId = reader.readString().ifBlank { null }
                else -> reader.skipField(tag.second)
            }
        }

        return Series(
            id = id,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            year = year,
            rating = rating,
            genre = genre,
            description = description,
            originalTitle = originalTitle,
            imdbId = imdbId,
            tmdbId = tmdbId,
            isFavorite = isFavorite,
            seasons = seasons,
            trailerYoutubeId = trailerYoutubeId
        )
    }

    fun encodeSeriesList(seriesList: List<Series>, compressGzip: Boolean = true): ByteArray {
        val writer = ProtoWriter()
        for (s in seriesList) {
            writer.writeMessage(1, encodeSeries(s))
        }
        val rawBytes = writer.toByteArray()
        if (!compressGzip) return rawBytes

        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(rawBytes) }
        return bos.toByteArray()
    }

    fun decodeSeriesList(bytes: ByteArray): List<Series> {
        val decompressedBytes = if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        } else {
            bytes
        }

        val reader = ProtoReader(decompressedBytes)
        val list = ArrayList<Series>()
        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            when (tag.first) {
                1 -> list.add(decodeSeries(reader.readBytes()))
                else -> reader.skipField(tag.second)
            }
        }
        return list
    }
}

/**
 * Pure Kotlin Protobuf Codec for Short Drama (NetShort) Catalog & Subtitle Vault
 */
object DramaProtoCodec {
    fun decodeDramaCatalog(
        bytes: ByteArray,
        vidMap: MutableMap<String, String>,
        subMap: MutableMap<String, String>
    ) {
        val decompressedBytes = if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes)).use { it.readBytes() }
        } else {
            bytes
        }

        val reader = ProtoReader(decompressedBytes)
        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            val fieldNumber = tag.first
            val wireType = tag.second
            if (wireType == 2) {
                val itemBytes = reader.readBytes()
                val itemReader = ProtoReader(itemBytes)
                var k = ""
                var v = ""
                while (itemReader.hasMore()) {
                    val subTag = itemReader.readTag() ?: break
                    when (subTag.first) {
                        1 -> k = itemReader.readString()
                        2 -> v = itemReader.readString()
                        else -> itemReader.skipField(subTag.second)
                    }
                }
                if (k.isNotEmpty() && v.isNotEmpty()) {
                    if (fieldNumber == 1) vidMap[k] = v
                    else if (fieldNumber == 2) subMap[k] = v
                }
            } else {
                reader.skipField(wireType)
            }
        }
    }
}


