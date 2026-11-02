package com.beouttv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.RemoteImage

@Composable
fun SearchScreen(vm: SearchVM, nav: NavVM) {
    val q by vm.query.collectAsState()
    val type by vm.type.collectAsState()
    val ps by vm.paged.state.collectAsState()
    val sug by vm.suggestions.collectAsState()
    val recent by vm.recent.collectAsState()
    val grid = rememberLazyGridState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(top = 12.dp, start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Ic.Back, { nav.pop() })
            SearchField(q, { vm.query.value = it }, S.searchHint, { vm.commit() }, Modifier.weight(1f).focusRequester(focus))
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip(S.all, type == null) { vm.type.value = null } }
            item { Chip(S.movie, type == "movie") { vm.type.value = "movie" } }
            item { Chip(S.series, type == "series") { vm.type.value = "series" } }
            item { Chip(S.episode, type == "episode") { vm.type.value = "episode" } }
            item { Chip(S.channel, type == "channel") { vm.type.value = "channel" } }
        }
        if (q.trim().length < 2) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recent.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Txt(S.recent, style = T.h2)
                            Txt(S.clear, Modifier.tap { vm.clearRecent() }.padding(8.dp), T.small, T.c.accent2)
                        }
                    }
                    items(recent, key = { it }) { r ->
                        Row(Modifier.fillMaxWidth().glass(CardShape).tvFocus(CardShape).tap { vm.query.value = r }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BIcon(Ic.Search, size = 18.dp, tint = T.c.textDim)
                            Txt(r, style = T.body, maxLines = 1)
                        }
                    }
                }
            }
        } else {
            if (sug.isNotEmpty() && ps.ui !is UiState.Success) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sug, key = { it }) { s -> Chip(s, false) { vm.query.value = s; vm.commit() } }
                }
            }
            StateHost(ps.ui, { vm.paged.reset() }, S.noResults) { list ->
                Grid(list, grid, vm.paged, PaddingValues(16.dp), ps.loadingMore, posterWidth()) { item ->
                    PosterCard(item, { vm.commit(); openItem(nav, item, list) }, posterWidth())
                }
            }
        }
    }
}
