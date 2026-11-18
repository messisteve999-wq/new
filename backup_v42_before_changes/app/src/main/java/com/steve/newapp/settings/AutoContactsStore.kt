package com.steve.newapp.settings

import android.content.Context

object AutoContactsStore {

    private const val PREFS = "new_prefs"
    private const val KEY = "contact_modes"

    const val MODE_AUTO = "auto"
    const val MODE_SUGGEST = "suggest"

    fun getModes(context: Context): MutableMap<String, String> {
        val raw = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getStringSet(KEY, emptySet()) ?: emptySet()

        val map = mutableMapOf<String, String>()

        for (entry in raw) {
            val i = entry.lastIndexOf('|')
            if (i > 0) {
                map[entry.substring(0, i)] = entry.substring(i + 1)
            }
        }

        return map
    }

    fun setModes(context: Context, modes: Map<String, String>) {
        val set = modes.map { "${it.key}|${it.value}" }.toSet()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, set)
            .apply()
    }

    private fun key(pkg: String, sender: String): String {
        return "${pkg.trim()}|${sender.trim()}"
    }

    fun modeFor(
        context: Context,
        pkg: String,
        sender: String
    ): String {
        return getModes(context)[key(pkg, sender)] ?: MODE_SUGGEST
    }

    fun setMode(
        context: Context,
        pkg: String,
        sender: String,
        mode: String
    ) {
        val modes = getModes(context)
        val contactKey = key(pkg, sender)

        if (mode == MODE_AUTO) {
            modes[contactKey] = MODE_AUTO
        } else {
            modes.remove(contactKey)
        }

        setModes(context, modes)
    }
}
