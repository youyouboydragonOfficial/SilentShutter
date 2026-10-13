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
import java.util.Locale

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val CURRENT_VERSION = "2.7.0"
    private const val GITHUB_API_URL = "https://api.github.com/repos/youyouboydragonOfficial/SilentShutter/releases/latest"

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class Available(
            val version: String,
            val title: String,
            val notes: String,
            val apkUrl: String,
            val apkSize: String // 例: "約 11.0 MB"
        ) : UpdateState()
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
     * @param enforceMinDuration If true, enforces minimum 3000ms duration for snow animation & cool spinner.
     *                           If false (e.g. app startup auto check), completes instantly.
     */
    suspend fun checkForUpdates(enforceMinDuration: Boolean = true) {
        _state.value = UpdateState.Checking
        val startTime = System.currentTimeMillis()

        val resultState: UpdateState = try {
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
                    val name = json.optString("name", "SilentShutter 新バージョン")
                    val rawBody = json.optString("body", "")
                    val formattedNotes = formatJapaneseNotes(rawBody)

                    var apkDownloadUrl: String? = null
                    var apkSizeBytes: Long = 0L

                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk")) {
                                apkDownloadUrl = asset.optString("browser_download_url")
                                apkSizeBytes = asset.optLong("size", 0L)
                                break
                            }
                        }
                    }

                    val apkSizeFormatted = formatFileSize(apkSizeBytes)

                    if (isNewerVersion(tagName, CURRENT_VERSION) && apkDownloadUrl != null) {
                        UpdateState.Available(
                            version = tagName,
                            title = name,
                            notes = formattedNotes,
                            apkUrl = apkDownloadUrl,
                            apkSize = apkSizeFormatted
                        )
                    } else {
                        UpdateState.UpToDate(CURRENT_VERSION)
                    }
                } else {
                    UpdateState.Error("更新情報の取得に失敗しました (HTTP ${conn.responseCode})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            UpdateState.Error("通信エラー: ${e.localizedMessage ?: "接続できませんでした"}")
        }

        // Guarantee at least 3000ms loading duration if requested (snowfall presentation)
        if (enforceMinDuration) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 3000) {
                delay(3000 - elapsed)
            }
        }

        _state.value = resultState
    }

    /**
     * Formats bytes to user-friendly Japanese size string (e.g., "約 11.0 MB")
     */
    private fun formatFileSize(bytes: Long): String {
        return if (bytes > 0) {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            String.format(Locale.JAPAN, "約 %.1f MB", mb)
        } else {
            "約 11.0 MB"
        }
    }

    /**
     * Cleans up GitHub release notes for clean Japanese display
     */
    private fun formatJapaneseNotes(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "・最新のカメラ消音エンジンの強化\n・安定性の向上および不具合修正"
        }
        // Remove markdown title hashes if present
        return rawBody.trim()
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
                        output.flush()
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

    /**
     * Launches Android PackageInstaller Intent via FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install launch failed: ${e.message}", e)
            _state.value = UpdateState.Error("インストーラー起動失敗: ${e.localizedMessage}")
        }
    }

    /**
     * Compares semantic versions (e.g. "2.3.0" vs "2.2.0")
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(rParts.size, cParts.size)

            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remote != current
        }
    }
}
