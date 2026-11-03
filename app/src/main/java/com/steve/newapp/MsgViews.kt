package com.steve.newapp

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Discussion ouverte (clé "appli|expéditeur"), null = liste des discussions
private var openChat: String? = null

private const val RED = "#FF1744"
private const val BUBBLE_IN = "#262633"

private fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

private fun rounded(ctx: Context, color: Int, radiusDp: Int, oval: Boolean = false) =
    GradientDrawable().apply {
        if (oval) shape = GradientDrawable.OVAL else cornerRadius = ctx.dp(radiusDp).toFloat()
        setColor(color)
    }

private fun conversationKey(m: Store.Msg) = "${m.pkg}|${m.sender}"

private fun timeLabel(t: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))

fun renderMessages(ctx: Context, parent: LinearLayout, dark: Boolean = false) {
    Store.init(ctx)
    parent.removeAllViews()
    if (dark) parent.setBackgroundColor(Color.parseColor("#0B0B12"))
    val chat = openChat
    if (chat == null) renderList(ctx, parent, dark) else renderChat(ctx, parent, chat)
}

private fun renderList(ctx: Context, parent: LinearLayout, dark: Boolean) {
    val fg = if (dark) Color.WHITE else Color.BLACK
    val sub = if (dark) Color.parseColor("#9AA0A6") else Color.DKGRAY
    if (Store.items.isEmpty()) {
        parent.addView(TextView(ctx).apply {
            text = "Aucune discussion pour le moment"
            setTextColor(sub)
            setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), 0)
        })
        return
    }
    for ((k, msgs) in Store.items.groupBy { conversationKey(it) }) {
        val last = msgs.first()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            setOnClickListener { openChat = k; Store.refresh() }
            setOnLongClickListener {
                doDelete(ctx) { Store.deleteWhere { conversationKey(it) == k } }
                true
            }
        }
        row.addView(
            TextView(ctx).apply {
                text = last.sender.take(1).uppercase()
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                background = rounded(ctx, Color.parseColor(RED), 0, oval = true)
            },
            LinearLayout.LayoutParams(ctx.dp(46), ctx.dp(46))
        )
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(12), 0, ctx.dp(8), 0)
        }
        texts.addView(TextView(ctx).apply {
            text = last.sender
            setTextColor(fg)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        })
        val shown = when {
            Prefs.bool(ctx, "hide_preview") -> "Message masqué"
            last.outgoing -> "Vous : ${last.text}"
            else -> last.text
        }
        texts.addView(TextView(ctx).apply {
            text = shown
            setTextColor(sub)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(ctx).apply {
            text = timeLabel(last.time)
            setTextColor(sub)
            textSize = 12f
        })
        parent.addView(row)
    }
}

private fun renderChat(ctx: Context, parent: LinearLayout, k: String) {
    val msgs = Store.items.filter { conversationKey(it) == k }.sortedBy { it.time }
    val lastIn = msgs.lastOrNull { !it.outgoing }
    val title = msgs.lastOrNull()?.sender ?: "Discussion"
    val hide = Prefs.bool(ctx, "hide_preview")

    // En-tête avec bouton retour
    parent.addView(LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(8), ctx.dp(10), ctx.dp(12), ctx.dp(10))
        setBackgroundColor(Color.parseColor("#14141F"))
        addView(TextView(ctx).apply {
            text = "←"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(ctx.dp(8), 0, ctx.dp(12), 0)
            setOnClickListener { openChat = null; Store.refresh() }
        })
        addView(TextView(ctx).apply {
            text = title
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })
    })

    // Bulles de messages : reçus à gauche, envoyés à droite
    val messages = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(10), ctx.dp(8), ctx.dp(10), ctx.dp(8))
    }
    for (m in msgs) {
        val shown = if (hide && !m.outgoing) "Message masqué" else m.text
        messages.addView(
            TextView(ctx).apply {
                text = shown
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(ctx.dp(14), ctx.dp(9), ctx.dp(14), ctx.dp(9))
                background = rounded(ctx, Color.parseColor(if (m.outgoing) RED else BUBBLE_IN), 18)
                setOnLongClickListener {
                    doDelete(ctx) { Store.deleteWhere { it === m } }
                    true
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (m.outgoing) Gravity.END else Gravity.START
                setMargins(0, ctx.dp(3), 0, ctx.dp(3))
            }
        )
    }
    parent.addView(messages)

    val input = EditText(ctx).apply {
        hint = "Message"
        setTextColor(Color.WHITE)
        setHintTextColor(Color.GRAY)
        background = rounded(ctx, Color.parseColor(BUBBLE_IN), 20)
        setPadding(ctx.dp(14), ctx.dp(8), ctx.dp(14), ctx.dp(8))
    }

    // Suggestions Gemini sous forme de pastilles
    val chips = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(ctx.dp(6), ctx.dp(4), ctx.dp(6), ctx.dp(4))
    }
    fun addChip(label: String, onTap: () -> Unit) {
        chips.addView(
            TextView(ctx).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(ctx.dp(12), ctx.dp(6), ctx.dp(12), ctx.dp(6))
                background = rounded(ctx, Color.parseColor("#33FF1744"), 16)
                setOnClickListener { onTap() }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(ctx.dp(4), 0, ctx.dp(4), 0)
            }
        )
    }
    if (lastIn != null) {
        addChip("✨ Gemini") {
            Toast.makeText(ctx, "Gemini réfléchit…", Toast.LENGTH_SHORT).show()
            Gemini.ask(lastIn.text) { list -> lastIn.suggestions = list; Store.refresh() }
        }
        lastIn.suggestions.forEach { s -> addChip(s) { input.setText(s) } }
    }
    addChip("🗑 Supprimer") {
        doDelete(ctx) { openChat = null; Store.deleteWhere { conversationKey(it) == k } }
    }
    parent.addView(HorizontalScrollView(ctx).apply {
        isHorizontalScrollBarEnabled = false
        addView(chips)
    })

    // Pièce jointe prête (ajoutée via 📎)
    val uri: Uri? = lastIn?.attachment
    if (lastIn != null && uri != null) {
        parent.addView(TextView(ctx).apply {
            text = "📎 Pièce jointe prête : ${lastIn.attachType ?: "fichier"} (appuyer pour envoyer)"
            setTextColor(Color.WHITE)
            setPadding(ctx.dp(14), ctx.dp(4), ctx.dp(14), ctx.dp(4))
            setOnClickListener { shareAttachment(ctx, lastIn.pkg, uri, lastIn.attachType, input.text.toString()) }
        })
    }

    // Barre de saisie
    val bar = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(10), ctx.dp(6), ctx.dp(10), ctx.dp(10))
    }
    bar.addView(TextView(ctx).apply {
        text = "📎"
        textSize = 20f
        setPadding(ctx.dp(6), 0, ctx.dp(8), 0)
        setOnClickListener {
            if (lastIn != null) {
                ctx.startActivity(
                    Intent(ctx, MediaPickActivity::class.java)
                        .putExtra("key", lastIn.key)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    })
    bar.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    bar.addView(
        TextView(ctx).apply {
            text = "➤"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(ctx, Color.parseColor(RED), 0, oval = true)
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isEmpty()) return@setOnClickListener
                val target = msgs.lastOrNull { !it.outgoing && it.action != null }
                val act = target?.action
                if (target == null || act == null) {
                    Toast.makeText(ctx, "Réponse impossible : notification expirée", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (sendReply(ctx, act, text)) {
                    val now = System.currentTimeMillis()
                    Store.add(
                        Store.Msg(
                            key = "out$now",
                            sender = target.sender,
                            text = text,
                            action = null,
                            pkg = target.pkg,
                            time = now,
                            outgoing = true
                        )
                    )
                } else {
                    Toast.makeText(ctx, "Envoi impossible", Toast.LENGTH_SHORT).show()
                }
            }
        },
        LinearLayout.LayoutParams(ctx.dp(42), ctx.dp(42)).apply { setMargins(ctx.dp(8), 0, 0, 0) }
    )
    parent.addView(bar)
}

private fun shareAttachment(ctx: Context, pkg: String, uri: Uri, type: String?, text: String) {
    // WhatsApp n'accepte pas les médias dans la notification : on ouvre son écran de partage
    val i = Intent(Intent.ACTION_SEND)
        .setType(type ?: "*/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .setPackage(pkg)
    i.clipData = ClipData.newUri(ctx.contentResolver, "média", uri)
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, "WhatsApp introuvable", Toast.LENGTH_SHORT).show()
    }
}

private fun doDelete(ctx: Context, onYes: () -> Unit) {
    onYes()
    Toast.makeText(ctx, "Supprimé", Toast.LENGTH_SHORT).show()
}
