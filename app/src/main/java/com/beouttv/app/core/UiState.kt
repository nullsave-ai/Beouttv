package com.beouttv.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Immutable
import com.beouttv.app.data.Item
import com.beouttv.app.data.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Initial : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Refreshing<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data object Offline : UiState<Nothing>
}

fun <T> UiState<T>.dataOrNull(): T? = when (this) {
    is UiState.Success -> data
    is UiState.Refreshing -> data
    else -> null
}

object Net {
    @Volatile private var cm: ConnectivityManager? = null
    fun init(c: Context) { cm = c.applicationContext.getSystemService(ConnectivityManager::class.java) }
    fun online(): Boolean {
        val m = cm ?: return true
        val caps = m.getNetworkCapabilities(m.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** محمّل بيانات أحادي: يلغي الطلب السابق، ويحفظ آخر بيانات ناجحة، ويمنع الكتابة ببيانات قديمة. */
class Loader<T>(
    private val scope: CoroutineScope,
    private val isEmpty: (T) -> Boolean = { false },
    private val fetch: suspend () -> T,
) {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Initial)
    val state: StateFlow<UiState<T>> = _state
    private var job: Job? = null

    fun load(force: Boolean = false) {
        if (!force && (job?.isActive == true || _state.value.dataOrNull() != null)) return
        job?.cancel()
        val prev = _state.value.dataOrNull()
        _state.value = if (prev != null) UiState.Refreshing(prev) else UiState.Loading
        job = scope.launch {
            try {
                val d = fetch()
                _state.value = if (isEmpty(d)) UiState.Empty else UiState.Success(d)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = when {
                    prev != null -> UiState.Success(prev)
                    !Net.online() -> UiState.Offline
                    else -> UiState.Error(e.message ?: "error")
                }
            }
        }
    }

    fun refresh() = load(true)
}

@Immutable
data class PagedState(
    val ui: UiState<List<Item>> = UiState.Initial,
    val loadingMore: Boolean = false,
    val end: Boolean = false,
)

/** تحميل تدريجي بمؤشر cursor. إعادة الضبط تلغي أي طلب قديم. */
class PagedLoader(
    private val scope: CoroutineScope,
    private val fetch: suspend (cursor: String?) -> Page,
) {
    private val _state = MutableStateFlow(PagedState())
    val state: StateFlow<PagedState> = _state
    private var job: Job? = null
    private var next: String? = null

    fun reset(keepOld: Boolean = false) {
        job?.cancel()
        next = null
        val prev = if (keepOld) _state.value.ui.dataOrNull() else null
        _state.value = PagedState(ui = if (prev != null) UiState.Refreshing(prev) else UiState.Loading)
        job = scope.launch {
            try {
                val p = fetch(null)
                next = p.next
                _state.value = PagedState(
                    ui = if (p.items.isEmpty()) UiState.Empty else UiState.Success(p.items),
                    end = p.next == null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = PagedState(
                    ui = when {
                        prev != null -> UiState.Success(prev)
                        !Net.online() -> UiState.Offline
                        else -> UiState.Error(e.message ?: "error")
                    },
                )
            }
        }
    }

    fun clear() {
        job?.cancel()
        next = null
        _state.value = PagedState()
    }

    fun loadMore() {
        val cur = _state.value
        val list = cur.ui.dataOrNull() ?: return
        val c = next ?: return
        if (cur.loadingMore || cur.end || job?.isActive == true) return
        _state.value = cur.copy(loadingMore = true)
        job = scope.launch {
            try {
                val p = fetch(c)
                next = p.next
                val merged = (list + p.items).distinctBy { it.id }
                _state.value = PagedState(UiState.Success(merged), loadingMore = false, end = p.next == null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = cur.copy(loadingMore = false)
            }
        }
    }
}
