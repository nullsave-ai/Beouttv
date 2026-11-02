package com.beouttv.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.core.Loader
import com.beouttv.app.core.PagedLoader
import com.beouttv.app.data.AppConfig
import com.beouttv.app.data.Category
import com.beouttv.app.data.Detail
import com.beouttv.app.data.HomeResponse
import com.beouttv.app.data.Img
import com.beouttv.app.data.Prefs
import com.beouttv.app.data.Repo
import com.beouttv.app.data.Api
import com.beouttv.app.data.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppVM : ViewModel() {
    val settings: StateFlow<Settings> = Prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val config = Loader<AppConfig>(viewModelScope) { Repo.config() }

    fun setTheme(v: String) = viewModelScope.launch { Prefs.setTheme(v) }
    fun setMaxHeight(v: Int) = viewModelScope.launch { Prefs.setMaxHeight(v) }
    fun setResume(v: Boolean) = viewModelScope.launch { Prefs.setResume(v) }
    fun setKeepScreen(v: Boolean) = viewModelScope.launch { Prefs.setKeepScreenOn(v) }
    fun setNotifications(v: Boolean, apply: (Boolean) -> Unit) = viewModelScope.launch {
        Prefs.setNotifications(v); apply(v)
    }
    fun clearCache() { Img.clear(); Api.clearCache() }
    fun clearHistory() = viewModelScope.launch { Prefs.clearHistory() }
}

class HomeVM : ViewModel() {
    val home = Loader<HomeResponse>(viewModelScope, isEmpty = { it.sections.isEmpty() }) { Repo.home() }
    val history = Prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private var last = 0L

    fun softRefresh() {
        val now = System.currentTimeMillis()
        if (home.state.value is com.beouttv.app.core.UiState.Initial) { home.load(); last = now }
        else if (now - last > 5 * 60_000L) { home.refresh(); last = now }
    }
}

class ChannelsVM : ViewModel() {
    val cats = Loader<List<Category>>(viewModelScope) { Repo.channelCategories() }
    val category = MutableStateFlow<String?>(null)
    val favOnly = MutableStateFlow(false)
    val query = MutableStateFlow("")
    val paged = PagedLoader(viewModelScope) { Repo.channels(category.value, it) }
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private var started = false

    fun start() {
        if (started) return
        started = true
        cats.load(); paged.reset()
    }

    fun select(c: String?) { category.value = c; paged.reset() }
    fun refresh() { cats.refresh(); paged.reset(keepOld = true) }
}

class CatalogVM : ViewModel() {
    val kind = MutableStateFlow("movie")
    val category = MutableStateFlow<String?>(null)
    val cats = MutableStateFlow<Loader<List<Category>>?>(null)
    val paged = PagedLoader(viewModelScope) { Repo.catalog(kind.value, category.value, it) }
    private var started = false

    private fun loaderFor(k: String) = Loader<List<Category>>(viewModelScope) { Repo.catalogCategories(k) }

    fun start() {
        if (started) return
        started = true
        cats.value = loaderFor(kind.value).also { it.load() }
        paged.reset()
    }

    fun setKind(k: String) {
        if (kind.value == k) return
        kind.value = k; category.value = null
        cats.value = loaderFor(k).also { it.load() }
        paged.reset()
    }

    fun select(c: String?) { category.value = c; paged.reset() }
    fun refresh() { cats.value?.refresh(); paged.reset(keepOld = true) }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchVM : ViewModel() {
    val query = MutableStateFlow("")
    val type = MutableStateFlow<String?>(null)
    val suggestions = MutableStateFlow<List<String>>(emptyList())
    val paged = PagedLoader(viewModelScope) { Repo.search(query.value.trim(), type.value, it) }
    val recent = Prefs.searchHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(query.debounce(250), type) { q, t -> q.trim() to t }
                .distinctUntilChanged()
                .collect { (q, _) ->
                    if (q.length < 2) paged.clear() else paged.reset()
                }
        }
        viewModelScope.launch {
            query.debounce(150).distinctUntilChanged().collect { q ->
                val t = q.trim()
                suggestions.value = if (t.length < 2) emptyList()
                else runCatching { Repo.suggest(t) }.getOrDefault(emptyList())
            }
        }
    }

    fun commit() { val q = query.value.trim(); if (q.length >= 2) viewModelScope.launch { Prefs.addSearch(q) } }
    fun clearRecent() { viewModelScope.launch { Prefs.clearSearch() } }
}

class DetailVM : ViewModel() {
    private val loaders = HashMap<String, Loader<Detail>>()
    val favorites = Prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = Prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loader(id: String): Loader<Detail> =
        loaders.getOrPut(id) { Loader(viewModelScope) { Repo.detail(id) } }

    fun toggleFav(item: com.beouttv.app.data.Item) { viewModelScope.launch { Repo.toggleFavorite(item) } }
}
