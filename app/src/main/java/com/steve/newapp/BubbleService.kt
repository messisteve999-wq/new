package com.steve.newapp

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubbleView: ImageView
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var panelRoot: LinearLayout? = null
    private var panelList: LinearLayout? = null
    private val refresher: () -> Unit = { panelList?.let { renderMessages(this, it, true) } }

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE

    private val screenW: Int get() = resources.displayMetrics.widthPixels
    private val screenH: Int get() = resources.displayMetrics.heightPixels

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun glassBackground(radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(Color.parseColor("#F00B0B12"))
        setStroke(dp(1), Color.parseColor("#66FF1744"))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        // Bulle sombre avec le démon rouge et un contour néon
        bubbleView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_demon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E6101018"))
                setStroke(dp(2), Color.parseColor("#FF1744"))
            }
            elevation = dp(8).toFloat()
        }

        val bubbleSize = dp(60)
        bubbleParams = WindowManager.LayoutParams(bubbleSize, bubbleSize, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - bubbleSize - dp(12)
            y = dp(200)
        }

        // Glisser sur tout l'écran ; un simple appui ouvre ou ferme la fenêtre
        var startX = 0f; var startY = 0f; var initX = 0; var initY = 0; var moved = false
        bubbleView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX; startY = e.rawY
                    initX = bubbleParams.x; initY = bubbleParams.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - startX).toInt()
                    val dy = (e.rawY - startY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    bubbleParams.x = (initX + dx).coerceIn(0, screenW - bubbleSize)
                    bubbleParams.y = (initY + dy).coerceIn(0, screenH - bubbleSize)
                    wm.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
        wm.addView(bubbleView, bubbleParams)
    }

    private fun togglePanel() {
        if (panelRoot != null) removePanel() else showPanel()
    }

    private fun showPanel() {
        val pw = dp(300)
        val ph = dp(440)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }
        panelList = list

        val title = TextView(this).apply {
            text = "Assistant IA"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { removePanel() }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(8), dp(12))
            addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(close)
        }
        val scroll = ScrollView(this).apply { addView(list) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassBackground(24)
            addView(header)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        val params = WindowManager.LayoutParams(pw, ph, overlayType, 0, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - pw - dp(12)
            y = screenH - ph - dp(120)
        }

        // Déplacer la fenêtre en la faisant glisser par son en-tête
        var hx = 0f; var hy = 0f; var px0 = 0; var py0 = 0
        header.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    hx = e.rawX; hy = e.rawY
                    px0 = params.x; py0 = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (px0 + (e.rawX - hx).toInt()).coerceIn(0, screenW - pw)
                    params.y = (py0 + (e.rawY - hy).toInt()).coerceIn(0, screenH - ph)
                    wm.updateViewLayout(root, params)
                    true
                }
                else -> true
            }
        }

        wm.addView(root, params)
        panelRoot = root
        bringBubbleToFront()

        Store.listeners.add(refresher)
        renderMessages(this, list, true)
    }

    private fun removePanel() {
        panelRoot?.let { wm.removeView(it) }
        panelRoot = null
        panelList = null
        Store.listeners.remove(refresher)
    }

    // La bulle doit rester au-dessus de la fenêtre pour pouvoir la fermer
    private fun bringBubbleToFront() {
        wm.removeView(bubbleView)
        wm.addView(bubbleView, bubbleParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        Store.listeners.remove(refresher)
        panelRoot?.let { wm.removeView(it) }
        wm.removeView(bubbleView)
    }
}
