package com.beouttv.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Consistent corner radius scale used across the entire app. */
object BeoutRadius {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val Hero = 20.dp
}

object BeoutShapes {
    val Small = RoundedCornerShape(BeoutRadius.Small)
    val Medium = RoundedCornerShape(BeoutRadius.Medium)
    val Large = RoundedCornerShape(BeoutRadius.Large)
    val Hero = RoundedCornerShape(BeoutRadius.Hero)

    val Material3 = Shapes(
        extraSmall = Small,
        small = Small,
        medium = Medium,
        large = Large,
        extraLarge = Hero
    )
}

/** Centralized spacing / sizing tokens — avoids magic numbers in composables. */
object BeoutDimensions {
    val SpaceXXS = 4.dp
    val SpaceXS = 8.dp
    val SpaceS = 12.dp
    val SpaceM = 16.dp
    val SpaceL = 20.dp
    val SpaceXL = 24.dp
    val SpaceXXL = 32.dp

    val ScreenPadding = 16.dp

    val IconSize = 24.dp
    val IconSizeSmall = 18.dp
    val IconSizeLarge = 32.dp

    val ChannelCardWidth = 105.dp
    val ChannelCardHeight = 78.dp
    val TvChannelCardWidth = 150.dp
    val TvChannelCardHeight = 82.dp

    val PosterCardWidth = 98.dp
    val PosterCardHeightRatio = 1.5f // 2:3
    val TvPosterCardWidth = 112.dp
    val TvPosterCardHeightRatio = 1.5f

    val ContinueWatchingCardWidth = 135.dp
    val TvContinueWatchingCardWidth = 165.dp

    val HeroAspectRatio = 16f / 9f

    val BottomBarHeight = 64.dp
    val TopBarHeight = 56.dp

    val CardElevation = 0.dp
}
