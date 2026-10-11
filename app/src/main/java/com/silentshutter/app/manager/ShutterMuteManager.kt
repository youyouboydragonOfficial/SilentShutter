package com.silentshutter.app.manager

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
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
        "jp.naver.line.android",                // LINE (In-app camera)
        "com.instagram.android",                // Instagram
        "com.zhiliaoapp.musically",             // TikTok
        "com.ss.android.ugc.trill"              // TikTok Global
    )

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    // UI Observable States
    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _isCurrentlyMuted = MutableStateFlow(false)
    val isCurrentlyMuted: StateFlow<Boolean> = _isCurrentlyMuted.asStateFlow()

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    private val _activeCameraPackage = MutableStateFlow<String?>(null)
    val activeCameraPackage: StateFlow<String?> = _activeCameraPackage.asStateFlow()

    private val _isFloatingHudEnabled = MutableStateFlow(false)
    val isFloatingHudEnabled: StateFlow<Boolean> = _isFloatingHudEnabled.asStateFlow()

    private val _targetPackages = MutableStateFlow<Set<String>>(DEFAULT_TARGET_PACKAGES)
    val targetPackages: StateFlow<Set<String>> = _targetPackages.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Backup states for restoration
    private var savedRingerMode: Int? = null
    private var savedSystemVolume: Int? = null
    private var savedNotificationVolume: Int? = null
    private var savedInterruptionFilter: Int? = null
    private var audioFocusRequest: AudioFocusRequest? = null

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
        _isFloatingHudEnabled.value = prefs.getBoolean(KEY_FLOATING_HUD, false)
        val savedTargets = prefs.getStringSet(KEY_TARGET_PACKAGES, null)
        if (savedTargets != null) {
            _targetPackages.value = savedTargets.toSet()
        }

        addLog("SYSTEM_INIT", "SilentShutter Engine initialized", false)
    }

    fun setMasterEnabled(enabled: Boolean) {
        _isMasterEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        addLog("MASTER_SWITCH", if (enabled) "Mute Engine ARMED" else "Mute Engine STANDBY (OFF)", false)

        if (!enabled && _isCurrentlyMuted.value) {
            restoreAudioState()
        }
    }

    fun setFloatingHudEnabled(enabled: Boolean) {
        _isFloatingHudEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FLOATING_HUD, enabled).apply()
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityServiceConnected.value = connected
        addLog("SERVICE", if (connected) "Accessibility Service CONNECTED" else "Accessibility Service DISCONNECTED", false)
    }

    fun isCameraApp(packageName: String?): Boolean {
        if (packageName == null) return false
        val targets = _targetPackages.value
        if (targets.contains(packageName)) return true
        // Heuristic detection for any custom camera app
        val lower = packageName.lowercase(Locale.ROOT)
        return lower.contains("camera") || lower.contains("camscanner")
    }

    /**
     * Triggered when camera app comes to foreground
     */
    @Synchronized
    fun onCameraOpened(packageName: String) {
        if (!_isMasterEnabled.value) return
        if (_isCurrentlyMuted.value && _activeCameraPackage.value == packageName) return

        _activeCameraPackage.value = packageName
        saveAndMuteAudioState()
        addLog("CAMERA_DETECTED", "App: $packageName -> MUTED", true)
    }

    /**
     * Triggered when camera app goes to background / user exits camera
     */
    @Synchronized
    fun onCameraClosed() {
        if (!_isCurrentlyMuted.value && _activeCameraPackage.value == null) return

        val prevApp = _activeCameraPackage.value ?: "unknown"
        _activeCameraPackage.value = null
        restoreAudioState()
        addLog("CAMERA_CLOSED", "Exited: $prevApp -> RESTORED", false)
    }

    /**
     * Manual Instant Mute toggle (for quick testing or manual HUD trigger)
     */
    @Synchronized
    fun toggleInstantMute() {
        if (_isCurrentlyMuted.value) {
            restoreAudioState()
            addLog("MANUAL_UNMUTE", "Manually restored audio state", false)
        } else {
            saveAndMuteAudioState()
            addLog("MANUAL_MUTE", "Manually activated silent mute", true)
        }
    }

    private fun saveAndMuteAudioState() {
        try {
            // 1. Save current volume and ringer mode
            savedRingerMode = audioManager.ringerMode
            savedSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            savedNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

            // 2. Mute system and notification stream
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)

            // 3. Switch to silent mode or DND if permission granted
            if (hasDndPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    savedInterruptionFilter = notificationManager.currentInterruptionFilter
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }

            // 4. Request transient audio focus to suppress shutter stream on supported devices
            requestTransientAudioFocus()

            _isCurrentlyMuted.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply mute: ${e.message}", e)
            addLog("ERROR", "Mute failed: ${e.message}", false)
        }
    }

    private fun restoreAudioState() {
        try {
            // 1. Abandon transient audio focus
            abandonTransientAudioFocus()

            // 2. Restore DND filter
            if (hasDndPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && savedInterruptionFilter != null) {
                    notificationManager.setInterruptionFilter(savedInterruptionFilter!!)
                }
                savedRingerMode?.let {
                    audioManager.ringerMode = it
                }
            }

            // 3. Restore system and notification volume
            savedSystemVolume?.let {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, it.coerceIn(0, max), 0)
            }
            savedNotificationVolume?.let {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it.coerceIn(0, max), 0)
            }

            // Clear saved state
            savedRingerMode = null
            savedSystemVolume = null
            savedNotificationVolume = null
            savedInterruptionFilter = null

            _isCurrentlyMuted.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio: ${e.message}", e)
            addLog("ERROR", "Restore failed: ${e.message}", false)
        }
    }

    private fun requestTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioManager.requestAudioFocus(audioFocusRequest!!)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request skipped: ${e.message}")
        }
    }

    private fun abandonTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon skipped: ${e.message}")
        }
    }

    fun hasDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * Samsung Galaxy Direct CSC key mute (Works if WRITE_SECURE_SETTINGS is granted via ADB)
     */
    fun toggleSamsungCscShutterSound(disableSound: Boolean): Boolean {
        return try {
            val value = if (disableSound) "0" else "1"
            Settings.System.putString(
                appContext.contentResolver,
                "csc_pref_camera_forced_shuttersound_key",
                value
            )
            addLog("SAMSUNG_CSC", "CSC forced shutter set to $value", disableSound)
            true
        } catch (e: Exception) {
            Log.w(TAG, "CSC direct setting failed: ${e.message}")
            addLog("SAMSUNG_CSC_FAIL", "ADB permission WRITE_SECURE_SETTINGS required", false)
            false
        }
    }

    fun isSamsungCscMuted(): Boolean {
        return try {
            val value = Settings.System.getString(
                appContext.contentResolver,
                "csc_pref_camera_forced_shuttersound_key"
            )
            value == "0"
        } catch (e: Exception) {
            false
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

    fun resetDefaultTargets() {
        _targetPackages.value = DEFAULT_TARGET_PACKAGES
        prefs.edit().putStringSet(KEY_TARGET_PACKAGES, DEFAULT_TARGET_PACKAGES).apply()
        addLog("TARGET_RESET", "Reset to default camera targets", false)
    }

    private fun addLog(tag: String, message: String, isMute: Boolean) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val entry = LogEntry("[$time][$tag]", message, isMute)
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 40) {
            _logs.value = current.take(40)
        } else {
            _logs.value = current
        }
    }
}
