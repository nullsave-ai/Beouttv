package com.beouttv.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * BEOUT TV typography scale.
 * FontFamily.Default resolves to the system font, which correctly
 * renders Arabic out of the box. Swap in a custom Arabic-friendly
 * font (e.g. IBM Plex Sans Arabic / Cairo) by replacing FontFamily.Default
 * across this file only.
 */
private val BeoutFontFamily = FontFamily.Default

object BeoutTextStyles {
    val ScreenTitle = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    )

    val SectionTitle = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    val CardTitle = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val SecondaryInfo = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val Body = SecondaryInfo
    val BodyRegular = SecondaryInfo
    val BodyBold = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )

    val Button = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val ButtonLabel = Button

    val Caption = TextStyle(
        fontFamily = BeoutFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
}

val BeoutTypography = Typography(
    headlineLarge = BeoutTextStyles.ScreenTitle,
    titleLarge = BeoutTextStyles.SectionTitle,
    titleMedium = BeoutTextStyles.CardTitle,
    bodyMedium = BeoutTextStyles.SecondaryInfo,
    labelLarge = BeoutTextStyles.Button,
    labelSmall = BeoutTextStyles.Caption
)
