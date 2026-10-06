package com.beouttv.app.data.repository

import android.content.Context
import com.beouttv.app.data.model.StreamServer
import com.beouttv.app.data.model.SubtitleTrack
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

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class DownloadTask(
    val id: String,
    val mediaId: String,
    val title: String,
    val mediaType: String = "movie", // "movie" or "series"
    val posterUrl: String = "",
    val quality: String = "1080p",
    val videoUrl: String,
    val subtitleUrl: String? = null,
    val localVideoPath: String = "",
    val localSubtitlePath: String? = null,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedText: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object MovieBoxDownloadManager {
    private const val REGISTRY_FILE = "moviebox_downloads_registry.json"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private var appContext: Context? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true
        loadRegistry()
    }

    private fun getStorageDir(context: Context): File {
        val base = context.getExternalFilesDir("moviebox_downloads") ?: File(context.filesDir, "moviebox_downloads")
        if (!base.exists()) base.mkdirs()
        return base
    }

    @Synchronized
    private fun loadRegistry() {
        val ctx = appContext ?: return
        val regFile = File(getStorageDir(ctx), REGISTRY_FILE)
        if (!regFile.exists()) return

        try {
            val jsonStr = regFile.readText()
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DownloadTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val task = DownloadTask(
                    id = obj.getString("id"),
                    mediaId = obj.getString("mediaId"),
                    title = obj.getString("title"),
                    mediaType = obj.optString("mediaType", "movie"),
                    posterUrl = obj.optString("posterUrl", ""),
                    quality = obj.optString("quality", "1080p"),
                    videoUrl = obj.getString("videoUrl"),
                    subtitleUrl = obj.optString("subtitleUrl").takeIf { it.isNotBlank() },
                    localVideoPath = obj.getString("localVideoPath"),
                    localSubtitlePath = obj.optString("localSubtitlePath").takeIf { it.isNotBlank() },
                    progress = obj.optDouble("progress", 0.0).toFloat(),
                    bytesDownloaded = obj.optLong("bytesDownloaded", 0L),
                    totalBytes = obj.optLong("totalBytes", 0L),
                    status = try {
                        val s = DownloadStatus.valueOf(obj.optString("status", "QUEUED"))
                        if (s == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else s
                    } catch (_: Exception) { DownloadStatus.PAUSED },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                list.add(task)
            }
            _downloads.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun saveRegistry() {
        val ctx = appContext ?: return
        val regFile = File(getStorageDir(ctx), REGISTRY_FILE)
        try {
            val arr = JSONArray()
            _downloads.value.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("mediaId", task.mediaId)
                    put("title", task.title)
                    put("mediaType", task.mediaType)
                    put("posterUrl", task.posterUrl)
                    put("quality", task.quality)
                    put("videoUrl", task.videoUrl)
                    put("subtitleUrl", task.subtitleUrl.orEmpty())
                    put("localVideoPath", task.localVideoPath)
                    put("localSubtitlePath", task.localSubtitlePath.orEmpty())
                    put("progress", task.progress)
                    put("bytesDownloaded", task.bytesDownloaded)
                    put("totalBytes", task.totalBytes)
                    put("status", task.status.name)
                    put("createdAt", task.createdAt)
                }
                arr.put(obj)
            }
            regFile.writeText(arr.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDownload(
        context: Context,
        mediaId: String,
        title: String,
        videoUrl: String,
        posterUrl: String = "",
        quality: String = "1080p",
        subtitleUrl: String? = null,
        mediaType: String = "movie"
    ) {
        init(context)
        val cleanMediaId = mediaId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val cleanName = title.replace("[^a-zA-Z0-9_ء-ي-]".toRegex(), "_")
        val taskId = "dl_${cleanMediaId}_${quality}"

        val existing = _downloads.value.find { it.id == taskId }
        if (existing != null && existing.status == DownloadStatus.COMPLETED) {
            return // Already completed
        }

        val videoFile = File(getStorageDir(context), "${cleanName}_${quality}.mp4")
        val subFile = subtitleUrl?.let { File(getStorageDir(context), "${cleanName}.srt") }

        val task = DownloadTask(
            id = taskId,
            mediaId = mediaId,
            title = title,
            mediaType = mediaType,
            posterUrl = posterUrl,
            quality = quality,
            videoUrl = videoUrl,
            subtitleUrl = subtitleUrl,
            localVideoPath = videoFile.absolutePath,
            localSubtitlePath = subFile?.absolutePath,
            status = DownloadStatus.DOWNLOADING
        )

        updateTask(task)
        pausedFlags[taskId] = false

        val job = scope.launch {
            runDownloadPipeline(context, task, videoFile, subFile)
        }
        activeJobs[taskId] = job
    }

    private suspend fun runDownloadPipeline(
        context: Context,
        initialTask: DownloadTask,
        videoFile: File,
        subFile: File?
    ) = withContext(Dispatchers.IO) {
        val taskId = initialTask.id

        // Download subtitle first if exists and not already downloaded
        if (!initialTask.subtitleUrl.isNullOrBlank() && subFile != null && !subFile.exists()) {
            try {
                val subReq = Request.Builder().url(initialTask.subtitleUrl).get().build()
                val subResp = httpClient.newCall(subReq).execute()
                if (subResp.isSuccessful) {
                    subResp.body?.bytes()?.let { subBytes ->
                        val cleanBytes = com.beouttv.app.data.subtitle.SubtitleCharsetHelper.sanitizeSubtitleBytes(subBytes)
                        subFile.writeBytes(cleanBytes)
                    }
                }
            } catch (_: Exception) {}
        }

        // Resumable Range-based Video Downloader
        var existingBytes = if (videoFile.exists()) videoFile.length() else 0L

        try {
            val reqBuilder = Request.Builder().url(initialTask.videoUrl)
            if (existingBytes > 0L) {
                reqBuilder.header("Range", "bytes=$existingBytes-")
            }
            val request = reqBuilder.build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful && response.code != 416) {
                updateTask(initialTask.copy(status = DownloadStatus.FAILED, errorMessage = "HTTP ${response.code}"))
                return@withContext
            }

            val body = response.body
            if (body == null) {
                updateTask(initialTask.copy(status = DownloadStatus.FAILED, errorMessage = "Empty body"))
                return@withContext
            }

            val contentLen = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + contentLen else contentLen

            val raf = RandomAccessFile(videoFile, "rw")
            if (response.code == 206) {
                raf.seek(existingBytes)
            } else {
                raf.setLength(0)
                existingBytes = 0L
            }

            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(64 * 1024)
            var read: Int
            var lastUpdate = System.currentTimeMillis()
            var bytesSinceLast = 0L
            var speedStr = ""

            while (inputStream.read(buffer).also { read = it } != -1) {
                if (pausedFlags[taskId] == true) {
                    raf.close()
                    inputStream.close()
                    updateTask(initialTask.copy(
                        status = DownloadStatus.PAUSED,
                        bytesDownloaded = existingBytes,
                        totalBytes = totalBytes,
                        speedText = ""
                    ))
                    saveRegistry()
                    return@withContext
                }

                raf.write(buffer, 0, read)
                existingBytes += read
                bytesSinceLast += read

                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 600) {
                    val timeDiff = (now - lastUpdate) / 1000f
                    val speed = if (timeDiff > 0) (bytesSinceLast / timeDiff) else 0f
                    speedStr = formatSpeed(speed)
                    val progress = if (totalBytes > 0) (existingBytes.toFloat() / totalBytes) else 0f

                    updateTask(initialTask.copy(
                        status = DownloadStatus.DOWNLOADING,
                        progress = progress.coerceIn(0f, 1f),
                        bytesDownloaded = existingBytes,
                        totalBytes = totalBytes,
                        speedText = speedStr
                    ))

                    lastUpdate = now
                    bytesSinceLast = 0L
                }
            }

            raf.close()
            inputStream.close()

            // Completed!
            updateTask(initialTask.copy(
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                bytesDownloaded = existingBytes,
                totalBytes = existingBytes,
                speedText = ""
            ))
            saveRegistry()

        } catch (e: Exception) {
            if (pausedFlags[taskId] == true) {
                updateTask(initialTask.copy(status = DownloadStatus.PAUSED))
            } else {
                updateTask(initialTask.copy(status = DownloadStatus.FAILED, errorMessage = e.message))
            }
            saveRegistry()
        }
    }

    fun pauseDownload(taskId: String) {
        pausedFlags[taskId] = true
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        val current = _downloads.value.find { it.id == taskId }
        if (current != null) {
            updateTask(current.copy(status = DownloadStatus.PAUSED, speedText = ""))
            saveRegistry()
        }
    }

    fun resumeDownload(context: Context, taskId: String) {
        val task = _downloads.value.find { it.id == taskId } ?: return
        startDownload(
            context = context,
            mediaId = task.mediaId,
            title = task.title,
            videoUrl = task.videoUrl,
            posterUrl = task.posterUrl,
            quality = task.quality,
            subtitleUrl = task.subtitleUrl,
            mediaType = task.mediaType
        )
    }

    fun deleteDownload(taskId: String) {
        pauseDownload(taskId)
        val task = _downloads.value.find { it.id == taskId }
        if (task != null) {
            try {
                val f = File(task.localVideoPath)
                if (f.exists()) f.delete()
                task.localSubtitlePath?.let {
                    val sf = File(it)
                    if (sf.exists()) sf.delete()
                }
            } catch (_: Exception) {}
        }
        _downloads.value = _downloads.value.filter { it.id != taskId }
        saveRegistry()
    }

    fun isDownloaded(mediaId: String): Boolean {
        return _downloads.value.any { it.mediaId == mediaId && it.status == DownloadStatus.COMPLETED }
    }

    fun getDownloadedTask(mediaId: String): DownloadTask? {
        return _downloads.value.find { it.mediaId == mediaId && it.status == DownloadStatus.COMPLETED }
    }

    private fun updateTask(task: DownloadTask) {
        val current = _downloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
        } else {
            current.add(0, task)
        }
        _downloads.value = current
    }

    private fun formatSpeed(bytesPerSec: Float): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024)
            else -> "${bytesPerSec.toInt()} B/s"
        }
    }
}
