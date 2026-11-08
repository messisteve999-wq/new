package com.steve.newapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var list: LinearLayout
    private val refresher: () -> Unit = { renderMessages(this, list) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        Gemini.lang = Prefs.text(ctx, "lang", "fr")
        Prefs.text(ctx, "server_url").takeIf { it.isNotEmpty() }?.let { Gemini.endpoint = it }

        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        root.addView(TextView(this).apply { text = "new"; textSize = 24f; setTextColor(Color.RED) })
        root.addView(Button(this).apply {
            text = "Activer la bulle flottante"; isAllCaps = false
            setOnClickListener { startBubble() }
        })
        root.addView(Button(this).apply {
            text = "Autoriser l'accès aux notifications"; isAllCaps = false
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "Mode : ${Prefs.mode(ctx)}"; isAllCaps = false
            setOnClickListener {
                val next = when (Prefs.mode(ctx)) { "manual" -> "suggest"; "suggest" -> "auto"; else -> "manual" }
                Prefs.setMode(ctx, next)
                text = "Mode : $next"
            }
        })
        root.addView(Button(this).apply {
            text = "Assistant IA"; isAllCaps = false
            setOnClickListener { startActivity(Intent(ctx, AssistantActivity::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "Paramètres"; isAllCaps = false
            setOnClickListener { startActivity(Intent(ctx, SettingsActivity::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "Applications connectées"; isAllCaps = false
            setOnClickListener { startActivity(Intent(ctx, ConnectedAppsActivity::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "Langue IA : ${Gemini.lang}"; isAllCaps = false
            setOnClickListener {
                val order = listOf("fr", "en", "el", "ja")
                val next = order[(order.indexOf(Gemini.lang) + 1) % order.size]
                Gemini.lang = next
                Prefs.setText(ctx, "lang", next)
                text = "Langue IA : $next"
            }
        })
        root.addView(Button(this).apply {
            text = "Applications connectées"; isAllCaps = false
            setOnClickListener { startActivity(Intent(ctx, ConnectedAppsActivity::class.java)) }
        })
        val bg = EditText(this).apply { hint = "Couleur de fond (ex. #FFFFFF)"; setText(Prefs.bg(ctx)); setTextColor(Color.BLACK) }
        root.addView(bg)
        root.addView(Button(this).apply {
            text = "Enregistrer la couleur"; isAllCaps = false
            setOnClickListener {
                try {
                    Color.parseColor(bg.text.toString())
                    Prefs.setBg(ctx, bg.text.toString())
                    applyBg()
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(ctx, "Couleur invalide", Toast.LENGTH_SHORT).show()
                }
            }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        applyBg()
        Store.listeners.add(refresher)
        renderMessages(this, list)
    }

    override fun onDestroy() {
        Store.listeners.remove(refresher)
        super.onDestroy()
    }

    private fun applyBg() {
        root.setBackgroundColor(Color.parseColor(Prefs.bg(this)))
    }

    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        Prefs.setBool(this, "bubble_on", true)
        startService(Intent(this, BubbleService::class.java))
    }
}
