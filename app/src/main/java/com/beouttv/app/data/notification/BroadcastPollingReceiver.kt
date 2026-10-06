package com.beouttv.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BroadcastPollingReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_POLL_BROADCAST = "com.beouttv.app.ACTION_POLL_BROADCAST"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationsManager.pollFirebaseBroadcastSync(context)
            } catch (_: Throwable) {
                // Ignore any background polling error
            } finally {
                // Schedule next periodic background check
                NotificationsManager.scheduleNextBackgroundSync(context)
                pendingResult.finish()
            }
        }
    }
}
