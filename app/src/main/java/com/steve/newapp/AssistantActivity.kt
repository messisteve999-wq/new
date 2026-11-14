package com.steve.newapp

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AssistantActivity : Activity() {
    private val history = StringBuilder()
    private lateinit var log: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "Assistant IA"; textSize = 22f; setTextColor(Color.BLACK) })
        log = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(log) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val input = EditText(this).apply { hint = "Dites-lui quoi faire…"; setTextColor(Color.BLACK) }
        root.addView(input)
        root.addView(Button(this).apply {
            text = "Envoyer"; isAllCaps = false
            setOnClickListener {
                val msg = input.text.toString().trim()
                if (msg.isEmpty()) return@setOnClickListener
                input.setText("")
                addLine(ctx, "Vous : $msg")
                Gemini.chat(buildPrompt(ctx, msg, history.toString())) { reply ->
                    runOnUiThread { addLine(ctx, "IA : $reply") }
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Questions pour mieux la connaître"; isAllCaps = false
            setOnClickListener {
                Gemini.ask(
                    "Aide-moi à poser des questions sincères et respectueuses pour mieux connaître la personne.",
                    Store.items.filter { !it.outgoing }.take(20).map { it.text }.reversed(),
                    false,
                    "questions"
                ) { list ->
                    runOnUiThread {
                        list.forEach { q ->
                            log.addView(Button(ctx).apply {
                                text = q; isAllCaps = false
                                setOnClickListener { input.setText(q) }
                            })
                        }
                    }
                }
            }
        })
        root.addView(Button(this).apply {
            text = personalityLabel(); isAllCaps = false
            setOnClickListener {
                val order = listOf("connaissance", "serieux", "debat", "chaleureux")
                Gemini.personality = order[(order.indexOf(Gemini.personality) + 1) % order.size]
                Prefs.setText(ctx, "personality", Gemini.personality)
                text = personalityLabel()
            }
        })
        setContentView(root)
    }

    private fun personalityLabel(): String =
        when (Gemini.personality) {
            "serieux" -> "Personnalité : sérieux"
            "debat" -> "Personnalité : débat sincère"
            "chaleureux" -> "Personnalité : chaleureuse et douce"
            else -> "Personnalité : faire connaissance"
        }

    private fun addLine(ctx: Context, s: String) {
        log.addView(TextView(ctx).apply { text = s; setTextColor(Color.BLACK); setPadding(0, 12, 0, 12) })
        history.append(s).append("\n")
    }
}

// Règles, infos sur l'utilisateur, contacts (si autorisés) et historique envoyés à Gemini
fun buildPrompt(ctx: Context, msg: String, history: String): String {
    val rules = Prefs.text(ctx, "rules", "Réponds en français, de façon courte, naturelle et polie.")
    val about = Prefs.text(ctx, "about")
    val contacts = if (Prefs.bool(ctx, "share_contacts")) Contacts.names(ctx).joinToString(", ") else ""
    return buildString {
        append("Règles de réponse : ").append(rules).append("\n")
        if (about.isNotEmpty()) append("À propos de l'utilisateur : ").append(about).append("\n")
        if (contacts.isNotEmpty()) append("Contacts : ").append(contacts).append("\n")
        append("Conversation :\n").append(history.takeLast(4000))
        append("\nUtilisateur : ").append(msg)
    }
}
