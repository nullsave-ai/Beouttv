package com.beouttv.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.ui.navigation.BeoutDestination
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String? = null,
    val isPrimary: Boolean = false
)

private data class DrawerNavSection(
    val title: String,
    val items: List<DrawerMenuItem>
)

@Composable
fun BeoutNavigationDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val sections = listOf(
        DrawerNavSection(
            title = tr("التصفح الرئيسي", "MAIN EXPLORE", "ANA MENÜ", "منوی اصلی", "NAVIGATION"),
            items = listOf(
                DrawerMenuItem(
                    route = BeoutDestination.Home.route,
                    title = S.home(),
                    subtitle = tr("الصفحة الرئيسية والمحتوى الموصى به", "Home & Recommendations", "Ana Sayfa", "خانه", "Accueil"),
                    icon = Icons.Rounded.Home,
                    accentColor = Color(0xFF007AFF) // iOS Blue
                ),
                DrawerMenuItem(
                    route = BeoutDestination.MovieBoxMoviesCatalog.route,
                    title = tr("أفلام وان+", "ONE+ Movies", "Filmler", "فیلم‌ها", "Films"),
                    subtitle = tr("مكتبة سينما بدقة FHD و 4K", "Full HD & 4K Cinema Catalog", "Full HD Sinema", "سینما کامل", "Films HD"),
                    icon = Icons.Rounded.Movie,
                    accentColor = Color(0xFF5856D6), // iOS Purple
                    badge = "FHD",
                    isPrimary = true
                ),
                DrawerMenuItem(
                    route = BeoutDestination.MovieBoxSeriesCatalog.route,
                    title = tr("مسلسلات وان+", "ONE+ Series", "Diziler", "سریال‌ها", "Séries"),
                    subtitle = tr("كافة المواسم والحلقات مترجمة", "All Seasons & Episodes", "Tüm Bölümler", "فصل‌ها و قسمت‌ها", "Toutes les saisons"),
                    icon = Icons.Rounded.Tv,
                    accentColor = Color(0xFF30B0C7), // iOS Teal
                    badge = "مترجم"
                ),
                DrawerMenuItem(
                    route = BeoutDestination.AnimeCatalog.route,
                    title = tr("عالم الأنمي", "Anime Universe", "Anime Dünyası", "دنیای انیمه", "Monde Anime"),
                    subtitle = tr("أفلام ومسلسلات الأنمي الياباني", "Japanese Anime & Movies", "Japon Animeleri", "انیمه ژاپنی", "Animes"),
                    icon = Icons.Rounded.Animation,
                    accentColor = Color(0xFFFF2D55), // iOS Pink
                    badge = "جديد",
                    isPrimary = true
                ),
                DrawerMenuItem(
                    route = BeoutDestination.TrendingCatalog.route,
                    title = tr("الأكثر رواجاً", "Trending", "En Çok İzlenenler", "پربازدیدترین‌ها", "Tendances"),
                    subtitle = tr("الأعمال الأكثر مشاهدة وتفاعلاً", "Top Trending Media", "Popüler", "ترند", "Populaire"),
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    accentColor = Color(0xFFFF9500), // iOS Orange
                    badge = "HOT"
                )
            )
        ),
        DrawerNavSection(
            title = tr("سينما وقنوات BEOUT", "BEOUT ENTERTAINMENT", "BEOUT MEDYA", "مدیای بی اوت", "BEOUT CINÉMA"),
            items = listOf(
                DrawerMenuItem(
                    route = BeoutDestination.Movies.route,
                    title = tr("سينما ومكتبة Beout", "Beout Cinema", "Beout Sinema", "سینما بی اوت", "Cinéma Beout"),
                    subtitle = tr("أحدث الأفلام العربية والأجنبية", "Latest Arabic & World Cinema", "En Son Filmler", "فیلم‌های جدید", "Derniers films"),
                    icon = Icons.Rounded.VideoLibrary,
                    accentColor = Color(0xFF007AFF)
                ),
                DrawerMenuItem(
                    route = BeoutDestination.Series.route,
                    title = tr("مسلسلات Beout الحصرية", "Beout Exclusive Series", "Beout Dizileri", "سریال‌های بی اوت", "Séries Beout"),
                    subtitle = tr("أعمال حصرية ومسلسلات درامية", "Exclusive Drama Series", "Özel Diziler", "سریال‌های اختصاصی", "Séries exclusives"),
                    icon = Icons.Rounded.VideoSettings,
                    accentColor = Color(0xFFAF52DE)
                ),
                DrawerMenuItem(
                    route = BeoutDestination.LiveTv.route,
                    title = S.liveTv(),
                    subtitle = tr("البث المباشر للقنوات الرياضية والفضائية", "Live Sports & Satellite Channels", "Canlı Kanallar", "کانال‌های زنده", "Chaînes en direct"),
                    icon = Icons.Rounded.LiveTv,
                    accentColor = Color(0xFFFF3B30) // iOS Red
                ),
                DrawerMenuItem(
                    route = BeoutDestination.Sports.route,
                    title = tr("المباريات والرياضة", "Sports & Matches", "Maçlar ve Spor", "مسابقات و ورزش", "Matchs & Sports"),
                    subtitle = tr("جدول المباريات والترتيب والبث", "Fixtures & Live Match Streams", "Maç Takvimi", "جدول مسابقات", "Calendrier"),
                    icon = Icons.Rounded.SportsSoccer,
                    accentColor = Color(0xFF34C759) // iOS Green
                )
            )
        ),
        DrawerNavSection(
            title = tr("مساحتي الخاصة", "MY SPACE", "KİŞİSEL", "فضای شخصی", "MON ESPACE"),
            items = listOf(
                DrawerMenuItem(
                    route = "downloads_screen",
                    title = tr("التحميلات", "Downloads", "İndirilenler", "دانلودها", "Téléchargements"),
                    subtitle = tr("مشاهدة بدون اتصال بالإنترنت", "Offline Playback & Storage", "Çevrimdışı İzleme", "پخش آفلاین", "Hors-ligne"),
                    icon = Icons.Rounded.Download,
                    accentColor = Color(0xFF007AFF),
                    badge = "Offline",
                    isPrimary = true
                ),
                DrawerMenuItem(
                    route = BeoutDestination.Favorites.route,
                    title = S.favorites(),
                    subtitle = tr("قائمتي المفضلة للأعمال والقنوات", "My Saved Content", "Favorilerim", "علاقه‌مندی‌ها", "Mes favoris"),
                    icon = Icons.Rounded.Favorite,
                    accentColor = Color(0xFFFF2D55)
                ),
                DrawerMenuItem(
                    route = BeoutDestination.Settings.route,
                    title = S.settings(),
                    subtitle = tr("إعدادات المشغل ومصادر البث", "Player & Source Configurations", "Ayarlar", "تنظیمات", "Paramètres"),
                    icon = Icons.Rounded.Settings,
                    accentColor = Color(0xFF8E8E93)
                )
            )
        )
    )

    ModalDrawerSheet(
        drawerContainerColor = BeoutColors.Surface,
        drawerContentColor = BeoutColors.TextPrimary,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.Surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                sections.forEach { section ->
                    item(key = "section_${section.title}") {
                        Column {
                            // iOS Section Header Label
                            Text(
                                text = section.title,
                                style = BeoutTextStyles.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = BeoutColors.TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp, top = 4.dp)
                            )

                            // iOS Inset Group Container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BeoutColors.ElevatedSurface)
                                    .border(0.5.dp, BeoutColors.Divider, RoundedCornerShape(16.dp))
                            ) {
                                section.items.forEachIndexed { index, item ->
                                    val isSelected = currentRoute == item.route

                                    val itemBgColor by animateColorAsState(
                                        targetValue = if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.14f) else Color.Transparent,
                                        animationSpec = tween(150),
                                        label = "drawerItemBg"
                                    )

                                    val itemTextColor by animateColorAsState(
                                        targetValue = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextPrimary,
                                        animationSpec = tween(150),
                                        label = "drawerItemText"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(itemBgColor)
                                            .clickable {
                                                onCloseDrawer()
                                                onNavigate(item.route)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // iOS Rounded Squircle Icon Container
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(item.accentColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.accentColor,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    style = BeoutTextStyles.Body.copy(
                                                        fontSize = 13.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                    ),
                                                    color = itemTextColor,
                                                    maxLines = 1
                                                )

                                                item.badge?.let { badgeText ->
                                                    val badgeBg = if (badgeText == "HOT") BeoutColors.LiveRed.copy(alpha = 0.2f)
                                                    else BeoutColors.BrightBlue.copy(alpha = 0.2f)

                                                    val badgeBorder = if (badgeText == "HOT") BeoutColors.LiveRed
                                                    else BeoutColors.BrightBlue

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .background(badgeBg)
                                                            .border(0.6.dp, badgeBorder.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            style = BeoutTextStyles.Caption.copy(
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = badgeBorder
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = item.subtitle,
                                                style = BeoutTextStyles.Caption.copy(
                                                    fontSize = 10.5.sp,
                                                    color = BeoutColors.TextSecondary
                                                ),
                                                maxLines = 1
                                            )
                                        }

                                        // Subtle iOS Chevron Indicator
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Inset Divider between items inside group (iOS list style)
                                    if (index < section.items.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 58.dp),
                                            thickness = 0.5.dp,
                                            color = BeoutColors.Divider.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}
