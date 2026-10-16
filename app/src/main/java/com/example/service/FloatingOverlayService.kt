package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val activeViews = mutableListOf<View>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALL) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeAllViews()
    }

    private fun removeAllViews() {
        for (v in activeViews) {
            try {
                windowManager.removeView(v)
            } catch (e: Exception) {
                // Ignore if view already detached
            }
        }
        activeViews.clear()
    }

    private fun buildForegroundNotification(): Notification {
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_ALL
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Macro Touch Pro فعال است")
            .setContentText("دکمه‌های شناور آماده لمس هستند.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "توقف سریع اتوماسیون",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Macro Touch Pro Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت سرویس شناور و دکمه توقف سریع"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "macro_touch_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALL = "com.example.service.ACTION_STOP_ALL"

        fun createOverlayLayoutParams(x: Int, y: Int, sizeDp: Int, density: Float): WindowManager.LayoutParams {
            val sizePx = (sizeDp * density).toInt()
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            return WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
            }
        }
    }
}
