package com.steve.newapp.settings

import android.content.Context

object AutoContactsStore {
    private const val PREFS = "new_prefs"
    private const val KEY = "contact_modes"

    const val MODE_AUTO = "auto"
    const val MODE_SUGGEST = "suggest"

    // Chaque entrée est stockée sous la forme "nom|mode"
    fun getModes(context: Context): MutableMap<String, String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()
        val map = mutableMapOf<String, String>()
        for (entry in raw) {
            val i = entry.lastIndexOf('|')
            if (i > 0) map[entry.substring(0, i)] = entry.substring(i + 1)
        }
        return map
    }

    fun setModes(context: Context, modes: Map<String, String>) {
        val set = modes.map { "${it.key}|${it.value}" }.toSet()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, set).apply()
    }

    // Retourne "auto", "suggest", ou null si le contact n'est pas coché
    fun modeFor(context: Context, senderName: String): String? =
        getModes(context)[senderName.trim()]
}
