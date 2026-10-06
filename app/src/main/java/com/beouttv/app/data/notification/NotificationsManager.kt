package com.beouttv.app.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import com.beouttv.app.data.model.Match
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.TimeUnit

data class AppNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String, // "MATCH_REMINDER", "BROADCAST", "SYSTEM", "AD", "URGENT"
    val isRead: Boolean = false,
    val matchId: String? = null,
    val matchHomeTeam: String? = null,
    val matchAwayTeam: String? = null,
    val matchTime: String? = null,
    val matchDate: String? = null,
    val matchCompetition: String? = null,
    val leadMinutes: Int = 15,
    val actionUrl: String? = null,
    val actionTitle: String? = null,
    val imageUrl: String? = null,
    val isAd: Boolean = false
)

object NotificationsManager {

    private const val PREFS_NAME = "beout_notifications_v4"
    private const val KEY_ITEMS_JSON = "saved_notification_items"
    private const val KEY_LAST_BROADCAST_ID = "last_seen_broadcast_id"

    private const val SETTINGS_PREFS = "beout_settings"
    private const val KEY_ALERT_TIME = "alert_time_minutes"

    private const val BACKGROUND_SYNC_INTERVAL_MILLIS = 8 * 60 * 1000L // 8 minutes background polling

    private val _notificationsFlow = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<AppNotificationItem>> = _notificationsFlow.asStateFlow()

    private val _unreadCountFlow = MutableStateFlow(0)
    val unreadCountFlow: StateFlow<Int> = _unreadCountFlow.asStateFlow()

    private var appContext: Context? = null
    private var listeningJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Infinite for SSE streaming
        .build()

    private val pollClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
        LocalNotificationManager.initChannels(context)
        loadSavedNotifications()

        // 1. Subscribe to Firebase Cloud Messaging (FCM) topic for 24/7 push to killed apps
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        } catch (_: Exception) {}

        // 2. Sync all historical broadcasts from Firebase Server
        syncServerBroadcastHistory(context)

        // 3. Start realtime in-app listener
        startRealtimeBroadcastListener(context)

        // 4. Schedule periodic background sync via AlarmManager
        scheduleNextBackgroundSync(context)
    }

    fun markAllAsRead() {
        val current = _notificationsFlow.value
        if (current.any { !it.isRead }) {
            val updated = current.map { it.copy(isRead = true) }
            saveNotifications(updated)
        }
    }

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        val ctx = context ?: appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadSavedNotifications() {
        val prefs = getPrefs() ?: return
        val rawJson = prefs.getString(KEY_ITEMS_JSON, "[]") ?: "[]"
        val list = mutableListOf<AppNotificationItem>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val typeStr = obj.optString("type", "SYSTEM")
                list.add(
                    AppNotificationItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.getLong("timestamp"),
                        type = typeStr,
                        isRead = obj.optBoolean("isRead", false),
                        matchId = obj.optString("matchId", "").takeIf { it.isNotBlank() },
                        matchHomeTeam = obj.optString("matchHomeTeam", "").takeIf { it.isNotBlank() },
                        matchAwayTeam = obj.optString("matchAwayTeam", "").takeIf { it.isNotBlank() },
                        matchTime = obj.optString("matchTime", "").takeIf { it.isNotBlank() },
                        matchDate = obj.optString("matchDate", "").takeIf { it.isNotBlank() },
                        matchCompetition = obj.optString("matchCompetition", "").takeIf { it.isNotBlank() },
                        leadMinutes = obj.optInt("leadMinutes", 15),
                        actionUrl = obj.optString("actionUrl", "").takeIf { it.isNotBlank() },
                        actionTitle = obj.optString("actionTitle", "").takeIf { it.isNotBlank() },
                        imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() },
                        isAd = obj.optBoolean("isAd", false) || typeStr.equals("AD", true)
                    )
                )
            }
        } catch (_: Exception) {}

        _notificationsFlow.value = list
        _unreadCountFlow.value = list.count { !it.isRead }
    }

    private fun saveNotifications(list: List<AppNotificationItem>, context: Context? = null) {
        _notificationsFlow.value = list
        _unreadCountFlow.value = list.count { !it.isRead }

        val prefs = getPrefs(context) ?: return
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("timestamp", item.timestamp)
                    put("type", item.type)
                    put("isRead", item.isRead)
                    put("matchId", item.matchId ?: "")
                    put("matchHomeTeam", item.matchHomeTeam ?: "")
                    put("matchAwayTeam", item.matchAwayTeam ?: "")
                    put("matchTime", item.matchTime ?: "")
                    put("matchDate", item.matchDate ?: "")
                    put("matchCompetition", item.matchCompetition ?: "")
                    put("leadMinutes", item.leadMinutes)
                    put("actionUrl", item.actionUrl ?: "")
                    put("actionTitle", item.actionTitle ?: "")
                    put("imageUrl", item.imageUrl ?: "")
                    put("isAd", item.isAd || item.type.equals("AD", true))
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_ITEMS_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * Schedules a match reminder according to the user's customized alert time from Settings.
     */
    fun scheduleMatchReminder(context: Context, match: Match): Boolean {
        LocalNotificationManager.initChannels(context)

        // Ensure battery optimization is ignored so alarms always ring on time
        if (!BatteryOptimizationHelper.isBatteryOptimizationIgnored(context)) {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }

        val settingsPrefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val leadMinutes = settingsPrefs.getInt(KEY_ALERT_TIME, 15)

        val kickoffMillis = parseMatchKickoffMillis(match)
        val reminderMillis = kickoffMillis - (leadMinutes * 60 * 1000L)
        val now = System.currentTimeMillis()

        val notifId = generateNotificationId(match.id)
        val cleanTime = match.startTimeLabel
        val channelStr = if (!match.channel.isNullOrBlank() && !match.channel.equals("null", true)) " على قناة ${match.channel}" else ""
        val commStr = if (!match.commentator.isNullOrBlank() && !match.commentator.equals("null", true)) " بصوت ${match.commentator}" else ""

        val timingLabel = when (leadMinutes) {
            0 -> "عند انطلاق المباراة"
            60 -> "قبل ساعة واحدة"
            else -> "قبل $leadMinutes دقيقة"
        }

        val title = "تذكير بمباراة: ${match.homeTeam} ضد ${match.awayTeam}"
        val message = if (leadMinutes == 0) {
            "تنطلق المباراة الآن (الساعة $cleanTime)${channelStr}${commStr}."
        } else {
            "تنطلق المباراة بعد $leadMinutes دقيقة (الساعة $cleanTime)${channelStr}${commStr}."
        }

        val item = AppNotificationItem(
            id = "reminder_${match.id}",
            title = "${match.homeTeam} ضد ${match.awayTeam}",
            message = "$timingLabel • الساعة $cleanTime ($channelStr)",
            timestamp = System.currentTimeMillis(),
            type = "MATCH_REMINDER",
            isRead = false,
            matchId = match.id,
            matchHomeTeam = match.homeTeam,
            matchAwayTeam = match.awayTeam,
            matchTime = cleanTime,
            matchDate = match.dateCategory,
            matchCompetition = match.competition,
            leadMinutes = leadMinutes
        )

        val currentList = _notificationsFlow.value.filter { it.matchId != match.id }.toMutableList()
        currentList.add(0, item)
        saveNotifications(currentList, context)

        // If reminder time has already arrived, notify immediately
        if (reminderMillis <= now + 60000L) {
            LocalNotificationManager.sendLocalNotification(
                context = context,
                title = "تبدأ المباراة الآن: ${match.homeTeam} ضد ${match.awayTeam}",
                message = "تنطلق المباراة في هذه اللحظات${channelStr}${commStr}. تابع البث الآن!",
                notificationId = notifId
            )
            Toast.makeText(context, "تم ضبط التذكير: تنطلق المباراة قريباً!", Toast.LENGTH_SHORT).show()
            return true
        }

        // Schedule exact AlarmManager offline reminder
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, MatchReminderReceiver::class.java).apply {
                action = MatchReminderReceiver.ACTION_MATCH_REMINDER
                putExtra(MatchReminderReceiver.EXTRA_TITLE, title)
                putExtra(MatchReminderReceiver.EXTRA_MESSAGE, message)
                putExtra(MatchReminderReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            val pendingIntent = PendingIntent.getBroadcast(context, notifId, intent, flags)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent)
                }
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent)
            }
        }

        Toast.makeText(context, "تم ضبط التذكير ($timingLabel)", Toast.LENGTH_LONG).show()
        return true
    }

    /**
     * Cancels an existing scheduled match reminder.
     */
    fun cancelMatchReminder(context: Context, matchId: String) {
        val notifId = generateNotificationId(matchId)
        val intent = Intent(context, MatchReminderReceiver::class.java).apply {
            action = MatchReminderReceiver.ACTION_MATCH_REMINDER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, notifId, intent, flags)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(pendingIntent)

        val updated = _notificationsFlow.value.filter { it.matchId != matchId }
        saveNotifications(updated, context)
        Toast.makeText(context, "تم إلغاء تذكير المباراة", Toast.LENGTH_SHORT).show()
    }

    /**
     * Schedules the next periodic background sync check via AlarmManager.
     * Keeps running even if app is completely closed or swiped from RAM.
     */
    fun scheduleNextBackgroundSync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BroadcastPollingReceiver::class.java).apply {
            action = BroadcastPollingReceiver.ACTION_POLL_BROADCAST
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, 9999, intent, flags)

        val nextTrigger = System.currentTimeMillis() + BACKGROUND_SYNC_INTERVAL_MILLIS
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    /**
     * Starts continuous real-time streaming and high-frequency polling while in-app.
     */
    fun startRealtimeBroadcastListener(context: Context) {
        listeningJob?.cancel()
        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            // Launch periodic fast polling ticker (every 8 seconds while in-app)
            launch {
                while (isActive) {
                    try {
                        pollFirebaseBroadcastSync(context)
                    } catch (_: Exception) {}
                    delay(8000)
                }
            }

            // Launch Server-Sent Events (SSE) stream for sub-second real-time push
            while (isActive) {
                try {
                    val url = "https://digite-bda04-default-rtdb.firebaseio.com/notifications/broadcast.json"
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("Accept", "text/event-stream")
                        .build()

                    httpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val source = response.body?.byteStream()
                            if (source != null) {
                                val reader = BufferedReader(InputStreamReader(source))
                                while (isActive) {
                                    val line = reader.readLine() ?: break
                                    val currentLine = line.trim()
                                    if (currentLine.startsWith("data:")) {
                                        val dataStr = currentLine.removePrefix("data:").trim()
                                        if (dataStr.isNotBlank() && dataStr != "null") {
                                            processBroadcastPayload(context, dataStr)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    delay(5000) // Reconnect on network drop
                }
            }
        }
    }

    fun syncServerBroadcastHistory(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val historyUrl = "https://digite-bda04-default-rtdb.firebaseio.com/notifications/history.json?t=" + System.currentTimeMillis()
                val req = Request.Builder().url(historyUrl).build()
                pollClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        if (body.isNotBlank() && body != "null") {
                            val historyObj = JSONObject(body)
                            val keys = historyObj.keys()
                            val serverItems = mutableListOf<AppNotificationItem>()

                            while (keys.hasNext()) {
                                val key = keys.next()
                                val obj = historyObj.optJSONObject(key) ?: continue
                                val notifId = obj.optString("id", key)
                                val title = obj.optString("title", "")
                                val message = obj.optString("message", "")
                                val rawTs = obj.optString("timestamp", "")
                                val ts = try {
                                    if (rawTs.isNotBlank()) {
                                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(rawTs)?.time ?: System.currentTimeMillis()
                                    } else {
                                        System.currentTimeMillis()
                                    }
                                } catch (_: Throwable) {
                                    System.currentTimeMillis()
                                }

                                if (title.isNotBlank()) {
                                    val typeStr = obj.optString("type", "BROADCAST")
                                    val actionUrl = obj.optString("actionUrl", "").takeIf { it.isNotBlank() }
                                    val actionTitle = obj.optString("actionTitle", "").takeIf { it.isNotBlank() }
                                    val imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() }
                                    val isAd = obj.optBoolean("isAd", false) || typeStr.equals("AD", true)

                                    serverItems.add(
                                        AppNotificationItem(
                                            id = notifId,
                                            title = title,
                                            message = message,
                                            timestamp = ts,
                                            type = typeStr,
                                            isRead = false,
                                            actionUrl = actionUrl,
                                            actionTitle = actionTitle,
                                            imageUrl = imageUrl,
                                            isAd = isAd
                                        )
                                    )
                                }
                            }

                            if (serverItems.isNotEmpty()) {
                                val current = _notificationsFlow.value.toMutableList()
                                for (srv in serverItems) {
                                    val existingIdx = current.indexOfFirst { it.id == srv.id }
                                    if (existingIdx == -1) {
                                        current.add(srv)
                                    }
                                }
                                current.sortByDescending { it.timestamp }
                                saveNotifications(current, context)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun pollFirebaseBroadcastSync(context: Context) {
        try {
            val url = "https://digite-bda04-default-rtdb.firebaseio.com/notifications/broadcast.json?t=" + System.currentTimeMillis()
            val req = Request.Builder().url(url).build()
            pollClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    if (body.isNotBlank() && body != "null") {
                        processBroadcastPayload(context, body)
                    }
                }
            }
        } catch (_: Throwable) {
            // Network timeout / offline: safely ignore without crashing the application
        }
    }

    private fun processBroadcastPayload(context: Context, jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val notifIdStr = json.optString("id", "")
            val title = json.optString("title", "")
            val message = json.optString("message", "")
            val type = json.optString("type", "BROADCAST")
            val actionUrl = json.optString("actionUrl", "").takeIf { it.isNotBlank() }
            val actionTitle = json.optString("actionTitle", "").takeIf { it.isNotBlank() }
            val imageUrl = json.optString("imageUrl", "").takeIf { it.isNotBlank() }
            val isAd = json.optBoolean("isAd", false) || type.equals("AD", true)
            val enabled = json.optBoolean("enabled", true)

            if (enabled && notifIdStr.isNotBlank() && title.isNotBlank()) {
                handleIncomingBroadcast(
                    context = context,
                    id = notifIdStr,
                    title = title,
                    message = message,
                    type = type,
                    actionUrl = actionUrl,
                    actionTitle = actionTitle,
                    imageUrl = imageUrl,
                    isAd = isAd
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Unified handler for incoming broadcast alerts from FCM, SSE, or Background Polling.
     */
    fun handleIncomingBroadcast(
        context: Context,
        id: String,
        title: String,
        message: String,
        type: String = "BROADCAST",
        actionUrl: String? = null,
        actionTitle: String? = null,
        imageUrl: String? = null,
        isAd: Boolean = false
    ) {
        val prefs = getPrefs(context)
        val lastSeen = prefs?.getString(KEY_LAST_BROADCAST_ID, "") ?: ""

        if (lastSeen != id) {
            prefs?.edit()?.putString(KEY_LAST_BROADCAST_ID, id)?.apply()

            // 1. Add to notifications history
            val item = AppNotificationItem(
                id = id,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                type = type,
                isRead = false,
                actionUrl = actionUrl,
                actionTitle = actionTitle,
                imageUrl = imageUrl,
                isAd = isAd
            )
            loadSavedNotifications()
            val currentList = _notificationsFlow.value.toMutableList()
            currentList.removeAll { it.id == id }
            currentList.add(0, item)
            saveNotifications(currentList, context)

            // 2. Raise local system heads-up notification with sound & vibration
            LocalNotificationManager.sendLocalNotification(
                context = context,
                title = title,
                message = message,
                notificationId = Math.abs(id.hashCode() % 50000) + 2000,
                channelId = "beout_broadcast_alerts_v1",
                actionUrl = actionUrl,
                actionTitle = actionTitle,
                imageUrl = imageUrl,
                isAd = isAd
            )
        }
    }

    fun deleteNotification(id: String) {
        val updated = _notificationsFlow.value.filter { it.id != id }
        saveNotifications(updated)
    }

    fun clearAll(context: Context) {
        _notificationsFlow.value.forEach { item ->
            if (item.matchId != null) {
                val notifId = generateNotificationId(item.matchId)
                val intent = Intent(context, MatchReminderReceiver::class.java).apply {
                    action = MatchReminderReceiver.ACTION_MATCH_REMINDER
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                val pendingIntent = PendingIntent.getBroadcast(context, notifId, intent, flags)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.cancel(pendingIntent)
            }
        }
        saveNotifications(emptyList(), context)
        Toast.makeText(context, "تم مسح جميع الإشعارات بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun parseMatchKickoffMillis(match: Match): Long {
        val cal = Calendar.getInstance()
        when (match.dateCategory) {
            "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "yesterday" -> cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val rawTime = match.startTimeLabel.trim()
        val matchResult = Regex("(\\d{1,2}):(\\d{2})").find(rawTime)
        if (matchResult != null) {
            val hour = matchResult.groupValues[1].toIntOrNull() ?: 18
            val minute = matchResult.groupValues[2].toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        return cal.timeInMillis + (30 * 60 * 1000L)
    }

    private fun generateNotificationId(idStr: String): Int {
        return Math.abs(idStr.hashCode() % 50000) + 1000
    }
}
