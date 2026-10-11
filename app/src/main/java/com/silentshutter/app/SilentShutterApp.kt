package com.silentshutter.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.silentshutter.app.manager.ShutterMuteManager

class SilentShutterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ShutterMuteManager.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                "SilentShutter HUD Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "フローティングHUDの常駐用通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_OVERLAY = "silentshutter_overlay_channel"
    }
}
