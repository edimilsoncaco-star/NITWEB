package com.nexus.painel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Sobe o OverlayService automaticamente após o boot do dispositivo.
// Requer permissão RECEIVE_BOOT_COMPLETED no Manifest.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        context.startForegroundService(Intent(context, OverlayService::class.java))
        context.startForegroundService(Intent(context, NexusVpnService::class.java))
    }
}
