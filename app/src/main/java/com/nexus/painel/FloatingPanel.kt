package com.nexus.painel

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class FloatingPanel(context: Context) : LinearLayout(context) {
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0

    init {
        orientation = VERTICAL
        setPadding(18, 14, 18, 16)
        background = GradientDrawable().apply {
            setColor(Color.argb(238, 15, 15, 20))
            cornerRadius = 22f
            setStroke(2, Color.rgb(255, 23, 68))
        }
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            text = "NEXUS PANEL"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val close = TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(18, 4, 4, 4)
            setOnClickListener { (context as? android.app.Service)?.stopSelf() }
        }
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(close)
        header.setOnTouchListener { _, event -> dragPanel(event) }
        addView(header)

        addView(optionSwitch("Vibração", "Alterna uma demonstração visual"))
        addView(optionSwitch("Modo compacto", "Reduz o espaço entre os controles"))
        addView(optionSwitch("Notificações", "Exibe confirmação ao alternar opções"))

        val footer = TextView(context).apply {
            text = "Painel utilitário • arraste pelo título"
            setTextColor(Color.LTGRAY)
            textSize = 10f
            setPadding(0, 12, 0, 0)
        }
        addView(footer)
    }

    private fun optionSwitch(label: String, description: String): Switch {
        return Switch(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 8, 0, 8)
            setOnCheckedChangeListener { _, enabled ->
                Toast.makeText(
                    context,
                    "$label: ${if (enabled) "ativado" else "desativado"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            contentDescription = description
        }
    }

    private fun dragPanel(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                val lp = layoutParams as? android.view.WindowManager.LayoutParams
                startX = lp?.x ?: 0
                startY = lp?.y ?: 0
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val lp = layoutParams as? android.view.WindowManager.LayoutParams ?: return false
                lp.x = startX + (event.rawX - downRawX).toInt()
                lp.y = startY + (event.rawY - downRawY).toInt()
                (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .updateViewLayout(this, lp)
                return true
            }
            MotionEvent.ACTION_UP -> return true
        }
        return false
    }
}
