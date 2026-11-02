package com.beouttv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beouttv.app.BuildConfig
import com.beouttv.app.core.dataOrNull

@Composable
fun Root() {
    val app: AppVM = viewModel()
    val nav: NavVM = viewModel()
    val home: HomeVM = viewModel()
    val channels: ChannelsVM = viewModel()
    val catalog: CatalogVM = viewModel()
    val search: SearchVM = viewModel()
    val detail: DetailVM = viewModel()

    val settings by app.settings.collectAsState()
    val stack by nav.stack.collectAsState()
    val tab by nav.tab.collectAsState()
    val cfg = app.config.state.collectAsState().value.dataOrNull()

    LaunchedEffect(Unit) { app.config.load() }
    val lo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lo) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) { home.softRefresh(); app.config.refresh() }
        }
        lo.addObserver(o)
        onDispose { lo.removeObserver(o) }
    }
    BackHandler(stack.isNotEmpty()) { nav.pop() }

    // الواجهة عربية: نفرض RTL بغض النظر عن لغة الجهاز
    CompositionLocalProvider(
        LocalColors provides colorsFor(settings.theme),
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
    ) {
        Box(Modifier.fillMaxSize().screenBackground().aeroGlow()) {
            when {
                cfg != null && cfg.minVersion > BuildConfig.VERSION_CODE -> MessageBox(S.updateRequired)
                cfg?.maintenance != null -> MessageBox(cfg.maintenance)
                else -> {
                    MainShell(tab, nav, home, channels, catalog)
                    val top = stack.lastOrNull()
                    if (top != null) {
                        Box(
                            Modifier.fillMaxSize().screenBackground()
                                .pointerInput(Unit) { detectTapGestures { } },
                        ) {
                            when (top) {
                                is Route.Detail -> key(top) { DetailScreen(top.id, top.seed, detail, nav) }
                                Route.Search -> SearchScreen(search, nav)
                                Route.Settings -> SettingsScreen(app, nav)
                                is Route.Player -> key(top) { PlayerScreen(top.req, nav, app) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainShell(tab: Int, nav: NavVM, home: HomeVM, channels: ChannelsVM, catalog: CatalogVM) {
    var toolbarVisible by remember { mutableStateOf(true) }
    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) toolbarVisible = false
                else if (available.y > 10f) toolbarVisible = true
                return Offset.Zero
            }
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pad = PaddingValues(top = top + 72.dp, bottom = bottom + 96.dp)

    Box(Modifier.fillMaxSize().nestedScroll(conn)) {
        CompositionLocalProvider(LocalContentPad provides pad) {
            when (tab) {
                0 -> HomeScreen(home, nav) { id -> nav.tab(2); catalog.start(); catalog.select(id) }
                1 -> ChannelsScreen(channels, nav)
                else -> CatalogScreen(catalog, nav)
            }
        }
        FloatingToolbar(
            title = when (tab) { 0 -> S.home; 1 -> S.channels; else -> S.movies },
            visible = toolbarVisible,
            onSearch = { nav.push(Route.Search) },
            onSettings = { nav.push(Route.Settings) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = top + 8.dp, start = 12.dp, end = 12.dp),
        )
        BottomNav(tab, { nav.tab(it) }, Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 12.dp))
    }
}

@Composable
private fun FloatingToolbar(title: String, visible: Boolean, onSearch: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "tb")
    if (a < 0.02f) return
    val shape = RoundedCornerShape(50)
    Row(
        modifier.graphicsLayer {
            alpha = a
            translationY = -(1f - a) * 36.dp.toPx()
            scaleX = 0.92f + 0.08f * a; scaleY = 0.92f + 0.08f * a
        }.glass(shape, true).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GlassIconButton(Ic.Settings, onSettings, size = 40.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(S.app, style = T.h2, color = T.c.accent2, maxLines = 1)
                Txt(title, style = T.small, color = T.c.textDim, maxLines = 1)
            }
        }
        GlassIconButton(Ic.Search, onSearch, size = 40.dp)
    }
}

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    val items = listOf(Ic.Home to S.home, Ic.Channels to S.channels, Ic.Movies to S.movies)
    Row(
        modifier.glass(shape, true).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (ic, label) ->
            val sel = tab == i
            val s = RoundedCornerShape(50)
            Row(
                Modifier.then(if (sel) Modifier.accentFill(s) else Modifier.clip(s)).tvFocus(s)
                    .tap { onTab(i) }.padding(horizontal = if (sel) 16.dp else 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BIcon(ic, size = 24.dp, tint = if (sel) Color.White else T.c.textDim, active = sel)
                if (sel) Txt(label, style = T.small, color = Color.White, maxLines = 1)
            }
        }
    }
}
