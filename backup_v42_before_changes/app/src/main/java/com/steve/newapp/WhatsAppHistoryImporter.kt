package com.steve.newapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object WhatsAppHistoryImporter {

    private data class Entry(
        val sender: String,
        val time: Long,
        val text: String
    )

    private val formats = listOf(
        "d/M/yy, H:mm", "dd/MM/yy, HH:mm",
        "d/M/yyyy, H:mm", "dd/MM/yyyy, HH:mm",
        "d/M/yy, H:mm:ss", "dd/MM/yy, HH:mm:ss",
        "d/M/yyyy, H:mm:ss", "dd/MM/yyyy, HH:mm:ss",
        "M/d/yy, h:mm a", "M/d/yyyy, h:mm a",
        "yyyy-MM-dd, HH:mm", "yyyy-MM-dd, HH:mm:ss"
    )

    private val androidLine =
        Regex("^(.+?) - ([^:]+): ?(.*)$")

    private val iosLine =
        Regex("^\\[([^]]+)\\]\\s+([^:]+): ?(.*)$")

    fun importText(
        context: Context,
        pkg: String,
        raw: String
    ): Int {
        Store.init(context)

        val parsed = mutableListOf<Entry>()
        var sender: String? = null
        var time = 0L
        val body = StringBuilder()

        fun flush() {
            val name = sender
            val message = body.toString().trim()

            if (!name.isNullOrBlank() && message.isNotBlank()) {
                parsed.add(Entry(name, time, message))
            }

            sender = null
            body.clear()
        }

        raw.removePrefix("\uFEFF").lineSequence().forEach { line ->
            val match = iosLine.matchEntire(line)
                ?: androidLine.matchEntire(line)

            if (match == null) {
                if (sender != null && line.isNotBlank()) {
                    if (body.isNotEmpty()) body.append('\n')
                    body.append(line)
                }
            } else {
                val date = parseDate(match.groupValues[1].trim())
                val name = match.groupValues[2].trim()
                val message = match.groupValues[3]

                if (date != null && name.isNotBlank()) {
                    flush()
                    sender = name
                    time = date
                    body.append(message)
                }
            }
        }

        flush()

        val imported = mutableListOf<Store.Msg>()

        parsed.forEach { entry ->
            val outgoing =
                entry.sender.equals("Vous", true) ||
                entry.sender.equals("You", true) ||
                entry.sender.equals("Moi", true)

            val key = "import:$pkg:${entry.time}:" +
                "${entry.sender.hashCode()}:${entry.text.hashCode()}"

            if (Store.items.none { it.key == key }) {
                imported.add(
                    Store.Msg(
                        key = key,
                        sender = entry.sender,
                        text = entry.text,
                        action = null,
                        pkg = pkg,
                        time = entry.time,
                        outgoing = outgoing
                    )
                )
            }
        }

        if (imported.isEmpty()) return 0

        Store.items.addAll(imported)
        Store.items.sortByDescending { it.time }

        val array = JSONArray()

        Store.items.forEach { msg ->
            array.put(
                JSONObject()
                    .put("key", msg.key)
                    .put("sender", msg.sender)
                    .put("text", msg.text)
                    .put("pkg", msg.pkg)
                    .put("time", msg.time)
                    .put("outgoing", msg.outgoing)
            )
        }

        context.openFileOutput("messages.json", Context.MODE_PRIVATE)
            .bufferedWriter()
            .use { it.write(array.toString()) }

        Store.refresh()
        return imported.size
    }

    private fun parseDate(value: String): Long? {
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(
                    pattern,
                    Locale.US
                ).apply {
                    isLenient = false
                    timeZone = TimeZone.getDefault()
                }

                val date = format.parse(value)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }

        return null
    }
}
