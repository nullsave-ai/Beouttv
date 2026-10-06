package com.beouttv.app.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BeoutFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Automatically subscribe device to all_users broadcast topic
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "تنبيه مباشر من BeoutTV"

        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "تابع البث المباشر وأحدث التحديثات الآن."

        val notifIdStr = remoteMessage.data["id"] ?: "fcm_${System.currentTimeMillis()}"
        val type = remoteMessage.data["type"] ?: "BROADCAST"
        val actionUrl = remoteMessage.data["actionUrl"]?.takeIf { it.isNotBlank() }
        val actionTitle = remoteMessage.data["actionTitle"]?.takeIf { it.isNotBlank() }
        val imageUrl = remoteMessage.data["imageUrl"]?.takeIf { it.isNotBlank() }
        val isAd = remoteMessage.data["isAd"]?.toBooleanStrictOrNull() ?: type.equals("AD", true)

        // Process through NotificationsManager to update UI list & counter
        NotificationsManager.handleIncomingBroadcast(
            context = applicationContext,
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
}
