package com.silentshutter.app.manager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.silentshutter.app.hack.AudioRoutingHijacker
import com.silentshutter.app.hack.ShizukuSystemHacker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShutterMuteManager {

    private const val TAG = "ShutterMuteManager"
    private const val PREFS_NAME = "silentshutter_prefs"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_FLOATING_HUD = "floating_hud_enabled"
    private const val KEY_AUDIO_HIJACK_ENABLED = "audio_hijack_enabled"
    private const val KEY_SHUTTER_TAP_INTERCEPT_ENABLED = "shutter_tap_intercept_enabled"
    private const val KEY_TARGET_PACKAGES = "target_packages"

    // Default target camera packages
    private val DEFAULT_TARGET_PACKAGES = setOf(
        "com.google.android.GoogleCamera",      // Pixel / Google Camera
        "com.sec.android.app.camera",           // Samsung Galaxy Camera
        "com.sonyericsson.android.camera",      // Sony Xperia Camera
        "com.sony.camera",                      // Sony Photography Pro
        "com.motorola.camera",                  // Motorola Camera
        "com.motorola.camera2",
        "com.motorola.camera3",
        "com.android.camera",                   // Xiaomi / AOSP Camera
        "com.oppo.camera",                      // OPPO / Realme Camera
        "com.oneplus.camera",                   // OnePlus Camera
        "jp.co.sharp.android.camera",           // AQUOS Camera
        "com.asus.camera",                      // ASUS Camera
        "net.sourceforge.opencamera",          // Open Camera
        "jp.naver.line.android",                // LINE
        "com.instagram.android",                // Instagram
        "com.zhiliaoapp.musically",             // TikTok
        "com.ss.android.ugc.trill"
    )

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    // Observable UI States
    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _isAudioHijackEnabled = MutableStateFlow(true)
    val isAudioHijackEnabled: StateFlow<Boolean> = _isAudioHijackEnabled.asStateFlow()

    private val _isShutterTapInterceptEnabled = MutableStateFlow(true)
    val isShutterTapInterceptEnabled: StateFlow<Boolean> = _isShutterTapInterceptEnabled.asStateFlow()

    private val _isCurrentlyMuted = MutableStateFlow(false)
    val isCurrentlyMuted: StateFlow<Boolean> = _isCurrentlyMuted.asStateFlow()

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    private val _activeCameraPackage = MutableStateFlow<String?>(null)
    val activeCameraPackage: StateFlow<String?> = _activeCameraPackage.asStateFlow()

    private val _detectedDefaultCamera = MutableStateFlow<String>("com.google.android.GoogleCamera")
    val detectedDefaultCamera: StateFlow<String> = _detectedDefaultCamera.asStateFlow()

    private val _isFloatingHudEnabled = MutableStateFlow(false)
    val isFloatingHudEnabled: StateFlow<Boolean> = _isFloatingHudEnabled.asStateFlow()

    private val _targetPackages = MutableStateFlow<Set<String>>(DEFAULT_TARGET_PACKAGES)
    val targetPackages: StateFlow<Set<String>> = _targetPackages.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: String,
        val message: String,
        val isMuteEvent: Boolean
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        _isMasterEnabled.value = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        _isAudioHijackEnabled.value = prefs.getBoolean(KEY_AUDIO_HIJACK_ENABLED, true)
        _isShutterTapInterceptEnabled.value = prefs.getBoolean(KEY_SHUTTER_TAP_INTERCEPT_ENABLED, true)
        _isFloatingHudEnabled.value = prefs.getBoolean(KEY_FLOATING_HUD, false)

        val savedTargets = prefs.getStringSet(KEY_TARGET_PACKAGES, null)
        if (savedTargets != null) {
            _targetPackages.value = savedTargets.toSet()
        }

        // Initialize sub-engines
        AudioRoutingHijacker.init(appContext)
        ShizukuSystemHacker.init(appContext)

        // Detect default camera
        detectDefaultCameraPackage()

        addLog("SYSTEM_INIT", "Default Camera Hacker Engine armed", false)
    }

    private fun detectDefaultCameraPackage() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolveInfo = appContext.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val pkg = resolveInfo?.activityInfo?.packageName
            if (pkg != null && pkg != appContext.packageName) {
                _detectedDefaultCamera.value = pkg
                addLog("TARGET_DETECT", "Default camera detected: $pkg", false)
                return
            }

            // Fallback: check installed targets
            for (p in DEFAULT_TARGET_PACKAGES) {
                try {
                    appContext.packageManager.getPackageInfo(p, 0)
                    _detectedDefaultCamera.value = p
                    addLog("TARGET_DETECT", "Detected installed camera: $p", false)
                    return
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect default camera: ${e.message}")
        }
    }

    fun launchDefaultCamera(): Boolean {
        return try {
            val defaultPkg = _detectedDefaultCamera.value
            val intent = appContext.packageManager.getLaunchIntentForPackage(defaultPkg)
                ?: Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            addLog("LAUNCH_CAMERA", "Launched default camera: $defaultPkg", true)
            true
        } catch (e: Exception) {
            addLog("LAUNCH_FAIL", "Failed to launch: ${e.message}", false)
            false
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _isMasterEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        addLog("MASTER_SWITCH", if (enabled) "Hacker Engine ARMED" else "Hacker Engine STANDBY", false)

        if (!enabled && _isCurrentlyMuted.value) {
            restoreAudioState()
        }
    }

    fun setAudioHijackEnabled(enabled: Boolean) {
        _isAudioHijackEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUDIO_HIJACK_ENABLED, enabled).apply()
        addLog("AUDIO_HIJACK", if (enabled) "Audio Route Hijacker ENABLED" else "Audio Route Hijacker DISABLED", false)
    }

    fun setShutterTapInterceptEnabled(enabled: Boolean) {
        _isShutterTapInterceptEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SHUTTER_TAP_INTERCEPT_ENABLED, enabled).apply()
    }

    fun setFloatingHudEnabled(enabled: Boolean) {
        _isFloatingHudEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FLOATING_HUD, enabled).apply()
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityServiceConnected.value = connected
        addLog("SERVICE", if (connected) "Accessibility Interceptor CONNECTED" else "Accessibility Interceptor DISCONNECTED", false)
    }

    fun isCameraApp(packageName: String?): Boolean {
        if (packageName == null) return false
        if (packageName == appContext.packageName) return false
        val targets = _targetPackages.value
        if (targets.contains(packageName)) return true
        val lower = packageName.lowercase(Locale.ROOT)
        return lower.contains("camera") || lower.contains("camscanner")
    }

    /**
     * Triggered when camera app comes to foreground.
     * Executes the Audio Routing Hijack to suppress the shutter sound.
     */
    @Synchronized
    fun onCameraOpened(packageName: String) {
        if (!_isMasterEnabled.value) return
        if (_isCurrentlyMuted.value && _activeCameraPackage.value == packageName) return

        _activeCameraPackage.value = packageName
        saveAndMuteAudioState()
        addLog("CAMERA_HIJACK", "App: $packageName -> ROUTE HIJACKED", true)
    }

    /**
     * Triggered when user taps the shutter button inside the camera app.
     * Emits a millisecond high-priority mute pulse.
     */
    fun onShutterButtonClicked(viewDesc: String) {
        if (!_isMasterEnabled.value || !_isShutterTapInterceptEnabled.value) return
        AudioRoutingHijacker.triggerShutterPulse(800)
        _isCurrentlyMuted.value = true
        addLog("SHUTTER_PULSE", "Shutter Click Intercepted [$viewDesc] -> PULSE MUTE", true)
    }

    /**
     * Triggered when camera app is closed.
     */
    @Synchronized
    fun onCameraClosed() {
        if (!_isCurrentlyMuted.value && _activeCameraPackage.value == null) return

        val prevApp = _activeCameraPackage.value ?: "unknown"
        _activeCameraPackage.value = null
        restoreAudioState()
        addLog("CAMERA_EXIT", "Exited: $prevApp -> RESTORED", false)
    }

    /**
     * Manual Instant Mute toggle
     */
    @Synchronized
    fun toggleInstantMute() {
        if (_isCurrentlyMuted.value) {
            restoreAudioState()
            addLog("MANUAL_RESTORE", "Audio state restored", false)
        } else {
            saveAndMuteAudioState()
            addLog("MANUAL_HIJACK", "Manual Audio Hijack activated", true)
        }
    }

    private fun saveAndMuteAudioState() {
        try {
            if (_isAudioHijackEnabled.value) {
                AudioRoutingHijacker.hijackAudioRouting()
            }
            _isCurrentlyMuted.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply mute: ${e.message}", e)
            addLog("ERROR", "Hijack failed: ${e.message}", false)
        }
    }

    private fun restoreAudioState() {
        try {
            AudioRoutingHijacker.restoreAudioRouting()
            _isCurrentlyMuted.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio: ${e.message}", e)
            addLog("ERROR", "Restore failed: ${e.message}", false)
        }
    }

    fun hasDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    fun addTargetPackage(pkg: String) {
        val updated = _targetPackages.value.toMutableSet().apply { add(pkg) }
        _targetPackages.value = updated
        prefs.edit().putStringSet(KEY_TARGET_PACKAGES, updated).apply()
        addLog("TARGET_ADDED", "Added: $pkg", false)
    }

    fun removeTargetPackage(pkg: String) {
        val updated = _targetPackages.value.toMutableSet().apply { remove(pkg) }
        _targetPackages.value = updated
        prefs.edit().putStringSet(KEY_TARGET_PACKAGES, updated).apply()
        addLog("TARGET_REMOVED", "Removed: $pkg", false)
    }

    fun addLog(tag: String, message: String, isMute: Boolean) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val entry = LogEntry("[$time][$tag]", message, isMute)
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) {
            _logs.value = current.take(50)
        } else {
            _logs.value = current
        }
    }
}
