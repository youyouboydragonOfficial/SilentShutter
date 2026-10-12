package com.silentshutter.app.hack

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AudioRoutingHijacker {

    private const val TAG = "AudioRoutingHijacker"

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    private var silentTrack: AudioTrack? = null
    private var isCurrentlyHijacked = false
    private var pulseJob: Job? = null
    private var volumeGuardJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Saved state for clean restoration
    private var savedAudioMode: Int? = null
    private var savedRingerMode: Int? = null
    private var savedMicMute: Boolean? = null
    private var savedSpeakerphone: Boolean? = null
    private var savedInterruptionFilter: Int? = null
    private val savedVolumes = mutableMapOf<Int, Int>()

    private val ALL_AUDIO_STREAMS = listOf(
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_DTMF,
        AudioManager.STREAM_VOICE_CALL
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Complete Audio Routing Hijack:
     * 1. Backup and force ALL streams to 0 + ADJUST_MUTE
     * 2. Force AudioMode to MODE_IN_COMMUNICATION
     * 3. Route audio exclusively to Earpiece (sub-speaker) away from main speaker (Android 12+)
     * 4. Play silent AudioTrack loop to hold focus
     * 5. Run continuous Volume Guard thread (prevent camera from resetting volume)
     */
    @Synchronized
    fun hijackAudioRouting(): Boolean {
        if (isCurrentlyHijacked) return true

        return try {
            // 1. Save all stream volumes
            savedVolumes.clear()
            for (stream in ALL_AUDIO_STREAMS) {
                try {
                    savedVolumes[stream] = audioManager.getStreamVolume(stream)
                } catch (_: Exception) {}
            }

            // 2. Save system states
            savedAudioMode = audioManager.mode
            savedRingerMode = audioManager.ringerMode
            savedMicMute = audioManager.isMicrophoneMute
            savedSpeakerphone = audioManager.isSpeakerphoneOn

            // 3. Immediately mute all streams
            muteAllStreams()

            // 4. Set communication mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = true
            audioManager.isSpeakerphoneOn = false

            // 5. Route to Earpiece (AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) on Android 12+
            routeToEarpiece()

            // 6. Start exclusive silent AudioTrack stream
            startSilentTrack()

            // 7. Apply DND Total Silence if permitted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                savedInterruptionFilter = notificationManager.currentInterruptionFilter
                // Total silence (FILTER_NONE) completely suppresses all alerts
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }

            // 8. Start Volume Guard thread (resets volume to 0 every 80ms while camera is active)
            startVolumeGuard()

            isCurrentlyHijacked = true
            Log.d(TAG, "Audio routing successfully hijacked into MODE_IN_COMMUNICATION + Earpiece routing")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Audio hijack failed: ${e.message}", e)
            false
        }
    }

    private fun muteAllStreams() {
        for (stream in ALL_AUDIO_STREAMS) {
            try {
                audioManager.setStreamVolume(stream, 0, 0)
                audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
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
                    Log.d(TAG, "Successfully routed communication device to Earpiece")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Earpiece route failed: ${e.message}")
            }
        }
    }

    private fun startVolumeGuard() {
        volumeGuardJob?.cancel()
        volumeGuardJob = scope.launch {
            while (isActive && isCurrentlyHijacked) {
                muteAllStreams()
                delay(80)
            }
        }
    }

    /**
     * Restores normal audio routing, releases silent audio track, and restores original volumes.
     */
    @Synchronized
    fun restoreAudioRouting() {
        if (!isCurrentlyHijacked) return

        try {
            // 1. Stop Volume Guard
            volumeGuardJob?.cancel()
            volumeGuardJob = null

            // 2. Stop and release silent AudioTrack
            stopSilentTrack()

            // 3. Clear communication device routing (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    audioManager.clearCommunicationDevice()
                } catch (_: Exception) {}
            }

            // 4. Restore audio mode & speakerphone
            savedAudioMode?.let { audioManager.mode = it } ?: run { audioManager.mode = AudioManager.MODE_NORMAL }
            savedSpeakerphone?.let { audioManager.isSpeakerphoneOn = it }
            savedMicMute?.let { audioManager.isMicrophoneMute = it }

            // 5. Restore DND & ringer mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                savedInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
                savedRingerMode?.let { audioManager.ringerMode = it }
            }

            // 6. Unmute and restore all stream volumes
            for ((stream, volume) in savedVolumes) {
                try {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                    val max = audioManager.getStreamMaxVolume(stream)
                    audioManager.setStreamVolume(stream, volume.coerceIn(0, max), 0)
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

    /**
     * Triggers an ultra-fast millisecond hijack pulse precisely when the user taps
     * the shutter button in the default camera app.
     */
    fun triggerShutterPulse(durationMs: Long = 900) {
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
