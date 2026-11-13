package com.steve.newapp

import com.steve.newapp.settings.AutoContactsStore
import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object Store {
    data class Msg(
        val key: String,
        val sender: String,
        val text: String,
        val action: Notification.Action?,
        val pkg: String,
        var suggestions: List<String> = emptyList(),
        var attachment: Uri? = null,
        var attachType: String? = null,
        val time: Long = System.currentTimeMillis(),
        val outgoing: Boolean = false
    )

    val items = mutableListOf<Msg>() // le plus récent en premier
    val listeners = mutableSetOf<() -> Unit>()
    val sent = ArrayDeque<String>()
    val APPS = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "com.facebook.orca", "com.facebook.katana",
        "com.snapchat.android",
        "com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging"
    ) // textes envoyés par l'appli

    private val main = Handler(Looper.getMainLooper())
    private var file: File? = null

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "messages.json")
        file = f
        if (!f.exists()) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(
                    Msg(
                        key = o.getString("key"),
                        sender = o.getString("sender"),
                        text = o.getString("text"),
                        action = null,
                        pkg = o.getString("pkg"),
                        time = o.getLong("time"),
                        outgoing = o.getBoolean("outgoing")
                    )
                )
            }
        } catch (e: Exception) {
            f.renameTo(File(ctx.filesDir, "messages.bak"))
            items.clear()
        }
    }

    private fun save() {
        val f = file ?: return
        val arr = JSONArray()
        items.forEach { m ->
            arr.put(
                JSONObject()
                    .put("key", m.key)
                    .put("sender", m.sender)
                    .put("text", m.text)
                    .put("pkg", m.pkg)
                    .put("time", m.time)
                    .put("outgoing", m.outgoing)
            )
        }
        try {
            f.writeText(arr.toString())
        } catch (e: Exception) {
        }
    }

    fun add(m: Msg) {
        items.add(0, m)
        save()
        refresh()
    }

    // Derniers messages reçus d'un contact, du plus ancien au plus récent
    fun historyFrom(sender: String, count: Int = 20): List<String> =
        items.filter { !it.outgoing && it.sender == sender }
            .take(count)
            .map { it.text }
            .reversed()

    fun rememberSent(text: String) {
        sent.addFirst(text)
        while (sent.size > 50) sent.removeLast()
    }

    fun deleteWhere(p: (Msg) -> Boolean) {
        items.removeAll(p)
        save()
        refresh()
    }

    private val asked = mutableMapOf<String, MutableList<String>>()
    private var askedFile: File? = null

    fun loadAsked(ctx: Context) {
        val f = File(ctx.filesDir, "asked.json")
        askedFile = f
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText())
            for (k in o.keys()) {
                val a = o.getJSONArray(k)
                asked[k] = MutableList(a.length()) { a.getString(it) }
            }
        } catch (e: Exception) {
        }
    }

    private fun saveAsked() {
        val f = askedFile ?: return
        val o = JSONObject()
        asked.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        try {
            f.writeText(o.toString())
        } catch (e: Exception) {
        }
    }

    fun askedTo(sender: String): List<String> = asked[sender].orEmpty().takeLast(40)

    fun rememberAsked(sender: String, list: List<String>) {
        asked.getOrPut(sender) { mutableListOf() }.addAll(list)
        saveAsked()
    }

    fun refresh() {
        main.post { listeners.toList().forEach { it() } }
    }
}

fun sendReply(ctx: Context, action: Notification.Action, text: String): Boolean = try {
    val bundle = Bundle()
    action.remoteInputs.forEach { bundle.putCharSequence(it.resultKey, text) }
    val intent = Intent()
    RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
    action.actionIntent.send(ctx, 0, intent)
    Store.rememberSent(text)
    true
} catch (e: Exception) {
    false
}

class NotifService : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Store.loadAsked(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in Store.APPS || !Prefs.bool(this, "app_$pkg", true)) return
        if (pkg == "com.whatsapp.w4b" && !Prefs.bool(this, "include_business", true)) return
        val n = sbn.notification
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val sender = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "WhatsApp"

        // Ignore les notifications de tes propres messages envoyés
        if (sender == "Vous" || sender == "You" || Store.sent.contains(text)) return

        // WhatsApp republie la même notification : on évite les doublons
        if (Store.items.any { it.key == sbn.key && it.text == text }) return

        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        val msg = Store.Msg(sbn.key, sender, text, action, pkg)
        Store.add(msg)

        val mode = Prefs.mode(this)
        val contactMode = AutoContactsStore.modeFor(this, sender)

        // Réglage par contact : "manual" = aucune IA, "suggest" ou "auto" = IA,
        // sans réglage (null) = on suit le mode global
        val useAi = when (contactMode) {
            null -> mode == "suggest" || mode == "auto"
            "manual" -> false
            else -> true
        }

        if (useAi) {
            val history = Store.historyFrom(sender).filter { it != text }

            Gemini.ask(text, history, false, avoid = Store.askedTo(sender), lang = Prefs.text(this, "lang_$sender", Gemini.lang), personality = Prefs.text(this, "persona_$sender", Gemini.personality)) { list ->
                msg.suggestions = list.take(1)
                Store.rememberAsked(sender, msg.suggestions)

                // Envoi automatique seulement pour un contact en Auto
                if (contactMode == AutoContactsStore.MODE_AUTO &&
                    msg.suggestions.isNotEmpty() && action != null) {
                    sendReply(this, action, msg.suggestions[0])
                }
            }
        }
        Store.refresh()
    }
}
