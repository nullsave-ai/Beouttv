package com.beouttv.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized BEOUT TV color tokens.
 * Every screen must reference these — never hardcode raw colors in composables.
 */
object BeoutColors {
    val Background = Color(0xFF08090B)
    val Surface = Color(0xFF111318)
    val SurfaceDark = Color(0xFF111318)
    val Card = Color(0xFF171A20)
    val ElevatedSurface = Color(0xFF1D2026)

    val PrimaryBlue = Color(0xFF1976D2)
    val BrightBlue = Color(0xFF2196F3)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA1A5AD)
    val TextDisabled = Color(0xFF62666E)

    val Divider = Color(0xFF23262D)
    val Overlay = Color(0xCC08090B)

    val Success = Color(0xFF2E7D32)
    val Error = Color(0xFFD32F2F)
    val LiveRed = Color(0xFFE53935)
    val Gold = Color(0xFFFFD54F)

    // Gradient stops for hero / poster overlays — dark cinematic fade only.
    val GradientTransparent = Color(0x0008090B)
    val GradientSolid = Color(0xFF08090B)
}
