package com.beouttv.app.ui.screens.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.ContentCategory
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

@Composable
fun LiveTvScreen(
    onChannelClick: (Channel) -> Unit,
    viewModel: LiveTvViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val query by viewModel.query.collectAsState()
    val bouquets by viewModel.bouquets.collectAsState()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val resolvingChannelId by viewModel.resolvingChannelId.collectAsState()
    var showCategorySheet by remember { mutableStateOf(false) }
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    // Listen for resolved channels and fire onChannelClick immediately
    LaunchedEffect(Unit) {
        viewModel.resolvedChannel.collect { resolvedChannel ->
            onChannelClick(resolvedChannel)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .then(if (isTvMode) Modifier.padding(top = 10.dp) else Modifier.statusBarsPadding())
    ) {
        if (isTvMode) {
            val activeBouquetLabel = remember(selectedCategory) {
                getBouquetDisplayLabel(selectedCategory)
            }
            val activeChannelCount = remember(selectedCategory, bouquets, totalCount) {
                if (selectedCategory == "All") totalCount
                else bouquets.firstOrNull { it.key == selectedCategory }?.count ?: 0
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 76.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Bouquet Dropdown Selector Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                        .tvFocusable(shape = BeoutShapes.Medium, onEnter = { showCategorySheet = true })
                        .clickable { showCategorySheet = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryVectorIcon(selectedCategory),
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$activeBouquetLabel ($activeChannelCount)",
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Quick Favorites Filter Toggle Button
                val favBorder = if (showOnlyFavorites) BeoutColors.BrightBlue else BeoutColors.Divider
                val favContentColor = if (showOnlyFavorites) BeoutColors.BrightBlue else BeoutColors.TextSecondary
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(BeoutShapes.Medium)
                        .background(if (showOnlyFavorites) BeoutColors.BrightBlue.copy(alpha = 0.2f) else BeoutColors.Card)
                        .border(1.dp, favBorder, BeoutShapes.Medium)
                        .tvFocusable(shape = BeoutShapes.Medium, onEnter = { viewModel.toggleOnlyFavorites() })
                        .clickable { viewModel.toggleOnlyFavorites() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorites",
                            tint = favContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr("المفضلة", "Favorites", "Favoriler", "علاقه‌مندی‌ها", "Favoris"),
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 12.sp,
                                fontWeight = if (showOnlyFavorites) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = favContentColor
                        )
                        if (favoritesCount > 0) {
                            Text(
                                text = "($favoritesCount)",
                                style = BeoutTextStyles.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = favContentColor
                            )
                        }
                    }
                }

                // Search Bar in remaining width
                BeoutSearchBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = tr("ابحث عن قناة...", "Search channels...", "Kanal ara...", "جستجوی شبکه...", "Rechercher une chaîne..."),
                    modifier = Modifier.weight(1f)
                )

                // Refresh Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, CircleShape)
                        .tvFocusable(shape = CircleShape, onEnter = { viewModel.load(showSkeleton = false) })
                        .clickable { viewModel.load(showSkeleton = false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        } else {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = S.liveTv(),
                        style = BeoutTextStyles.ScreenTitle,
                        color = BeoutColors.TextPrimary
                    )
                    Text(
                        text = tr("بث مباشر عالي الجودة • سيرفرات سحابية فائقة الثبات", "High Quality Live Streams • Ultra Stable Cloud Servers", "Yüksek Kaliteli Canlı Yayın", "پخش زنده با کیفیت بالا", "Diffusion en direct de haute qualité"),
                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                        color = BeoutColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, CircleShape)
                        .tvFocusable(shape = CircleShape)
                        .clickable { viewModel.load(showSkeleton = false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Search Bar
            BeoutSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = tr("ابحث عن قناة أو باقة...", "Search channels or bouquets...", "Kanal veya paket ara...", "جستجوی شبکه یا پکیج...", "Rechercher chaînes ou bouquets..."),
                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
            )
            Spacer(Modifier.height(BeoutDimensions.SpaceS))

            // Category Selector & Quick Favorites Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Bouquet Dropdown Selector Button
                val activeBouquetLabel = remember(selectedCategory) {
                    getBouquetDisplayLabel(selectedCategory)
                }
                val activeChannelCount = remember(selectedCategory, bouquets, totalCount) {
                    if (selectedCategory == "All") totalCount
                    else bouquets.firstOrNull { it.key == selectedCategory }?.count ?: 0
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                        .tvFocusable(shape = BeoutShapes.Medium)
                        .clickable { showCategorySheet = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryVectorIcon(selectedCategory),
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr("الباقة المحددة", "Selected Bouquet", "Seçilen Paket", "پکیج انتخابی", "Bouquet sélectionné"),
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                                    color = BeoutColors.TextSecondary
                                )
                                Text(
                                    text = activeBouquetLabel,
                                    style = BeoutTextStyles.BodyBold.copy(fontSize = 13.sp),
                                    color = BeoutColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BeoutColors.ElevatedSurface)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$activeChannelCount ${tr("قناة", "ch", "kanal", "شبکه", "ch")}",
                                    style = BeoutTextStyles.Caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Open Bouquets",
                                tint = BeoutColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quick Favorites Filter Toggle Button (Blue Enterprise Identity)
                val favBorder = if (showOnlyFavorites) BeoutColors.BrightBlue else BeoutColors.Divider
                val favBg = BeoutColors.Card
                val favContentColor = if (showOnlyFavorites) BeoutColors.BrightBlue else BeoutColors.TextSecondary

                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(BeoutShapes.Medium)
                        .background(favBg)
                        .border(1.dp, favBorder, BeoutShapes.Medium)
                        .tvFocusable(shape = BeoutShapes.Medium)
                        .clickable { viewModel.toggleOnlyFavorites() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorites",
                            tint = favContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr("المفضلة", "Favorites", "Favoriler", "علاقه‌مندی‌ها", "Favoris"),
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 12.sp,
                                fontWeight = if (showOnlyFavorites) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = favContentColor
                        )
                        if (favoritesCount > 0) {
                            Text(
                                text = "($favoritesCount)",
                                style = BeoutTextStyles.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = favContentColor
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(BeoutDimensions.SpaceS))

        if (showCategorySheet) {
            BeoutCategoryBottomSheet(
                bouquets = bouquets,
                selectedCategory = selectedCategory,
                totalCount = totalCount,
                onCategorySelected = { catKey ->
                    viewModel.onCategorySelected(catKey)
                    showCategorySheet = false
                },
                onDismissRequest = { showCategorySheet = false }
            )
        }

        when (val s = state) {
            is LiveTvUiState.Loading -> LiveTvLoading()
            is LiveTvUiState.Error -> BeoutErrorState(onRetry = { viewModel.load(showSkeleton = true) })
            is LiveTvUiState.Success -> {
                val filtered = remember(s.channels, query) {
                    if (query.isBlank()) s.channels
                    else s.channels.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        cleanChannelDisplayName(it.name).contains(query, ignoreCase = true)
                    }
                }

                if (filtered.isEmpty()) {
                    BeoutEmptyState(
                        title = S.noResults(),
                        description = tr("يرجى تجربة البحث عن باقة أو قناة أخرى.", "Try searching for a different channel or category.", "Farklı bir kanal veya kategori aramayı deneyin.", "شبکه یا دسته‌بندی دیگری را جستجو کنید.", "Essayez de rechercher une autre chaîne.")
                    )
                } else {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val liveGridCols = when {
                        isTvMode -> 5
                        isLandscape -> 5
                        screenWidth >= 700.dp -> 5
                        screenWidth >= 480.dp -> 4
                        screenWidth >= 340.dp -> 3
                        else -> 2
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(liveGridCols),
                        contentPadding = PaddingValues(
                            start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding,
                            end = BeoutDimensions.ScreenPadding,
                            top = 8.dp,
                            bottom = if (isTvMode) 32.dp else 80.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.id },
                            contentType = { "live_channel_card" }
                        ) { channel ->
                            val isResolving = resolvingChannelId == channel.id
                            LiveChannelCard(
                                channel = channel,
                                isTvMode = isTvMode,
                                isResolving = isResolving,
                                onClick = { if (!isResolving) viewModel.resolveAndPlay(channel) },
                                onFavoriteClick = { viewModel.toggleFavorite(channel.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: Channel,
    isTvMode: Boolean = false,
    isResolving: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isCompact = isTvMode || isLandscape

    val favTint by animateColorAsState(
        targetValue = if (channel.isFavorite) BeoutColors.BrightBlue else BeoutColors.TextSecondary.copy(alpha = 0.7f),
        label = "fav_tint"
    )
    val cleanName = remember(channel.name) { cleanChannelDisplayName(channel.name) }
    val cleanCat = remember(channel.category) { cleanCategoryDisplayName(channel.category) }

    Column(
        modifier = modifier
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(
                width = if (isResolving) 1.5.dp else 1.dp,
                color = if (isResolving) BeoutColors.BrightBlue else BeoutColors.Divider,
                shape = BeoutShapes.Medium
            )
            .tvFocusable(shape = BeoutShapes.Medium)
            .clickable(onClick = onClick)
            .padding(if (isCompact) 8.dp else 10.dp)
    ) {
        // Logo Container - Pure matte Obsidian surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 50.dp else 60.dp)
                .clip(BeoutShapes.Small)
                .background(BeoutColors.ElevatedSurface)
                .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                .padding(if (isCompact) 5.dp else 8.dp)
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = cleanName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Show spinner overlay when resolving stream URL
            if (isResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BeoutColors.BrightBlue,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                // Live Badge
                LiveBadge(modifier = Modifier.align(Alignment.TopStart))
            }

            // Favorite Icon Button
            if (!isResolving) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onFavoriteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = favTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Channel Name
        Text(
            text = if (isResolving) tr("جارٍ تحميل البث...", "Loading stream...", "Yayın yükleniyor...", "در حال بارگذاری...", "Chargement...") else cleanName,
            style = BeoutTextStyles.CardTitle.copy(
                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isResolving) BeoutColors.BrightBlue else BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(1.dp))

        Text(
            text = cleanCat.ifBlank { tr("قناة تلفزيونية", "TV Channel", "TV Kanalı", "شبکه تلویزیونی", "Chaîne TV") },
            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = if (isCompact) 9.5.sp else 10.5.sp),
            color = BeoutColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LiveTvLoading() {
    Column(modifier = Modifier.padding(BeoutDimensions.ScreenPadding)) {
        BeoutChannelSkeletonRow()
        Spacer(Modifier.height(BeoutDimensions.SpaceS))
        BeoutChannelSkeletonRow()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeoutCategoryBottomSheet(
    bouquets: List<LiveBouquet>,
    selectedCategory: String,
    totalCount: Int,
    onCategorySelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                        text = tr("باقات البث المباشر", "Live TV Bouquets", "Canlı Yayın Paketleri", "پکیج‌های پخش زنده", "Bouquets TV en direct"),
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp),
                        color = BeoutColors.TextPrimary
                    )
                    Text(
                        text = "$totalCount ${tr("قناة متاحة للتنقل السريع", "channels available for quick access", "kanal mevcut", "شبکه موجود", "chaînes disponibles")}",
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                        color = BeoutColors.TextSecondary
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
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

            // Bouquets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bouquets, key = { it.key }) { bouquet ->
                    val isSelected = bouquet.key == selectedCategory
                    val label = getBouquetDisplayLabel(bouquet.key)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BeoutShapes.Medium)
                            .border(BorderStroke(1.dp, BeoutColors.Divider), BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                            .clickable {
                                onCategorySelected(bouquet.key)
                                onDismissRequest()
                            }
                            .padding(BeoutDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.ElevatedSurface)
                                .padding(if (bouquet.key != "All" && bouquet.firstChannelLogoUrl.isNotBlank()) 3.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bouquet.key != "All" && bouquet.firstChannelLogoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = bouquet.firstChannelLogoUrl,
                                    contentDescription = bouquet.key,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (bouquet.key == "All") Icons.Rounded.Tv else getCategoryVectorIcon(bouquet.key),
                                    contentDescription = null,
                                    tint = BeoutColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = BeoutTextStyles.CardTitle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BeoutColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${bouquet.count} ${tr("قناة", "ch", "kanal", "شبکه", "ch")}",
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                                color = BeoutColors.TextSecondary
                            )
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

private fun getCategoryVectorIcon(key: String): ImageVector {
    val k = key.lowercase()
    return when {
        k == "all" -> Icons.Rounded.Tv
        k.contains("bein") || k.contains("ssc") || k.contains("ثمانية") || k.contains("كأس") || k.contains("sport") || k.contains("رياض") -> Icons.Rounded.SportsSoccer
        k.contains("movie") || k.contains("أفلام") || k.contains("سينما") -> Icons.Rounded.Movie
        k.contains("news") || k.contains("إخبار") || k.contains("حدث") || k.contains("جزيرة") || k.contains("عربية") -> Icons.Rounded.Newspaper
        k.contains("kid") || k.contains("أطفال") || k.contains("spacetoon") || k.contains("cartoon") -> Icons.Rounded.ChildCare
        k.contains("mbc") || k.contains("ترفيه") || k.contains("entertainment") -> Icons.Rounded.LiveTv
        else -> Icons.Rounded.Layers
    }
}

private fun getBouquetDisplayLabel(key: String): String {
    return when (key) {
        "All" -> tr("جميع القنوات", "All Channels", "Tüm Kanallar", "همه شبکه‌ها", "Toutes les chaînes")
        "Sports" -> tr("رياضة عالمية", "World Sports", "Dünya Sporu", "ورزش جهانی", "Sports Mondiaux")
        "Movies" -> tr("سينما وأفلام", "Movies & Cinema", "Sinema", "سینما", "Cinéma")
        "Entertainment" -> tr("ترفيه وموسيقى", "Entertainment", "Eğlence", "سرگرمی", "Divertissement")
        "News" -> tr("إخبارية ووثائقية", "News & Doc", "Haber", "اخبار", "Infos")
        "Kids" -> tr("أطفال وعائلة", "Kids & Family", "Çocuk", "کودکان", "Enfants")
        else -> cleanCategoryDisplayName(key)
    }
}
