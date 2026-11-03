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
    val sent = ArrayDeque<String>() // textes envoyés par l'appli

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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return
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

                // Un contact coché (suggestion ou auto) déclenche l'IA, même si le mode global est désactivé
                if (contactMode != null || mode == "suggest" || mode == "auto") {
                    val history = Store.historyFrom(sender).filter { it != text }

                    Gemini.ask(text, history, false) { list ->
                        msg.suggestions = list.take(1)

                        // Réponse envoyée seulement si le contact est en « Auto IA »
                        if (contactMode == AutoContactsStore.MODE_AUTO &&
                            msg.suggestions.isNotEmpty() && action != null) {
                            sendReply(this, action, msg.suggestions[0])
                        }
                    }
                }
        Store.refresh()
    }
}
