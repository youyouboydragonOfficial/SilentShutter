package com.silentshutter.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.silentshutter.app.MainActivity
import com.silentshutter.app.R
import com.silentshutter.app.SilentShutterApp
import com.silentshutter.app.manager.ShutterMuteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingHudService : Service() {

    private var windowManager: WindowManager? = null
    private var floatView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        initFloatingView()
        observeMuteState()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SilentShutterApp.CHANNEL_ID_OVERLAY)
            .setContentTitle("SilentShutter HUD Active")
            .setContentText("画面端のHUDボタンで瞬時にシャッター音を消音できます")
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingView() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = (54 * resources.displayMetrics.density).toInt()
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_tile)
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            updateHudBackground(this, ShutterMuteManager.isCurrentlyMuted.value)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false
        var touchDownTime = 0L

        imageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    touchDownTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                        hasMoved = true
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(imageView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        val duration = System.currentTimeMillis() - touchDownTime
                        if (duration > 450) {
                            // Long-press: Launch zero-sound camera
                            val camIntent = Intent(this, com.silentshutter.app.camera.SilentCameraActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(camIntent)
                        } else {
                            // Tap: Toggle instant mute
                            ShutterMuteManager.toggleInstantMute()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        floatView = imageView
        windowManager?.addView(floatView, params)
    }

    private fun updateHudBackground(view: View, isMuted: Boolean) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (isMuted) {
                setColor(Color.parseColor("#E600E676")) // Neon Green Muted
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#00F0FF"))
            } else {
                setColor(Color.parseColor("#CC111827")) // Cyber Dark
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#00F0FF"))
            }
        }
        view.background = bg
    }

    private fun observeMuteState() {
        scope.launch {
            ShutterMuteManager.isCurrentlyMuted.collect { muted ->
                floatView?.let {
                    updateHudBackground(it, muted)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        floatView?.let {
            windowManager?.removeView(it)
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
