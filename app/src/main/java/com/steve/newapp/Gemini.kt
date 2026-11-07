package com.steve.newapp

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    // Modifiable dans Paramètres > Avancé. La route doit correspondre à server.js.
    var endpoint = "http://127.0.0.1:3001/reply"

    private fun post(body: JSONObject): JSONObject? = try {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    } catch (e: Exception) {
        null
    }

    // Une seule suggestion, basée sur l'historique des notifications.
    // Si fromMe = true, le serveur ne répond pas (message envoyé par Steve).
    fun ask(
        message: String,
        history: List<String> = emptyList(),
        fromMe: Boolean = false,
        mode: String = "suggestions",
        cb: (List<String>) -> Unit
    ) {
        Thread {
            val body = JSONObject()
                .put("message", message)
                .put("history", JSONArray(history))
                .put("mode", mode)
                .put("from_me", fromMe)
            val reply = post(body)?.optString("reply")?.trim().orEmpty()
            val items = if (mode == "questions") reply.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        else if (reply.isNotEmpty()) listOf(reply) else emptyList()
            cb(items)
        }.start()
    }

    fun chat(prompt: String, cb: (String) -> Unit) {
        Thread {
            val body = JSONObject()
                .put("message", prompt)
                .put("mode", "reply")
            val reply = post(body)?.optString("reply")?.trim().orEmpty()
            cb(reply.ifEmpty { "Pas de réponse du serveur Gemini." })
        }.start()
    }
}
