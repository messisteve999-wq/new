package com.steve.newapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ManualSend {
    fun open(ctx: Context, phone: String, text: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) {
            Toast.makeText(ctx, "Numéro invalide", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://wa.me/$digits?text=" + Uri.encode(text)
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Impossible d'ouvrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}
