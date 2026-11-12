package com.nexus.painel

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_OVERLAY = 1001
        private const val REQUEST_VPN     = 1002
    }

    private lateinit var btnActivate: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnActivate = findViewById(R.id.btn_activate)
        tvStatus    = findViewById(R.id.tv_status)

        btnActivate.setOnClickListener { startPermissionFlow() }
        updateGameStatus()
    }

    private fun startPermissionFlow() {
        // Passo 1: permissão de overlay
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                REQUEST_OVERLAY
            )
            tvStatus.text = "Conceda permissão de sobreposição e volte"
            return
        }
        requestVpnPermission()
    }

    private fun requestVpnPermission() {
        // Passo 2: permissão de VPN
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN)
        } else {
            activateServices()
        }
    }

    private fun activateServices() {
        startForegroundService(Intent(this, OverlayService::class.java))
        startForegroundService(Intent(this, NexusVpnService::class.java))
        tvStatus.text    = "Painel Nexus ativo ✓"
        btnActivate.isEnabled = false
    }

    private fun updateGameStatus() {
        val code = NexusBridge.detectGame()
        tvStatus.text = when (code) {
            1    -> "Free Fire detectado"
            2    -> "Free Fire MAX detectado"
            else -> "Nenhum jogo detectado — abra o Free Fire primeiro"
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY -> {
                if (Settings.canDrawOverlays(this)) requestVpnPermission()
                else tvStatus.text = "Permissão de sobreposição negada"
            }
            REQUEST_VPN -> {
                if (resultCode == Activity.RESULT_OK) activateServices()
                else tvStatus.text = "Permissão de VPN negada"
            }
        }
    }
}
