package com.steve.newapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle

// Ouvre le sélecteur de photos et vidéos, puis rattache le fichier au message concerné
class MediaPickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = Intent(Intent.ACTION_GET_CONTENT)
            .setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        val key = intent.getStringExtra("key")
        if (resultCode == RESULT_OK && uri != null && key != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // accès temporaire seulement, suffisant pour l'envoi immédiat
            }
            Store.items.find { it.key == key }?.apply {
                attachment = uri
                attachType = contentResolver.getType(uri)
            }
            Store.refresh()
        }
        finish()
    }
}
