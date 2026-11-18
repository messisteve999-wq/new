package com.steve.newapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object MemoryStore {
    private const val PREFS = "new_memory"
    private const val KEY = "contacts"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(ctx: Context): Map<String, List<String>> {
        val result = linkedMapOf<String, List<String>>()
        try {
            val json = JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}")
            val keys = json.keys()
            while (keys.hasNext()) {
                val contact = keys.next()
                val array = json.optJSONArray(contact) ?: JSONArray()
                result[contact] = (0 until array.length())
                    .map { array.optString(it) }
                    .filter { it.isNotBlank() }
            }
        } catch (_: Exception) {
        }
        return result
    }

    @Synchronized
    fun memories(ctx: Context, contact: String): List<String> =
        all(ctx)[contact].orEmpty()

    @Synchronized
    fun add(ctx: Context, contact: String, memory: String): Boolean {
        val name = contact.trim()
        val value = memory.trim()

        if (name.isEmpty() || value.isEmpty()) return false

        val data = all(ctx).mapValues { it.value.toMutableList() }
            .toMutableMap()

        val list = data.getOrPut(name) { mutableListOf() }

        if (list.any { it.equals(value, ignoreCase = true) }) return false

        list.add(value.take(500))
        while (list.size > 30) list.removeAt(0)

        save(ctx, data)
        return true
    }

    @Synchronized
    fun delete(ctx: Context, contact: String, index: Int) {
        val data = all(ctx).mapValues { it.value.toMutableList() }
            .toMutableMap()

        val list = data[contact] ?: return
        if (index !in list.indices) return

        list.removeAt(index)
        if (list.isEmpty()) data.remove(contact)

        save(ctx, data)
    }

    @Synchronized
    fun deleteContact(ctx: Context, contact: String) {
        val data = all(ctx).mapValues { it.value.toMutableList() }
            .toMutableMap()
        data.remove(contact)
        save(ctx, data)
    }

    fun context(ctx: Context, contact: String): String =
        memories(ctx, contact).takeLast(12)
            .joinToString("\n") { "- ${it.take(300)}" }

    private fun save(ctx: Context, data: Map<String, List<String>>) {
        val json = JSONObject()
        data.forEach { (contact, memories) ->
            json.put(contact, JSONArray(memories))
        }
        prefs(ctx).edit().putString(KEY, json.toString()).apply()
    }
}
