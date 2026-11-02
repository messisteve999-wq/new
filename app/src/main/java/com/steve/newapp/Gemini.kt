package com.steve.newapp

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
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        null
    }

    fun ask(message: String, cb: (List<String>) -> Unit) {
        Thread {
            val j = post(JSONObject().put("message", message).put("mode", "suggestions"))
            val arr = j?.optJSONArray("suggestions")
            cb(if (arr != null) (0 until arr.length()).map { arr.getString(it) }
               else listOfNotNull(j?.optString("reply")?.takeIf { it.isNotEmpty() }))
        }.start()
    }

    fun chat(prompt: String, cb: (String) -> Unit) {
        Thread {
            val j = post(JSONObject().put("message", prompt).put("mode", "chat"))
            cb(j?.optString("reply")?.takeIf { it.isNotEmpty() } ?: "Pas de réponse du serveur Gemini.")
        }.start()
    }
}
