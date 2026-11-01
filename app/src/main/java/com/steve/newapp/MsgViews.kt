package com.steve.newapp

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

fun renderMessages(ctx: Context, parent: LinearLayout) {
    parent.removeAllViews()
    if (Store.items.isEmpty()) {
        parent.addView(TextView(ctx).apply { text = "Aucun message WhatsApp pour le moment"; setTextColor(Color.BLACK) })
        return
    }
    val hide = Prefs.bool(ctx, "hide_preview")
    for (m in Store.items) {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 16) }
        val shown = if (hide) "message masqué" else m.text
        box.addView(TextView(ctx).apply { text = "${m.sender} : $shown"; setTextColor(Color.BLACK) })
        val reply = EditText(ctx).apply { hint = "Votre réponse"; setTextColor(Color.BLACK) }
        box.addView(reply)
        m.suggestions.forEach { s ->
            box.addView(Button(ctx).apply { text = s; isAllCaps = false; setOnClickListener { reply.setText(s) } })
        }
        box.addView(Button(ctx).apply {
            text = "Suggestions Gemini"; isAllCaps = false
            setOnClickListener {
                Toast.makeText(ctx, "Gemini réfléchit…", Toast.LENGTH_SHORT).show()
                Gemini.ask(m.text) { list -> m.suggestions = list; Store.refresh() }
            }
        })
        box.addView(Button(ctx).apply {
            text = "Envoyer"; isAllCaps = false
            setOnClickListener {
                val a = m.action
                val ok = a != null && reply.text.isNotBlank() && sendReply(ctx, a, reply.text.toString())
                Toast.makeText(ctx, if (ok) "Envoyé" else "Envoi impossible", Toast.LENGTH_SHORT).show()
            }
        })

        // Photos et vidéos
        box.addView(Button(ctx).apply {
            text = "Joindre photo ou vidéo"; isAllCaps = false
            setOnClickListener {
                ctx.startActivity(Intent(ctx, MediaPickActivity::class.java)
                    .putExtra("key", m.key)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        val uri = m.attachment
        if (uri != null) {
            box.addView(TextView(ctx).apply { text = "Pièce jointe prête : ${m.attachType ?: "fichier"}"; setTextColor(Color.BLACK) })
            box.addView(Button(ctx).apply {
                text = "Envoyer la pièce jointe"; isAllCaps = false
                setOnClickListener {
                    // WhatsApp n'accepte pas les médias dans la notification : on ouvre son écran de partage
                    val i = Intent(Intent.ACTION_SEND)
                        .setType(m.attachType ?: "*/*")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .putExtra(Intent.EXTRA_TEXT, reply.text.toString())
                        .setPackage(m.pkg)
                    i.clipData = ClipData.newUri(ctx.contentResolver, "média", uri)
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        ctx.startActivity(i)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "WhatsApp introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
        parent.addView(box)
    }
}
