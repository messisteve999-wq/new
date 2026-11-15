package com.steve.newapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var menu: ScrollView
    private val refresher: () -> Unit = { renderMessages(this, list, true) }
    private val langOrder = listOf("fr", "en", "el", "ja")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun pill(label: String, onClick: (TextView) -> Unit): TextView {
        val tv = TextView(this)
        tv.text = label
        tv.textSize = 15f
        tv.setTextColor(Color.WHITE)
        tv.setPadding(dp(16), dp(14), dp(16), dp(14))
        tv.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.parseColor("#1E1E2E"))
            setStroke(dp(1), Color.parseColor("#55FF1744"))
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, dp(6), 0, dp(6))
        tv.layoutParams = lp
        tv.setOnClickListener { onClick(tv) }
        return tv
    }

    private fun modeLabel() = "Mode : ${Prefs.mode(this)}"
    private fun langLabel() = "Langue IA : ${Gemini.lang}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Gemini.lang = Prefs.text(this, "lang", "fr")
        Gemini.personality = Prefs.text(this, "personality", "connaissance")
        Prefs.text(this, "server_url").takeIf { it.isNotEmpty() }?.let { Gemini.endpoint = it }

        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // En-tête
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.parseColor("#14141F"))
        }
        header.addView(TextView(this).apply {
            text = "☰ Menu"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(8), 0, dp(14), 0)
            setOnClickListener {
                menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        })
        header.addView(TextView(this).apply {
            text = "new"
            textSize = 22f
            setTextColor(Color.parseColor("#FF1744"))
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(header)

        // Liste des discussions
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply { addView(list) }

        // Menu
        val menuBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        menuBox.addView(TextView(this).apply {
            text = "MENU"
            textSize = 18f
            setTextColor(Color.parseColor("#FF1744"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })
        menuBox.addView(pill("Activer la bulle flottante") { startBubble() })
        menuBox.addView(pill("Autoriser l'accès aux notifications") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        menuBox.addView(pill(modeLabel()) { t ->
            val next = when (Prefs.mode(this)) { "manual" -> "suggest"; "suggest" -> "auto"; else -> "manual" }
            Prefs.setMode(this, next)
            t.text = modeLabel()
        })
        menuBox.addView(pill("Assistant IA") {
            startActivity(Intent(this, AssistantActivity::class.java))
        })
        menuBox.addView(pill("Applications connectées") {
            startActivity(Intent(this, ConnectedAppsActivity::class.java))
        })
        menuBox.addView(pill(langLabel()) { t ->
            val next = langOrder[(langOrder.indexOf(Gemini.lang) + 1) % langOrder.size]
            Gemini.lang = next
            Prefs.setText(this, "lang", next)
            t.text = langLabel()
        })
        menuBox.addView(pill("Paramètres") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })

        val bg = EditText(this).apply {
            hint = "Couleur de fond (ex. #0B0B12)"
            setText(Prefs.text(this@MainActivity, "theme_bg", "#0B0B12"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        menuBox.addView(bg)
        menuBox.addView(pill("Enregistrer la couleur") {
            try {
                Color.parseColor(bg.text.toString())
                Prefs.setText(this, "theme_bg", bg.text.toString())
                applyBg()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Couleur invalide", Toast.LENGTH_SHORT).show()
            }
        })
        menuBox.addView(pill("Fermer le menu") { menu.visibility = View.GONE })

        menu = ScrollView(this).apply {
            addView(menuBox)
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#F00B0B12"))
        }

        val body = FrameLayout(this)
        body.addView(listScroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        body.addView(menu, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        applyBg()
        Store.listeners.add(refresher)
        renderMessages(this, list, true)
    }

    override fun onDestroy() {
        Store.listeners.remove(refresher)
        super.onDestroy()
    }

    private fun applyBg() {
        try {
            root.setBackgroundColor(Color.parseColor(Prefs.text(this, "theme_bg", "#0B0B12")))
        } catch (e: IllegalArgumentException) {
        }
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
