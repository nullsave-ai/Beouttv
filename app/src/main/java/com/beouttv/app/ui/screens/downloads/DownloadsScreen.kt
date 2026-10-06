package com.beouttv.app.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.compose.AsyncImage
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.repository.DownloadStatus
import com.beouttv.app.data.repository.DownloadTask
import com.beouttv.app.data.repository.MovieBoxDownloadManager
import com.beouttv.app.ui.components.BeoutIconButton
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayOffline: (localPath: String, title: String, subtitlePath: String?) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        MovieBoxDownloadManager.init(context)
    }

    val downloads by MovieBoxDownloadManager.downloads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BeoutDimensions.TopBarHeight)
                .padding(horizontal = BeoutDimensions.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeoutIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = S.back(),
                onClick = onBack
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = tr("التحميلات والمشاهدة بدون إنترنت", "Downloads & Offline Playback", "İndirilenler", "دانلودها", "Téléchargements"),
                style = BeoutTextStyles.SectionTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = BeoutColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (downloads.isNotEmpty()) {
                Text(
                    text = "${downloads.size}",
                    style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BeoutColors.Divider)

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = tr("لا توجد ملفات محملة حالياً", "No downloaded files yet", "Henüz indirilen dosya yok", "هنوز فایلی دانلود نشده است", "Aucun téléchargement"),
                        style = BeoutTextStyles.SectionTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = BeoutColors.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tr("يمكنك تحميل أي فيلم أو حلقة مسلسل من MovieBox لمشاهدتها في أي وقت بدون إنترنت.", "Download any movie or episode to watch offline anytime.", "İstediğiniz filmi veya diziyi indirip çevrimdışı izleyebilirsiniz.", "فیلم یا سریال مورد نظر خود را برای تماشای آفلاین دانلود کنید.", "Téléchargez des films et séries pour les regarder hors-ligne."),
                        style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, color = BeoutColors.TextSecondary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(downloads, key = { it.id }) { task ->
                    DownloadItemCard(
                        task = task,
                        onPlay = {
                            if (task.status == DownloadStatus.COMPLETED) {
                                onPlayOffline(task.localVideoPath, task.title, task.localSubtitlePath)
                            }
                        },
                        onPause = { MovieBoxDownloadManager.pauseDownload(task.id) },
                        onResume = { MovieBoxDownloadManager.resumeDownload(context, task.id) },
                        onDelete = { MovieBoxDownloadManager.deleteDownload(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    task: DownloadTask,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == DownloadStatus.COMPLETED
    val isDownloading = task.status == DownloadStatus.DOWNLOADING

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, if (isDownloading) BeoutColors.BrightBlue.copy(alpha = 0.5f) else BeoutColors.Divider, BeoutShapes.Medium)
            .clickable(enabled = isCompleted, onClick = onPlay)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BeoutColors.ElevatedSurface)
            ) {
                AsyncImage(
                    model = task.posterUrl,
                    contentDescription = task.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.PrimaryBlue.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = BeoutTextStyles.Body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = BeoutColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BeoutColors.ElevatedSurface)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = task.quality,
                            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold)
                        )
                    }

                    if (task.subtitleUrl != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BeoutColors.ElevatedSurface)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SRT",
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (task.totalBytes > 0L) {
                        val sizeMb = task.totalBytes / (1024 * 1024)
                        Text(
                            text = "$sizeMb MB",
                            style = BeoutTextStyles.Caption.copy(fontSize = 11.sp, color = BeoutColors.TextSecondary)
                        )
                    }
                }

                if (!isCompleted) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = BeoutColors.BrightBlue,
                        trackColor = BeoutColors.Divider
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (task.status == DownloadStatus.PAUSED) tr("متوقف مؤقتاً", "Paused", "Duraklatıldı", "متوقف", "En pause")
                            else if (task.status == DownloadStatus.FAILED) tr("فشل التحميل", "Failed", "Hata", "ناموفق", "Échoué")
                            else "${(task.progress * 100).toInt()}%",
                            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, color = if (task.status == DownloadStatus.FAILED) BeoutColors.LiveRed else BeoutColors.TextSecondary)
                        )
                        if (task.speedText.isNotBlank()) {
                            Text(
                                text = task.speedText,
                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, color = BeoutColors.BrightBlue)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloading) {
                    IconButton(onClick = onPause) {
                        Icon(imageVector = Icons.Rounded.Pause, contentDescription = "إيقاف مؤقت", tint = BeoutColors.BrightBlue)
                    }
                } else if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
                    IconButton(onClick = onResume) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "استئناف", tint = BeoutColors.BrightBlue)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = BeoutColors.LiveRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}
