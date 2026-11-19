package com.steve.newapp

import android.content.Context

object ServerManager {

    const val MY_IA_ID = "my_ia_v22"
    const val NEW_SERVER_ID = "new_server"

    const val MY_IA_V22 =
        "https://my-server-taai.onrender.com/v1/generate"

    const val NEW_SERVER =
        "http://127.0.0.1:3001/v1/generate"

    fun select(context: Context, id: String) {
        val url = when (id) {
            NEW_SERVER_ID -> NEW_SERVER
            else -> MY_IA_V22
        }

        Prefs.setText(context, "server_choice", id)
        Prefs.setText(context, "server_url", url)

        Gemini.endpoint = url
        Gemini.warmUp()
    }

    fun label(id: String): String {
        return when (id) {
            NEW_SERVER_ID -> "NEW-SERVER (Termux)"
            else -> "MY_IA V2.2"
        }
    }
}
