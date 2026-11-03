package com.steve.newapp.settings

import android.content.Context

object AutoContactsStore {
    private const val PREFS = "new_prefs"
    private const val KEY = "auto_allowed_numbers"

    fun getAllowed(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()

    fun setAllowed(context: Context, numbers: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, numbers).apply()
    }

    fun normalize(raw: String): String = raw.filter { it.isDigit() }

    fun isAllowed(context: Context, sender: String): Boolean {
        val s = normalize(sender)
        if (s.isEmpty()) return false
        return getAllowed(context).any { allowed ->
            s.endsWith(allowed) || allowed.endsWith(s)
        }
    }
}
