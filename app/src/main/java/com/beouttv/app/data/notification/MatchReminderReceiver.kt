package com.beouttv.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MatchReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MATCH_REMINDER = "com.beouttv.app.ACTION_MATCH_REMINDER"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == ACTION_MATCH_REMINDER) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير بموعد المباراة"
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "تنطلق المباراة بعد قليل، تابع البث الآن!"
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (System.currentTimeMillis() % 10000).toInt() + 1000)

            LocalNotificationManager.sendLocalNotification(
                context = context,
                title = title,
                message = message,
                notificationId = notifId
            )
        } else if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            NotificationsManager.init(context)
        }
    }
}
