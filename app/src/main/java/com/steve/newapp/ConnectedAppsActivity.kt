package com.steve.newapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

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

    private var selectedPackage = "com.whatsapp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                isChecked = Prefs.bool(this@ConnectedAppsActivity, "app_$pkg", true)
                setOnCheckedChangeListener { _, on ->
                    Prefs.setBool(this@ConnectedAppsActivity, "app_$pkg", on)
                }
            })
        }

        list.addView(TextView(this).apply {
            text = "Désactiver = déconnecter. Réactiver = reconnecter."
            setTextColor(Color.DKGRAY)
            setPadding(0, 24, 0, 24)
        })

        list.addView(TextView(this).apply {
            text = "IMPORTER LES ANCIENNES CONVERSATIONS"
            textSize = 18f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 12)
        })

        list.addView(Button(this).apply {
            text = "Importer historique WhatsApp (.txt)"
            isAllCaps = false
            setOnClickListener {
                openImporter("com.whatsapp")
            }
        })

        list.addView(Button(this).apply {
            text = "Importer historique WhatsApp Business (.txt)"
            isAllCaps = false
            setOnClickListener {
                openImporter("com.whatsapp.w4b")
            }
        })

        list.addView(TextView(this).apply {
            text = "Dans WhatsApp : ouvre une discussion, " +
                "choisis Exporter discussion, puis Sans médias. " +
                "Sélectionne ensuite le fichier texte ici."
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 20)
        })

        list.addView(Button(this).apply {
            text = "Vider l'historique"
            isAllCaps = false
            setOnClickListener {
                Store.deleteWhere { true }
                Toast.makeText(
                    this@ConnectedAppsActivity,
                    "Historique vidé",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        setContentView(ScrollView(this).apply {
            addView(list)
        })
    }

    private fun openImporter(pkg: String) {
        selectedPackage = pkg

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }

        startActivityForResult(intent, 501)
    }

    @Deprecated("Utilisation compatible avec le projet actuel")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 501 || resultCode != Activity.RESULT_OK) {
            return
        }

        val uri: Uri = data?.data ?: return

        try {
            val content = contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: throw IllegalStateException("Fichier illisible")

            val count = WhatsAppHistoryImporter.importText(
                this,
                selectedPackage,
                content
            )

            val message = if (count > 0) {
                "$count messages importés avec succès"
            } else {
                "Aucun nouveau message reconnu. Vérifie le fichier TXT."
            }

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Erreur d'importation : ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
