package com.nexus.painel

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView

class FloatingPanel(context: Context) : FrameLayout(context) {

    // --- Estado das features ---
    var espLineEnabled: Boolean     = true
    var espBoxEnabled: Boolean      = true
    var espSkeletonEnabled: Boolean = true
    var espDistanceEnabled: Boolean = true
    var aimbotEnabled: Boolean      = false
    var currentFov: Float           = 150f

    // --- Dados de renderização ---
    private var playerData: FloatArray   = floatArrayOf()
    private var aimbotTarget: FloatArray = floatArrayOf(2)
    private var screenW: Float           = 1080f
    private var screenH: Float           = 1920f

    // --- Paints ---
    private val espPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.WHITE
        strokeWidth = 2f
        style       = Paint.Style.STROKE
        textSize    = 28f
    }
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.RED
        strokeWidth = 2f
        style       = Paint.Style.STROKE
        textSize    = 28f
    }
    private val aimbotDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val fovPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.WHITE
        strokeWidth = 1.5f
        style       = Paint.Style.STROKE
        alpha       = 160
    }
    private val iconBgPaint = Paint().apply {
        color = Color.BLACK; alpha = 200; style = Paint.Style.FILL
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED; textSize = 48f; typeface = Typeface.DEFAULT_BOLD
    }

    // Ícone N — posição inicial
    private val iconRect = RectF(20f, 200f, 76f, 256f)

    // --- Mod menu ---
    private var menuVisible = false
    private lateinit var menuView: View
    private var menuX     = 20f
    private var menuY     = 260f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging  = false

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = false
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setupMenu(context)
    }

    private fun setupMenu(context: Context) {
        menuView = LayoutInflater.from(context).inflate(R.layout.floating_panel, this, false)

        val switchLine     = menuView.findViewById<SwitchCompat>(R.id.sw_esp_line)
        val switchBox      = menuView.findViewById<SwitchCompat>(R.id.sw_esp_box)
        val switchSkeleton = menuView.findViewById<SwitchCompat>(R.id.sw_esp_skeleton)
        val switchDistance = menuView.findViewById<SwitchCompat>(R.id.sw_esp_distance)
        val switchAimbot   = menuView.findViewById<SwitchCompat>(R.id.sw_aimbot)
        val seekFov        = menuView.findViewById<SeekBar>(R.id.seek_fov)
        val tvFov          = menuView.findViewById<TextView>(R.id.tv_fov_label)

        switchLine.isChecked     = espLineEnabled
        switchBox.isChecked      = espBoxEnabled
        switchSkeleton.isChecked = espSkeletonEnabled
        switchDistance.isChecked = espDistanceEnabled
        switchAimbot.isChecked   = aimbotEnabled
        seekFov.progress         = currentFov.toInt()

        switchLine.setOnCheckedChangeListener     { _, v -> espLineEnabled     = v; invalidate() }
        switchBox.setOnCheckedChangeListener      { _, v -> espBoxEnabled      = v; invalidate() }
        switchSkeleton.setOnCheckedChangeListener { _, v -> espSkeletonEnabled = v; invalidate() }
        switchDistance.setOnCheckedChangeListener { _, v -> espDistanceEnabled = v; invalidate() }
        switchAimbot.setOnCheckedChangeListener   { _, v -> aimbotEnabled      = v; invalidate() }

        seekFov.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                currentFov = p.toFloat(); tvFov.text = "FOV: $p"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        menuView.visibility = View.GONE
        addView(menuView)
        positionMenu()
    }

    private fun positionMenu() {
        val lp = menuView.layoutParams as LayoutParams
        lp.leftMargin = menuX.toInt()
        lp.topMargin  = menuY.toInt()
        menuView.layoutParams = lp
    }

    fun updateData(raw: FloatArray, aimbot: FloatArray, sw: Float, sh: Float) {
        playerData   = raw
        aimbotTarget = aimbot
        screenW      = sw
        screenH      = sh
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (playerData.isEmpty()) return

        val count = playerData[0].toInt()
        if (count <= 0) return

        // Círculo de FOV centrado na tela
        if (aimbotEnabled) {
            canvas.drawCircle(screenW / 2f, screenH / 2f, currentFov, fovPaint)
        }

        for (i in 0 until count) {
            val baseIdx = 1 + i * 4
            if (baseIdx + 3 >= playerData.size) break

            // Formato vindo de getPlayerPositions: [count, id, x, y, z, ...]
            // x/y já são coordenadas de tela projetadas pelo C++ via world_to_screen
            val screenX = playerData[baseIdx + 1]
            val screenY = playerData[baseIdx + 2]
            val dist    = playerData[baseIdx + 3] // distância em metros do local player

            if (screenX < 0f || screenY < 0f) continue // fora da tela

            // Altura aproximada do player na tela baseada na distância
            val playerHeight = (screenH * 0.15f / (dist.coerceAtLeast(1f) / 50f)).coerceIn(20f, 300f)
            val headY        = screenY - playerHeight
            val footY        = screenY
            val width        = playerHeight * 0.5f
            val cx           = screenX

            val paint = if (dist < 20f) redPaint else espPaint

            // ESP Line — centro inferior da tela até os pés do player
            if (espLineEnabled) {
                canvas.drawLine(screenW / 2f, screenH, cx, footY, paint)
            }

            // ESP Box
            if (espBoxEnabled) {
                val rect = RectF(cx - width / 2f, headY, cx + width / 2f, footY)
                canvas.drawRect(rect, paint)
            }

            // ESP Distance
            if (espDistanceEnabled) {
                val label = "${dist.toInt()}m"
                canvas.drawText(label, cx - espPaint.measureText(label) / 2f, headY - 6f, espPaint)
            }
        }

        // Ponto alvo do aimbot
        if (aimbotEnabled && aimbotTarget.size >= 2) {
            val ax = aimbotTarget[0]; val ay = aimbotTarget[1]
            if (ax >= 0f && ay >= 0f) canvas.drawCircle(ax, ay, 8f, aimbotDotPaint)
        }

        // Ícone N
        canvas.drawRoundRect(iconRect, 8f, 8f, iconBgPaint)
        canvas.drawText("N", iconRect.left + 8f, iconRect.bottom - 8f, iconPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val ex = event.rawX
        val ey = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Clique no ícone N
                if (ex in iconRect.left..iconRect.right && ey in iconRect.top..iconRect.bottom) {
                    return true
                }
                // Início de drag no menu
                if (menuVisible) {
                    val lp = menuView.layoutParams as LayoutParams
                    val mLeft = lp.leftMargin.toFloat()
                    val mTop  = lp.topMargin.toFloat()
                    if (ex in mLeft..(mLeft + menuView.width) &&
                        ey in mTop..(mTop + menuView.height)) {
                        isDragging = true
                        dragStartX = ex - mLeft
                        dragStartY = ey - mTop
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    menuX = (ex - dragStartX).coerceIn(0f, screenW - menuView.width)
                    menuY = (ey - dragStartY).coerceIn(0f, screenH - menuView.height)
                    positionMenu()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Toggle menu ao soltar o toque no ícone
                    if (ex in iconRect.left..iconRect.right && ey in iconRect.top..iconRect.bottom) {
                        menuVisible = !menuVisible
                        menuView.visibility = if (menuVisible) View.VISIBLE else View.GONE
                        return true
                    }
                }
                isDragging = false
            }
        }
        return super.onTouchEvent(event)
    }
}
