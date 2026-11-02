package com.beouttv.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.UiState
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import kotlin.math.roundToInt

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = T.body,
    color: Color = T.c.text,
    maxLines: Int = Int.MAX_VALUE,
) = BasicText(text, modifier, style.copy(color = color), maxLines = maxLines, overflow = TextOverflow.Ellipsis)

/** إطار تركيز واضح للتحكم بالريموت (Android TV). */
@Composable
fun Modifier.tvFocus(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp)): Modifier {
    var f by remember { mutableStateOf(false) }
    val a = T.c.accent2
    return this.onFocusChanged { f = it.isFocused }.then(if (f) Modifier.border(2.dp, a, shape) else Modifier)
}

@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, null, onClick = onClick)

@Composable
fun GlassIconButton(ic: Ic, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, active: Boolean = false, tint: Color = T.c.text) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.size(size).glass(shape).tvFocus(shape).tap(onClick),
        contentAlignment = Alignment.Center,
    ) { BIcon(ic, size = size * 0.5f, tint = if (active) T.c.accent2 else tint, active = active) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val m = if (selected) Modifier.accentFill(shape) else Modifier.glass(shape)
    Box(Modifier.then(m).tvFocus(shape).tap(onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Txt(text, style = T.small, color = if (selected) Color.White else T.c.text, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.accentFill(shape).tvFocus(shape).tap(onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, style = T.h2, color = Color.White, maxLines = 1) }
}

/** عرض البطاقات حسب حجم الشاشة. */
@Composable
fun posterWidth(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w < 600 -> 118.dp
        w < 840 -> 140.dp
        else -> 160.dp
    }
}

@Composable
fun PosterCard(item: Item, onClick: () -> Unit, width: Dp = posterWidth()) {
    Column(Modifier.size(width, width * 1.78f)) {
        Box(Modifier.fillMaxWidth().height(width * 1.4f).glass(CardShape).tvFocus(CardShape).tap(onClick)) {
            RemoteImage(item.poster ?: item.logo, Modifier.fillMaxSize(), reqWidthPx = 360) {}
            if (item.isChannel && !item.online)
                Box(Modifier.align(Alignment.TopStart).padding(6.dp).glass(RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Txt(S.offlineChannel, style = T.small, color = T.c.danger)
                }
            item.quality?.let {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).glass(RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Txt(it, style = T.small)
                }
            }
        }
        Txt(item.title, Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp), T.small, maxLines = 2)
    }
}

@Composable
fun SectionTitle(text: String) =
    Txt(text, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), T.h2)

@Composable
private fun Skeleton() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(if (it == 0) 180.dp else 140.dp).glass(CardShape))
        }
    }
}

@Composable
fun MessageBox(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Txt(text, style = T.h2, color = T.c.textDim)
        if (action != null) PrimaryButton(action, onAction, Modifier.padding(top = 16.dp))
    }
}

/** تعرض كل حالات الواجهة الموحدة. */
@Composable
fun <T> StateHost(
    state: UiState<T>,
    onRetry: () -> Unit,
    emptyText: String = S.empty,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Initial, UiState.Loading -> Skeleton()
        is UiState.Success -> content(state.data)
        is UiState.Refreshing -> content(state.data)
        UiState.Empty -> MessageBox(emptyText)
        is UiState.Error -> MessageBox(S.loadErr, S.retry, onRetry)
        UiState.Offline -> MessageBox(S.offline, S.retry, onRetry)
    }
}

@Composable
fun PullRefresh(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var pull by remember { mutableFloatStateOf(0f) }
    val thr = with(LocalDensity.current) { 72.dp.toPx() }
    val curRefresh by rememberUpdatedState(onRefresh)
    val isRef by rememberUpdatedState(refreshing)
    val conn = remember(thr) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pull > 0f && available.y < 0f) {
                    val d = maxOf(available.y, -pull); pull += d; return Offset(0f, d)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && !isRef) {
                    pull = (pull + available.y * 0.5f).coerceAtMost(thr * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull >= thr && !isRef) curRefresh()
                pull = 0f
                return Velocity.Zero
            }
        }
    }
    Box(modifier.nestedScroll(conn)) {
        content()
        val show = refreshing || pull > 0f
        if (show) {
            val c = T.c
            val spin = if (refreshing) {
                val t = rememberInfiniteTransition(label = "spin")
                t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "r").value
            } else 0f
            Box(
                Modifier.align(Alignment.TopCenter)
                    .offset { IntOffset(0, (if (refreshing) 24.dp.toPx() else pull - 20.dp.toPx()).roundToInt()) }
                    .size(36.dp).glass(RoundedCornerShape(50), true),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val sweep = if (refreshing) 270f else (pull / thr).coerceIn(0f, 1f) * 300f
                    drawArc(c.accent2, spin - 90f, sweep, false, Offset.Zero, Size(size.width, size.height), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}

/** خلفية محيطية خفيفة (لمسة انكسار ضوئي) ترسم مرة واحدة بدون حركة مستمرة. */
@Composable
fun Modifier.aeroGlow(): Modifier {
    val c = T.c
    return drawBehind {
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(c.accent.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.05f), radius = size.width * 0.9f,
            ),
            radius = size.width * 0.9f, center = Offset(size.width * 0.85f, size.height * 0.05f),
        )
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(c.accent2.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.95f), radius = size.width * 0.8f,
            ),
            radius = size.width * 0.8f, center = Offset(size.width * 0.05f, size.height * 0.95f),
        )
    }
}
