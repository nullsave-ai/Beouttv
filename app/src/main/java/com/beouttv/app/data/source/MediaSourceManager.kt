package com.beouttv.app.data.source

import android.content.Context
import android.content.SharedPreferences
import com.beouttv.app.data.model.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object MediaSourceManager {
    private const val PREFS_NAME = "beout_media_sources_vault"
    private const val KEY_ACTIVE_SOURCE_ID = "k_active_source_id"
    private const val KEY_CUSTOM_SOURCES = "k_custom_sources_json"

    // Primary Official WeCima Source
    val DEFAULT_SOURCES = listOf(
        MediaSource(
            id = "wecima_primary",
            name = "سيرفر WeCima (الرئيسي)",
            baseUrl = "https://wecima.bar/api.php",
            description = "السيرفر الرسمي عالي السرعة بمكتبة أفلام ومسلسلات غير محدودة",
            isDefault = true,
            speedLabel = "فائق السرعة (FHD/4K)"
        )
    )

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _sourcesFlow = MutableStateFlow<List<MediaSource>>(DEFAULT_SOURCES)
    val sourcesFlow: StateFlow<List<MediaSource>> = _sourcesFlow.asStateFlow()

    private val _activeSourceFlow = MutableStateFlow<MediaSource>(DEFAULT_SOURCES.first())
    val activeSourceFlow: StateFlow<MediaSource> = _activeSourceFlow.asStateFlow()

    // Event emitted whenever user switches source, so Repositories and ViewModels invalidate caches
    private val _sourceSwitchEvent = MutableSharedFlow<MediaSource>(extraBufferCapacity = 1)
    val sourceSwitchEvent: SharedFlow<MediaSource> = _sourceSwitchEvent.asSharedFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedSources()
    }

    private fun loadSavedSources() {
        val customList = mutableListOf<MediaSource>()
        val customJson = prefs.getString(KEY_CUSTOM_SOURCES, null)
        if (!customJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(customJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    customList.add(
                        MediaSource(
                            id = obj.optString("id", "custom_$i"),
                            name = obj.optString("name", "مصدر مخصص"),
                            baseUrl = obj.optString("baseUrl", ""),
                            description = obj.optString("description", "مصدر مخصص بواسطة المستخدم"),
                            isCustom = true,
                            speedLabel = obj.optString("speedLabel", "مخصص")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val allSources = DEFAULT_SOURCES + customList
        _sourcesFlow.value = allSources

        val activeId = prefs.getString(KEY_ACTIVE_SOURCE_ID, DEFAULT_SOURCES.first().id)
        val found = allSources.find { it.id == activeId } ?: DEFAULT_SOURCES.first()
        _activeSourceFlow.value = found
    }

    fun getActiveSource(): MediaSource = _activeSourceFlow.value

    fun selectSource(sourceId: String) {
        val found = _sourcesFlow.value.find { it.id == sourceId } ?: return
        if (found.id == _activeSourceFlow.value.id) return

        prefs.edit().putString(KEY_ACTIVE_SOURCE_ID, found.id).apply()
        _activeSourceFlow.value = found
        _sourceSwitchEvent.tryEmit(found)
    }

    fun addCustomSource(name: String, url: String, description: String = ""): MediaSource {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val id = "custom_${System.currentTimeMillis()}"
        val newSource = MediaSource(
            id = id,
            name = name.trim().ifBlank { "مصدر جديد" },
            baseUrl = cleanUrl,
            description = description.trim().ifBlank { "مصدر مخصص تمت إضافته يدوياً" },
            isCustom = true,
            speedLabel = "مخصص"
        )

        val currentList = _sourcesFlow.value.toMutableList()
        currentList.add(newSource)
        _sourcesFlow.value = currentList
        saveCustomSources()

        // Auto select newly added source
        selectSource(newSource.id)
        return newSource
    }

    fun removeCustomSource(sourceId: String) {
        val currentList = _sourcesFlow.value.toMutableList()
        val itemToRemove = currentList.find { it.id == sourceId && it.isCustom } ?: return
        currentList.remove(itemToRemove)
        _sourcesFlow.value = currentList
        saveCustomSources()

        if (_activeSourceFlow.value.id == sourceId) {
            selectSource(DEFAULT_SOURCES.first().id)
        }
    }

    private fun saveCustomSources() {
        val customSources = _sourcesFlow.value.filter { it.isCustom }
        val arr = JSONArray()
        for (src in customSources) {
            val obj = JSONObject()
            obj.put("id", src.id)
            obj.put("name", src.name)
            obj.put("baseUrl", src.baseUrl)
            obj.put("description", src.description)
            obj.put("speedLabel", src.speedLabel)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SOURCES, arr.toString()).apply()
    }
}
