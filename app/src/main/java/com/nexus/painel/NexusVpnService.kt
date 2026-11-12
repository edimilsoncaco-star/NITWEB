package com.nexus.painel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class NexusVpnService : VpnService() {

    companion object {
        private const val CHANNEL_ID = "nexus_vpn_channel"
        private const val NOTIF_ID   = 2
        private const val MTU        = 1500
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var readerThread: Thread? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        setupTunInterface()
        return START_STICKY
    }

    private fun setupTunInterface() {
        tunFd = Builder()
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setSession("NexusProxy")
            .setMtu(MTU)
            // só intercepta pacotes do Free Fire — resto passa direto
            .addAllowedApplication("com.dts.freefireth")
            .addAllowedApplication("com.dts.freefiremax")
            .establish() ?: return

        running = true
        readerThread = Thread(::readLoop, "NexusVpnReader").apply { isDaemon = true; start() }
    }

    private fun readLoop() {
        val fd     = tunFd ?: return
        val input  = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val raw    = ByteArray(MTU)

        // protect() deve ser chamado UMA VEZ no fd, antes do loop
        protect(fd.fd)

        while (running) {
            val n = try { input.read(raw, 0, MTU) } catch (e: Exception) { break }
            if (n <= 0) continue

            // envia pro parser C++
            NexusBridge.parsePacket(raw, n)

            // CRÍTICO: reencaminha o pacote original pelo tun — sem isso o jogo perde rede
            try { output.write(raw, 0, n) } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        running = false
        readerThread?.interrupt()
        tunFd?.close()
        tunFd = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nexus VPN", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.nexus_icon)
            .setContentIntent(pi)
            .build()
    }
}
