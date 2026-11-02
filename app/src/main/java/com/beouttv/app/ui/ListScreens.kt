package com.beouttv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.beouttv.app.core.PagedLoader
import com.beouttv.app.core.UiState
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.Category
import com.beouttv.app.data.Item

/** محفّز التحميل التدريجي عند الاقتراب من نهاية الشبكة. */
@Composable
private fun LoadMoreTrigger(state: LazyGridState, paged: PagedLoader) {
    val near by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(near) { if (near) paged.loadMore() }
}

@Composable
private fun CategoryChips(cats: List<Category>, selected: String?, onSelect: (String?) -> Unit, extra: @Composable () -> Unit = {}) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { extra() }
        item { Chip(S.all, selected == null) { onSelect(null) } }
        items(cats, key = { it.id }) { Chip(it.name, selected == it.id) { onSelect(it.id) } }
    }
}

@Composable
fun ChannelsScreen(vm: ChannelsVM, nav: NavVM) {
    LaunchedEffect(Unit) { vm.start() }
    val cats by vm.cats.state.collectAsState()
    val ps by vm.paged.state.collectAsState()
    val sel by vm.category.collectAsState()
    val favOnly by vm.favOnly.collectAsState()
    val q by vm.query.collectAsState()
    val favs by vm.favorites.collectAsState()
    val grid = rememberLazyGridState()
    val pad = LocalContentPad.current

    PullRefresh(ps.ui is UiState.Refreshing, { vm.refresh() }, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(top = pad.calculateTopPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(q, { vm.query.value = it }, S.channels)
                CategoryChips(cats.dataOrNull() ?: emptyList(), sel, { vm.select(it) }) {
                    Chip(S.favOnly, favOnly) { vm.favOnly.value = !favOnly }
                }
            }
            val ui = if (favOnly) {
                val l = favs.filter { it.isChannel && (sel == null || it.category == sel) }
                if (l.isEmpty()) UiState.Empty else UiState.Success(l)
            } else ps.ui
            StateHost(ui, { vm.paged.reset() }) { all ->
                val list = if (q.isBlank()) all else all.filter { it.title.contains(q.trim(), true) }
                Grid(list, grid, vm.paged, PaddingValues(16.dp, 10.dp, 16.dp, pad.calculateBottomPadding()), ps.loadingMore, 110.dp) { it ->
                    ChannelTile(it) { openItem(nav, it, list) }
                }
            }
        }
    }
}

@Composable
fun CatalogScreen(vm: CatalogVM, nav: NavVM) {
    LaunchedEffect(Unit) { vm.start() }
    val kind by vm.kind.collectAsState()
    val sel by vm.category.collectAsState()
    val catLoader by vm.cats.collectAsState()
    val cats = catLoader?.state?.collectAsState()?.value
    val ps by vm.paged.state.collectAsState()
    val grid = rememberLazyGridState()
    val pad = LocalContentPad.current

    PullRefresh(ps.ui is UiState.Refreshing, { vm.refresh() }, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(top = pad.calculateTopPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Chip(S.movie, kind == "movie") { vm.setKind("movie") } }
                    item { Chip(S.series, kind == "series") { vm.setKind("series") } }
                }
                CategoryChips(cats?.dataOrNull() ?: emptyList(), sel, { vm.select(it) })
            }
            StateHost(ps.ui, { vm.paged.reset() }) { list ->
                Grid(list, grid, vm.paged, PaddingValues(16.dp, 10.dp, 16.dp, pad.calculateBottomPadding()), ps.loadingMore, posterWidth()) { it ->
                    PosterCard(it, { openItem(nav, it, list) }, posterWidth())
                }
            }
        }
    }
}

@Composable
fun Grid(
    list: List<Item>,
    state: LazyGridState,
    paged: PagedLoader,
    pad: PaddingValues,
    loadingMore: Boolean,
    minCell: androidx.compose.ui.unit.Dp,
    cell: @Composable (Item) -> Unit,
) {
    LoadMoreTrigger(state, paged)
    LazyVerticalGrid(
        GridCells.Adaptive(minCell), Modifier.fillMaxSize(), state, pad,
        verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(list, key = { it.id }) { cell(it) }
        if (loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) { Txt("...", color = T.c.textDim) }
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, onSearch: () -> Unit = {}, modifier: Modifier = Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(shape).padding(horizontal = 16.dp, vertical = 11.dp)) {
        BasicTextField(
            value, onChange, Modifier.fillMaxWidth(), singleLine = true,
            textStyle = T.body.copy(color = T.c.text),
            cursorBrush = SolidColor(T.c.accent2),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        if (value.isEmpty()) Txt(hint, style = T.body, color = T.c.textDim, maxLines = 1)
    }
}
