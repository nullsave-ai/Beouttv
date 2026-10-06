@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.beouttv.app.ui.screens.shortdrama

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.beouttv.app.data.model.ShortDramaCategory
import com.beouttv.app.data.model.ShortDramaEpisode
import com.beouttv.app.data.model.ShortDramaFeedItem
import com.beouttv.app.data.repository.ShortDramaDownloadManager
import com.beouttv.app.data.repository.ShortDramaRepository
import com.beouttv.app.ui.theme.BeoutColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortDramaEpisodesBottomSheet(
    feedItem: ShortDramaFeedItem,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onEpisodeSelected: (ShortDramaEpisode) -> Unit,
    onSourceSelected: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<ShortDramaEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0 = Episodes, 1 = Servers

    LaunchedEffect(feedItem.series.id) {
        isLoadingEpisodes = true
        try {
            val fetched = ShortDramaRepository.getEpisodes(feedItem.series.id)
            episodes = fetched
        } catch (_: Exception) {}
        isLoadingEpisodes = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BeoutColors.Surface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.Divider)
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Series Title & Subtitle Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feedItem.series.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BeoutColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "بي اوت TV • ${feedItem.series.episodeCount} حلقة",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BeoutColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = BeoutColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs: Episodes vs Streaming Servers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BeoutColors.Card)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Episodes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) BeoutColors.BrightBlue else Color.Transparent)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else BeoutColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "قائمة الحلقات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == 0) Color.White else BeoutColors.TextSecondary
                            )
                        )
                    }
                }

                // Tab 1: Multi-Servers
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) BeoutColors.BrightBlue else Color.Transparent)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else BeoutColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "خيارات الجودة (${feedItem.sources.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == 1) Color.White else BeoutColors.TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                val context = LocalContext.current
                val activeDownloads by ShortDramaDownloadManager.activeDownloads.collectAsState()
                val downloadedSeriesList by ShortDramaDownloadManager.downloadedSeriesList.collectAsState()
                val batchProgress = activeDownloads[feedItem.series.id]
                var showQualityPicker by remember { mutableStateOf(false) }

                // ── Batch Download Action / Progress Card ──
                if (batchProgress != null && (batchProgress.isDownloading || batchProgress.isPaused)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BeoutColors.Card)
                            .border(1.dp, BeoutColors.Divider, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (batchProgress.isPaused) Icons.Rounded.PauseCircle else Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = batchProgress.statusText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            if (batchProgress.speedText.isNotBlank()) {
                                Text(
                                    text = batchProgress.speedText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BeoutColors.BrightBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { batchProgress.overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = BeoutColors.BrightBlue,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (batchProgress.isDownloading) {
                                        ShortDramaDownloadManager.pauseBatchDownload(feedItem.series.id)
                                    } else {
                                        ShortDramaDownloadManager.startBatchDownload(context, feedItem.series)
                                    }
                                }
                            ) {
                                Text(
                                    text = if (batchProgress.isDownloading) "إيقاف مؤقت" else "استئناف",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BeoutColors.BrightBlue)
                                )
                            }
                            TextButton(
                                onClick = {
                                    ShortDramaDownloadManager.cancelBatchDownload(feedItem.series.id)
                                }
                            ) {
                                Text(
                                    text = "إلغاء",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Red.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    // Sleek, Premium Matte Download Button (Non-glowing)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BeoutColors.Card)
                            .border(1.dp, BeoutColors.Divider, RoundedCornerShape(12.dp))
                            .clickable { showQualityPicker = true }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BeoutColors.Surface)
                                    .border(1.dp, BeoutColors.Divider, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "تنزيل الحلقات للمشاهدة بدون إنترنت",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BeoutColors.TextPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "تنزيل الكل (${feedItem.series.episodeCount} حلقة) أو تحديد نطاق مخصص",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BeoutColors.TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Advanced Download Range & Quality Configuration Dialog ──
                if (showQualityPicker) {
                    val maxEpisodes = feedItem.series.episodeCount.coerceAtLeast(1)
                    var isCustomRange by remember { mutableStateOf(false) }
                    var startEpisode by remember { mutableIntStateOf(1) }
                    var endEpisode by remember { mutableIntStateOf(maxEpisodes) }
                    var selectedQuality by remember { mutableStateOf("720p HD") }

                    val episodeCountToDownload = (endEpisode - startEpisode + 1).coerceAtLeast(1)

                    AlertDialog(
                        onDismissRequest = { showQualityPicker = false },
                        containerColor = BeoutColors.Card,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "إعدادات تحميل الحلقات",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Mode Toggle: All vs Custom Range
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BeoutColors.Surface)
                                        .padding(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (!isCustomRange) BeoutColors.BrightBlue else Color.Transparent)
                                            .clickable {
                                                isCustomRange = false
                                                startEpisode = 1
                                                endEpisode = maxEpisodes
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "كافة الحلقات (1 إلى $maxEpisodes)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isCustomRange) Color.White else BeoutColors.TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCustomRange) BeoutColors.BrightBlue else Color.Transparent)
                                            .clickable { isCustomRange = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "تحديد حلقات معينة",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCustomRange) Color.White else BeoutColors.TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                // Custom Range Pickers (Visible if Custom Range selected)
                                if (isCustomRange) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BeoutColors.Surface)
                                            .border(1.dp, BeoutColors.Divider, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "من حلقة:",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = BeoutColors.TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (startEpisode > 1) startEpisode-- },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Remove, null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = "$startEpisode",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                IconButton(
                                                    onClick = { if (startEpisode < endEpisode) startEpisode++ },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Add, null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "إلى حلقة:",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = BeoutColors.TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (endEpisode > startEpisode) endEpisode-- },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Remove, null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = "$endEpisode",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                IconButton(
                                                    onClick = { if (endEpisode < maxEpisodes) endEpisode++ },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Add, null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        // Quick Presets
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val presets = listOf(
                                                "أول 10" to Pair(1, 10.coerceAtMost(maxEpisodes)),
                                                "أول 25" to Pair(1, 25.coerceAtMost(maxEpisodes)),
                                                "أول 50" to Pair(1, 50.coerceAtMost(maxEpisodes))
                                            )
                                            presets.forEach { (label, range) ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(BeoutColors.Card)
                                                        .border(0.8.dp, BeoutColors.Divider, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            startEpisode = range.first
                                                            endEpisode = range.second
                                                        }
                                                        .padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = BeoutColors.BrightBlue,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quality Options
                                Text(
                                    text = "اختر الجودة:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = BeoutColors.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )

                                val qualityOptions = listOf(
                                    Triple("1080p FHD", "دقة فائقة الوضوح (أعلى جودة)", "1080p FHD"),
                                    Triple("720p HD", "دقة متوازنة (موصى بها وسريعة)", "720p HD"),
                                    Triple("540p", "توفير البيانات والمساحة", "540p")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    qualityOptions.forEach { (title, subtitle, qKey) ->
                                        val isSelected = selectedQuality == qKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.15f) else BeoutColors.Surface)
                                                .border(
                                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                                    color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedQuality = qKey }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) BeoutColors.BrightBlue else Color.White,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = BeoutColors.TextSecondary,
                                                        fontSize = 9.5.sp
                                                    )
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedQuality = qKey },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = BeoutColors.BrightBlue,
                                                    unselectedColor = BeoutColors.TextSecondary
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    ShortDramaDownloadManager.startBatchDownload(
                                        context = context,
                                        series = feedItem.series,
                                        quality = selectedQuality,
                                        startEp = startEpisode,
                                        endEp = endEpisode
                                    )
                                    showQualityPicker = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "بدء التحميل ($episodeCountToDownload حلقة)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showQualityPicker = false }) {
                                Text("إلغاء", color = BeoutColors.TextSecondary)
                            }
                        }
                    )
                }

                // EPISODES GRID VIEW
                val displayEpisodes = if (episodes.isNotEmpty()) {
                    episodes
                } else {
                    val count = feedItem.series.episodeCount.coerceAtLeast(40)
                    val videoId = ShortDramaRepository.resolveVideoId(feedItem.series.id)
                    (1..count).map { num ->
                        ShortDramaEpisode(
                            id = "epservers_${videoId}_$num",
                            seriesId = feedItem.series.id,
                            episodeNumber = num,
                            title = "الحلقة $num"
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayEpisodes) { ep ->
                        val isCurrent = ep.episodeNumber == feedItem.episode.episodeNumber
                        val isDownloaded = remember(feedItem.series.id, ep.episodeNumber, downloadedSeriesList) {
                            ShortDramaDownloadManager.isEpisodeDownloaded(feedItem.series.id, ep.episodeNumber)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1.25f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isCurrent) BeoutColors.BrightBlue.copy(alpha = 0.2f)
                                    else BeoutColors.Card
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) BeoutColors.BrightBlue else BeoutColors.Divider,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onEpisodeSelected(ep)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onDismiss()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "محملة أوفلاين",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopStart)
                                        .padding(start = 3.dp, top = 3.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${ep.episodeNumber}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) BeoutColors.BrightBlue else BeoutColors.TextPrimary
                                    )
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "تشغيل",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = BeoutColors.BrightBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // QUALITY / SOURCES LIST VIEW
                Text(
                    text = "اختر الجودة المناسبة لسرعة اتصالك:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BeoutColors.TextSecondary
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(feedItem.sources) { index, source ->
                        val isSelected = index == feedItem.currentSourceIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.15f)
                                    else BeoutColors.Card
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSourceSelected(index)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) BeoutColors.BrightBlue
                                            else BeoutColors.ElevatedSurface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (source.isDirectCdn) Icons.Rounded.FlashOn else Icons.Rounded.CloudQueue,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = source.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = source.quality,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BeoutColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortDramaPlatformsBottomSheet(
    platforms: List<ShortDramaCategory>,
    selectedPlatformId: String?,
    onDismiss: () -> Unit,
    onPlatformSelected: (ShortDramaCategory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BeoutColors.Surface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.Divider)
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تصنيفات الدراما القصيرة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BeoutColors.TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مكتبة MovieBox الشاملة للدراما والمسلسلات القصيرة",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BeoutColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = BeoutColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(platforms) { platform ->
                    val isSelected = platform.id == selectedPlatformId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.15f)
                                else BeoutColors.Card
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onPlatformSelected(platform)
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) BeoutColors.BrightBlue
                                    else BeoutColors.ElevatedSurface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = platform.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextPrimary,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

