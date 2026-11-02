package com.beouttv.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.beouttv.app.ui.Root

class MainActivity : ComponentActivity() {
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeAskNotificationPermission()
        setContent { Root() }
    }

    /** يُطلب الإذن مرة واحدة فقط (Android 13+). */
    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val sp = getSharedPreferences("core", MODE_PRIVATE)
        if (sp.getBoolean("askedNotif", false)) return
        sp.edit().putBoolean("askedNotif", true).apply()
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
