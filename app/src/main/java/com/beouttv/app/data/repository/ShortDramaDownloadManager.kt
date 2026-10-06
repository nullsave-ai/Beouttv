package com.beouttv.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.beouttv.app.data.model.ShortDramaSeries
import com.beouttv.app.data.proto.ProtoReader
import com.beouttv.app.data.proto.ProtoWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadedEpisode(
    val seriesId: String,
    val seriesName: String,
    val episodeNumber: Int,
    val totalEpisodes: Int,
    val localVideoPath: String,
    val localSubtitlePath: String?,
    val quality: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

data class DownloadedSeries(
    val seriesId: String,
    val seriesName: String,
    val coverUrl: String,
    val totalEpisodes: Int,
    val downloadedCount: Int,
    val totalBytes: Long,
    val episodes: List<DownloadedEpisode>
)

data class DownloadBatchProgress(
    val seriesId: String,
    val seriesName: String,
    val coverUrl: String = "",
    val currentEpisode: Int = 0,
    val totalEpisodes: Int = 0,
    val currentEpisodeProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val speedText: String = "",
    val statusText: String = "",
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

object ShortDramaDownloadManager {
    private const val TAG = "DramaDownloader"
    private const val REGISTRY_FILE = "download_registry.pb"
    private const val LEGACY_JSON_FILE = "download_registry.json"

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadBatchProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadBatchProgress>> = _activeDownloads.asStateFlow()

    private val _downloadedSeriesList = MutableStateFlow<List<DownloadedSeries>>(emptyList())
    val downloadedSeriesList: StateFlow<List<DownloadedSeries>> = _downloadedSeriesList.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadRegistry(context)
    }

    fun getDownloadDir(context: Context): File {
        val base = context.getExternalFilesDir("drama_downloads") ?: File(context.filesDir, "drama_downloads")
        if (!base.exists()) base.mkdirs()
        return base
    }

    private fun getSeriesDir(context: Context, seriesId: String): File {
        val clean = seriesId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val dir = File(getDownloadDir(context), clean)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getRegistryFile(context: Context): File {
        return File(getDownloadDir(context), REGISTRY_FILE)
    }

    @Synchronized
    private fun loadRegistry(context: Context) {
        val pbFile = getRegistryFile(context)
        val legacyJsonFile = File(getDownloadDir(context), LEGACY_JSON_FILE)

        // 1. Native Protobuf v3 binary registry loader (0ms microsecond parsing!)
        if (pbFile.exists() && pbFile.length() > 0) {
            try {
                val bytes = pbFile.readBytes()
                val list = decodeRegistryProtobuf(bytes)
                _downloadedSeriesList.value = list
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error reading Protobuf download registry", e)
            }
        }

        // 2. Migration fallback from legacy JSON if it exists
        if (legacyJsonFile.exists()) {
            try {
                val content = legacyJsonFile.readText()
                val json = JSONObject(content)
                val seriesArray = json.optJSONArray("series") ?: JSONArray()
                val list = mutableListOf<DownloadedSeries>()

                for (i in 0 until seriesArray.length()) {
                    val sObj = seriesArray.getJSONObject(i)
                    val sId = sObj.getString("id")
                    val sName = sObj.getString("name")
                    val cover = sObj.optString("cover", "")
                    val totalEp = sObj.optInt("totalEpisodes", 0)

                    val epArray = sObj.optJSONArray("episodes") ?: JSONArray()
                    val episodes = mutableListOf<DownloadedEpisode>()
                    var totalBytes = 0L

                    for (j in 0 until epArray.length()) {
                        val epObj = epArray.getJSONObject(j)
                        val epNum = epObj.getInt("episodeNumber")
                        val vPath = epObj.getString("localVideoPath")
                        val subPath = epObj.optString("localSubtitlePath").ifBlank { null }
                        val q = epObj.optString("quality", "720p HD")
                        val size = epObj.optLong("fileSize", 0L)
                        val time = epObj.optLong("downloadedAt", System.currentTimeMillis())

                        val vFile = File(vPath)
                        if (vFile.exists() && vFile.length() > 0) {
                            val actualSize = if (size > 0) size else vFile.length()
                            totalBytes += actualSize
                            episodes.add(
                                DownloadedEpisode(
                                    seriesId = sId,
                                    seriesName = sName,
                                    episodeNumber = epNum,
                                    totalEpisodes = totalEp,
                                    localVideoPath = vPath,
                                    localSubtitlePath = subPath?.takeIf { File(it).exists() },
                                    quality = q,
                                    fileSize = actualSize,
                                    downloadedAt = time
                                )
                            )
                        }
                    }

                    if (episodes.isNotEmpty()) {
                        list.add(
                            DownloadedSeries(
                                seriesId = sId,
                                seriesName = sName,
                                coverUrl = cover,
                                totalEpisodes = totalEp.coerceAtLeast(episodes.size),
                                downloadedCount = episodes.size,
                                totalBytes = totalBytes,
                                episodes = episodes.sortedBy { it.episodeNumber }
                            )
                        )
                    }
                }

                _downloadedSeriesList.value = list
                saveRegistry(context)
                try { legacyJsonFile.delete() } catch (_: Exception) {}
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating legacy JSON registry", e)
            }
        }

        _downloadedSeriesList.value = emptyList()
    }

    @Synchronized
    private fun saveRegistry(context: Context) {
        try {
            val file = getRegistryFile(context)
            val bytes = encodeRegistryProtobuf(_downloadedSeriesList.value)
            file.writeBytes(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Protobuf download registry", e)
        }
    }

    private fun encodeRegistryProtobuf(seriesList: List<DownloadedSeries>): ByteArray {
        val writer = ProtoWriter()
        for (s in seriesList) {
            val sWriter = ProtoWriter()
            sWriter.writeString(1, s.seriesId)
            sWriter.writeString(2, s.seriesName)
            sWriter.writeString(3, s.coverUrl)
            sWriter.writeInt32(4, s.totalEpisodes)

            for (ep in s.episodes) {
                val epWriter = ProtoWriter()
                epWriter.writeString(1, ep.seriesId)
                epWriter.writeString(2, ep.seriesName)
                epWriter.writeInt32(3, ep.episodeNumber)
                epWriter.writeInt32(4, ep.totalEpisodes)
                epWriter.writeString(5, ep.localVideoPath)
                epWriter.writeString(6, ep.localSubtitlePath)
                epWriter.writeString(7, ep.quality)
                epWriter.writeInt64(8, ep.fileSize)
                epWriter.writeInt64(9, ep.downloadedAt)
                sWriter.writeMessage(5, epWriter.toByteArray())
            }
            writer.writeMessage(1, sWriter.toByteArray())
        }
        return writer.toByteArray()
    }

    private fun decodeRegistryProtobuf(bytes: ByteArray): List<DownloadedSeries> {
        val reader = ProtoReader(bytes)
        val list = mutableListOf<DownloadedSeries>()

        while (reader.hasMore()) {
            val tag = reader.readTag() ?: break
            if (tag.first == 1) {
                val sReader = ProtoReader(reader.readBytes())
                var sId = ""
                var sName = ""
                var cover = ""
                var totalEp = 0
                val episodes = mutableListOf<DownloadedEpisode>()

                while (sReader.hasMore()) {
                    val sTag = sReader.readTag() ?: break
                    when (sTag.first) {
                        1 -> sId = sReader.readString()
                        2 -> sName = sReader.readString()
                        3 -> cover = sReader.readString()
                        4 -> totalEp = sReader.readInt32()
                        5 -> {
                            val epReader = ProtoReader(sReader.readBytes())
                            var epSeriesId = ""
                            var epSeriesName = ""
                            var epNum = 1
                            var epTotal = 1
                            var vPath = ""
                            var subPath: String? = null
                            var quality = "720p HD"
                            var fileSize = 0L
                            var time = 0L

                            while (epReader.hasMore()) {
                                val epTag = epReader.readTag() ?: break
                                when (epTag.first) {
                                    1 -> epSeriesId = epReader.readString()
                                    2 -> epSeriesName = epReader.readString()
                                    3 -> epNum = epReader.readInt32()
                                    4 -> epTotal = epReader.readInt32()
                                    5 -> vPath = epReader.readString()
                                    6 -> subPath = epReader.readString().ifBlank { null }
                                    7 -> quality = epReader.readString()
                                    8 -> fileSize = epReader.readVarint64()
                                    9 -> time = epReader.readVarint64()
                                    else -> epReader.skipField(epTag.second)
                                }
                            }
                            if (File(vPath).exists()) {
                                episodes.add(
                                    DownloadedEpisode(
                                        seriesId = epSeriesId.ifBlank { sId },
                                        seriesName = epSeriesName.ifBlank { sName },
                                        episodeNumber = epNum,
                                        totalEpisodes = epTotal.coerceAtLeast(totalEp),
                                        localVideoPath = vPath,
                                        localSubtitlePath = subPath?.takeIf { File(it).exists() },
                                        quality = quality,
                                        fileSize = fileSize,
                                        downloadedAt = time
                                    )
                                )
                            }
                        }
                        else -> sReader.skipField(sTag.second)
                    }
                }

                if (episodes.isNotEmpty()) {
                    val sortedEps = episodes.sortedBy { it.episodeNumber }
                    list.add(
                        DownloadedSeries(
                            seriesId = sId,
                            seriesName = sName,
                            coverUrl = cover,
                            totalEpisodes = totalEp.coerceAtLeast(sortedEps.size),
                            downloadedCount = sortedEps.size,
                            totalBytes = sortedEps.sumOf { it.fileSize },
                            episodes = sortedEps
                        )
                    )
                }
            } else {
                reader.skipField(tag.second)
            }
        }
        return list
    }

    fun isEpisodeDownloaded(seriesId: String, episodeNumber: Int): Boolean {
        val series = _downloadedSeriesList.value.firstOrNull { it.seriesId == seriesId } ?: return false
        return series.episodes.any { it.episodeNumber == episodeNumber && File(it.localVideoPath).exists() }
    }

    fun getDownloadedEpisode(seriesId: String, episodeNumber: Int): DownloadedEpisode? {
        val series = _downloadedSeriesList.value.firstOrNull { it.seriesId == seriesId } ?: return null
        val ep = series.episodes.firstOrNull { it.episodeNumber == episodeNumber } ?: return null
        if (!File(ep.localVideoPath).exists()) return null
        // Safeguard: Ensure episode 1 subtitle (arabic.vtt or arabic_ep1.vtt) is never played on episode > 1
        if (episodeNumber > 1 && ep.localSubtitlePath != null) {
            val fileName = File(ep.localSubtitlePath).name
            if (fileName == "arabic.vtt" || fileName == "arabic_ep1.vtt") {
                return ep.copy(localSubtitlePath = null)
            }
        }
        return ep
    }

    fun getDownloadedEpisodesForSeries(seriesId: String): List<DownloadedEpisode> {
        val series = _downloadedSeriesList.value.firstOrNull { it.seriesId == seriesId } ?: return emptyList()
        return series.episodes.filter { File(it.localVideoPath).exists() }.map { ep ->
            if (ep.episodeNumber > 1 && ep.localSubtitlePath != null) {
                val fileName = File(ep.localSubtitlePath).name
                if (fileName == "arabic.vtt" || fileName == "arabic_ep1.vtt") {
                    ep.copy(localSubtitlePath = null)
                } else {
                    ep
                }
            } else {
                ep
            }
        }
    }

    /**
     * Launch sequential, resilient download of all episodes for a series
     */
    fun startBatchDownload(
        context: Context,
        series: ShortDramaSeries,
        quality: String = "720p HD",
        startEp: Int = 1,
        endEp: Int = series.episodeCount
    ) {
        init(context)
        val seriesId = series.id

        // Cancel previous if any
        activeJobs[seriesId]?.cancel()
        pausedFlags[seriesId] = false

        val initialProgress = DownloadBatchProgress(
            seriesId = seriesId,
            seriesName = series.name,
            coverUrl = series.coverUrl,
            currentEpisode = startEp,
            totalEpisodes = endEp - startEp + 1,
            isDownloading = true,
            statusText = "بدء التحميل..."
        )
        updateBatchProgress(seriesId, initialProgress)

        val job = downloadScope.launch {
            try {
                val videoId = ShortDramaRepository.resolveVideoId(series.id)
                val seriesDir = getSeriesDir(context, seriesId)

                // Known subtitles from official repository are for episode 1.
                // Subtitles must only be assigned to their matching episode.
                val cleanVid = videoId.removePrefix("series_")
                val ep1SubUrl: String? = ShortDramaRepository.knownSubtitleUrls["${cleanVid}_1"]
                    ?: ShortDramaRepository.knownSubtitleUrls[cleanVid]
                    ?: ShortDramaRepository.knownSubtitleUrls[videoId]
                    ?: ShortDramaRepository.knownSubtitleUrls["series_$cleanVid"]

                var ep1SubFile: File? = null
                if (!ep1SubUrl.isNullOrBlank()) {
                    try {
                        val subFile = File(seriesDir, "arabic_ep1.vtt")
                        if (!subFile.exists() || subFile.length() == 0L) {
                            downloadFileWithRetry(ep1SubUrl, subFile)
                        }
                        if (subFile.exists() && subFile.length() > 0) {
                            ep1SubFile = subFile
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed downloading subtitle file: ${e.message}")
                    }
                }

                val totalEpisodesToDownload = endEp - startEp + 1
                var completedCount = 0

                for (epNum in startEp..endEp) {
                    if (pausedFlags[seriesId] == true) {
                        updateBatchProgress(
                            seriesId,
                            getCurrentProgress(seriesId)?.copy(
                                isDownloading = false,
                                isPaused = true,
                                statusText = "تم إيقاف التحميل مؤقتاً"
                            )
                        )
                        return@launch
                    }

                    if (!isActive) break

                    // Check if already downloaded
                    val targetMp4 = File(seriesDir, "ep_${epNum}.mp4")
                    val targetSub = File(seriesDir, "arabic_ep${epNum}.vtt")

                    // Ensure subtitle is resolved and downloaded for this specific episode
                    if (!targetSub.exists() || targetSub.length() == 0L) {
                        try {
                            val subUrl: String = if (epNum == 1 && !ep1SubUrl.isNullOrBlank()) {
                                ep1SubUrl
                            } else {
                                ShortDramaRepository.resolveSubtitleForEpisode(cleanVid, epNum)
                            }
                            if (subUrl.isNotBlank()) {
                                downloadFileWithRetry(subUrl, targetSub)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed downloading subtitle for ep $epNum: ${e.message}")
                        }
                    }
                    val epSub = if (targetSub.exists() && targetSub.length() > 0) targetSub.absolutePath else null

                    if (targetMp4.exists() && targetMp4.length() > 200_000L) {
                        completedCount++
                        recordEpisodeDownloaded(
                            context = context,
                            series = series,
                            episodeNumber = epNum,
                            videoPath = targetMp4.absolutePath,
                            subPath = epSub,
                            quality = quality,
                            fileSize = targetMp4.length()
                        )
                        val overall = completedCount.toFloat() / totalEpisodesToDownload.toFloat()
                        updateBatchProgress(
                            seriesId,
                            getCurrentProgress(seriesId)?.copy(
                                currentEpisode = epNum,
                                overallProgress = overall,
                                statusText = "الحلقة $epNum محملة مسبقاً ($completedCount/$totalEpisodesToDownload)"
                            )
                        )
                        continue
                    }

                    // Build stream URL
                    val sources = ShortDramaRepository.buildSourcesForEpisode(videoId, epNum, series.platform)
                    val targetSource = sources.firstOrNull { it.quality.contains(quality.take(4)) }
                        ?: sources.firstOrNull()
                    val cdnApi = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_NETSHORT_CDN_API).ifBlank { "https://www.netshortt.com/api/" }
                    val streamUrl = targetSource?.streamUrl ?: "${cdnApi}video/$cleanVid/$epNum.mp4"

                    // Update UI status
                    updateBatchProgress(
                        seriesId,
                        getCurrentProgress(seriesId)?.copy(
                            currentEpisode = epNum,
                            currentEpisodeProgress = 0f,
                            statusText = "جاري تحميل حلقة $epNum من $endEp..."
                        )
                    )

                    // Download with auto-retries and exponential backoff
                    val partFile = File(seriesDir, "ep_${epNum}.part")
                    val success = downloadEpisodeWithRetry(
                        streamUrl = streamUrl,
                        partFile = partFile,
                        targetFile = targetMp4,
                        onProgress = { epProgress, speed ->
                            val overall = (completedCount + epProgress) / totalEpisodesToDownload.toFloat()
                            updateBatchProgress(
                                seriesId,
                                getCurrentProgress(seriesId)?.copy(
                                    currentEpisodeProgress = epProgress,
                                    overallProgress = overall,
                                    speedText = speed,
                                    statusText = "حلقة $epNum ($completedCount/$totalEpisodesToDownload)"
                                )
                            )
                        }
                    )

                    if (success && targetMp4.exists() && targetMp4.length() > 0) {
                        completedCount++
                        recordEpisodeDownloaded(
                            context = context,
                            series = series,
                            episodeNumber = epNum,
                            videoPath = targetMp4.absolutePath,
                            subPath = epSub,
                            quality = quality,
                            fileSize = targetMp4.length()
                        )
                    } else {
                        Log.e(TAG, "Failed downloading episode $epNum")
                    }
                }

                // Batch finished!
                updateBatchProgress(
                    seriesId,
                    getCurrentProgress(seriesId)?.copy(
                        isDownloading = false,
                        isCompleted = true,
                        overallProgress = 1f,
                        statusText = "اكتمل تحميل كافة الحلقات بنجاح!"
                    )
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "Download job cancelled for $seriesId")
            } catch (e: Exception) {
                Log.e(TAG, "Download job error for $seriesId", e)
                updateBatchProgress(
                    seriesId,
                    getCurrentProgress(seriesId)?.copy(
                        isDownloading = false,
                        error = e.localizedMessage ?: "حدث خطأ أثناء التحميل",
                        statusText = "فشل التحميل، يرجى المحاولة لاحقاً"
                    )
                )
            } finally {
                activeJobs.remove(seriesId)
            }
        }

        activeJobs[seriesId] = job
    }

    private suspend fun downloadEpisodeWithRetry(
        streamUrl: String,
        partFile: File,
        targetFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var retries = 0
        val maxRetries = 5

        while (retries < maxRetries && isActive) {
            try {
                val downloaded = downloadStreamToFile(streamUrl, partFile, onProgress)
                if (downloaded && partFile.exists() && partFile.length() > 100_000L) {
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = partFile.renameTo(targetFile)
                    if (renamed) return@withContext true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retries++
                Log.w(TAG, "Retry $retries/$maxRetries for $streamUrl due to: ${e.message}")
                delay(1000L * retries)
            }
        }
        false
    }

    private fun downloadStreamToFile(
        url: String,
        destFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        var downloadedBytes = if (destFile.exists()) destFile.length() else 0L

        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13)")

        if (downloadedBytes > 0) {
            reqBuilder.header("Range", "bytes=$downloadedBytes-")
        }

        val response = httpClient.newCall(reqBuilder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            if (response.code == 416) {
                // Range Not Satisfiable: file might already be complete
                destFile.delete()
                downloadedBytes = 0L
                return downloadStreamToFile(url, destFile, onProgress)
            }
            response.close()
            return false
        }

        val body = response.body ?: return false
        val isAppend = response.code == 206
        val contentLength = body.contentLength()
        val totalBytes = if (isAppend) downloadedBytes + contentLength else contentLength

        var stream: InputStream? = null
        var raf: RandomAccessFile? = null
        var fos: FileOutputStream? = null

        try {
            stream = body.byteStream()
            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var lastUpdate = System.currentTimeMillis()
            var bytesSinceUpdate = 0L

            if (isAppend) {
                raf = RandomAccessFile(destFile, "rw")
                raf.seek(downloadedBytes)
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate >= 400) {
                        val speedKb = (bytesSinceUpdate * 1000f) / ((now - lastUpdate) * 1024f)
                        val speedText = if (speedKb > 1024) String.format("%.1f MB/s", speedKb / 1024f) else String.format("%.0f KB/s", speedKb)
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0.5f
                        onProgress(progress.coerceIn(0f, 1f), speedText)
                        lastUpdate = now
                        bytesSinceUpdate = 0L
                    }
                }
            } else {
                fos = FileOutputStream(destFile)
                downloadedBytes = 0L
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate >= 400) {
                        val speedKb = (bytesSinceUpdate * 1000f) / ((now - lastUpdate) * 1024f)
                        val speedText = if (speedKb > 1024) String.format("%.1f MB/s", speedKb / 1024f) else String.format("%.0f KB/s", speedKb)
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0.5f
                        onProgress(progress.coerceIn(0f, 1f), speedText)
                        lastUpdate = now
                        bytesSinceUpdate = 0L
                    }
                }
            }
            onProgress(1f, "")
            return true
        } finally {
            try { stream?.close() } catch (_: Exception) {}
            try { raf?.close() } catch (_: Exception) {}
            try { fos?.close() } catch (_: Exception) {}
            response.close()
        }
    }

    private fun downloadFileWithRetry(url: String, destFile: File) {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                resp.body?.byteStream()?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun recordEpisodeDownloaded(
        context: Context,
        series: ShortDramaSeries,
        episodeNumber: Int,
        videoPath: String,
        subPath: String?,
        quality: String,
        fileSize: Long
    ) {
        val currentList = _downloadedSeriesList.value.toMutableList()
        val index = currentList.indexOfFirst { it.seriesId == series.id }

        val newEp = DownloadedEpisode(
            seriesId = series.id,
            seriesName = series.name,
            episodeNumber = episodeNumber,
            totalEpisodes = series.episodeCount,
            localVideoPath = videoPath,
            localSubtitlePath = subPath,
            quality = quality,
            fileSize = fileSize
        )

        if (index >= 0) {
            val s = currentList[index]
            val updatedEps = s.episodes.filter { it.episodeNumber != episodeNumber }.toMutableList()
            updatedEps.add(newEp)
            val sorted = updatedEps.sortedBy { it.episodeNumber }
            currentList[index] = s.copy(
                downloadedCount = sorted.size,
                totalBytes = sorted.sumOf { it.fileSize },
                episodes = sorted
            )
        } else {
            currentList.add(
                DownloadedSeries(
                    seriesId = series.id,
                    seriesName = series.name,
                    coverUrl = series.coverUrl,
                    totalEpisodes = series.episodeCount,
                    downloadedCount = 1,
                    totalBytes = fileSize,
                    episodes = listOf(newEp)
                )
            )
        }

        _downloadedSeriesList.value = currentList
        saveRegistry(context)
    }

    fun pauseBatchDownload(seriesId: String) {
        pausedFlags[seriesId] = true
        activeJobs[seriesId]?.cancel()
        activeJobs.remove(seriesId)
        val cur = getCurrentProgress(seriesId)
        if (cur != null) {
            updateBatchProgress(seriesId, cur.copy(isDownloading = false, isPaused = true, statusText = "تم إيقاف التحميل مؤقتاً"))
        }
    }

    fun cancelBatchDownload(seriesId: String) {
        activeJobs[seriesId]?.cancel()
        activeJobs.remove(seriesId)
        pausedFlags.remove(seriesId)
        val current = _activeDownloads.value.toMutableMap()
        current.remove(seriesId)
        _activeDownloads.value = current
    }

    fun deleteDownloadedSeries(context: Context, seriesId: String) {
        cancelBatchDownload(seriesId)
        val seriesDir = getSeriesDir(context, seriesId)
        try {
            seriesDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting series dir: ${e.message}")
        }

        val currentList = _downloadedSeriesList.value.filter { it.seriesId != seriesId }
        _downloadedSeriesList.value = currentList
        saveRegistry(context)
    }

    fun deleteDownloadedEpisode(context: Context, seriesId: String, episodeNumber: Int) {
        val series = _downloadedSeriesList.value.firstOrNull { it.seriesId == seriesId } ?: return
        val ep = series.episodes.firstOrNull { it.episodeNumber == episodeNumber }
        if (ep != null) {
            try {
                File(ep.localVideoPath).delete()
            } catch (_: Exception) {}
        }

        val updatedEps = series.episodes.filter { it.episodeNumber != episodeNumber }
        val currentList = _downloadedSeriesList.value.toMutableList()
        val index = currentList.indexOfFirst { it.seriesId == seriesId }
        if (index >= 0) {
            if (updatedEps.isEmpty()) {
                currentList.removeAt(index)
            } else {
                currentList[index] = series.copy(
                    downloadedCount = updatedEps.size,
                    totalBytes = updatedEps.sumOf { it.fileSize },
                    episodes = updatedEps
                )
            }
            _downloadedSeriesList.value = currentList
            saveRegistry(context)
        }
    }

    private fun getCurrentProgress(seriesId: String): DownloadBatchProgress? {
        return _activeDownloads.value[seriesId]
    }

    private fun updateBatchProgress(seriesId: String, progress: DownloadBatchProgress?) {
        val map = _activeDownloads.value.toMutableMap()
        if (progress != null) {
            map[seriesId] = progress
        } else {
            map.remove(seriesId)
        }
        _activeDownloads.value = map
    }
}
