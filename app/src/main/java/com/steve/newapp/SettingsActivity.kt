package com.steve.newapp

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }

        fun title(t: String) = TextView(ctx).apply { text = t; textSize = 18f; setTextColor(Color.BLACK); setPadding(0, 32, 0, 8) }
        fun sw(label: String, key: String, def: Boolean = false, onOn: (() -> Unit)? = null) = Switch(ctx).apply {
            text = label; setTextColor(Color.BLACK); isChecked = Prefs.bool(ctx, key, def)
            setOnCheckedChangeListener { _, v -> Prefs.setBool(ctx, key, v); if (v) onOn?.invoke() }
        }
        fun field(hint: String, value: String, multi: Boolean = false) = EditText(ctx).apply {
            this.hint = hint; setText(value); setTextColor(Color.BLACK)
            if (multi) minLines = 3
        }

        col.addView(title("Bulle flottante"))
        col.addView(Switch(ctx).apply {
            text = "Afficher la bulle flottante"; setTextColor(Color.BLACK)
            isChecked = Prefs.bool(ctx, "bubble_on")
            setOnCheckedChangeListener { _, v ->
                val svc = Intent(ctx, BubbleService::class.java)
                if (v && Settings.canDrawOverlays(ctx)) {
                    Prefs.setBool(ctx, "bubble_on", true)
                    startService(svc)
                } else if (v) {
                    Toast.makeText(ctx, "Autorisez d'abord l'affichage par-dessus les autres applis", Toast.LENGTH_LONG).show()
                    Prefs.setBool(ctx, "bubble_on", false)
                } else {
                    Prefs.setBool(ctx, "bubble_on", false)
                    stopService(svc)
                }
            }
        })
        col.addView(title("Confidentialité"))
        col.addView(sw("Masquer le texte des messages", "hide_preview"))
        col.addView(sw("Partager mes contacts avec l'IA", "share_contacts") {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 1)
        })

        col.addView(title("Avancé"))
        col.addView(sw("Inclure WhatsApp Business", "include_business", def = true))
        val server = field("Adresse du serveur Gemini", Prefs.text(ctx, "server_url").ifEmpty { Gemini.endpoint })
        col.addView(server)

        col.addView(title("Assistant IA"))
        val rules = field("Comment l'IA doit répondre", Prefs.text(ctx, "rules", "Réponds en français, de façon courte, naturelle et polie."), multi = true)
        val about = field("Ce que l'IA doit savoir de moi", Prefs.text(ctx, "about"), multi = true)
        col.addView(rules)
        col.addView(about)

        col.addView(Button(this).apply {
            text = "Enregistrer"; isAllCaps = false
            setOnClickListener {
                Prefs.setText(ctx, "rules", rules.text.toString().trim())
                Prefs.setText(ctx, "about", about.text.toString().trim())
                val url = server.text.toString().trim()
                if (url.startsWith("https://")) { Prefs.setText(ctx, "server_url", url); Gemini.endpoint = url }
                Toast.makeText(ctx, "Enregistré", Toast.LENGTH_SHORT).show()
            }
        })
        val btnContacts = android.widget.Button(this).apply {
    text = "Contacts du mode automatique"
    setOnClickListener {
        startActivity(
            android.content.Intent(this@SettingsActivity,
                com.steve.newapp.settings.AutoContactsActivity::class.java)
        )
    }
}
col.addView(btnContacts)
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
