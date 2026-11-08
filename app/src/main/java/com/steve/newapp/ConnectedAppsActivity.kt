package com.steve.newapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class ConnectedAppsActivity : Activity() {
    private val names = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook",
        "com.snapchat.android" to "Snapchat",
        "com.google.android.apps.messaging" to "Messages (SMS)",
        "com.android.mms" to "SMS (système)",
        "com.samsung.android.messaging" to "Messages Samsung"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }
        list.addView(TextView(this).apply {
            text = "Applications connectées"
            textSize = 22f
            setTextColor(Color.BLACK)
        })
        for (pkg in Store.APPS.sorted()) {
            list.addView(Switch(this).apply {
                text = names[pkg] ?: pkg
                setTextColor(Color.BLACK)
                isChecked = Prefs.bool(ctx, "app_$pkg", true)
                setOnCheckedChangeListener { _, on -> Prefs.setBool(ctx, "app_$pkg", on) }
            })
        }
        list.addView(TextView(this).apply {
            text = "Désactiver = déconnecter. Réactiver = reconnecter."
            setTextColor(Color.DKGRAY)
            setPadding(0, 24, 0, 24)
        })
        list.addView(Button(this).apply {
            text = "Vider l'historique"
            isAllCaps = false
            setOnClickListener { Store.deleteWhere { true } }
        })
        setContentView(ScrollView(this).apply { addView(list) })
    }
}
