package com.beouttv.app.data.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.beouttv.app.MainActivity
import com.beouttv.app.data.model.Match
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object LocalNotificationManager {

    private const val CHANNEL_ID_MATCHES = "beout_match_reminders_v3"
    private const val CHANNEL_NAME_MATCHES = "تنبيهات المباريات والبث المباشر"
    private const val CHANNEL_DESC_MATCHES = "إشعارات تذكيرية دقيقة قبل انطلاق المباريات وإشعارات عاجلة من الإدارة"

    private const val CHANNEL_ID_BROADCAST = "beout_broadcast_alerts_v1"
    private const val CHANNEL_NAME_BROADCAST = "إشعارات البث المباشر والإدارة"

    private const val PREFS_NAME = "beout_notifications_prefs"
    private const val KEY_LAST_BROADCAST_ID = "last_seen_broadcast_id"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Match Reminder Channel
            val matchChannel = NotificationChannel(
                CHANNEL_ID_MATCHES,
                CHANNEL_NAME_MATCHES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_MATCHES
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(soundUri, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Broadcast Channel
            val broadcastChannel = NotificationChannel(
                CHANNEL_ID_BROADCAST,
                CHANNEL_NAME_BROADCAST,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إعلانات البث المباشر والتحديثات العامة"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(soundUri, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            manager?.createNotificationChannel(matchChannel)
            manager?.createNotificationChannel(broadcastChannel)
        }
    }

    /**
     * Schedules an exact Alarm to fire 15 minutes (or custom minutes) before match kickoff.
     * Works 100% offline even if the phone is asleep or app is killed.
     */
    fun scheduleMatchReminder(
        context: Context,
        match: Match,
        minutesBefore: Int = 15
    ): Boolean {
        initChannels(context)

        val kickoffMillis = parseMatchKickoffMillis(match)
        val reminderMillis = kickoffMillis - (minutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()

        val notifId = generateNotificationId(match.id)
        val cleanTime = match.startTimeLabel
        val channelStr = if (!match.channel.isNullOrBlank() && !match.channel.equals("null", true)) " على قناة ${match.channel}" else ""
        val commStr = if (!match.commentator.isNullOrBlank() && !match.commentator.equals("null", true)) " بصوت ${match.commentator}" else ""

        val title = "اقترب موعد المباراة: ${match.homeTeam} ضد ${match.awayTeam}"
        val message = "تنطلق المباراة بعد $minutesBefore دقيقة (الساعة $cleanTime)${channelStr}${commStr}."

        // If reminder time has already passed or is within 2 minutes, fire immediately
        if (reminderMillis <= now + 120000L) {
            sendLocalNotification(
                context = context,
                title = "تبدأ المباراة الآن: ${match.homeTeam} ضد ${match.awayTeam}",
                message = "تنطلق المباراة في هذه اللحظات${channelStr}${commStr}. تابع البث الآن!",
                notificationId = notifId
            )
            Toast.makeText(context, "تم ضبط التذكير: تنطلق المباراة قريباً جداً!", Toast.LENGTH_SHORT).show()
            return true
        }

        // Schedule exact alarm via AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Toast.makeText(context, "تعذر الوصول إلى خدمة المنبه", Toast.LENGTH_SHORT).show()
            return false
        }

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
            Toast.makeText(
                context,
                "تم ضبط التذكير: سيصلك إشعار قبل $minutesBefore دقيقة من انطلاق المباراة",
                Toast.LENGTH_LONG
            ).show()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for devices restricting exact alarms
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent)
            Toast.makeText(context, "تم ضبط تذكير المباراة بنجاح", Toast.LENGTH_SHORT).show()
            return true
        }
    }

    /**
     * Cancels any scheduled reminder for a given match.
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
    }

    /**
     * Triggers an immediate high-priority Heads-up Notification.
     */
    fun sendLocalNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt() + 1000,
        channelId: String = CHANNEL_ID_MATCHES,
        actionUrl: String? = null,
        actionTitle: String? = null,
        imageUrl: String? = null,
        isAd: Boolean = false
    ) {
        initChannels(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pendingIntent = if (!actionUrl.isNullOrBlank()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(actionUrl)).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            PendingIntent.getActivity(context, notificationId, browserIntent, flags)
        } else {
            val intent = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            PendingIntent.getActivity(context, notificationId, intent, flags)
        }

        val appIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val displayTitle = if (isAd) "[إعلان] $title" else title

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(appIcon)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (isAd) NotificationCompat.CATEGORY_PROMO else NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!actionUrl.isNullOrBlank()) {
            val actionIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(actionUrl)).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val actionPending = PendingIntent.getActivity(context, notificationId + 7, actionIntent, flags)
            val btnLabel = actionTitle ?: if (isAd) "عرض الإعلان" else "فتح الرابط"
            builder.addAction(android.R.drawable.ic_menu_view, btnLabel, actionPending)
        }

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Checks Firebase Realtime Database (/notifications/broadcast.json) for live online announcements.
     */
    fun checkFirebaseBroadcastNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://digite-bda04-default-rtdb.firebaseio.com/notifications/broadcast.json"
                val req = Request.Builder().url(url).build()
                httpClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        if (body.isNotBlank() && body != "null") {
                            val json = JSONObject(body)
                            val notifIdStr = json.optString("id", "")
                            val title = json.optString("title", "")
                            val message = json.optString("message", "")
                            val enabled = json.optBoolean("enabled", true)

                            if (enabled && notifIdStr.isNotBlank() && title.isNotBlank() && message.isNotBlank()) {
                                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                val lastSeenId = prefs.getString(KEY_LAST_BROADCAST_ID, "")

                                if (lastSeenId != notifIdStr) {
                                    // New broadcast notification from Firebase Admin!
                                    prefs.edit().putString(KEY_LAST_BROADCAST_ID, notifIdStr).apply()
                                    sendLocalNotification(
                                        context = context,
                                        title = title,
                                        message = message,
                                        notificationId = notifIdStr.hashCode(),
                                        channelId = CHANNEL_ID_BROADCAST
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun parseMatchKickoffMillis(match: Match): Long {
        val cal = Calendar.getInstance()

        // 1. Try date category offset
        when (match.dateCategory) {
            "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "yesterday" -> cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // 2. Parse time string (e.g. "18:00" or "06:30" or "18:30:00")
        val rawTime = match.startTimeLabel.trim()
        val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
        val matchResult = timeRegex.find(rawTime)

        if (matchResult != null) {
            val hour = matchResult.groupValues[1].toIntOrNull() ?: 18
            val minute = matchResult.groupValues[2].toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        return cal.timeInMillis + (30 * 60 * 1000L) // Default 30 min ahead
    }

    private fun generateNotificationId(idStr: String): Int {
        return Math.abs(idStr.hashCode() % 50000) + 1000
    }
}
