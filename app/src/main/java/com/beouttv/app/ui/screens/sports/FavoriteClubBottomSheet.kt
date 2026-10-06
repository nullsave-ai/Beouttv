package com.beouttv.app.ui.screens.sports

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beouttv.app.data.favorites.FavoriteClubManager
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.ClubInfo
import com.beouttv.app.data.model.LeagueInfo
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.launch

@Composable
fun getAvailableLeagues(): List<LeagueInfo> = listOf(
    LeagueInfo("7", tr("الدوري الإنجليزي", "Premier League", "Premier Lig", "لیگ برتر انگلیس", "Premier League"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/7", tr("إنجلترا", "England", "İngiltere", "انگلستان", "Angleterre")),
    LeagueInfo("11", tr("الدوري الإسباني", "La Liga", "La Liga", "لالیگا اسپانیا", "La Liga"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/11", tr("إسبانيا", "Spain", "İspanya", "اسپانیا", "Espagne")),
    LeagueInfo("17", tr("الدوري الإيطالي", "Serie A", "Serie A", "سری آ ایتالیا", "Serie A"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/17", tr("إيطاليا", "Italy", "İtalya", "ایتالیا", "Italie")),
    LeagueInfo("25", tr("الدوري الألماني", "Bundesliga", "Bundesliga", "بوندسلیگا", "Bundesliga"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/25", tr("ألمانيا", "Germany", "Almanya", "آلمان", "Allemagne")),
    LeagueInfo("35", tr("الدوري الفرنسي", "Ligue 1", "Ligue 1", "لوشامپیونه", "Ligue 1"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/35", tr("فرنسا", "France", "Fransa", "فرانسه", "France")),
    LeagueInfo("6822", tr("دوري نجوم العراق", "Iraq Stars League", "Irak Ligi", "لیگ ستارگان عراق", "Ligue d'Irak"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/6822", tr("العراق", "Iraq", "Irak", "عراق", "Irak")),
    LeagueInfo("572", tr("دوري أبطال أوروبا", "Champions League", "Şampiyonlar Ligi", "لیگ قهرمانان اروپا", "Ligue des Champions"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572", tr("أوروبا", "Europe", "Avrupa", "اروپا", "Europe"), true),
    LeagueInfo("649", tr("دوري روشن السعودي", "Saudi Pro League", "Suudi Ligi", "لیگ عربستان", "Ligue Saoudienne"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/649", tr("السعودية", "Saudi Arabia", "Suudi Arabistan", "عربستان", "Arabie Saoudite")),
    LeagueInfo("104", tr("الدوري الأمريكي", "MLS", "MLS", "لیگ برتر آمریکا", "MLS"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/104", tr("أمريكا", "USA", "ABD", "آمریکا", "États-Unis")),
    LeagueInfo("552", tr("الدوري المصري", "Egyptian Premier League", "Mısır Ligi", "لیگ مصر", "Ligue Égyptienne"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/552", tr("مصر", "Egypt", "Mısır", "مصر", "Égypte")),
    LeagueInfo("549", tr("دوري أدنوك الإماراتي", "UAE Pro League", "BAE Ligi", "لیگ امارات", "Ligue des EAU"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/549", tr("الإمارات", "UAE", "BAE", "امارات", "Émirats")),
    LeagueInfo("5455", tr("دوري نجوم قطر", "Qatar Stars League", "Katar Ligi", "لیگ ستارگان قطر", "Ligue du Qatar"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/5455", tr("قطر", "Qatar", "Katar", "قطر", "Qatar")),
    LeagueInfo("557", tr("الدوري المغربي", "Botola Pro", "Fas Ligi", "لیگ مراکش", "Botola Pro"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/557", tr("المغرب", "Morocco", "Fas", "مراکش", "Maroc")),
    LeagueInfo("554", tr("الرابطة التونسية المحترفة", "Tunisian Ligue 1", "Tunus Ligi", "لیگ تونس", "Ligue Tunisienne"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/554", tr("تونس", "Tunisia", "Tunus", "تونس", "Tunisie")),
    LeagueInfo("560", tr("الرابطة الجزائرية المحترفة", "Algerian Ligue 1", "Cezayir Ligi", "لیگ الجزایر", "Ligue Algérienne"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/560", tr("الجزائر", "Algeria", "Cezayir", "الجزایر", "Algérie")),
    LeagueInfo("73", tr("الدوري البرتغالي", "Liga Portugal", "Portekiz Ligi", "لیگ پرتغال", "Liga Portugal"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/73", tr("البرتغال", "Portugal", "Portekiz", "پرتغال", "Portugal")),
    LeagueInfo("57", tr("الدوري الهولندي", "Eredivisie", "Hollanda Ligi", "لیگ هلند", "Eredivisie"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/57", tr("هولندا", "Netherlands", "Hollanda", "هلند", "Pays-Bas")),
    LeagueInfo("78", tr("الدوري التركي", "Süper Lig", "Süper Lig", "لیگ ترکیه", "Süper Lig"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/78", tr("تركيا", "Turkey", "Türkiye", "ترکیه", "Turquie")),
    LeagueInfo("98", tr("الدوري البلجيكي", "Belgian Pro League", "Belçika Ligi", "لیگ بلژیک", "Pro League"), "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/98", tr("بلجيكا", "Belgium", "Belçika", "بلژیک", "Belgique"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteClubBottomSheet(
    onDismiss: () -> Unit,
    onClubSelected: (ClubInfo) -> Unit,
    repository: BeoutRepository = ServiceLocator.repository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLeague by remember { mutableStateOf<LeagueInfo?>(null) }
    var isLoadingTeams by remember { mutableStateOf(false) }
    var teamsList by remember { mutableStateOf<List<ClubInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val leagues = getAvailableLeagues()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = BeoutDimensions.ScreenPadding)
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedLeague != null) {
                    IconButton(
                        onClick = {
                            selectedLeague = null
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.ElevatedSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Back",
                            tint = BeoutColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedLeague == null) {
                            tr("اختر بطولة أو دوري", "Select a League", "Bir Lig Seçin", "انتخاب لیگ", "Choisir une ligue")
                        } else {
                            tr("اختر ناديك المفضل", "Select Favorite Club", "Favori Kulübünü Seç", "انتخاب تیم محبوب", "Choisir votre club")
                        },
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Text(
                        text = if (selectedLeague == null) {
                            tr("تصفح الدوريات الكبرى لعرض أنديتها", "Browse top leagues to view their clubs", "Kulüpleri görmek için liglere göz atın", "لیگ‌های برتر را برای مشاهده تیم‌ها بررسی کنید", "Parcourez les ligues majeures")
                        } else {
                            selectedLeague?.name ?: ""
                        },
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                        color = if (selectedLeague != null) BeoutColors.BrightBlue else BeoutColors.TextSecondary
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

            // Step 1: Browse Leagues Grid
            if (selectedLeague == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                    verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(leagues, key = { it.id }) { league ->
                        LeagueCardItem(
                            league = league,
                            onClick = {
                                selectedLeague = league
                                isLoadingTeams = true
                                coroutineScope.launch {
                                    teamsList = repository.getTeamsForLeague(league.id)
                                    isLoadingTeams = false
                                }
                            }
                        )
                    }
                }
            } else {
                // Step 2: Browse Clubs of the Selected League
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = tr("ابحث عن اسم النادي...", "Search club name...", "Kulüp ara...", "جستجوی نام تیم...", "Rechercher un club..."),
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp),
                            color = BeoutColors.TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = BeoutColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = BeoutShapes.Medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BeoutColors.Card,
                        unfocusedContainerColor = BeoutColors.Card,
                        focusedBorderColor = BeoutColors.BrightBlue,
                        unfocusedBorderColor = BeoutColors.Divider,
                        focusedTextColor = BeoutColors.TextPrimary,
                        unfocusedTextColor = BeoutColors.TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                Spacer(Modifier.height(BeoutDimensions.SpaceM))

                if (isLoadingTeams) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BeoutColors.BrightBlue)
                    }
                } else {
                    val filteredTeams = teamsList.filter {
                        if (searchQuery.isBlank()) true else it.name.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredTeams.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tr("لم يتم العثور على أندية مطابقة", "No matching clubs found", "Eşleşen kulüp bulunamadı", "تیمی یافت نشد", "Aucun club trouvé"),
                                style = BeoutTextStyles.CardTitle,
                                color = BeoutColors.TextSecondary
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                            verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredTeams, key = { it.id }) { club ->
                                val updatedClub = club.copy(
                                    leagueId = selectedLeague?.id ?: club.leagueId,
                                    leagueName = selectedLeague?.name ?: club.leagueName
                                )
                                ClubGridItem(
                                    club = updatedClub,
                                    onClick = {
                                        FavoriteClubManager.setFavoriteClub(context, updatedClub)
                                        onClubSelected(updatedClub)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueCardItem(
    league: LeagueInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .clickable(onClick = onClick)
            .padding(BeoutDimensions.SpaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = league.logoUrl,
            contentDescription = league.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BeoutColors.ElevatedSurface)
                .padding(3.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = league.name,
                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = if (league.isUcl) Color(0xFFB0D2FF) else BeoutColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = league.country,
                style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                color = BeoutColors.TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronLeft,
            contentDescription = null,
            tint = BeoutColors.TextDisabled,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ClubGridItem(
    club: ClubInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .clickable(onClick = onClick)
            .padding(BeoutDimensions.SpaceS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(BeoutColors.ElevatedSurface)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = club.logoUrl,
                contentDescription = club.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = club.name,
            style = BeoutTextStyles.CardTitle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary,
            maxLines = 1
        )

        if (club.rank > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = tr(
                    "المركز #${club.rank} • ${club.points}ن",
                    "Rank #${club.rank} • ${club.points}pts",
                    "Sıra #${club.rank} • ${club.points}p",
                    "رتبه #${club.rank} • ${club.points} امتیاز",
                    "Rang #${club.rank} • ${club.points}pts"
                ),
                style = BeoutTextStyles.Caption.copy(fontSize = 9.5.sp),
                color = BeoutColors.BrightBlue
            )
        }
    }
}
