package com.steve.newapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

object Contacts {
    // Utilisé seulement si « Partager mes contacts avec l'IA » est activé
    fun names(ctx: Context): List<String> {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val out = mutableListOf<String>()
        ctx.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null
        )?.use { c -> while (c.moveToNext() && out.size < 200) c.getString(0)?.let { out.add(it) } }
        return out
    }
}
