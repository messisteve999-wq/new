package com.steve.newapp

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.steve.newapp.settings.AutoContactsStore

object ContactModesPanel {

    private val options = listOf(
        "suggest" to "Suggestions",
        "manual" to "Manuel",
        "auto" to "Auto"
    )

    fun knownContacts(): List<String> =
        Store.items.filter { !it.outgoing }.map { it.sender }.distinct()

    fun build(ctx: Context): LinearLayout {
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val modes = AutoContactsStore.getModes(ctx)
        val contacts = knownContacts()
        if (contacts.isEmpty()) {
            root.addView(TextView(ctx).apply {
                text = "Aucun contact pour le moment."
                setTextColor(Color.WHITE)
                textSize = 14f
            })
            return root
        }
        contacts.forEach { name -> root.addView(buildRow(ctx, name, modes[name])) }
        return root
    }

    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private fun buildRow(ctx: Context, name: String, current: String?): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(ctx, 16), 0, dp(ctx, 16))
        }
        row.addView(TextView(ctx).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 15f
        })

        val buttons = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(ctx, 8), 0, 0)
        }

        options.forEach { (value, label) ->
            val selected = value == current
            val btn = TextView(ctx).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(ctx, 8), 0, dp(ctx, 8))
                setTextColor(if (selected) Color.WHITE else Color.parseColor("#FF1744"))
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 50).toFloat()
                    setColor(if (selected) Color.parseColor("#FF1744") else Color.BLACK)
                    setStroke(dp(ctx, 1), Color.parseColor("#FF1744"))
                }
                setOnClickListener {
                    val newModes = AutoContactsStore.getModes(ctx)
                    if (newModes[name] == value) newModes.remove(name) else newModes[name] = value
                    AutoContactsStore.setModes(ctx, newModes)
                    val parent = row.parent as? LinearLayout
                    if (parent != null) {
                        val index = parent.indexOfChild(row)
                        parent.removeViewAt(index)
                        parent.addView(buildRow(ctx, name, newModes[name]), index)
                    }
                }
            }
            buttons.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(ctx, 3), 0, dp(ctx, 3), 0)
            })
        }

        row.addView(buttons)
        return row
    }
}
