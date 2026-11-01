package com.steve.newapp

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

object Store {
    data class Msg(
        val key: String,
        val sender: String,
        val text: String,
        val action: Notification.Action?,
        val pkg: String,
        var suggestions: List<String> = emptyList(),
        var attachment: Uri? = null,
        var attachType: String? = null
    )

    val items = mutableListOf<Msg>()
    val listeners = mutableSetOf<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun add(m: Msg) {
        items.add(0, m)
        if (items.size > 50) items.removeAt(items.size - 1)
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
    true
} catch (e: Exception) {
    false
}

class NotifService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return
        if (pkg == "com.whatsapp.w4b" && !Prefs.bool(this, "include_business", true)) return
        val n = sbn.notification
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val sender = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "WhatsApp"
        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        val msg = Store.Msg(sbn.key, sender, text, action, pkg)
        Store.add(msg)

        val mode = Prefs.mode(this)
        if (mode == "suggest" || mode == "auto") {
            Gemini.ask(text) { list ->
                msg.suggestions = list
                if (mode == "auto" && list.isNotEmpty() && action != null) sendReply(this, action, list[0])
                Store.refresh()
            }
        }
    }
}
