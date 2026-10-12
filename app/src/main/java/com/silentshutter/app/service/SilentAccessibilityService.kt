package com.silentshutter.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.silentshutter.app.manager.ShutterMuteManager
import java.util.Locale

class SilentAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ShutterMuteManager.setAccessibilityConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                if (packageName != currentForegroundPackage) {
                    currentForegroundPackage = packageName

                    if (ShutterMuteManager.isCameraApp(packageName)) {
                        ShutterMuteManager.onCameraOpened(packageName)
                    } else {
                        ShutterMuteManager.onCameraClosed()
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Intercept shutter button clicks inside default camera apps
                if (ShutterMuteManager.isCameraApp(currentForegroundPackage)) {
                    val desc = (event.contentDescription?.toString() ?: "") + " " + (event.text?.joinToString(" ") ?: "")
                    val viewId = event.source?.viewIdResourceName ?: ""
                    val combined = (desc + " " + viewId).lowercase(Locale.ROOT)

                    if (isShutterElement(combined)) {
                        val label = if (desc.isNotBlank()) desc.trim() else viewId.substringAfterLast("/")
                        ShutterMuteManager.onShutterButtonClicked(label)
                    }
                }
            }
        }
    }

    private fun isShutterElement(text: String): Boolean {
        return text.contains("shutter") ||
                text.contains("シャッター") ||
                text.contains("写真") ||
                text.contains("撮影") ||
                text.contains("take photo") ||
                text.contains("capture") ||
                text.contains("camera_button") ||
                text.contains("snapshot")
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
