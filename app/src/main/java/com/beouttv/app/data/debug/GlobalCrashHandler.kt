package com.beouttv.app.data.debug

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object GlobalCrashHandler : Thread.UncaughtExceptionHandler {

    const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    const val EXTRA_STACK_TRACE = "extra_stack_trace"
    const val EXTRA_THREAD_NAME = "extra_thread_name"
    const val EXTRA_DEVICE_INFO = "extra_device_info"
    const val EXTRA_TIMESTAMP = "extra_timestamp"

    private var application: Application? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(app: Application) {
        application = app
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val app = application
        if (app != null) {
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val errorMessage = throwable.localizedMessage ?: throwable.message ?: throwable.javaClass.simpleName
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                val deviceInfo = buildString {
                    appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                    appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("App Version: 1.0 (Build 1)")
                    appendLine("Crash Timestamp: $timestamp")
                    appendLine("Crashing Thread: ${thread.name} (ID: ${thread.id})")
                    appendLine("Exception Type: ${throwable.javaClass.name}")
                }

                val intent = Intent(app, Class.forName("com.beouttv.app.ui.screens.debug.CrashActivity")).apply {
                    putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
                    putExtra(EXTRA_STACK_TRACE, stackTrace)
                    putExtra(EXTRA_THREAD_NAME, thread.name)
                    putExtra(EXTRA_DEVICE_INFO, deviceInfo)
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                app.startActivity(intent)
                Process.killProcess(Process.myPid())
                System.exit(10)
                return
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }
}
