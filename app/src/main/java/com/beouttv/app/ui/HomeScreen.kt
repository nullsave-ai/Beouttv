package com.beouttv.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.UiState
import com.beouttv.app.data.HistoryEntry
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage
import com.beouttv.app.data.Section

val LocalContentPad = compositionLocalOf { PaddingValues(0.dp) }

fun openItem(nav: NavVM, item: Item, siblings: List<Item> = emptyList()) {
    when {
        item.isChannel -> {
            val list = siblings.filter { it.isChannel }.ifEmpty { listOf(item) }
            nav.push(Route.Player(PlayReq(list.map { PlayEntry(it) }, list.indexOfFirst { it.id == item.id }.coerceAtLeast(0))))
        }
        item.kind == "episode" && item.parentId != null -> nav.push(Route.Detail(item.parentId, item))
        else -> nav.push(Route.Detail(item.id, item))
    }
}

@Composable
fun HomeScreen(vm: HomeVM, nav: NavVM, onPickCategory: (String) -> Unit) {
    val st by vm.home.state.collectAsState()
    val hist by vm.history.collectAsState()
    PullRefresh(st is UiState.Refreshing, { vm.home.refresh() }, Modifier.fillMaxSize()) {
        StateHost(st, { vm.home.refresh() }) { data ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = LocalContentPad.current,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(data.sections, key = { it.id }) { s -> SectionView(s, hist, nav, onPickCategory) }
            }
        }
    }
}

@Composable
private fun SectionView(s: Section, hist: List<HistoryEntry>, nav: NavVM, onPickCategory: (String) -> Unit) {
    // عدة عناصر داخل item واحد تتراكب فوق بعضها في LazyColumn، لذلك نلفّها بـ Column
    Column { SectionBody(s, hist, nav, onPickCategory) }
}

@Composable
private fun SectionBody(s: Section, hist: List<HistoryEntry>, nav: NavVM, onPickCategory: (String) -> Unit) {
    when (s.type) {
        "hero" -> if (s.items.isNotEmpty()) Hero(s.items, nav)
        "continue" -> {
            val list = hist.filter { it.durationMs > 0 && it.positionMs in 1 until (it.durationMs * 95 / 100) && it.item.kind != "channel" }
            if (list.isNotEmpty()) {
                SectionTitle(s.title.ifBlank { S.resume })
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.key }) { e ->
                        Box {
                            PosterCard(e.item, {
                                if (e.item.kind == "movie")
                                    nav.push(Route.Player(PlayReq(listOf(PlayEntry(e.item)))))
                                else nav.push(Route.Detail(e.item.id, e.item))
                            })
                            Box(
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp, start = 6.dp, end = 6.dp)
                                    .fillMaxWidth().height(3.dp).background(Color(0x55FFFFFF), RoundedCornerShape(50)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth((e.positionMs.toFloat() / e.durationMs).coerceIn(0f, 1f)).height(3.dp)
                                        .background(T.c.accent2, RoundedCornerShape(50)),
                                )
                            }
                        }
                    }
                }
            }
        }
        "channels" -> if (s.items.isNotEmpty()) {
            SectionTitle(s.title)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.items, key = { it.id }) { ChannelTile(it) { openItem(nav, it, s.items) } }
            }
        }
        "categories" -> if (s.items.isNotEmpty()) {
            SectionTitle(s.title.ifBlank { S.genres })
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.items, key = { it.id }) { Chip(it.title, false) { onPickCategory(it.id) } }
            }
        }
        else -> if (s.items.isNotEmpty()) {
            SectionTitle(s.title)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.items, key = { it.id }) { PosterCard(it, { openItem(nav, it, s.items) }) }
            }
        }
    }
}

@Composable
fun ChannelTile(item: Item, onClick: () -> Unit) {
    val w = posterWidth() * 0.85f
    Column(Modifier.size(w, w + 28.dp)) {
        Box(Modifier.size(w).glass(CardShape).tvFocus(CardShape).tap(onClick), contentAlignment = Alignment.Center) {
            RemoteImage(item.logo ?: item.poster, Modifier.fillMaxSize().padding(14.dp), reqWidthPx = 240, scale = androidx.compose.ui.layout.ContentScale.Fit) {}
        }
        Txt(item.title, Modifier.padding(top = 4.dp), T.small, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Hero(items: List<Item>, nav: NavVM) {
    val pager = rememberPagerState { items.size }
    HorizontalPager(pager, Modifier.fillMaxWidth().padding(horizontal = 16.dp), pageSpacing = 10.dp) { i ->
        val it = items[i]
        Box(Modifier.fillMaxWidth().height(210.dp).glass(GlassShape, true).tvFocus(GlassShape).tap { openItem(nav, it, items) }) {
            RemoteImage(it.backdrop ?: it.poster, Modifier.fillMaxSize(), reqWidthPx = 900) {}
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Txt(it.title, style = T.title, color = Color.White, maxLines = 2)
                it.description?.let { d -> Txt(d, Modifier.padding(top = 4.dp), T.small, Color(0xFFDDE6F5), maxLines = 2) }
            }
        }
    }
}
