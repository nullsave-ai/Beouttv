package com.beouttv.app.ui.screens.settings

import androidx.compose.animation.*
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.beouttv.app.data.model.ShortDramaSeries
import com.beouttv.app.data.repository.DownloadedSeries
import com.beouttv.app.data.repository.ShortDramaDownloadManager
import com.beouttv.app.ui.theme.BeoutColors

@Composable
fun DownloadedDramaDialog(
    onDismiss: () -> Unit,
    onPlaySeries: (ShortDramaSeries, Int) -> Unit
) {
    val context = LocalContext.current
    val downloadedSeriesList by ShortDramaDownloadManager.downloadedSeriesList.collectAsState()
    var expandedSeriesId by remember { mutableStateOf<String?>(null) }
    var seriesToDelete by remember { mutableStateOf<DownloadedSeries?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.Background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.Card)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "دراما ريلز المحملة (أوفلاين)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            )
                            val totalEps = downloadedSeriesList.sumOf { it.downloadedCount }
                            val totalBytes = downloadedSeriesList.sumOf { it.totalBytes }
                            val totalMb = totalBytes / (1024f * 1024f)
                            val sizeText = if (totalMb > 1024) String.format("%.2f GB", totalMb / 1024f) else String.format("%.0f MB", totalMb)
                            Text(
                                text = "${downloadedSeriesList.size} مسلسلات • $totalEps حلقة • $sizeText",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BeoutColors.BrightBlue,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B5E20))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "بدون إنترنت",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (downloadedSeriesList.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BeoutColors.Card),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadForOffline,
                                    contentDescription = null,
                                    tint = BeoutColors.TextDisabled,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = "لا توجد مسلسلات محملة حالياً",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "يمكنك تحميل حلقات أي مسلسل مباشرة من زر 'تحميل كافة الحلقات' في قائمة الحلقات داخل مشغل دراما ريلز لتتمكن من مشاهدتها في أي وقت بدون إنترنت وبأعلى دقة.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BeoutColors.TextSecondary,
                                    fontSize = 12.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // List of Downloaded Series
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(downloadedSeriesList, key = { it.seriesId }) { s ->
                            val isExpanded = expandedSeriesId == s.seriesId
                            val sizeMb = s.totalBytes / (1024f * 1024f)
                            val sSizeText = if (sizeMb > 1024) String.format("%.2f GB", sizeMb / 1024f) else String.format("%.0f MB", sizeMb)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BeoutColors.Card)
                                    .border(1.dp, BeoutColors.Divider, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Poster Cover
                                        AsyncImage(
                                            model = s.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .width(48.dp)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BeoutColors.ElevatedSurface),
                                            contentScale = ContentScale.Crop
                                        )

                                        Column {
                                            Text(
                                                text = s.seriesName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = "تم تحميل ${s.downloadedCount} حلقة من ${s.totalEpisodes} • $sSizeText",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = BeoutColors.TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { seriesToDelete = s }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "حذف المسلسل",
                                                tint = Color.Red.copy(alpha = 0.75f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                expandedSeriesId = if (isExpanded) null else s.seriesId
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = "عرض الحلقات",
                                                tint = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                // Quick Play Series Action Button
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val seriesObj = ShortDramaSeries(
                                                id = s.seriesId,
                                                name = s.seriesName,
                                                coverUrl = s.coverUrl,
                                                episodeCount = s.totalEpisodes,
                                                platform = "NetShort",
                                                categoryId = "all_ar"
                                            )
                                            val firstEp = s.episodes.firstOrNull()?.episodeNumber ?: 1
                                            onPlaySeries(seriesObj, firstEp)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "مشاهدة المسلسل الآن",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                // Expandable Episodes Grid
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        HorizontalDivider(color = BeoutColors.Divider, thickness = 0.8.dp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "الحلقات المحملة (${s.episodes.size} حلقة):",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BeoutColors.TextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 64.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(s.episodes, key = { it.episodeNumber }) { ep ->
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1.2f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(BeoutColors.ElevatedSurface)
                                                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            val seriesObj = ShortDramaSeries(
                                                                id = s.seriesId,
                                                                name = s.seriesName,
                                                                coverUrl = s.coverUrl,
                                                                episodeCount = s.totalEpisodes,
                                                                platform = "NetShort",
                                                                categoryId = "all_ar"
                                                            )
                                                            onPlaySeries(seriesObj, ep.episodeNumber)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "${ep.episodeNumber}",
                                                            style = MaterialTheme.typography.titleSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = BeoutColors.BrightBlue
                                                            )
                                                        )
                                                        val mb = ep.fileSize / (1024f * 1024f)
                                                        Text(
                                                            text = String.format("%.0fMB", mb),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 8.sp,
                                                                color = BeoutColors.TextSecondary
                                                            )
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
                }
            }
        }

        // Delete Confirmation Dialog
        if (seriesToDelete != null) {
            val toDel = seriesToDelete!!
            AlertDialog(
                onDismissRequest = { seriesToDelete = null },
                containerColor = BeoutColors.Card,
                title = {
                    Text(
                        text = "حذف المسلسل المحمل",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف كافة حلقات '${toDel.seriesName}' من ذاكرة الجهاز؟",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BeoutColors.TextSecondary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            ShortDramaDownloadManager.deleteDownloadedSeries(context, toDel.seriesId)
                            seriesToDelete = null
                        }
                    ) {
                        Text("نعم، حذف", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { seriesToDelete = null }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}
