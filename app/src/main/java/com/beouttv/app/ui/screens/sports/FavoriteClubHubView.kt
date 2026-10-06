package com.beouttv.app.ui.screens.sports

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beouttv.app.data.favorites.FavoriteClubManager
import com.beouttv.app.data.filter.ChannelMatcher
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.*
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.BeoutMatchCard
import com.beouttv.app.ui.components.BeoutStandingsTable
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.launch

@Composable
fun FavoriteClubHubView(
    club: ClubInfo?,
    channels: List<Channel> = emptyList(),
    onChangeClubClick: () -> Unit,
    onWatchMatch: (Match) -> Unit = {},
    onChannelClick: (Channel) -> Unit = {},
    onReminderClick: (Match) -> Unit = {},
    repository: BeoutRepository = ServiceLocator.repository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTvMode by com.beouttv.app.core.tv.DeviceTypeDetector.isTvModeFlow.collectAsState()
    val hubStartPadding = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding

    var clubMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var leagueStanding by remember { mutableStateOf<LeagueStanding?>(null) }
    var isLoadingData by remember { mutableStateOf(false) }

    var notifEnabled by remember(club) { mutableStateOf(club?.notificationsEnabled ?: true) }

    LaunchedEffect(club?.id, club?.name) {
        if (club != null) {
            isLoadingData = true
            coroutineScope.launch {
                try {
                    clubMatches = repository.getTeamFixtures(club.id, club.name)
                    if (club.leagueId.isNotBlank()) {
                        leagueStanding = repository.getStandingsByLeague(club.leagueId, club.leagueName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoadingData = false
                }
            }
        }
    }

    if (club == null) {
        // Empty State: Prompt to select a favorite club
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = hubStartPadding, end = BeoutDimensions.ScreenPadding, top = BeoutDimensions.ScreenPadding, bottom = BeoutDimensions.ScreenPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BeoutShapes.Large)
                    .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Large)
                    .background(BeoutColors.Card)
                    .padding(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x221E88E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = tr("اختر ناديك المفضل", "Choose Your Favorite Club", "Favori Kulübünü Seç", "انتخاب تیم محبوب", "Choisissez votre club"),
                    style = BeoutTextStyles.ScreenTitle.copy(fontSize = 20.sp),
                    color = BeoutColors.TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = tr(
                        "تابع مباريات فريقك القادمة، استلم إشعارات حية، وتعرف على موقعه في جدول الترتيب وإحصائياته فوراً.",
                        "Track upcoming club matches, get live alerts, and check real-time standings.",
                        "Kulübünüzün maçlarını takip edin, bildirimler alın ve puan durumunu görüntüleyin.",
                        "مسابقات آینده، نوتیفیکیشن‌ها و موقعیت تیم محبوبتان را در جدول دنبال کنید.",
                        "Suivez les prochains matchs, recevez des alertes et consultez le classement de votre club."
                    ),
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp),
                    color = BeoutColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onChangeClubClick,
                    shape = BeoutShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue),
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("اختيار النادي الآن", "Select Club Now", "Kulüp Seç", "انتخاب تیم", "Sélectionner"),
                        style = BeoutTextStyles.Button.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    } else {
        // Detailed Hub for Favorite Club
        LazyColumn(
            contentPadding = PaddingValues(
                start = hubStartPadding,
                end = BeoutDimensions.ScreenPadding,
                top = 8.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Club Hero Profile Card
            item {
                ClubHeroProfileCard(
                    club = club,
                    onChangeClubClick = onChangeClubClick
                )
            }

            // 2. Notification Toggle Switch Card
            item {
                ClubNotificationCard(
                    notificationsEnabled = notifEnabled,
                    onToggle = { enabled ->
                        notifEnabled = enabled
                        FavoriteClubManager.setNotificationsEnabled(context, enabled)
                        val msg = if (enabled) "تم تفعيل إشعارات مباريات ${club.name}" else "تم إيقاف إشعارات مباريات ${club.name}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. Loading or Matches Sections
            if (isLoadingData) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BeoutColors.BrightBlue)
                    }
                }
            } else if (clubMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr(
                                "لا توجد مباريات مجدولة حالياً للفريق الأول",
                                "No scheduled first-team matches currently",
                                "Şu anda planlanmış maç bulunmuyor",
                                "در حال حاضر مسابقه‌ای برنامه‌ریزی نشده است",
                                "Aucun match prévu pour l'équipe première"
                            ),
                            style = BeoutTextStyles.SecondaryInfo,
                            color = BeoutColors.TextSecondary
                        )
                    }
                }
            } else {
                // Upcoming Matches Section
                val upcomingMatches = clubMatches.filter { it.status == MatchStatus.UPCOMING || it.status == MatchStatus.LIVE }
                if (upcomingMatches.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(
                            icon = Icons.Rounded.Event,
                            title = tr("المباريات القادمة للنادي", "Upcoming Club Matches", "Gelecek Kulüp Maçları", "مسابقات آینده تیم", "Prochains Matchs"),
                            count = upcomingMatches.size
                        )
                    }

                    items(upcomingMatches, key = { it.id }) { match ->
                        val matchedCh = ChannelMatcher.findMatchedChannel(match.channel, channels)
                        BeoutMatchCard(
                            match = match,
                            isFavoriteMatch = true,
                            isHomeFavorite = match.homeTeam.contains(club.name, true),
                            isAwayFavorite = match.awayTeam.contains(club.name, true),
                            matchedChannel = matchedCh,
                            onWatchClick = onWatchMatch,
                            onChannelClick = onChannelClick,
                            onReminderClick = onReminderClick
                        )
                    }
                }

                // Recent Matches & Results Section
                val recentMatches = clubMatches.filter { it.status == MatchStatus.FINISHED }
                if (recentMatches.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(
                            icon = Icons.Rounded.History,
                            title = tr("النتائج السابقة والأخيرة", "Recent Match Results", "Son Maç Sonuçları", "نتایج اخیر", "Derniers Résultats"),
                            count = recentMatches.size
                        )
                    }

                    items(recentMatches, key = { it.id }) { match ->
                        val matchedCh = ChannelMatcher.findMatchedChannel(match.channel, channels)
                        BeoutMatchCard(
                            match = match,
                            isFavoriteMatch = true,
                            isHomeFavorite = match.homeTeam.contains(club.name, true),
                            isAwayFavorite = match.awayTeam.contains(club.name, true),
                            matchedChannel = matchedCh,
                            onWatchClick = onWatchMatch,
                            onChannelClick = onChannelClick,
                            onReminderClick = onReminderClick
                        )
                    }
                }
            }

            // 5. Mini Standings Table Context
            if (leagueStanding != null && leagueStanding!!.rows.isNotEmpty()) {
                item {
                    Column {
                        SectionHeaderTitle(
                            icon = Icons.Rounded.EmojiEvents,
                            title = tr("موقع النادي في جدول الترتيب", "Club Standings Context", "Lig Puan Durumu", "موقعیت در جدول رده‌بندی", "Classement en ligue"),
                            count = null
                        )

                        Spacer(Modifier.height(8.dp))

                        BeoutStandingsTable(standing = leagueStanding!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubHeroProfileCard(
    club: ClubInfo,
    onChangeClubClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .padding(BeoutDimensions.SpaceS)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Compact Club Logo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.ElevatedSurface)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = club.logoUrl,
                        contentDescription = club.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = club.name,
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )

                    Spacer(Modifier.height(1.dp))

                    Text(
                        text = club.leagueName,
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp),
                        color = BeoutColors.BrightBlue
                    )

                    if (club.stadiumName.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = BeoutColors.TextDisabled,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = club.stadiumName,
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                color = BeoutColors.TextDisabled
                            )
                        }
                    }
                }

                // Change Club Button
                Box(
                    modifier = Modifier
                        .clip(BeoutShapes.Small)
                        .background(BeoutColors.ElevatedSurface)
                        .clickable(onClick = onChangeClubClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = tr("تغيير", "Change", "Değiştir", "تغییر", "Modifier"),
                            style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = BeoutColors.BrightBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick Stats Row (المركز • النقاط • لعب • فوز • تعادل • خسارة)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BeoutShapes.Medium)
                    .background(BeoutColors.ElevatedSurface)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(label = tr("المركز", "Rank", "Sıra", "رتبه", "Rang"), value = if (club.rank > 0) "#${club.rank}" else "-")
                StatDivider()
                StatBadge(label = tr("النقاط", "Points", "Puan", "امتیاز", "Points"), value = "${club.points}")
                StatDivider()
                StatBadge(label = tr("لعب", "Played", "Oynanan", "بازی", "Joués"), value = "${club.played}")
                if (club.won > 0 || club.draw > 0 || club.lost > 0) {
                    StatDivider()
                    StatBadge(label = tr("فوز", "Won", "Galibiyet", "برد", "Victoires"), value = "${club.won}", valueColor = BeoutColors.BrightBlue)
                    StatDivider()
                    StatBadge(label = tr("تعادل", "Draw", "Beraberlik", "مساوی", "Nuls"), value = "${club.draw}", valueColor = BeoutColors.TextSecondary)
                    StatDivider()
                    StatBadge(label = tr("خسارة", "Lost", "Mağlubiyet", "باخت", "Défaites"), value = "${club.lost}", valueColor = BeoutColors.LiveRed)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = valueColor
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
            color = BeoutColors.TextSecondary
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(BeoutColors.Divider)
    )
}

@Composable
private fun ClubNotificationCard(
    notificationsEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .padding(horizontal = BeoutDimensions.SpaceM, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (notificationsEnabled) Color(0x221E88E5) else BeoutColors.ElevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notificationsEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint = if (notificationsEnabled) BeoutColors.BrightBlue else BeoutColors.TextDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = tr("إشعارات مباريات النادي", "Club Match Notifications", "Maç Bildirimleri", "نوتیفیکیشن مسابقات", "Notifications des matchs"),
                    style = BeoutTextStyles.CardTitle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextPrimary
                )
                Text(
                    text = tr("تنبيه فوري قبل انطلاق أي مباراة بـ 15 دقيقة", "Instant reminder 15m before kickoff", "Maçtan 15 dakika önce anında uyarı", "هشدار ۱۵ دقیقه پیش از شروع بازی", "Rappel 15 min avant le coup d'envoi"),
                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                    color = BeoutColors.TextSecondary
                )
            }
        }

        Switch(
            checked = notificationsEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BeoutColors.PrimaryBlue,
                uncheckedThumbColor = BeoutColors.TextDisabled,
                uncheckedTrackColor = BeoutColors.ElevatedSurface
            )
        )
    }
}

@Composable
private fun SectionHeaderTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BeoutColors.BrightBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = BeoutTextStyles.SectionTitle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "($count)",
                style = BeoutTextStyles.Caption.copy(fontSize = 12.sp),
                color = BeoutColors.TextDisabled
            )
        }
    }
}
