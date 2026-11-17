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
import java.util.Locale

object Store {
    data class Msg(
        val key: String, val sender: String, val text: String,
        val action: Notification.Action?, val pkg: String,
        var suggestions: List<String> = emptyList(),
        var attachment: Uri? = null, var attachType: String? = null,
        val time: Long = System.currentTimeMillis(), val outgoing: Boolean = false,
        var translation: String? = null, var pending: Runnable? = null,
        var read: Boolean = outgoing
    )

    val items = mutableListOf<Msg>()
    val listeners = mutableSetOf<() -> Unit>()
    val sent = ArrayDeque<String>()
    val APPS = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "com.facebook.orca", "com.facebook.katana",
        "com.snapchat.android", "com.google.android.apps.messaging",
        "com.android.mms", "com.samsung.android.messaging"
    )
    private val main = Handler(Looper.getMainLooper())
    private var file: File? = null
    private val processed = LinkedHashMap<String, Long>()

    fun init(ctx: Context) {
        if (file != null) return
        file = File(ctx.filesDir, "messages.json")
        val f = file ?: return
        if (!f.exists()) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Msg(
                    key = o.getString("key"),
                    sender = o.getString("sender"),
                    text = o.getString("text"),
                    action = null,
                    pkg = o.getString("pkg"),
                    time = o.getLong("time"),
                    outgoing = o.getBoolean("outgoing"),
                    read = o.optBoolean("read", true)
                ))
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
            arr.put(JSONObject()
                .put("key", m.key).put("sender", m.sender).put("text", m.text)
                .put("pkg", m.pkg).put("time", m.time).put("outgoing", m.outgoing)
                .put("read", m.read))
        }
        try { f.writeText(arr.toString()) } catch (_: Exception) {}
    }

    fun add(m: Msg) { items.add(0, m); save(); refresh() }

    // Applications actuellement synchronisées.
    fun enabledApps(ctx: Context): Set<String> =
        APPS.filterTo(mutableSetOf()) { pkg ->
            Prefs.bool(ctx, "app_$pkg", true) &&
                (pkg != "com.whatsapp.w4b" ||
                    Prefs.bool(ctx, "include_business", true))
        }

    // Nombre de messages reçus non lus.
    fun unreadCount(ctx: Context, pkg: String? = null): Int {
        val enabled = enabledApps(ctx)
        return items.count { m ->
            !m.outgoing && !m.read &&
                m.pkg in enabled &&
                (pkg == null || m.pkg == pkg)
        }
    }

    // Ouvrir une conversation dans NEW marque ses messages comme lus.
    fun markConversationRead(pkg: String, sender: String) {
        var changed = false
        items.filter {
            it.pkg == pkg && it.sender == sender && !it.outgoing && !it.read
        }.forEach {
            it.read = true
            changed = true
        }
        if (changed) {
            save()
            refresh()
        }
    }

    // Historique complet : messages reçus et réponses envoyées par NEW.
    fun historyFrom(sender: String, count: Int = 30): List<String> =
        items.asSequence()
            .filter { it.sender.equals(sender, ignoreCase = true) && it.text.isNotBlank() }
            .sortedByDescending { it.time }
            .take(count)
            .toList()
            .asReversed()
            .map { if (it.outgoing) "Steve : ${it.text}" else "$sender : ${it.text}" }

    private fun normalizeText(value: String) =
        value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    // Garde une trace des messages envoyés, même après fermeture ou redémarrage.
    fun rememberSent(ctx: Context, text: String) {
        if (text.isBlank()) return
        sent.addFirst(text)
        while (sent.size > 50) sent.removeLast()

        val prefs = ctx.getSharedPreferences("new_outgoing_guard", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val values = mutableListOf<Pair<Long, String>>()
        try {
            val old = JSONArray(prefs.getString("sent_texts", "[]") ?: "[]")
            for (i in 0 until old.length()) {
                val item = old.getJSONObject(i)
                if (now - item.optLong("time") < 10 * 60 * 1000L) {
                    values.add(item.optLong("time") to item.optString("text"))
                }
            }
        } catch (_: Exception) {}
        values.add(now to text)
        val saved = JSONArray()
        values.takeLast(100).forEach { (time, value) ->
            saved.put(JSONObject().put("time", time).put("text", value))
        }
        prefs.edit().putString("sent_texts", saved.toString()).apply()
    }

    fun wasSentByUs(ctx: Context, text: String): Boolean {
        val target = normalizeText(text)
        if (target.isEmpty()) return false
        if (sent.any { normalizeText(it) == target }) return true

        val prefs = ctx.getSharedPreferences("new_outgoing_guard", Context.MODE_PRIVATE)
        return try {
            val arr = JSONArray(prefs.getString("sent_texts", "[]") ?: "[]")
            val now = System.currentTimeMillis()
            (0 until arr.length()).any { i ->
                val item = arr.getJSONObject(i)
                now - item.optLong("time") in 0..(10 * 60 * 1000L) &&
                    normalizeText(item.optString("text")) == target
            }
        } catch (_: Exception) { false }
    }

    // Déduplique les notifications répétées, même si Android change leur identifiant.
    fun isDuplicateNotification(pkg: String, sender: String, text: String): Boolean {
        val now = System.currentTimeMillis()
        val fingerprint = "$pkg|$sender|" + normalizeText(text)
        val previous = processed[fingerprint]
        processed[fingerprint] = now
        processed.entries.removeAll { now - it.value > 2 * 60 * 1000L }
        while (processed.size > 300) processed.remove(processed.keys.first())
        return previous != null && now - previous < 90 * 1000L
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
        } catch (_: Exception) {}
    }

    private fun saveAsked() {
        val f = askedFile ?: return
        val o = JSONObject()
        asked.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        try { f.writeText(o.toString()) } catch (_: Exception) {}
    }

    fun askedTo(sender: String): List<String> = asked[sender].orEmpty().takeLast(40)

    fun rememberAsked(sender: String, list: List<String>) {
        asked.getOrPut(sender) { mutableListOf() }.addAll(list)
        saveAsked()
    }

    fun later(ms: Long, r: Runnable) { main.postDelayed(r, ms) }

    fun cancel(m: Msg) {
        m.pending?.let { main.removeCallbacks(it) }
        m.pending = null
    }

    fun cancelPendingFor(pkg: String, sender: String) {
        items.filter { it.pkg == pkg && it.sender == sender && !it.outgoing }
            .forEach { cancel(it) }
    }

    fun refresh() { main.post { listeners.toList().forEach { it() } } }
}

fun sendReply(ctx: Context, action: Notification.Action, text: String): Boolean {
    return try {
        val inputs = action.remoteInputs
        if (inputs.isNullOrEmpty() || text.isBlank()) return false
        val bundle = Bundle()
        inputs.forEach { bundle.putCharSequence(it.resultKey, text) }
        val intent = Intent()
        RemoteInput.addResultsToIntent(inputs, intent, bundle)
        action.actionIntent.send(ctx, 0, intent)
        Store.rememberSent(ctx, text)
        true
    } catch (e: Exception) {
        false
    }
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
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        val sender = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            .orEmpty().ifEmpty { "Message" }

        // Ignore les notifications des messages envoyés par Steve.
        if (sender.equals("Vous", true) || sender.equals("You", true) ||
            Store.wasSentByUs(this, text)) return

        // Ignore les actualisations et répétitions d'une même notification.
        if (Store.items.any { it.key == sbn.key && it.text == text && !it.outgoing }) return
        if (Store.isDuplicateNotification(pkg, sender, text)) return

        val action = n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        val msg = Store.Msg(sbn.key, sender, text, action, pkg)
        Store.add(msg)

        // Chaque message reçu reçoit des suggestions.
        // Le mode global ne peut jamais activer l'envoi automatique.
        val useAi = true

        if (useAi) {
            val history = Store.historyFrom(sender).filter { it != text }
            val persona = Personality.normalize(Prefs.text(this, "persona_$sender", Gemini.personality))
            Gemini.ask(
                message = text,
                history = history,
                mode = "brouillon",
                avoid = Store.askedTo(sender),
                lang = Prefs.text(this, "lang_$sender", Gemini.lang),
                personality = persona,
                rules = Prefs.text(this, "rules"),
                about = Prefs.text(this, "about"),
                memory = MemoryStore.context(this, sender)
            ) { list ->
                // Une seule réponse IA pour ce message.
                msg.suggestions = list.take(1)
                Store.rememberAsked(sender, msg.suggestions)

                // Vérification au moment de la réponse Gemini :
                // Auto doit toujours être activé pour ce contact précis.
                val shouldAutoReply =
                    AutoContactsStore.modeFor(this, pkg, sender) ==
                        AutoContactsStore.MODE_AUTO

                if (shouldAutoReply &&
                    msg.suggestions.isNotEmpty() && action != null && msg.pending == null) {
                    val delayMs = (Prefs.text(this, "auto_delay", "60").toLongOrNull() ?: 60L)
                        .coerceIn(0L, 120L) * 1000L
                    val task = Runnable {
                        if (msg.pending == null ||
                            AutoContactsStore.modeFor(this, pkg, sender) !=
                                AutoContactsStore.MODE_AUTO) {
                            msg.pending = null
                            return@Runnable
                        }
                        msg.pending = null
                        val answer = msg.suggestions.first()
                        if (sendReply(this, action, answer)) {
                            val now = System.currentTimeMillis()
                            Store.add(
                                Store.Msg(
                                    key = "auto" + now,
                                    sender = msg.sender,
                                    text = answer,
                                    action = null,
                                    pkg = msg.pkg,
                                    time = now,
                                    outgoing = true
                                )
                            )
                        } else {
                            Store.refresh()
                        }
                    }
                    msg.pending = task
                    Store.later(delayMs, task)
                }
            }
        }
        Store.refresh()
    }
}
