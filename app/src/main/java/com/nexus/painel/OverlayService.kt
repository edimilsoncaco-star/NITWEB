package com.nexus.painel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "nexus_overlay_channel"
        private const val NOTIF_ID   = 1
        private const val FRAME_MS   = 16L // ~60fps
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingPanel: FloatingPanel
    private val handler = Handler(Looper.getMainLooper())

    private val renderRunnable = object : Runnable {
        override fun run() {
            renderFrame()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager  = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingPanel  = FloatingPanel(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE mantido; FLAG_NOT_TOUCHABLE REMOVIDO — sem isso o menu nunca recebe toques
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(floatingPanel, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        handler.post(renderRunnable)
        return START_STICKY
    }

    private fun renderFrame() {
        val screenW       = resources.displayMetrics.widthPixels.toFloat()
        val screenH       = resources.displayMetrics.heightPixels.toFloat()
        val rawPositions  = NexusBridge.getPlayerPositions()
        val aimbotResult  = NexusBridge.runAimbot(floatingPanel.currentFov, screenW, screenH)

        floatingPanel.updateData(rawPositions, aimbotResult, screenW, screenH)
        floatingPanel.invalidate()
    }

    override fun onDestroy() {
        handler.removeCallbacks(renderRunnable)
        if (::floatingPanel.isInitialized) windowManager.removeView(floatingPanel)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nexus Overlay", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Painel Nexus")
            .setContentText("ESP ativo")
            .setSmallIcon(R.drawable.nexus_icon)
            .setContentIntent(pi)
            .build()
    }
}
