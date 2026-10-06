package com.beouttv.app.ui.screens.sports

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.favorites.FavoriteClubManager
import com.beouttv.app.data.filter.ChannelMatcher
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.*
import com.beouttv.app.data.notification.LocalNotificationManager
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SportsUiState {
    data object Loading : SportsUiState
    data class Error(val message: String) : SportsUiState
    data class Success(
        val matches: List<Match>,
        val currentStanding: LeagueStanding?,
        val allStandings: List<LeagueStanding>,
        val channels: List<Channel> = emptyList()
    ) : SportsUiState
}

class SportsViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SportsUiState>(SportsUiState.Loading)
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private var currentFilterDate = "today"
    private var currentLeagueId = "572"
    private var currentLeagueName = "دوري أبطال أوروبا"
    private var cachedChannels: List<Channel> = emptyList()
    private var livePollingJob: kotlinx.coroutines.Job? = null

    init {
        loadData("today", "572", "دوري أبطال أوروبا")
        startLivePolling()
    }

    private fun startLivePolling() {
        livePollingJob?.cancel()
        livePollingJob = viewModelScope.launch {
            while (isActive) {
                delay(12000) // 12-second live ticker
                try {
                    val realDate = getDateStringForFilter(currentFilterDate)
                    val rawMatches = repository.getMatchesByDate(realDate)
                    val matches = rawMatches.filter { SportsFilterHelper.isMajorFootballMatch(it) }
                    val current = _uiState.value
                    if (current is SportsUiState.Success && matches.isNotEmpty() && current.matches != matches) {
                        _uiState.value = current.copy(matches = matches)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun loadForDate(dateFilter: String) {
        currentFilterDate = dateFilter
        loadData(dateFilter, currentLeagueId, currentLeagueName)
    }

    fun loadLeagueStanding(leagueId: String, leagueName: String) {
        currentLeagueId = leagueId
        currentLeagueName = leagueName
        viewModelScope.launch {
            val currentState = _uiState.value
            val existingMatches = if (currentState is SportsUiState.Success) currentState.matches else emptyList()
            try {
                val standing = repository.getStandingsByLeague(leagueId, leagueName)
                _uiState.value = SportsUiState.Success(
                    matches = existingMatches,
                    currentStanding = standing,
                    allStandings = if (standing != null) listOf(standing) else emptyList(),
                    channels = cachedChannels
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadData(dateFilter: String, leagueId: String, leagueName: String) {
        currentFilterDate = dateFilter
        viewModelScope.launch {
            _uiState.value = SportsUiState.Loading
            try {
                if (cachedChannels.isEmpty()) {
                    cachedChannels = repository.getChannels("All")
                }
                val realDate = getDateStringForFilter(currentFilterDate)
                val rawMatches = repository.getMatchesByDate(realDate)
                val matches = rawMatches.filter { SportsFilterHelper.isMajorFootballMatch(it) }
                val standing = repository.getStandingsByLeague(leagueId, leagueName) ?: repository.getStandings().firstOrNull()
                _uiState.value = SportsUiState.Success(
                    matches = matches,
                    currentStanding = standing,
                    allStandings = repository.getStandings(),
                    channels = cachedChannels
                )
            } catch (e: Exception) {
                _uiState.value = SportsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        loadData(currentFilterDate, currentLeagueId, currentLeagueName)
    }

    fun setReminder(context: Context, match: Match) {
        com.beouttv.app.data.notification.NotificationsManager.scheduleMatchReminder(
            context = context,
            match = match
        )
    }

    companion object {
        fun getDateStringForFilter(filter: String): String {
            return when (filter) {
                "tomorrow" -> "tomorrow"
                "yesterday" -> "yesterday"
                "live" -> "live"
                else -> "today"
            }
        }
    }
}

enum class SportsTabMode {
    SCHEDULE,
    STANDINGS,
    MY_CLUB
}

@Composable
fun SportsScreen(
    onWatchMatch: (Match) -> Unit = {},
    onChannelClick: (Channel) -> Unit = {},
    viewModel: SportsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val favoriteClub by FavoriteClubManager.favoriteClubFlow.collectAsState()

    var activeTab by remember { mutableStateOf(SportsTabMode.SCHEDULE) }
    var selectedDateFilter by remember { mutableStateOf("today") }
    var selectedCompetition by remember { mutableStateOf("All") }
    var selectedLeagueId by remember { mutableStateOf("7") }

    var showClubBottomSheet by remember { mutableStateOf(false) }
    var showLeagueBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FavoriteClubManager.init(context)
    }
    var selectedLeagueName by remember { mutableStateOf("دوري أبطال أوروبا") }
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    val dateFilters = listOf(
        "today" to S.today(),
        "live" to S.liveNow(),
        "tomorrow" to S.tomorrow()
    )

    // Priority Ordered Standings Leagues (Top 5 Major European Leagues + Iraq League First)
    val topLeagues = listOf(
        "7" to tr("الدوري الإنجليزي", "Premier League", "Premier Lig", "لیگ برتر انگلیس", "Premier League"),
        "11" to tr("الدوري الإسباني", "La Liga", "La Liga", "لالیگا اسپانیا", "La Liga"),
        "17" to tr("الدوري الإيطالي", "Serie A", "Serie A", "سری آ ایتالیا", "Serie A"),
        "25" to tr("الدوري الألماني", "Bundesliga", "Bundesliga", "بوندسلیگا", "Bundesliga"),
        "35" to tr("الدوري الفرنسي", "Ligue 1", "Ligue 1", "لوشامپیونه", "Ligue 1"),
        "6822" to tr("دوري نجوم العراق", "Iraq Stars League", "Irak Ligi", "لیگ ستارگان عراق", "Ligue d'Irak"),
        "572" to tr("دوري أبطال أوروبا", "Champions League", "Şampiyonlar Ligi", "لیگ قهرمانان اروپا", "Ligue des Champions"),
        "649" to tr("دوري روشن السعودي", "Saudi Pro League", "Suudi Ligi", "لیگ عربستان", "Ligue Saoudienne"),
        "104" to tr("الدوري الأمريكي", "MLS", "MLS", "لیگ برتر آمریکا", "MLS"),
        "552" to tr("الدوري المصري", "Egyptian Premier League", "Mısır Ligi", "لیگ مصر", "Ligue Égyptienne"),
        "549" to tr("دوري أدنوك الإماراتي", "UAE Pro League", "BAE Ligi", "لیگ امارات", "Ligue des EAU"),
        "5455" to tr("دوري نجوم قطر", "Qatar Stars League", "Katar Ligi", "لیگ ستارگان قطر", "Ligue du Qatar"),
        "557" to tr("الدوري المغربي", "Botola Pro", "Fas Ligi", "لیگ مراکش", "Botola Pro"),
        "554" to tr("الرابطة التونسية المحترفة", "Tunisian Ligue 1", "Tunus Ligi", "لیگ تونس", "Ligue Tunisienne"),
        "560" to tr("الرابطة الجزائرية المحترفة", "Algerian Ligue 1", "Cezayir Ligi", "لیگ الجزایر", "Ligue Algérienne"),
        "73" to tr("الدوري البرتغالي", "Liga Portugal", "Portekiz Ligi", "لیگ پرتغال", "Liga Portugal"),
        "57" to tr("الدوري الهولندي", "Eredivisie", "Hollanda Ligi", "لیگ هلند", "Eredivisie"),
        "78" to tr("الدوري التركي", "Süper Lig", "Süper Lig", "لیگ ترکیه", "Süper Lig"),
        "98" to tr("الدوري البلجيكي", "Belgian Pro League", "Belçika Ligi", "لیگ بلژیک", "Pro League")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .then(if (isTvMode) Modifier.padding(top = 10.dp) else Modifier.statusBarsPadding())
    ) {
        if (isTvMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 76.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Primary 3-Tab Switcher (Compact Smurf/TV Scale)
                Row(
                    modifier = Modifier
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.Card)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Match Schedule
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(if (activeTab == SportsTabMode.SCHEDULE) BeoutColors.PrimaryBlue else Color.Transparent)
                            .tvFocusable(shape = BeoutShapes.Small, onEnter = { activeTab = SportsTabMode.SCHEDULE })
                            .clickable { activeTab = SportsTabMode.SCHEDULE }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SportsSoccer,
                                contentDescription = null,
                                tint = if (activeTab == SportsTabMode.SCHEDULE) Color.White else BeoutColors.TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = S.matches(),
                                style = BeoutTextStyles.Button.copy(fontSize = 11.sp),
                                color = if (activeTab == SportsTabMode.SCHEDULE) Color.White else BeoutColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Tab 2: Standings Table
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(if (activeTab == SportsTabMode.STANDINGS) BeoutColors.PrimaryBlue else Color.Transparent)
                            .tvFocusable(shape = BeoutShapes.Small, onEnter = { activeTab = SportsTabMode.STANDINGS })
                            .clickable { activeTab = SportsTabMode.STANDINGS }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = if (activeTab == SportsTabMode.STANDINGS) Color.White else BeoutColors.TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = S.standingsTable(),
                                style = BeoutTextStyles.Button.copy(fontSize = 11.sp),
                                color = if (activeTab == SportsTabMode.STANDINGS) Color.White else BeoutColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Tab 3: My Club
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(if (activeTab == SportsTabMode.MY_CLUB) BeoutColors.PrimaryBlue else Color.Transparent)
                            .tvFocusable(shape = BeoutShapes.Small, onEnter = { activeTab = SportsTabMode.MY_CLUB })
                            .clickable { activeTab = SportsTabMode.MY_CLUB }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = if (activeTab == SportsTabMode.MY_CLUB) Color.White else BeoutColors.BrightBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (favoriteClub != null) favoriteClub!!.name else tr("ناديي المفضل", "My Club", "Kulübüm", "تیم من", "Mon Club"),
                                style = BeoutTextStyles.Button.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = if (activeTab == SportsTabMode.MY_CLUB) Color.White else BeoutColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Inline Action Controls & Refresh Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeTab == SportsTabMode.SCHEDULE) {
                        dateFilters.forEach { (dateKey, dateLabel) ->
                            val isSelected = selectedDateFilter == dateKey
                            val applyDateFilter = {
                                selectedDateFilter = dateKey
                                if (dateKey != "live") {
                                    viewModel.loadForDate(dateKey)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                                    .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
                                    .tvFocusable(shape = BeoutShapes.Small, onEnter = applyDateFilter)
                                    .clickable(onClick = applyDateFilter)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = BeoutTextStyles.Caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color.White else BeoutColors.TextSecondary
                                )
                            }
                        }

                        val selectedLeagueLabel = if (selectedCompetition == "All") {
                            tr("كافة البطولات", "All Leagues", "Tüm Ligler", "همه لیگ‌ها", "Toutes les ligues")
                        } else {
                            selectedCompetition
                        }
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Small)
                                .background(BeoutColors.Card)
                                .border(BorderStroke(1.dp, if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.Divider), BeoutShapes.Small)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = { showLeagueBottomSheet = true })
                                .clickable { showLeagueBottomSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = selectedLeagueLabel,
                                    style = BeoutTextStyles.Caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selectedCompetition != "All") FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Refresh Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.ElevatedSurface)
                            .border(1.dp, BeoutColors.Divider, CircleShape)
                            .tvFocusable(shape = CircleShape, onEnter = { viewModel.refresh() })
                            .clickable { viewModel.refresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        } else {
            // 1. Top Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = BeoutDimensions.ScreenPadding,
                        end = BeoutDimensions.ScreenPadding,
                        top = BeoutDimensions.ScreenPadding,
                        bottom = BeoutDimensions.SpaceS
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = S.sports(),
                        style = BeoutTextStyles.ScreenTitle,
                        color = BeoutColors.TextPrimary
                    )
                    Text(
                        text = tr("تغطية حية ومباشرة لكافة البطولات والمباريات العالمية والعربية", "Live coverage of all global & Arab leagues", "Tüm dünya ve Arap ligleri canlı", "پوشش زنده مسابقات و لیگ‌های جهانی و عربی", "Couverture en direct des matchs"),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                        color = BeoutColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BeoutColors.ElevatedSurface)
                        .tvFocusable(shape = CircleShape)
                        .clickable { viewModel.refresh() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Primary 3-Tab Switcher (جدول المباريات | جدول الترتيب | ناديي المفضل)
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isSmallPhone = configuration.screenWidthDp < 360
            val tabFontSize = if (isSmallPhone) 10.5.sp else 12.sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding)
                    .clip(BeoutShapes.Medium)
                    .background(BeoutColors.Card)
                    .padding(4.dp)
            ) {
                // Tab 1: Match Schedule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(BeoutShapes.Small)
                        .background(if (activeTab == SportsTabMode.SCHEDULE) BeoutColors.PrimaryBlue else Color.Transparent)
                        .tvFocusable(shape = BeoutShapes.Small)
                        .clickable { activeTab = SportsTabMode.SCHEDULE }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SportsSoccer,
                            contentDescription = null,
                            tint = if (activeTab == SportsTabMode.SCHEDULE) Color.White else BeoutColors.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = S.matchesSchedule(),
                            style = BeoutTextStyles.Button.copy(fontSize = tabFontSize),
                            color = if (activeTab == SportsTabMode.SCHEDULE) Color.White else BeoutColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Tab 2: Standings Table
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(BeoutShapes.Small)
                        .background(if (activeTab == SportsTabMode.STANDINGS) BeoutColors.PrimaryBlue else Color.Transparent)
                        .tvFocusable(shape = BeoutShapes.Small)
                        .clickable { activeTab = SportsTabMode.STANDINGS }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = if (activeTab == SportsTabMode.STANDINGS) Color.White else BeoutColors.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = S.standingsTable(),
                            style = BeoutTextStyles.Button.copy(fontSize = tabFontSize),
                            color = if (activeTab == SportsTabMode.STANDINGS) Color.White else BeoutColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Tab 3: My Club
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(BeoutShapes.Small)
                        .background(if (activeTab == SportsTabMode.MY_CLUB) BeoutColors.PrimaryBlue else Color.Transparent)
                        .tvFocusable(shape = BeoutShapes.Small)
                        .clickable { activeTab = SportsTabMode.MY_CLUB }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (favoriteClub != null && favoriteClub!!.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = favoriteClub!!.logoUrl,
                                contentDescription = favoriteClub!!.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = if (activeTab == SportsTabMode.MY_CLUB) Color.White else BeoutColors.BrightBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (favoriteClub != null) favoriteClub!!.name else tr("ناديي المفضل", "My Club", "Kulübüm", "تیم من", "Mon Club"),
                            style = BeoutTextStyles.Button.copy(fontSize = tabFontSize, fontWeight = FontWeight.Bold),
                            color = if (activeTab == SportsTabMode.MY_CLUB) Color.White else BeoutColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(BeoutDimensions.SpaceS))

        // 3. Main Content based on Tab & UiState
        when (activeTab) {
            SportsTabMode.MY_CLUB -> {
                val channels = (state as? SportsUiState.Success)?.channels ?: emptyList()
                FavoriteClubHubView(
                    club = favoriteClub,
                    channels = channels,
                    onChangeClubClick = { showClubBottomSheet = true },
                    onWatchMatch = onWatchMatch,
                    onChannelClick = onChannelClick,
                    onReminderClick = { viewModel.setReminder(context, it) }
                )
            }
            SportsTabMode.SCHEDULE -> {
                when (val s = state) {
                    is SportsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = BeoutColors.BrightBlue)
                        }
                    }
                    is SportsUiState.Error -> BeoutErrorState(onRetry = viewModel::refresh)
                    is SportsUiState.Success -> {
                        if (!isTvMode) {
                            // Space-efficient unified Action Bar: Date Segmented Control + Tournament Bottom Sheet Trigger
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Date Segmented Control (اليوم | مباشر | غداً)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(BeoutShapes.Medium)
                                        .background(BeoutColors.Card)
                                        .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    dateFilters.forEach { (key, label) ->
                                        val isSelected = selectedDateFilter == key
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(BeoutShapes.Small)
                                                .background(if (isSelected) BeoutColors.PrimaryBlue else Color.Transparent)
                                            .clickable {
                                                if (selectedDateFilter != key) {
                                                    selectedDateFilter = key
                                                    if (key != "live") {
                                                        viewModel.loadForDate(key)
                                                    }
                                                }
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = BeoutTextStyles.Caption.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) Color.White else BeoutColors.TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // League / Competition Bottom Sheet Trigger Button
                                val selectedLeagueLabel = if (selectedCompetition == "All") {
                                    tr("كافة البطولات", "All Leagues", "Tüm Ligler", "همه لیگ‌ها", "Toutes les ligues")
                                } else {
                                    selectedCompetition
                                }

                                Row(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(BeoutShapes.Medium)
                                        .background(BeoutColors.Card)
                                        .border(BorderStroke(1.dp, if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.Divider), BeoutShapes.Medium)
                                        .clickable { showLeagueBottomSheet = true }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedLeagueLabel,
                                        style = BeoutTextStyles.Caption.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selectedCompetition != "All") FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selectedCompetition != "All") BeoutColors.BrightBlue else BeoutColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 130.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BeoutColors.TextDisabled,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                        }

                        // Filter matches based on Date and Competition with memoization
                        val filteredMatches = remember(s.matches, selectedDateFilter, selectedCompetition) {
                            s.matches.filter { match ->
                                val dateMatch = if (selectedDateFilter == "live") match.status == MatchStatus.LIVE else true
                                val compMatch = when (selectedCompetition) {
                                    "All" -> true
                                    else -> match.competition.equals(selectedCompetition, ignoreCase = true)
                                }
                                dateMatch && compMatch
                            }
                        }

                        if (filteredMatches.isEmpty()) {
                            BeoutEmptyState(
                                title = tr("لا توجد مباريات مطابقة", "No Matches Found", "Maç Bulunamadı", "مسابقه‌ای یافت نشد", "Aucun match trouvé"),
                                description = tr("يرجى اختيار يوم أو بطولة أخرى لعرض المباريات.", "Please select another date or league to view matches.", "Lütfen maçları görüntülemek için başka bir tarih veya lig seçin.", "لطفاً برای مشاهده مسابقات، روز یا لیگ دیگری را انتخاب کنید.", "Veuillez choisir une autre date ou ligue pour voir les matchs.")
                            )
                        } else {
                            val groupedMatches = remember(filteredMatches) {
                                filteredMatches
                                    .groupBy { it.competition }
                                    .toList()
                                    .sortedBy { (compName, _) -> getCompetitionPriority(compName) }
                                    .toMap()
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding,
                                    end = BeoutDimensions.ScreenPadding,
                                    bottom = if (isTvMode) 32.dp else 80.dp,
                                    top = 4.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                            ) {
                                groupedMatches.forEach { (competition, matchesInComp) ->
                                    item(key = "header_$competition", contentType = "competition_header") {
                                        val firstMatchLogo = matchesInComp.firstOrNull()?.competitionLogoUrl
                                        val leagueLogo = LeagueLogoHelper.getLogo(competition, firstMatchLogo)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        ) {
                                            AsyncImage(
                                                model = leagueLogo,
                                                contentDescription = competition,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(BeoutColors.ElevatedSurface)
                                                    .padding(2.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                text = competition,
                                                style = BeoutTextStyles.SectionTitle.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = BeoutColors.TextPrimary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "(${matchesInComp.size})",
                                                style = BeoutTextStyles.Caption,
                                                color = BeoutColors.TextDisabled
                                            )
                                        }
                                    }

                                    if (isTvMode) {
                                        val matchPairs = matchesInComp.chunked(2)
                                        items(
                                            items = matchPairs,
                                            key = { pair -> pair.joinToString("_") { it.id } },
                                            contentType = { "match_row_tv" }
                                        ) { pair ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                pair.forEach { match ->
                                                    val isMatchFav = (favoriteClub != null && (match.homeTeam.contains(favoriteClub!!.name, true) || match.awayTeam.contains(favoriteClub!!.name, true)))
                                                    val matchedCh = ChannelMatcher.findMatchedChannel(match.channel, s.channels)
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        BeoutMatchCard(
                                                            match = match,
                                                            isFavoriteMatch = isMatchFav,
                                                            isHomeFavorite = favoriteClub != null && match.homeTeam.contains(favoriteClub!!.name, true),
                                                            isAwayFavorite = favoriteClub != null && match.awayTeam.contains(favoriteClub!!.name, true),
                                                            matchedChannel = matchedCh,
                                                            onWatchClick = onWatchMatch,
                                                            onChannelClick = onChannelClick,
                                                            onReminderClick = { viewModel.setReminder(context, match) }
                                                        )
                                                    }
                                                }
                                                if (pair.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        items(
                                            items = matchesInComp,
                                            key = { it.id },
                                            contentType = { "match_card" }
                                        ) { match ->
                                            val isMatchFav = (favoriteClub != null && (match.homeTeam.contains(favoriteClub!!.name, true) || match.awayTeam.contains(favoriteClub!!.name, true)))
                                            val matchedCh = ChannelMatcher.findMatchedChannel(match.channel, s.channels)

                                            BeoutMatchCard(
                                                match = match,
                                                isFavoriteMatch = isMatchFav,
                                                isHomeFavorite = favoriteClub != null && match.homeTeam.contains(favoriteClub!!.name, true),
                                                isAwayFavorite = favoriteClub != null && match.awayTeam.contains(favoriteClub!!.name, true),
                                                matchedChannel = matchedCh,
                                                onWatchClick = onWatchMatch,
                                                onChannelClick = onChannelClick,
                                                onReminderClick = { viewModel.setReminder(context, match) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SportsTabMode.STANDINGS -> {
                when (val s = state) {
                    is SportsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = BeoutColors.BrightBlue)
                        }
                    }
                    is SportsUiState.Error -> BeoutErrorState(onRetry = viewModel::refresh)
                    is SportsUiState.Success -> {
                        BeoutCategoryRow(
                            categories = topLeagues.map { it.second },
                            selected = topLeagues.firstOrNull { it.first == selectedLeagueId }?.second ?: topLeagues.first().second,
                            onSelect = { selectedLabel ->
                                val match = topLeagues.firstOrNull { it.second == selectedLabel }
                                if (match != null) {
                                    selectedLeagueId = match.first
                                    viewModel.loadLeagueStanding(match.first, match.second)
                                }
                            }
                        )

                        Spacer(Modifier.height(BeoutDimensions.SpaceM))

                        val standing = s.currentStanding ?: s.allStandings.firstOrNull()

                        if (standing != null && standing.rows.isNotEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding,
                                    end = BeoutDimensions.ScreenPadding,
                                    bottom = if (isTvMode) 32.dp else 80.dp
                                )
                            ) {
                                item {
                                    BeoutStandingsTable(standing = standing)
                                }
                            }
                        } else {
                            BeoutEmptyState(
                                title = tr("جدول الترتيب غير متوفر حالياً", "Standings Not Available", "Puan Durumu Mevcut Değil", "جدول در دسترس نیست", "Classement indisponible"),
                                description = tr("جاري تحديث بيانات الترتيب للبطولة المختارة.", "Standings are currently updating.", "Puan durumu güncelleniyor.", "داده‌های جدول در حال به‌روزرسانی است.", "Mise à jour du classement en cours.")
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Selecting / Changing Favorite Club
    if (showClubBottomSheet) {
        FavoriteClubBottomSheet(
            onDismiss = { showClubBottomSheet = false },
            onClubSelected = { club ->
                showClubBottomSheet = false
                activeTab = SportsTabMode.MY_CLUB
                val successMsg = tr(
                    "تم تعيين ${club.name} كناديك المفضل بنجاح",
                    "${club.name} set as your favorite club",
                    "${club.name} favori kulübünüz olarak seçildi",
                    "${club.name} به عنوان تیم محبوب شما انتخاب شد",
                    "${club.name} défini comme club favori"
                )
                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Bottom Sheet for Filtering Matches by Tournament / League
    if (showLeagueBottomSheet) {
        val currentMatches = (state as? SportsUiState.Success)?.matches ?: emptyList()
        val allComps = currentMatches
            .map { it.competition }
            .distinct()
            .sortedBy { getCompetitionPriority(it) }
            .map { comp ->
                comp to currentMatches.count { it.competition.equals(comp, ignoreCase = true) }
            }
        val compList = listOf("All" to currentMatches.size) + allComps

        SportsLeagueBottomSheet(
            competitions = compList,
            selectedCompetition = selectedCompetition,
            onSelectCompetition = { selectedCompetition = it },
            onDismiss = { showLeagueBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SportsLeagueBottomSheet(
    competitions: List<Pair<String, Int>>,
    selectedCompetition: String,
    onSelectCompetition: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BeoutColors.Background,
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.ElevatedSurface)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = BeoutDimensions.ScreenPadding)
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("اختر البطولة", "Select Tournament", "Turnuva Seçin", "انتخاب مسابقات", "Choisir la compétition"),
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Text(
                        text = tr("تصفية المباريات بحسب البطولة المحددة", "Filter matches by tournament", "Maçları turnuvaya göre filtrele", "فیلتر مسابقات بر اساس لیگ", "Filtrer les matchs par compétition"),
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                        color = BeoutColors.TextSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.ElevatedSurface)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = BeoutColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(BeoutDimensions.SpaceM))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(competitions, key = { it.first }) { (compName, count) ->
                    val isSelected = compName == selectedCompetition
                    val isAll = compName == "All"
                    val displayName = if (isAll) tr("كافة البطولات", "All Tournaments", "Tüm Turnuvalar", "همه مسابقات", "Toutes les compétitions") else compName
                    val logo = if (!isAll) LeagueLogoHelper.getLogo(compName, null) else ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                            .clickable {
                                onSelectCompetition(compName)
                                onDismiss()
                            }
                            .padding(BeoutDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.ElevatedSurface)
                                .padding(if (!isAll && logo.isNotBlank()) 3.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isAll && logo.isNotBlank()) {
                                AsyncImage(
                                    model = logo,
                                    contentDescription = compName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isAll) Icons.Rounded.EmojiEvents else Icons.Rounded.SportsSoccer,
                                    contentDescription = null,
                                    tint = BeoutColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = BeoutTextStyles.CardTitle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BeoutColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (count > 0) {
                                Text(
                                    text = "$count ${tr("مباراة", "matches", "maç", "بازی", "matchs")}",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                                    color = BeoutColors.TextSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = BeoutColors.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCompetitionPriority(compName: String): Int {
    val comp = compName.lowercase()
    return when {
        comp.contains("إنجليزي") || comp.contains("انكليزي") || comp.contains("premier") -> 1
        comp.contains("إسباني") || comp.contains("اسباني") || comp.contains("la liga") || comp.contains("laliga") -> 2
        comp.contains("إيطالي") || comp.contains("ايطالي") || comp.contains("serie a") -> 3
        comp.contains("ألماني") || comp.contains("الماني") || comp.contains("bundesliga") -> 4
        comp.contains("فرنسي") || comp.contains("ligue 1") -> 5
        comp.contains("عراق") || comp.contains("iraq") || comp.contains("نجوم العراق") -> 6
        comp.contains("أبطال أوروبا") || comp.contains("ابطال اوروبا") || comp.contains("champions league") -> 7
        comp.contains("روشن") || comp.contains("سعودي") || comp.contains("saudi") -> 8
        comp.contains("أبطال آسيا") || comp.contains("afc champions") -> 9
        comp.contains("أبطال إفريقيا") || comp.contains("caf champions") -> 10
        comp.contains("أمريكي") || comp.contains("mls") || comp.contains("major league") -> 11
        comp.contains("مصري") || comp.contains("egypt") -> 12
        comp.contains("أدنوك") || comp.contains("إماراتي") || comp.contains("uae") -> 13
        comp.contains("قطر") -> 14
        comp.contains("مغربي") || comp.contains("botola") -> 15
        comp.contains("تونسي") -> 16
        comp.contains("جزائري") -> 17
        comp.contains("برتغالي") -> 18
        comp.contains("هولندي") -> 19
        comp.contains("تركي") -> 20
        comp.contains("بلجيكي") -> 21
        comp.contains("برازيلي") || comp.contains("brazil") -> 80
        else -> 99
    }
}
