package com.beouttv.app.data.xtream

import android.content.Context
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.proto.ChannelProtoCodec
import com.beouttv.app.data.proto.MovieProtoCodec
import com.beouttv.app.data.proto.SeriesProtoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance Protobuf v3 binary disk cache for Personal Xtream & M3U content.
 * Delivers instantaneous (<15ms) catalog deserialization without heavy JSON overhead.
 */
object XtreamProtobufCache {

    private const val DIR_NAME = "xtream_pb_cache"
    private var baseDir: File? = null

    fun init(context: Context) {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        baseDir = dir
    }

    private fun getFile(prefix: String, accountId: String): File? {
        val dir = baseDir ?: return null
        val safeId = accountId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(dir, "${prefix}_${safeId}.pb")
    }

    suspend fun saveChannels(accountId: String, channels: List<Channel>) = withContext(Dispatchers.IO) {
        try {
            val file = getFile("channels", accountId) ?: return@withContext
            val bytes = ChannelProtoCodec.encodeChannelList(channels, compressGzip = true)
            file.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadChannels(accountId: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val file = getFile("channels", accountId) ?: return@withContext emptyList()
            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                return@withContext ChannelProtoCodec.decodeChannelList(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun saveMovies(accountId: String, movies: List<Movie>) = withContext(Dispatchers.IO) {
        try {
            val file = getFile("movies", accountId) ?: return@withContext
            val bytes = MovieProtoCodec.encodeMovieList(movies, compressGzip = true)
            file.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadMovies(accountId: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val file = getFile("movies", accountId) ?: return@withContext emptyList()
            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                return@withContext MovieProtoCodec.decodeMovieList(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun saveSeries(accountId: String, seriesList: List<Series>) = withContext(Dispatchers.IO) {
        try {
            val file = getFile("series", accountId) ?: return@withContext
            val bytes = SeriesProtoCodec.encodeSeriesList(seriesList, compressGzip = true)
            file.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadSeries(accountId: String): List<Series> = withContext(Dispatchers.IO) {
        try {
            val file = getFile("series", accountId) ?: return@withContext emptyList()
            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                return@withContext SeriesProtoCodec.decodeSeriesList(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun deleteAccountCache(accountId: String) = withContext(Dispatchers.IO) {
        try {
            getFile("channels", accountId)?.delete()
            getFile("movies", accountId)?.delete()
            getFile("series", accountId)?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            baseDir?.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
