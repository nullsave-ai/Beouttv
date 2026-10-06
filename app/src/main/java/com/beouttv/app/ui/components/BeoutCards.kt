package com.beouttv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beouttv.app.data.i18n.AppLanguage
import com.beouttv.app.data.i18n.LanguageManager
import com.beouttv.app.data.imdb.ImdbMetadataEngine
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Match
import com.beouttv.app.data.model.MatchStatus
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.theme.tv.tvFocusable

fun cleanChannelDisplayName(name: String): String {
    return name
        .replace(Regex("(?i)\\s*\\(\\s*(1080p|720p|480p|4k|uhd|fhd|hd|sd|50fps|60fps)[^)]*\\)"), "")
        .replace(Regex("(?i)\\b(1080p|720p|480p|4k|uhd|fhd|50fps|60fps)\\b"), "")
        .replace(Regex("(?i)\\bVIP\\b"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun cleanCategoryDisplayName(category: String): String {
    return category
        .replace("باقة", "")
        .replace(Regex("(?i)\\s*\\(\\s*(1080p|720p|480p|4k|uhd|fhd|hd|sd|50fps|60fps)[^)]*\\)"), "")
        .replace(Regex("(?i)\\b(1080p|720p|480p|4k|uhd|fhd|50fps|60fps)\\b"), "")
        .replace(Regex("(?i)\\bVIP\\b"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
fun BeoutChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isTvMode: Boolean? = null
) {
    val tvMode = isTvMode ?: DeviceTypeDetector.isTvModeFlow.value
    val cleanName = remember(channel.name) { cleanChannelDisplayName(channel.name) }
    val cleanCat = remember(channel.category) { cleanCategoryDisplayName(channel.category) }
    val defaultWidth = if (tvMode) BeoutDimensions.TvChannelCardWidth else BeoutDimensions.ChannelCardWidth
    val subtitle = remember(channel.currentProgram, cleanCat) {
        channel.currentProgram?.let { prog ->
            if (prog.contains("بث مباشر", true) || prog.contains("live", true) || prog.contains("canlı", true) || prog.contains("زنده", true) || prog.contains("direct", true)) {
                com.beouttv.app.data.i18n.tr("بث مباشر عالي الدقة", "HD Live Stream", "HD Canlı Yayın", "پخش زنده با کیفیت بالا", "Diffusion en direct HD")
            } else prog
        } ?: cleanCat
    }

    Column(
        modifier = Modifier
            .width(defaultWidth)
            .then(modifier)
            .tvFocusable(
                shape = BeoutShapes.Medium,
                onEnter = onClick
            )
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .clickable(onClick = onClick)
            .padding(BeoutDimensions.SpaceS)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(BeoutShapes.Small)
                    .background(BeoutColors.ElevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LiveTv,
                    contentDescription = null,
                    tint = BeoutColors.TextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp)
                )
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = cleanName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (channel.isLive) {
                LiveBadge(modifier = Modifier.align(Alignment.TopStart))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    .tvFocusable(shape = CircleShape, onEnter = onFavoriteClick)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            cleanName,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp),
            color = BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.sp),
            color = BeoutColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BeoutMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTvMode: Boolean? = null,
    language: AppLanguage? = null
) {
    val currentLang = language ?: LanguageManager.currentLanguage.value
    val displayTitle = remember(movie.id, movie.title, currentLang) {
        when (currentLang) {
            AppLanguage.ENGLISH,
            AppLanguage.TURKISH,
            AppLanguage.FRENCH -> {
                if (!movie.originalTitle.isNullOrBlank()) movie.originalTitle!! else ImdbMetadataEngine.cleanDisplayTitle(movie.title)
            }
            AppLanguage.ARABIC,
            AppLanguage.PERSIAN -> ImdbMetadataEngine.cleanDisplayTitle(movie.title)
        }
    }
    val genreSub = remember(movie.id, movie.genre, currentLang) {
        val cat = ImdbMetadataEngine.localizeCategoryString(movie.genre, currentLang)
        cat.split("•").map { it.trim() }.filter { it.isNotBlank() }.take(2).joinToString(" • ").ifBlank { "${movie.year}" }
    }
    val qualityLabel = remember(movie.quality) {
        movie.quality.split(" ").firstOrNull() ?: "FHD"
    }

    val tvMode = isTvMode ?: DeviceTypeDetector.isTvModeFlow.value
    val defaultWidth = if (tvMode) BeoutDimensions.TvPosterCardWidth else BeoutDimensions.PosterCardWidth

    Column(
        modifier = Modifier
            .width(defaultWidth)
            .then(modifier)
            .tvFocusable(
                shape = BeoutShapes.Medium,
                onEnter = onClick
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(BeoutShapes.Medium)
                .background(BeoutColors.ElevatedSurface)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top-Start Quality/Year Badge
            if (movie.year > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(BeoutShapes.Small)
                        .background(BeoutColors.ElevatedSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${movie.year}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = BeoutColors.BrightBlue
                    )
                }
            }
        }
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            text = displayTitle,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp),
            color = BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = genreSub,
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.sp),
                color = BeoutColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = qualityLabel,
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 9.sp),
                color = BeoutColors.TextDisabled
            )
        }
    }
}

@Composable
fun BeoutSeriesCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTvMode: Boolean? = null
) {
    val tvMode = isTvMode ?: DeviceTypeDetector.isTvModeFlow.value
    val defaultWidth = if (tvMode) BeoutDimensions.TvPosterCardWidth else BeoutDimensions.PosterCardWidth
    val genreSub = remember(series.id, series.genre, series.year) {
        if (series.year > 0) "${series.year} • ${series.genre}" else series.genre
    }

    Column(
        modifier = Modifier
            .width(defaultWidth)
            .then(modifier)
            .tvFocusable(
                shape = BeoutShapes.Medium,
                onEnter = onClick
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(BeoutShapes.Medium)
                .background(BeoutColors.ElevatedSurface)
        ) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top-Start Year Badge
            if (series.year > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(BeoutShapes.Small)
                        .background(BeoutColors.ElevatedSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${series.year}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = BeoutColors.BrightBlue
                    )
                }
            }
        }
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            text = series.title,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp),
            color = BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = genreSub,
            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.sp),
            color = BeoutColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

object SportsTimeHelper {
    /**
     * Formats 24-hour time to 12-hour Arabic time with صباحاً / مساءً.
     * Examples:
     * "21:30" -> "9:30 مساءً"
     * "11:00" -> "11:00 صباحاً"
     * "00:45" -> "12:45 صباحاً"
     */
    fun format12HourTime(rawTime: String?): String {
        if (rawTime.isNullOrBlank() || rawTime.equals("null", true)) return ""
        val trimmed = rawTime.trim()

        val match = Regex("""(\d{1,2}):(\d{2})""").find(trimmed) ?: return trimmed
        val h24 = match.groupValues[1].toIntOrNull() ?: return trimmed
        val mins = match.groupValues[2]

        val period = if (h24 >= 12) "مساءً" else "صباحاً"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return "$h12:$mins $period"
    }

    /**
     * Generates a clean side-by-side date and 12-hour time string.
     */
    fun getFormattedDateTimeLabel(match: Match): String {
        val time12 = format12HourTime(match.startTimeLabel)
        val datePart = when (match.dateCategory) {
            "today" -> "اليوم"
            "tomorrow" -> "غداً"
            "yesterday" -> "أمس"
            else -> match.kickoffDateFormatted ?: match.dateCategory
        }

        return when (match.status) {
            MatchStatus.LIVE -> {
                val minText = match.gameMinute ?: match.startTimeLabel.takeIf { it.contains("'") } ?: "مباشر الآن"
                "مباشر • $minText"
            }
            MatchStatus.FINISHED -> {
                "انتهت • $datePart"
            }
            MatchStatus.UPCOMING -> {
                if (time12.isNotBlank()) "$datePart • $time12" else datePart
            }
        }
    }
}

@Composable
fun BeoutMatchCard(
    match: Match,
    modifier: Modifier = Modifier,
    isFavoriteMatch: Boolean = false,
    isHomeFavorite: Boolean = false,
    isAwayFavorite: Boolean = false,
    matchedChannel: Channel? = null,
    onToggleFavoriteTeam: ((String) -> Unit)? = null,
    onWatchClick: (Match) -> Unit = {},
    onChannelClick: ((Channel) -> Unit)? = null,
    onReminderClick: (Match) -> Unit = {}
) {
    val compLogo = LeagueLogoHelper.getLogo(match.competition, match.competitionLogoUrl)
    val formatted12hTime = remember(match.startTimeLabel) { SportsTimeHelper.format12HourTime(match.startTimeLabel) }
    val dateTimeLabel = remember(match) { SportsTimeHelper.getFormattedDateTimeLabel(match) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = BeoutShapes.Medium,
                onEnter = { if (match.status == MatchStatus.LIVE) onWatchClick(match) }
            )
            .clip(BeoutShapes.Medium)
            .border(
                BorderStroke(
                    1.dp,
                    if (isFavoriteMatch) Color(0x66FFB300) else BeoutColors.Divider
                ),
                BeoutShapes.Medium
            )
            .background(BeoutColors.Card)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val stadiumInfo = match.stadium ?: match.roundOrStadium?.takeIf { !it.contains("جولة", true) && !it.contains("دور", true) && !it.equals("null", true) }
        val dateLabelText = when (match.dateCategory) {
            "today" -> "اليوم"
            "tomorrow" -> "غداً"
            "yesterday" -> "أمس"
            else -> match.kickoffDateFormatted ?: match.dateCategory
        }

        // 1. Top Header: League Logo + League Name + Round Pill + Stadium (NO duplicate time!)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // League Logo
            AsyncImage(
                model = compLogo,
                contentDescription = match.competition,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.ElevatedSurface)
                    .padding(2.dp)
            )

            Spacer(Modifier.width(8.dp))

            // Competition Title
            Text(
                text = match.competition,
                style = BeoutTextStyles.CardTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = BeoutColors.TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1
            )

            // Round Number / Stage Pill
            val rawRound = match.roundNumber ?: match.roundOrStadium
            val roundText = if (!rawRound.isNullOrBlank() && !rawRound.equals("null", true) && !rawRound.equals("الجولة", true) && !rawRound.equals("جولة", true)) {
                rawRound
            } else null

            if (!roundText.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(BeoutShapes.Small)
                        .background(Color(0xFF1E293B))
                        .border(0.5.dp, Color(0xFF334155), BeoutShapes.Small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = roundText,
                        style = BeoutTextStyles.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Only Live or Finished badge in header (Upcoming time is cleanly placed under VS below!)
            when (match.status) {
                MatchStatus.LIVE -> {
                    val rawMinute = match.gameMinute ?: match.startTimeLabel.takeIf { it.contains("'") } ?: "مباشر"
                    val isHalftimeBadge = rawMinute.contains("استراحة") || (rawMinute.contains("شوط") && !rawMinute.contains("الأول") && !rawMinute.contains("الثاني") && !rawMinute.contains("إضافي"))
                    val isPenaltiesBadge = rawMinute.contains("ترجيح")

                    val badgeColor = when {
                        isHalftimeBadge -> Color(0xFFD97706)
                        isPenaltiesBadge -> Color(0xFF7C3AED)
                        else -> BeoutColors.LiveRed
                    }

                    val badgeText = when {
                        isHalftimeBadge -> "استراحة"
                        isPenaltiesBadge -> "ترجيح"
                        rawMinute.startsWith("مباشر") -> rawMinute
                        rawMinute.startsWith("إضافي") -> rawMinute
                        else -> "مباشر $rawMinute"
                    }

                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
                MatchStatus.FINISHED -> {
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = com.beouttv.app.data.i18n.S.finished(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextDisabled
                        )
                    }
                }
                MatchStatus.UPCOMING -> {
                    if (!stadiumInfo.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = BeoutColors.TextDisabled,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = stadiumInfo,
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                                color = BeoutColors.TextDisabled,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Teams & Score / VS Central Display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Home Team
            TeamColumn(
                name = match.homeTeam,
                logoUrl = match.homeTeamLogoUrl,
                modifier = Modifier.weight(1f)
            )

            // Center Display: Score for Live/Finished, or VS + Date & Time for Upcoming
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (match.status == MatchStatus.LIVE || match.status == MatchStatus.FINISHED) {
                    val hScore = match.homeScore ?: 0
                    val aScore = match.awayScore ?: 0
                    Text(
                        text = "$hScore  -  $aScore",
                        style = BeoutTextStyles.ScreenTitle.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (match.status == MatchStatus.LIVE) BeoutColors.LiveRed else BeoutColors.TextPrimary
                        )
                    )
                    Text(
                        text = if (match.status == MatchStatus.LIVE) "مباشر" else "النتيجة النهائية",
                        style = BeoutTextStyles.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = if (match.status == MatchStatus.LIVE) BeoutColors.LiveRed else BeoutColors.TextDisabled
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VS",
                            style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = BeoutColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$dateLabelText • ${if (formatted12hTime.isNotBlank()) formatted12hTime else match.startTimeLabel}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = BeoutColors.BrightBlue,
                        maxLines = 1
                    )
                }
            }

            // Away Team
            TeamColumn(
                name = match.awayTeam,
                logoUrl = match.awayTeamLogoUrl,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Footer: Commentator & Reminder (No Channels, No Click-to-Play)
        val rawCommentator = match.commentator?.takeIf { !it.equals("null", true) && it.isNotBlank() }
        if (rawCommentator != null || match.status == MatchStatus.UPCOMING) {
            Spacer(Modifier.height(BeoutDimensions.SpaceS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rawCommentator != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = rawCommentator,
                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                            color = BeoutColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (match.status == MatchStatus.UPCOMING) {
                    Box(
                        modifier = Modifier
                            .tvFocusable(
                                shape = BeoutShapes.Small,
                                onEnter = { onReminderClick(match) }
                            )
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .clickable { onReminderClick(match) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "تنبيه بالمباراة",
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.BrightBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Signature BeoutTV Club Badge Composable.
 * If the club doesn't have an image or fails to load, never show the ugly 365 default image!
 * Instead, displays a luxury radial-gradient circular badge with an electric blue border
 * and the club's monogram / bold initial letter.
 */
@Composable
fun BeoutTeamBadge(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val cleanUrl = remember(logoUrl) {
        if (logoUrl.isNullOrBlank() || 
            logoUrl.contains("default1.png") || 
            logoUrl.contains("default.png") || 
            logoUrl.contains("Competitors/0")) {
            ""
        } else {
            logoUrl.replace(",d_Competitors:default1.png", "")
                   .replace(",d_Competitions:default.png", "")
        }
    }

    val fallbackBadge: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E283C),
                            Color(0xFF0F1420)
                        )
                    )
                )
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initial = remember(name) {
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    trimmed.take(1).uppercase()
                } else "B"
            }
            Text(
                text = initial,
                style = BeoutTextStyles.CardTitle.copy(
                    fontSize = (size.value * 0.44f).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = BeoutColors.BrightBlue
            )
        }
    }

    if (cleanUrl.isNotBlank()) {
        coil.compose.SubcomposeAsyncImage(
            model = cleanUrl,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(BeoutColors.ElevatedSurface)
                .padding(if (size >= 32.dp) 4.dp else 2.dp),
            loading = { fallbackBadge() },
            error = { fallbackBadge() }
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            fallbackBadge()
        }
    }
}

@Composable
private fun TeamColumn(
    name: String,
    logoUrl: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BeoutTeamBadge(
            name = name,
            logoUrl = logoUrl,
            size = 48.dp
        )
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            text = name,
            style = BeoutTextStyles.CardTitle.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = BeoutColors.TextPrimary,
            maxLines = 1
        )
    }
}

object SportsFilterHelper {
    private val BLOCKED_KEYWORDS = listOf(
        "تحت 21", "تحت 19", "تحت 20", "تحت 23", "تحت 17", "تحت 18", "تحت 16", "تحت 15",
        "u21", "u19", "u20", "u23", "u17", "u18", "u-21", "u-19", "u-20",
        "دوري أبطال أوروبا للشباب", "للشباب", "youth league", "شباب",
        "كأس الاتحاد الإنجليزي - الجولات التمهيدية", "الجولات التمهيدية", "التصفيات الأولية", "التصفيات التمهيدية", "preliminary", "qualifying rounds",
        "الدرجة الثانية", "الدرجة الثالثة", "الدرجة الرابعة", "الدرجة الخامسة",
        "2nd division", "3rd division", "division 2", "division 3", "serie b", "serie c",
        "segunda", "liga 2", "liga 3", "2. bundesliga", "3. liga", "ligue 2", "championship",
        "سيدات", "نساء", "women", "female", "w-league", "feminin",
        "الهواة", "amateur", "الرديف", "reserve", "شبيبة", "دوري الشباب",
        "الدوري الثاني", "الدوري الثالث", "b division", "المناطق", "دوري المناطق",
        "next pro", "برايم بي", "primera b", "primera c", "pro league b",
        "الدنماركي الدرجة الأولى", "السويدي - الدرجة الأولى", "النرويجي - الدرجة الأولى",
        "النمساوي الدرجة الأولى", "الهولندي - الدرجة الأولى", "الروسي الدرجة الأولى",
        "التركي الدرجة الأولى", "الإماراتي الدرجة الأولى", "الأرجنتيني - برايم",
        "الأوكراني الدرجة الأولى", "الصيني - الدرجة الأولى", "المكسيكي الدرجة",
        "كرة السلة", "basketball", "كرة اليد", "handball", "كرة الطائرة", "volleyball",
        "كرة المضرب", "tennis", "تنس", "سلة", "كرة الماء", "بيسبول", "baseball", "هوكي", "hockey"
    )

    private val MAJOR_KEYWORDS = listOf(
        "أبطال أوروبا", "champions league", "الدوري الإنجليزي", "premier league",
        "الدوري الإسباني", "la liga", "الدوري الإيطالي", "serie a",
        "الدوري الألماني", "bundesliga", "الدوري الفرنسي", "ligue 1",
        "روشن", "السعودي", "saudi", "العراق", "iraq", "المصري", "egypt",
        "أدنوك", "الإماراتي", "uae", "قطر", "qatar", "المغربي", "botola", "morocco",
        "التونسي", "tunisia", "الجزائري", "algeria", "أبطال آسيا", "afc",
        "أبطال إفريقيا", "caf", "الدوري الأوروبي", "europa league",
        "المؤتمر", "conference league", "كأس العالم", "world cup",
        "كأس الاتحاد", "fa cup", "كأس ملك", "copa del rey", "كأس إيطاليا", "coppa italia",
        "كأس ألمانيا", "dfb", "كأس فرنسا", "coupe de france",
        "كأس خادم الحرمين", "كأس السوبر", "super cup", "supercup", "supercopa",
        "كأس الرابطة", "carabao", "efl cup", "كأس البرتغال", "الدوري البرتغالي", "liga portugal",
        "الدوري الهولندي", "eredivisie", "الدوري التركي", "super lig", "süper lig",
        "الدوري البلجيكي", "الدوري البرازيلي", "brasileirao", "brasileirão",
        "الدوري الأرجنتيني الممتاز", "الدوري الأمريكي الممتاز", "mls", "كوبا ليبرتادوريس", "libertadores",
        "كوبا سود أمريكانا", "sudamericana", "كأس أمم", "nations league",
        "الدوري الكويتي", "الدوري الأردني", "الدوري العماني", "الدوري البحريني",
        "الدوري الأسكتلندي", "الدوري السويسري", "الدوري النمساوي الممتاز", "الدوري اليوناني",
        "الدوري الروسي الممتاز", "الدوري الدنماركي الممتاز", "الدوري السويدي الممتاز", "الدوري النرويجي الممتاز",
        "المباريات الودية", "ودية للأندية", "ودية دولية", "friendly"
    )

    fun isMajorFootballMatch(match: Match): Boolean {
        if (match.sport.isNotBlank() && !match.sport.equals("Football", ignoreCase = true) && !match.sport.equals("كرة القدم", ignoreCase = true)) {
            return false
        }
        val compLower = match.competition.lowercase()
        val homeLower = match.homeTeam.lowercase()
        val awayLower = match.awayTeam.lowercase()

        // 1. Strict Exclusion: Youth leagues, reserve teams, non-first team, and preliminary qualifiers
        for (b in BLOCKED_KEYWORDS) {
            val bLower = b.lowercase()
            if (compLower.contains(bLower) || homeLower.contains(bLower) || awayLower.contains(bLower)) {
                return false
            }
        }

        if (homeLower.contains("تحت 19") || awayLower.contains("تحت 19") ||
            homeLower.contains("u19") || awayLower.contains("u19") ||
            homeLower.contains("تحت 21") || awayLower.contains("تحت 21") ||
            homeLower.contains("u21") || awayLower.contains("u21")) {
            return false
        }

        val roundOrStage = (match.roundOrStadium ?: "").lowercase()
        if (roundOrStage.contains("تمهيدي") || roundOrStage.contains("تصفيات أولية") || roundOrStage.contains("preliminary")) {
            if (compLower.contains("كأس الاتحاد") || compLower.contains("fa cup")) return false
        }

        // 2. Allow Major Tournaments & Leagues
        for (k in MAJOR_KEYWORDS) {
            if (compLower.contains(k.lowercase())) {
                return true
            }
        }
        return false
    }
}

object LeagueLogoHelper {
    private val leagueLogos = mapOf(
        "أبطال أوروبا" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572",
        "champions league" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572",
        "الدوري الإنجليزي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/7",
        "premier league" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/7",
        "الدوري الإسباني" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/11",
        "la liga" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/11",
        "الدوري الإيطالي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/17",
        "serie a" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/17",
        "الدوري الألماني" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/25",
        "bundesliga" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/25",
        "الدوري الفرنسي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/35",
        "ligue 1" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/35",
        "روشن" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/649",
        "السعودي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/649",
        "العراق" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/6822",
        "أبطال آسيا" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/549",
        "أبطال إفريقيا" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/550",
        "الدوري الأوروبي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/571",
        "المؤتمر" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/573",
        "المصري" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/552",
        "الإماراتي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/549",
        "أدنوك" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/549",
        "المغربي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/557",
        "الجزائري" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/560",
        "التونسي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/554",
        "قطر" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/5455",
        "البرتغالي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/73",
        "كأس البرتغال" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/75",
        "الهولندي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/57",
        "التركي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/78",
        "البلجيكي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/98",
        "الأمريكي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/104",
        "أمريكي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/104",
        "mls" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/104",
        "major league" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/104",
        "الدنماركي" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/119",
        "الصيني" to "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/5625"
    )

    fun getLogo(competitionName: String, fallbackUrl: String?): String {
        // If fallbackUrl is a valid competition URL with an actual ID (e.g. not Competitions/0 and not blank), use it directly!
        if (!fallbackUrl.isNullOrBlank() && !fallbackUrl.contains("Competitions/0") && !fallbackUrl.endsWith("Competitions/")) {
            return fallbackUrl
        }
        val compLower = competitionName.lowercase()
        for ((key, url) in leagueLogos) {
            if (compLower.contains(key.lowercase())) {
                return url
            }
        }
        return "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572"
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(BeoutShapes.Small)
            .background(BeoutColors.LiveRed)
            .padding(horizontal = BeoutDimensions.SpaceXS, vertical = 2.dp)
    ) {
        Text(
            text = com.beouttv.app.data.i18n.tr("مباشر", "LIVE", "CANLI", "زنده", "EN DIRECT"),
            style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary
        )
    }
}

/**
 * BEOUT TV Official League Standings Table Component
 */
@Composable
fun BeoutStandingsTable(
    standing: com.beouttv.app.data.model.LeagueStanding,
    modifier: Modifier = Modifier
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val leagueLogo = remember(standing.leagueName) {
        LeagueLogoHelper.getLogo(standing.leagueName, null)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .clip(BeoutShapes.Medium)
                .background(BeoutColors.Card)
                .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                .padding(if (isTvMode) 16.dp else 12.dp)
        ) {
            // League Header: Official Logo + Name + Season
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = leagueLogo,
                        contentDescription = standing.leagueName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.ElevatedSurface)
                            .padding(2.dp)
                    )
                    Column {
                        Text(
                            text = standing.leagueName,
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )
                        Text(
                            text = standing.season,
                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                            color = BeoutColors.BrightBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Google Sports / YouTube Style Column Headers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BeoutColors.ElevatedSurface)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#",
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(26.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.team(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.played(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(26.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.won(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.draw(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.lost(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.goalDiff(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextDisabled,
                    modifier = Modifier.width(28.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = com.beouttv.app.data.i18n.S.points(),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.BrightBlue,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.height(4.dp))

            // Rows
            val totalRows = standing.rows.size
            standing.rows.forEach { row ->
                val isChampionsLeague = row.rank <= 4
                val isRelegation = totalRows > 12 && row.rank > totalRows - 3

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank with qualification stripe
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.width(26.dp)
                    ) {
                        if (isChampionsLeague) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BeoutColors.BrightBlue)
                            )
                            Spacer(Modifier.width(3.dp))
                        } else if (isRelegation) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(BeoutColors.LiveRed)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            text = "${row.rank}",
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isChampionsLeague || isRelegation) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = when {
                                isChampionsLeague -> BeoutColors.BrightBlue
                                isRelegation -> BeoutColors.LiveRed
                                else -> BeoutColors.TextSecondary
                            }
                        )
                    }

                    // Team Logo + Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        BeoutTeamBadge(
                            name = row.teamName,
                            logoUrl = row.teamLogoUrl,
                            size = 20.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = row.teamName,
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                            color = BeoutColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // P, W, D, L
                    Text(
                        text = "${row.played}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary,
                        modifier = Modifier.width(26.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "${row.won}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary,
                        modifier = Modifier.width(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "${row.draw}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary,
                        modifier = Modifier.width(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "${row.lost}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary,
                        modifier = Modifier.width(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    // GD
                    Text(
                        text = if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}",
                        style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                        color = if (row.goalDiff > 0) BeoutColors.Success else if (row.goalDiff < 0) BeoutColors.LiveRed else BeoutColors.TextDisabled,
                        modifier = Modifier.width(28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    // Points in BrightBlue Pill
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${row.points}",
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    color = BeoutColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
            }

            Spacer(Modifier.height(10.dp))
            // Legend footnotes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.BrightBlue)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = com.beouttv.app.data.i18n.tr("دوري أبطال أوروبا", "Champions League", "Şampiyonlar Ligi", "لیگ قهرمانان", "Ligue des Champions"),
                        style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                        color = BeoutColors.TextSecondary
                    )
                }
                if (totalRows > 12) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.LiveRed)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = com.beouttv.app.data.i18n.tr("منطقة الهبوط", "Relegation Zone", "Küme Düşme Hattı", "منطقه سقوط", "Zone de relégation"),
                            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                            color = BeoutColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeoutContinueWatchingCard(
    title: String,
    thumbnailUrl: String,
    progressPercent: Float,
    remainingLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val defaultWidth = if (isTvMode) BeoutDimensions.TvContinueWatchingCardWidth else BeoutDimensions.ContinueWatchingCardWidth

    Column(
        modifier = Modifier
            .width(defaultWidth)
            .then(modifier)
            .tvFocusable(
                shape = BeoutShapes.Medium,
                onEnter = onClick
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(BeoutShapes.Medium)
                .background(BeoutColors.ElevatedSurface)
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // YouTube-style bottom progress bar right on the thumbnail
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = BeoutColors.LiveRed,
                trackColor = Color.Black.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            text = title,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp),
            color = BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = remainingLabel,
            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 10.sp),
            color = BeoutColors.TextSecondary,
            maxLines = 1
        )
    }
}
