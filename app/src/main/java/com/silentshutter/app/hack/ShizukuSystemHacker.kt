package com.silentshutter.app.hack

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuSystemHacker {

    private const val TAG = "ShizukuSystemHacker"
    const val REQUEST_CODE_SHIZUKU = 1001

    private lateinit var appContext: Context

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    private val _hasSecureSettingsPermission = MutableStateFlow(false)
    val hasSecureSettingsPermission: StateFlow<Boolean> = _hasSecureSettingsPermission.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuAvailable.value = false
        _hasShizukuPermission.value = false
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                _hasShizukuPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
                if (_hasShizukuPermission.value) {
                    injectAllSystemHacks()
                }
            }
        }

    fun init(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        checkShizukuState()
    }

    fun checkShizukuState() {
        val ping = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        _isShizukuAvailable.value = ping

        if (ping) {
            val perm = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
            _hasShizukuPermission.value = perm
        } else {
            _hasShizukuPermission.value = false
        }

        checkSecureSettingsPermission()
    }

    fun requestShizukuPermission() {
        if (_isShizukuAvailable.value && !_hasShizukuPermission.value) {
            try {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
            }
        }
    }

    fun checkSecureSettingsPermission(): Boolean {
        val granted = appContext.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        _hasSecureSettingsPermission.value = granted
        return granted
    }

    /**
     * Executes rootless ADB commands directly through Shizuku service.
     */
    fun executeShizukuCommand(cmd: String): Pair<Boolean, String> {
        if (!_isShizukuAvailable.value || !_hasShizukuPermission.value) {
            return Pair(false, "Shizuku service not available or permission denied")
        }

        return try {
            // Execute via Shizuku newProcess via reflection to handle visibility
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            val success = (exitCode == 0)
            Pair(success, output.toString().trim())
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku exec failed: ${e.message}", e)
            Pair(false, e.message ?: "Execution error")
        }
    }

    /**
     * Injects all known camera shutter disabling system settings:
     * - Samsung CSC key
     * - AOSP / OEM camera sound keys
     * - Self-grants WRITE_SECURE_SETTINGS to SilentShutter app
     */
    fun injectAllSystemHacks(): List<String> {
        val results = mutableListOf<String>()

        if (_hasShizukuPermission.value) {
            // 1. Grant WRITE_SECURE_SETTINGS to self
            val (grantOk, _) = executeShizukuCommand("pm grant ${appContext.packageName} android.permission.WRITE_SECURE_SETTINGS")
            results.add("Self-grant WRITE_SECURE_SETTINGS: " + if (grantOk) "SUCCESS" else "FAILED")

            // 2. Samsung Galaxy CSC Shutter Sound disable
            val (cscOk, _) = executeShizukuCommand("settings put system csc_pref_camera_forced_shuttersound_key 0")
            results.add("Galaxy CSC camera shutter mute: " + if (cscOk) "APPLIED (0)" else "FAILED")

            // 3. OEM generic camera sound disables
            executeShizukuCommand("settings put system camera_sound 0")
            executeShizukuCommand("settings put system sound_effects_enabled 0")
            results.add("OEM camera sound keys: APPLIED")

            checkSecureSettingsPermission()
        } else if (_hasSecureSettingsPermission.value) {
            // Apply directly if WRITE_SECURE_SETTINGS was granted via manual ADB
            try {
                Settings.System.putString(appContext.contentResolver, "csc_pref_camera_forced_shuttersound_key", "0")
                Settings.System.putInt(appContext.contentResolver, "camera_sound", 0)
                results.add("Direct System Settings write: APPLIED")
            } catch (e: Exception) {
                results.add("Direct System write error: ${e.message}")
            }
        } else {
            results.add("Neither Shizuku nor WRITE_SECURE_SETTINGS is active")
        }

        return results
    }

    fun getAdbCommandGuide(): String {
        return "adb shell pm grant ${appContext.packageName} android.permission.WRITE_SECURE_SETTINGS\n" +
                "adb shell settings put system csc_pref_camera_forced_shuttersound_key 0"
    }
}
