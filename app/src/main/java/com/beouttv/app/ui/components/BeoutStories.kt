@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.beouttv.app.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.beouttv.app.data.api.moviebox.MovieBoxStoryItem
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.delay

/**
 * Modern iOS / Instagram style Stories Shelf for Movies & Series Trailers
 */
@Composable
fun BeoutStoriesRow(
    stories: List<MovieBoxStoryItem>,
    onStoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated live/trailer ring icon
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.BrightBlue)
                )

                Text(
                    text = tr("تريلرات وقصص", "Trailers & Stories", "Hikayeler ve Fragmanlar", "داستان‌ها و تریلرها", "Stories & Bandes-annonces"),
                    style = BeoutTextStyles.SectionTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BeoutColors.TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tr("جديد", "NEW", "YENİ", "جدید", "NOUVEAU"),
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BeoutColors.BrightBlue
                    )
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        ) {
            itemsIndexed(stories, key = { _, item -> item.id }) { index, story ->
                StoryCircleItem(
                    story = story,
                    onClick = { onStoryClick(index) }
                )
            }
        }
    }
}

@Composable
private fun StoryCircleItem(
    story: MovieBoxStoryItem,
    onClick: () -> Unit
) {
    val ringGradient = Brush.sweepGradient(
        listOf(
            BeoutColors.BrightBlue,
            Color(0xFF00E5FF),
            Color(0xFF7C3AED),
            Color(0xFFFF2D55),
            BeoutColors.BrightBlue
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient Ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(ringGradient)
            )

            // Inner Separation Ring
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.Background)
            )

            // Cover Image
            AsyncImage(
                model = story.coverUrl,
                contentDescription = story.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(59.dp)
                    .clip(CircleShape)
            )

            // Micro Play Badge at bottom-end
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.BrightBlue)
                    .border(1.5.dp, BeoutColors.Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = story.title,
            style = BeoutTextStyles.Caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = BeoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fullscreen iOS Story Viewer with Segmented Progress and Fast Video Playback
 */
@Composable
fun BeoutStoryViewerModal(
    stories: List<MovieBoxStoryItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onWatchDetails: (MovieBoxStoryItem) -> Unit
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    var isMuted by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val currentStory = stories[currentIndex]

    // Create fast ExoPlayer instance
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 2000, 200, 400)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
    }

    // Set media item cleanly when current story changes
    LaunchedEffect(currentIndex) {
        currentProgress = 0f
        if (currentStory.videoUrl.isNotBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(currentStory.videoUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Handle mute
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Handle pause
    LaunchedEffect(isPaused) {
        if (isPaused) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    // Progress update loop
    LaunchedEffect(currentIndex, isPaused) {
        while (true) {
            if (!isPaused && exoPlayer.isPlaying) {
                val dur = exoPlayer.duration.coerceAtLeast(1L)
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (dur > 0) {
                    val p = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    currentProgress = p
                    if (p >= 0.99f || exoPlayer.playbackState == Player.STATE_ENDED) {
                        // Auto-advance to next story
                        if (currentIndex < stories.size - 1) {
                            currentIndex++
                        } else {
                            onDismiss()
                            break
                        }
                    }
                }
            }
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Video Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Touch gesture overlay for navigation and pausing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.35f) {
                                    // Previous
                                    if (currentIndex > 0) {
                                        currentIndex--
                                    } else {
                                        onDismiss()
                                    }
                                } else {
                                    // Next
                                    if (currentIndex < stories.size - 1) {
                                        currentIndex++
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
            )

            // Top Gradient & Header (Segments, Title, Mute, Close)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Segmented Progress Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { idx, _ ->
                            val segProgress = when {
                                idx < currentIndex -> 1f
                                idx == currentIndex -> currentProgress
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(segProgress)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = currentStory.coverUrl,
                                contentDescription = currentStory.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, BeoutColors.BrightBlue, CircleShape)
                            )

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = currentStory.title,
                                    style = BeoutTextStyles.Body.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentStory.genre.ifBlank { "تريلر رسمي" }} • ★ ${currentStory.score}",
                                    style = BeoutTextStyles.Caption.copy(
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            // Mute Toggle Button
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "كتم الصوت",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar ("مشاهدة العمل" / Details)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onWatchDetails(currentStory)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeoutColors.BrightBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tr("مشاهدة العمل بالكامل", "Watch Full Title", "Filmi / Diziyi İzle", "مشاهده کامل اثر", "Voir le titre complet"),
                            style = BeoutTextStyles.Body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
