package com.silentshutter.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.silentshutter.app.manager.ShutterMuteManager

class SilentAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ShutterMuteManager.setAccessibilityConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Avoid processing our own app state changes
            if (packageName == packageName) {
                // If switching within SilentShutter, ignore
            }

            if (packageName != currentForegroundPackage) {
                currentForegroundPackage = packageName

                if (ShutterMuteManager.isCameraApp(packageName)) {
                    ShutterMuteManager.onCameraOpened(packageName)
                } else {
                    ShutterMuteManager.onCameraClosed()
                }
            }
        }
    }

    override fun onInterrupt() {
        ShutterMuteManager.onCameraClosed()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ShutterMuteManager.setAccessibilityConnected(false)
        ShutterMuteManager.onCameraClosed()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ShutterMuteManager.setAccessibilityConnected(false)
        ShutterMuteManager.onCameraClosed()
        super.onDestroy()
    }
}
