package com.silentshutter.app.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val CURRENT_VERSION = "2.1.0"
    private const val GITHUB_API_URL = "https://api.github.com/repos/youyouboydragonOfficial/SilentShutter/releases/latest"

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class Available(val version: String, val title: String, val notes: String, val apkUrl: String) : UpdateState()
        data class UpToDate(val currentVersion: String) : UpdateState()
        data class Downloading(val progress: Float) : UpdateState()
        data class ReadyToInstall(val apkFile: File) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun resetState() {
        _state.value = UpdateState.Idle
    }

    /**
     * Checks GitHub for updates.
     * Guaranteed to take at least 3000ms (3 seconds) to ensure the loading & snow effect displays.
     */
    suspend fun checkForUpdates() {
        _state.value = UpdateState.Checking
        val startTime = System.currentTimeMillis()

        var resultState: UpdateState = try {
            withContext(Dispatchers.IO) {
                val url = URL(GITHUB_API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val tagName = json.optString("tag_name", "").removePrefix("v")
                    val name = json.optString("name", "New Release")
                    val body = json.optString("body", "バグ修正と機能向上")

                    var apkDownloadUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk")) {
                                apkDownloadUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }

                    if (isNewerVersion(tagName, CURRENT_VERSION) && apkDownloadUrl != null) {
                        UpdateState.Available(
                            version = tagName,
                            title = name,
                            notes = body,
                            apkUrl = apkDownloadUrl
                        )
                    } else {
                        UpdateState.UpToDate(CURRENT_VERSION)
                    }
                } else {
                    UpdateState.Error("更新情報の取得に失敗しました (Code: ${conn.responseCode})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            UpdateState.Error("通信エラー: ${e.localizedMessage ?: "接続できませんでした"}")
        }

        // Guarantee at least 3000ms loading duration
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 3000) {
            delay(3000 - elapsed)
        }

        _state.value = resultState
    }

    /**
     * Downloads APK in-app and launches the installer package.
     */
    suspend fun downloadAndInstallApk(context: Context, apkUrl: String) {
        _state.value = UpdateState.Downloading(0f)

        val apkFile = try {
            withContext(Dispatchers.IO) {
                val url = URL(apkUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 10000
                    readTimeout = 15000
                }

                val totalLength = conn.contentLength
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "SilentShutter-update.apk")
                if (file.exists()) file.delete()

                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalLength > 0) {
                                val prog = downloaded.toFloat() / totalLength.toFloat()
                                _state.value = UpdateState.Downloading(prog.coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                file
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            _state.value = UpdateState.Error("ダウンロードに失敗しました: ${e.localizedMessage}")
            return
        }

        _state.value = UpdateState.ReadyToInstall(apkFile)
        installApk(context, apkFile)
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _state.value = UpdateState.Error("インストーラーの起動に失敗しました: ${e.localizedMessage}")
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
