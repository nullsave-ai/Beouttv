package com.beouttv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.UiState
import com.beouttv.app.data.Detail
import com.beouttv.app.data.Item
import com.beouttv.app.data.RemoteImage

@Composable
fun DetailScreen(id: String, seed: Item?, vm: DetailVM, nav: NavVM) {
    val loader = remember(id) { vm.loader(id) }
    LaunchedEffect(id) { loader.load() }
    val st by loader.state.collectAsState()
    val favs by vm.favorites.collectAsState()
    val hist by vm.history.collectAsState()

    Box(Modifier.fillMaxSize().screenBackground()) {
        StateHost(st, { loader.refresh() }) { d ->
            Content(d, favs.any { it.id == d.item.id }, hist, vm, nav, seed)
        }
        GlassIconButton(Ic.Back, { nav.pop() }, Modifier.statusBarsPadding().padding(12.dp))
    }
}

@Composable
private fun Content(
    d: Detail,
    isFav: Boolean,
    hist: List<com.beouttv.app.data.HistoryEntry>,
    vm: DetailVM,
    nav: NavVM,
    seed: Item?,
) {
    val it = d.item
    var seasonIdx by remember(it.id) { mutableIntStateOf(0) }
    val season = d.seasons.getOrNull(seasonIdx)
    val lastEntry = hist.firstOrNull { e -> e.item.id == it.id }
    val allEpisodes = remember(d) { d.seasons.flatMap { s -> s.episodes.map { e -> s to e } } }
    val entries = remember(d) {
        allEpisodes.map { (s, e) ->
            PlayEntry(it, e.id, "${S.season} ${s.number} - ${S.episodeWord} ${e.number}" + (if (e.title.isNotBlank()) ": ${e.title}" else ""))
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                RemoteImage(it.backdrop ?: it.poster, Modifier.fillMaxSize(), reqWidthPx = 900) {}
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, T.c.bgBottom))))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt(it.title, style = T.title, maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOfNotNull(
                        it.year?.toString(), it.category, it.quality,
                        it.durationMin?.let { m -> "$m ${S.minutes}" },
                    ).forEach { m -> Txt(m, style = T.small, color = T.c.textDim) }
                }
                it.description?.let { t -> Txt(t, style = T.body, color = T.c.textDim) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val playLabel = if (lastEntry != null && lastEntry.positionMs > 0) S.resume else S.play
                    PrimaryButton(playLabel, {
                        if (it.isSeries) {
                            val idx = lastEntry?.episodeId?.let { eid -> entries.indexOfFirst { e -> e.episodeId == eid } }?.takeIf { x -> x >= 0 } ?: 0
                            if (entries.isNotEmpty()) nav.push(Route.Player(PlayReq(entries, idx)))
                        } else nav.push(Route.Player(PlayReq(listOf(PlayEntry(it)))))
                    })
                    GlassIconButton(Ic.Heart, { vm.toggleFav(it) }, active = isFav, tint = if (isFav) T.c.danger else T.c.text)
                }
            }
        }
        if (it.isSeries && d.seasons.isNotEmpty()) {
            item {
                LazyRow(Modifier.padding(top = 14.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(d.seasons.size) { i -> Chip("${S.season} ${d.seasons[i].number}", i == seasonIdx) { seasonIdx = i } }
                }
            }
            season?.let { s ->
                items(s.episodes, key = { e -> e.id }) { e ->
                    val progress = hist.firstOrNull { h -> h.episodeId == e.id }
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().glass(CardShape).tvFocus(CardShape)
                            .tap {
                                val idx = entries.indexOfFirst { x -> x.episodeId == e.id }.coerceAtLeast(0)
                                nav.push(Route.Player(PlayReq(entries, idx)))
                            }.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(96.dp, 56.dp).glass(RoundedCornerShape(10.dp))) {
                            RemoteImage(e.poster ?: it.backdrop, Modifier.fillMaxSize(), reqWidthPx = 240) {}
                        }
                        Column(Modifier.weight(1f)) {
                            Txt("${S.episodeWord} ${e.number}" + if (e.title.isNotBlank()) " - ${e.title}" else "", style = T.body, maxLines = 1)
                            e.durationMin?.let { m -> Txt("$m ${S.minutes}", style = T.small, color = T.c.textDim) }
                            if (progress != null && progress.durationMs > 0)
                                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).background(Color(0x33FFFFFF), RoundedCornerShape(50))) {
                                    Box(Modifier.fillMaxWidth((progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)).height(3.dp).background(T.c.accent2, RoundedCornerShape(50)))
                                }
                        }
                        BIcon(Ic.Play, size = 22.dp, tint = T.c.accent2)
                    }
                }
            }
        }
        if (d.related.isNotEmpty()) {
            item { SectionTitle(S.related) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(d.related, key = { r -> r.id }) { r -> PosterCard(r, { nav.replaceTop(Route.Detail(r.id, r)) }) }
                }
            }
        }
    }
}
