package com.beouttv.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * Intelligent iOS-Native Navigation Engine for BeoutTV.
 * Features:
 * 1. iOS Push & Pop Parallax Flow: Detail screens slide 100% with a 30% under-layer parallax shift.
 * 2. Apple Cupertino Ease: CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.0f).
 * 3. RTL Auto-adaptive: SlideDirection.Start and SlideDirection.End dynamically resolve
 *    correctly for Arabic Right-To-Left layout.
 */
object BeoutNavTransitions {

    // Apple Cupertino Official Ease Curve
    val IOSEasing = CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.0f)

    private const val DURATION_IOS_SCREEN_MS = 340

    /**
     * Enter transition for Detail Screens (iOS Push):
     * 100% horizontal slide in from trailing edge with Cupertino ease.
     */
    fun enter(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): EnterTransition {
        return scope.slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
                durationMillis = DURATION_IOS_SCREEN_MS,
                easing = IOSEasing
            )
        )
    }

    /**
     * Exit transition for Under-Layer (iOS Push Under-layer):
     * Under-layer slides back by 30% parallax with Cupertino ease.
     */
    fun exit(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): ExitTransition {
        return scope.slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            targetOffset = { fullOffset -> fullOffset / 3 },
            animationSpec = tween(
                durationMillis = DURATION_IOS_SCREEN_MS,
                easing = IOSEasing
            )
        )
    }

    /**
     * Pop Enter transition for Under-Layer (iOS Pop Under-layer):
     * Under-layer slides back into focus from 30% offset.
     */
    fun popEnter(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): EnterTransition {
        return scope.slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            initialOffset = { fullOffset -> -fullOffset / 3 },
            animationSpec = tween(
                durationMillis = DURATION_IOS_SCREEN_MS,
                easing = IOSEasing
            )
        )
    }

    /**
     * Pop Exit transition for Dismissed Screen (iOS Pop):
     * Dismissed screen slides 100% off-screen to trailing edge.
     */
    fun popExit(
        scope: AnimatedContentTransitionScope<NavBackStackEntry>
    ): ExitTransition {
        return scope.slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(
                durationMillis = DURATION_IOS_SCREEN_MS,
                easing = IOSEasing
            )
        )
    }
}
