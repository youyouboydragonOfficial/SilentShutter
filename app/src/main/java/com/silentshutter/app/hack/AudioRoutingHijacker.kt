package com.silentshutter.app.hack

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.reflect.Method

/**
 * Supercharged Hardware Audio Routing Hijacker Engine (v2.2.0+)
 *
 * 【究極の消音メカニズム】
 * 1. STREAM_SYSTEM_ENFORCED (Stream 7: 日本・韓国キャリアのカメラ強制シャッター音ストリーム) を直接ミュート & 音量0
 * 2. ハードウェア・通話ロック (AudioRecord VOICE_COMMUNICATION):
 *    通話中・音声入力中としてハードウェアを排他占有し、Androidのスピーカー保護機構により強制音のスピーカー再生を完全遮断
 * 3. AudioManager を MODE_IN_COMMUNICATION に固定し、スピーカーフォンを完全OFF (isSpeakerphoneOn = false)
 * 4. 受話口 (TYPE_BUILTIN_EARPIECE) への完全オーディオルーティング
 * 5. 無音 PCM AudioTrack による AudioFlinger の排他ストリームループ
 * 6. 30ms 超高速ガードループによる音量・モードの再奪取の完全阻止
 * 7. Galaxy (Samsung) CSC キーの書き換え (csc_pref_camera_forced_shuttersound_key = 0)
 */
object AudioRoutingHijacker {

    private const val TAG = "AudioRoutingHijacker"

    // Hidden / Enforced Android Audio Streams
    const val STREAM_VOICE_CALL = 0
    const val STREAM_SYSTEM = 1
    const val STREAM_RING = 2
    const val STREAM_MUSIC = 3
    const val STREAM_ALARM = 4
    const val STREAM_NOTIFICATION = 5
    const val STREAM_BLUETOOTH_SCO = 6
    const val STREAM_SYSTEM_ENFORCED = 7   // ★ 日本/韓国の強制シャッター音ストリーム！
    const val STREAM_DTMF = 8
    const val STREAM_TTS = 9
    const val STREAM_ACCESSIBILITY = 10

    private val ALL_AUDIO_STREAMS = intArrayOf(
        STREAM_VOICE_CALL,
        STREAM_SYSTEM,
        STREAM_RING,
        STREAM_MUSIC,
        STREAM_ALARM,
        STREAM_NOTIFICATION,
        STREAM_BLUETOOTH_SCO,
        STREAM_SYSTEM_ENFORCED,
        STREAM_DTMF,
        STREAM_TTS,
        STREAM_ACCESSIBILITY
    )

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    private var silentTrack: AudioTrack? = null
    private var silentRecorder: AudioRecord? = null
    private var isCurrentlyHijacked = false
    private var volumeGuardJob: Job? = null
    private var silentTrackJob: Job? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Saved state for clean restoration
    private var savedAudioMode: Int? = null
    private var savedRingerMode: Int? = null
    private var savedMicMute: Boolean? = null
    private var savedSpeakerphone: Boolean? = null
    private var savedInterruptionFilter: Int? = null
    private val savedVolumes = mutableMapOf<Int, Int>()

    // Reflection cache
    private var setStreamMuteMethod: Method? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            setStreamMuteMethod = AudioManager::class.java.getMethod(
                "setStreamMute",
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
        } catch (_: Exception) {}

        // Pre-attempt system setting injection
        tryInjectSystemSettings()
    }

    /**
     * Complete Audio Routing Hijack:
     * 全ての消音・ハイジャック機構を同時に発動！
     */
    @Synchronized
    fun hijackAudioRouting(): Boolean {
        if (isCurrentlyHijacked) return true

        return try {
            // 1. 全ストリーム音量の退避
            savedVolumes.clear()
            for (stream in ALL_AUDIO_STREAMS) {
                try {
                    savedVolumes[stream] = audioManager.getStreamVolume(stream)
                } catch (_: Exception) {}
            }

            // 2. システム状態の退避
            savedAudioMode = audioManager.mode
            savedRingerMode = audioManager.ringerMode
            savedMicMute = audioManager.isMicrophoneMute
            savedSpeakerphone = audioManager.isSpeakerphoneOn

            // 3. 全ストリーム (Stream 7 STREAM_SYSTEM_ENFORCED を含む) を完全ミュート
            muteAllStreams()

            // 4. 通話通信モードへ強制移行 (スピーカー強制遮断)
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = true
            audioManager.isSpeakerphoneOn = false

            // 5. 受話口 (耳元極小スピーカー) へのルーティング
            routeToEarpiece()

            // 6. DND 完全サイレントの適用
            applyTotalSilenceDnd()

            // 7. Galaxy CSC 設定等の注入
            tryInjectSystemSettings()

            // 8. ハードウェア・通話ロック (マイク排他占有) を開始
            startHardwareMicLock()

            // 9. 無音 AudioTrack による AudioFlinger 排他ループを開始
            startContinuousSilentTrack()

            isCurrentlyHijacked = true

            // 10. 30ms 超高速ガードループ開始 (カメラアプリの割り込みを即時再ハイジャック)
            startVolumeGuard()

            Log.d(TAG, "Audio routing successfully hijacked into VOICE_COMMUNICATION + Stream 7 Muted + Mic Lock")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Audio hijack failed: ${e.message}", e)
            false
        }
    }

    /**
     * Mutes every audio stream, explicitly targeting STREAM_SYSTEM_ENFORCED (7).
     */
    fun muteAllStreams() {
        for (stream in ALL_AUDIO_STREAMS) {
            try {
                audioManager.setStreamVolume(stream, 0, 0)
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                setStreamMuteMethod?.invoke(audioManager, stream, true)
            } catch (_: Exception) {}
        }
    }

    private fun routeToEarpiece() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val availableDevices = audioManager.availableCommunicationDevices
                val earpiece = availableDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                if (earpiece != null) {
                    audioManager.setCommunicationDevice(earpiece)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Earpiece route failed: ${e.message}")
            }
        }
    }

    private fun applyTotalSilenceDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            try {
                if (savedInterruptionFilter == null) {
                    savedInterruptionFilter = notificationManager.currentInterruptionFilter
                }
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (_: Exception) {}
        }
    }

    /**
     * Injects camera sound suppression keys into system settings.
     */
    fun tryInjectSystemSettings() {
        try {
            val resolver = appContext.contentResolver
            Settings.System.putString(resolver, "csc_pref_camera_forced_shuttersound_key", "0")
            Settings.System.putInt(resolver, "camera_sound", 0)
            Settings.System.putInt(resolver, "sound_effects_enabled", 0)
        } catch (_: Exception) {
            // Requires WRITE_SETTINGS or WRITE_SECURE_SETTINGS; silent catch
        }
    }

    /**
     * 🎙️ Hardware Mic Lock (VOICE_COMMUNICATION)
     * Android OS に通話中・音声通話アプリとして認識させ、ハードウェア Audio HAL の
     * スピーカー保護仕様により強制シャッター音のスピーカー出力を根底から遮断する。
     */
    @SuppressLint("MissingPermission")
    private fun startHardwareMicLock() {
        stopHardwareMicLock()
        try {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
                silentRecorder = recorder

                recordJob?.cancel()
                recordJob = scope.launch {
                    val dummyBuf = ByteArray(bufferSize)
                    while (isActive && isCurrentlyHijacked && silentRecorder != null) {
                        try {
                            silentRecorder?.read(dummyBuf, 0, dummyBuf.size)
                            delay(25)
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
                Log.d(TAG, "Hardware Mic Lock (VOICE_COMMUNICATION) engaged")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Mic Lock skipped: ${e.message}")
        }
    }

    private fun stopHardwareMicLock() {
        recordJob?.cancel()
        recordJob = null
        try {
            silentRecorder?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
            silentRecorder = null
        } catch (_: Exception) {}
    }

    /**
     * Runs a persistent background loop continuously writing silent PCM buffers
     * to hold exclusive audio focus on hardware AudioFlinger.
     */
    private fun startContinuousSilentTrack() {
        stopSilentTrack()
        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            silentTrack = track

            val silentData = ByteArray(bufferSize) // Zeroed PCM = absolute silence
            silentTrackJob?.cancel()
            silentTrackJob = scope.launch {
                while (isActive && isCurrentlyHijacked && silentTrack != null) {
                    try {
                        silentTrack?.write(silentData, 0, silentData.size)
                        delay(25)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Continuous Silent AudioTrack error: ${e.message}")
        }
    }

    private fun stopSilentTrack() {
        silentTrackJob?.cancel()
        silentTrackJob = null
        try {
            silentTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            silentTrack = null
        } catch (_: Exception) {}
    }

    /**
     * Ultra-fast guard loop (every 30ms) that prevents camera apps from stealing back
     * audio modes or raising stream volumes.
     */
    private fun startVolumeGuard() {
        volumeGuardJob?.cancel()
        volumeGuardJob = scope.launch {
            while (isActive && isCurrentlyHijacked) {
                try {
                    muteAllStreams()
                    if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    }
                    if (audioManager.isSpeakerphoneOn) {
                        audioManager.isSpeakerphoneOn = false
                    }
                    routeToEarpiece()
                } catch (_: Exception) {}
                delay(30)
            }
        }
    }

    /**
     * High-priority shutter tap pulse.
     */
    fun triggerShutterPulse() {
        try {
            muteAllStreams()
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = false
            routeToEarpiece()
        } catch (_: Exception) {}
    }

    /**
     * Restores normal audio routing, releases silent audio track and recorder, and restores original volumes.
     */
    @Synchronized
    fun restoreAudioRouting() {
        if (!isCurrentlyHijacked) return

        try {
            // 1. Stop Volume Guard, AudioTrack, and Mic Lock
            volumeGuardJob?.cancel()
            volumeGuardJob = null
            stopSilentTrack()
            stopHardwareMicLock()

            // 2. Clear communication device routing (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    audioManager.clearCommunicationDevice()
                } catch (_: Exception) {}
            }

            // 3. Restore audio mode & speakerphone
            savedAudioMode?.let { audioManager.mode = it } ?: run { audioManager.mode = AudioManager.MODE_NORMAL }
            savedSpeakerphone?.let { audioManager.isSpeakerphoneOn = it }
            savedMicMute?.let { audioManager.isMicrophoneMute = it }

            // 4. Restore DND & ringer mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                savedInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
                savedRingerMode?.let { audioManager.ringerMode = it }
            }

            // 5. Unmute and restore all stream volumes
            for ((stream, volume) in savedVolumes) {
                try {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                    val max = audioManager.getStreamMaxVolume(stream)
                    audioManager.setStreamVolume(stream, volume.coerceIn(0, max), 0)
                    setStreamMuteMethod?.invoke(audioManager, stream, false)
                } catch (_: Exception) {}
            }

            savedVolumes.clear()
            savedAudioMode = null
            savedRingerMode = null
            savedMicMute = null
            savedSpeakerphone = null
            savedInterruptionFilter = null

            isCurrentlyHijacked = false
            Log.d(TAG, "Audio routing restored to normal state")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio routing: ${e.message}", e)
        }
    }

    fun isHijacked(): Boolean = isCurrentlyHijacked
}
