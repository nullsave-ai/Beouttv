package com.beouttv.app.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.beouttv.app.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class PlayEntry(val item: Item, val episodeId: String? = null, val label: String? = null) {
    val key get() = episodeId ?: item.id
    val kind get() = if (episodeId != null) "episode" else item.kind
}

@Immutable
data class PlayReq(val entries: List<PlayEntry>, val index: Int = 0)

sealed interface Route {
    data class Detail(val id: String, val seed: Item? = null) : Route
    data object Search : Route
    data object Settings : Route
    data class Player(val req: PlayReq) : Route
}

/** تنقل مخصص: 3 أقسام رئيسية + مكدس صفحات. يعيش في ViewModel فيبقى عبر تغيير الاتجاه. */
class NavVM : ViewModel() {
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab
    private val _stack = MutableStateFlow<List<Route>>(emptyList())
    val stack: StateFlow<List<Route>> = _stack

    fun tab(i: Int) { _tab.value = i }
    fun push(r: Route) { _stack.value = _stack.value + r }
    fun replaceTop(r: Route) { _stack.value = _stack.value.dropLast(1) + r }
    fun pop(): Boolean {
        if (_stack.value.isEmpty()) return false
        _stack.value = _stack.value.dropLast(1)
        return true
    }
}
