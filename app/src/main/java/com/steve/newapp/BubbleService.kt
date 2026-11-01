package com.steve.newapp

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var bubbleView: TextView? = null
    private var panel: LinearLayout? = null
    private var panelList: LinearLayout? = null
    private val refresher: () -> Unit = { panelList?.let { renderMessages(this, it) } }

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val bubble = TextView(this).apply {
            text = "S"; textSize = 22f; setTextColor(Color.BLACK); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke(4, Color.BLACK)
            }
        }
        bubbleParams = WindowManager.LayoutParams(150, 150, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20; y = 300
        }
        // Glisser pour déplacer la bulle ; un simple appui ouvre ou ferme la fenêtre
        var startX = 0f; var startY = 0f; var initX = 0; var initY = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
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
                    bubbleParams.x = initX - dx
                    bubbleParams.y = initY + dy
                    wm.updateViewLayout(bubble, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
        bubbleView = bubble
        wm.addView(bubble, bubbleParams)
    }

    private fun togglePanel() {
        val existing = panel
        if (existing != null) {
            wm.removeView(existing)
            panel = null
            panelList = null
            Store.listeners.remove(refresher)
            return
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }
        panelList = list
        val close = Button(this).apply { text = "Fermer"; isAllCaps = false; setOnClickListener { togglePanel() } }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(close)
            addView(ScrollView(this@BubbleService).apply { addView(list) })
        }
        wm.addView(
            box,
            WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, 1000, overlayType, 0,
                PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM }
        )
        panel = box
        Store.listeners.add(refresher)
        renderMessages(this, list)
    }

    override fun onDestroy() {
        super.onDestroy()
        Store.listeners.remove(refresher)
        panel?.let { wm.removeView(it) }
        bubbleView?.let { wm.removeView(it) }
    }
}
