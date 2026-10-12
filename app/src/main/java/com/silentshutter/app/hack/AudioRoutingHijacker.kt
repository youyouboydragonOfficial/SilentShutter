package com.silentshutter.app.hack

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioRoutingHijacker {

    private const val TAG = "AudioRoutingHijacker"

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    private var silentTrack: AudioTrack? = null
    private var isCurrentlyHijacked = false
    private var pulseJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Saved state for clean restoration
    private var savedAudioMode: Int? = null
    private var savedRingerMode: Int? = null
    private var savedSystemVolume: Int? = null
    private var savedNotificationVolume: Int? = null
    private var savedMicMute: Boolean? = null
    private var savedInterruptionFilter: Int? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Hijacks the OS audio routing pipeline by switching to in-communication mode
     * and occupying the exclusive voice stream with silent PCM data.
     * This forces the OS AudioPolicy to divert or suppress STREAM_SYSTEM_ENFORCED shutter sound.
     */
    @Synchronized
    fun hijackAudioRouting(): Boolean {
        if (isCurrentlyHijacked) return true

        return try {
            // 1. Save original audio states
            savedAudioMode = audioManager.mode
            savedRingerMode = audioManager.ringerMode
            savedSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            savedNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            savedMicMute = audioManager.isMicrophoneMute

            // 2. Mute system & notification streams
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)

            // 3. Switch audio mode to IN_COMMUNICATION (Voice Call routing hack)
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = true

            // 4. Start exclusive silent AudioTrack stream
            startSilentTrack()

            // 5. Apply DND if permitted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                savedInterruptionFilter = notificationManager.currentInterruptionFilter
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }

            isCurrentlyHijacked = true
            Log.d(TAG, "Audio routing successfully hijacked into MODE_IN_COMMUNICATION")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Audio hijack failed: ${e.message}", e)
            false
        }
    }

    /**
     * Restores normal audio routing, releases silent audio track, and restores original volumes.
     */
    @Synchronized
    fun restoreAudioRouting() {
        if (!isCurrentlyHijacked) return

        try {
            // 1. Stop and release silent AudioTrack
            stopSilentTrack()

            // 2. Restore audio mode
            savedAudioMode?.let {
                audioManager.mode = it
            } ?: run {
                audioManager.mode = AudioManager.MODE_NORMAL
            }

            // 3. Restore microphone mute
            savedMicMute?.let {
                audioManager.isMicrophoneMute = it
            }

            // 4. Restore DND & ringer mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                savedInterruptionFilter?.let {
                    notificationManager.setInterruptionFilter(it)
                }
                savedRingerMode?.let {
                    audioManager.ringerMode = it
                }
            }

            // 5. Unmute and restore system & notification volumes
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)

            savedSystemVolume?.let {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, it.coerceIn(0, max), 0)
            }
            savedNotificationVolume?.let {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it.coerceIn(0, max), 0)
            }

            // Clear saved states
            savedAudioMode = null
            savedRingerMode = null
            savedSystemVolume = null
            savedNotificationVolume = null
            savedMicMute = null
            savedInterruptionFilter = null

            isCurrentlyHijacked = false
            Log.d(TAG, "Audio routing restored to normal state")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio routing: ${e.message}", e)
        }
    }

    /**
     * Triggers an ultra-fast millisecond hijack pulse precisely when the user taps
     * the shutter button in the default camera app.
     */
    fun triggerShutterPulse(durationMs: Long = 800) {
        pulseJob?.cancel()
        pulseJob = scope.launch {
            hijackAudioRouting()
            delay(durationMs)
            restoreAudioRouting()
        }
    }

    private fun startSilentTrack() {
        try {
            stopSilentTrack()

            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build()

            silentTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Write silence
            val silentData = ByteArray(bufferSize)
            silentTrack?.write(silentData, 0, silentData.size)
            silentTrack?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Silent AudioTrack start skipped: ${e.message}")
        }
    }

    private fun stopSilentTrack() {
        try {
            silentTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            silentTrack = null
        } catch (e: Exception) {
            Log.w(TAG, "Silent AudioTrack stop skipped: ${e.message}")
        }
    }

    fun isHijacked(): Boolean = isCurrentlyHijacked
}
