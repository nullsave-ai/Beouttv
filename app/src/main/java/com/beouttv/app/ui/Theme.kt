package com.beouttv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
class BeoutColors(
    val bgTop: Color,
    val bgBottom: Color,
    val glassHi: Color,
    val glassLo: Color,
    val edgeHi: Color,
    val edgeLo: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val accent2: Color,
    val danger: Color,
    val dark: Boolean,
)

val BeoutBlue = Color(0xFF1E88FF)
val BeoutCyan = Color(0xFF4FD1FF)

val DarkColors = BeoutColors(
    bgTop = Color(0xFF0B1220), bgBottom = Color(0xFF05080F),
    glassHi = Color(0x2EFFFFFF), glassLo = Color(0x0FFFFFFF),
    edgeHi = Color(0x66FFFFFF), edgeLo = Color(0x14FFFFFF),
    text = Color(0xFFF2F6FF), textDim = Color(0xFF9FB0C9),
    accent = BeoutBlue, accent2 = BeoutCyan, danger = Color(0xFFFF5470), dark = true,
)

val LightColors = BeoutColors(
    bgTop = Color(0xFFEAF2FF), bgBottom = Color(0xFFD6E4F7),
    glassHi = Color(0xB3FFFFFF), glassLo = Color(0x66FFFFFF),
    edgeHi = Color(0xFFFFFFFF), edgeLo = Color(0x33305A99),
    text = Color(0xFF0C1A2E), textDim = Color(0xFF5A6E8A),
    accent = Color(0xFF0B6BE0), accent2 = Color(0xFF1AA8E8),
    danger = Color(0xFFD93A57), dark = false,
)

val BlueColors = BeoutColors(
    bgTop = Color(0xFF0B3C9E), bgBottom = Color(0xFF041A4D),
    glassHi = Color(0x3DFFFFFF), glassLo = Color(0x14FFFFFF),
    edgeHi = Color(0x80FFFFFF), edgeLo = Color(0x1AFFFFFF),
    text = Color(0xFFFFFFFF), textDim = Color(0xFFB7CBF2),
    accent = Color(0xFF5CB4FF), accent2 = Color(0xFF9BE3FF),
    danger = Color(0xFFFF6B86), dark = true,
)

fun colorsFor(theme: String) = when (theme) {
    "light" -> LightColors
    "blue" -> BlueColors
    else -> DarkColors
}

val LocalColors = staticCompositionLocalOf { DarkColors }

object T {
    val c: BeoutColors @Composable get() = LocalColors.current
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val h2 = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 14.sp)
    val small = TextStyle(fontSize = 12.sp)
}

val GlassShape: Shape = RoundedCornerShape(20)
val CardShape: Shape = RoundedCornerShape(14)

/** سطح زجاجي: تدرج شبه شفاف + حافة مضيئة. بدون Blur لذا رخيص على الأجهزة الضعيفة. */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier {
    val c = T.c
    val hi = if (strong) c.glassHi else c.glassHi.copy(alpha = c.glassHi.alpha * 0.8f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(hi, c.glassLo)))
        .border(1.dp, Brush.verticalGradient(listOf(c.edgeHi, c.edgeLo)), shape)
}

@Composable
fun Modifier.screenBackground(): Modifier {
    val c = T.c
    return background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
}

@Composable
fun Modifier.accentFill(shape: Shape): Modifier {
    val c = T.c
    return clip(shape).background(Brush.linearGradient(listOf(c.accent2, c.accent)))
}
