package com.nexus.painel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var activate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.tv_status)
        activate = findViewById(R.id.btn_activate)
        activate.setOnClickListener { enablePanel() }
        refreshStatus()
    }

    private fun enablePanel() {
        if (!Settings.canDrawOverlays(this)) {
            status.text = "Autorize a sobreposição e volte ao aplicativo."
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_OVERLAY)
            return
        }
        startPanel()
    }

    private fun startPanel() {
        val intent = Intent(this, OverlayService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
        status.text = "Painel flutuante ativado."
        activate.text = "Painel ativado"
        activate.isEnabled = false
    }

    private fun refreshStatus() {
        status.text = if (Settings.canDrawOverlays(this)) {
            "Permissão de sobreposição disponível."
        } else {
            "Permissão de sobreposição necessária."
        }
    }

    @Deprecated("Deprecated by Android; retained for compatibility with API 23+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            refreshStatus()
            if (Settings.canDrawOverlays(this)) startPanel()
        }
    }

    companion object { private const val REQUEST_OVERLAY = 1001 }
}
