package com.beouttv.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles

/** Base shimmering block — the building unit for all skeleton loaders. */
@Composable
fun BeoutSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.ElevatedSurface)
            .alpha(alpha)
    )
}

@Composable
fun BeoutChannelSkeletonRow(count: Int = 6) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
    ) {
        items(count) {
            BeoutSkeleton(modifier = Modifier.size(BeoutDimensions.ChannelCardWidth, BeoutDimensions.ChannelCardHeight))
        }
    }
}

@Composable
fun BeoutPosterSkeletonRow(count: Int = 5) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
    ) {
        items(count) {
            BeoutSkeleton(
                modifier = Modifier
                    .width(BeoutDimensions.PosterCardWidth)
                    .aspectRatio(2f / 3f)
            )
        }
    }
}

@Composable
fun BeoutHeroSkeleton(modifier: Modifier = Modifier) {
    BeoutSkeleton(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BeoutDimensions.HeroAspectRatio)
    )
}

@Composable
fun BeoutEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.FavoriteBorder,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BeoutDimensions.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = BeoutColors.TextDisabled, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(BeoutDimensions.SpaceM))
        Text(title, style = BeoutTextStyles.SectionTitle, color = BeoutColors.TextPrimary)
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            description,
            style = BeoutTextStyles.SecondaryInfo,
            color = BeoutColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.height(BeoutDimensions.SpaceL))
            BeoutPrimaryButton(text = actionLabel, onClick = onActionClick)
        }
    }
}

@Composable
fun BeoutErrorState(
    title: String = "Unable to load content",
    description: String = "Something went wrong while loading this page.",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BeoutDimensions.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = BeoutColors.TextDisabled, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(BeoutDimensions.SpaceM))
        Text(title, style = BeoutTextStyles.SectionTitle, color = BeoutColors.TextPrimary)
        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))
        Text(
            description,
            style = BeoutTextStyles.SecondaryInfo,
            color = BeoutColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(BeoutDimensions.SpaceL))
        Row(horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)) {
            onBack?.let { BeoutSecondaryButton(text = "Back", onClick = it) }
            onRetry?.let { BeoutPrimaryButton(text = "Retry", onClick = it) }
        }
    }
}
