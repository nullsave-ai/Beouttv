package com.beouttv.app

import android.app.Application
import com.beouttv.app.data.config.AppGatewayManager
import com.beouttv.app.data.debug.GlobalCrashHandler
import com.beouttv.app.data.repository.ServiceLocator

class BeoutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.init(this)
        try {
            com.beouttv.app.data.source.MediaSourceManager.init(this)
            com.beouttv.app.data.ads.AdsterraManager.init(this)
            AppGatewayManager.init(this)
            com.beouttv.app.data.config.PlayerPreferences.let { prefs ->
                com.beouttv.app.data.filter.ContentFilterEngine.isAdultFilterEnabled = prefs.isAdultFilterEnabled(this)
            }
            ServiceLocator.init(this)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}

