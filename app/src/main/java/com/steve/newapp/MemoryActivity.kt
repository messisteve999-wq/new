package com.steve.newapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MemoryActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var contactField: EditText
    private lateinit var memoryField: EditText

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun title(value: String) = TextView(this).apply {
        text = value
        textSize = 17f
        setTextColor(Color.BLACK)
        setPadding(0, dp(16), 0, dp(8))
    }

    private fun button(value: String, action: () -> Unit) =
        Button(this).apply {
            text = value
            isAllCaps = false
            setOnClickListener { action() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Store.init(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        root.addView(title("NEW V32 - Mémoire"))

        root.addView(TextView(this).apply {
            text = "Les souvenirs restent enregistrés sur cet appareil."
            textSize = 13f
            setTextColor(Color.DKGRAY)
        })

        contactField = EditText(this).apply {
            hint = "Nom du contact"
            setText(intent.getStringExtra("contact").orEmpty())
        }
        root.addView(contactField)

        memoryField = EditText(this).apply {
            hint = "Souvenir à retenir pour ce contact"
            minLines = 2
            setTextColor(Color.BLACK)
        }
        root.addView(memoryField)

        root.addView(button("Enregistrer le souvenir") {
            val contact = contactField.text.toString().trim()
            val memory = memoryField.text.toString().trim()

            if (contact.isEmpty() || memory.isEmpty()) {
                Toast.makeText(
                    this, "Remplis le contact et le souvenir",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (MemoryStore.add(this, contact, memory)) {
                memoryField.text.clear()
                Toast.makeText(this, "Souvenir enregistré", Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                Toast.makeText(
                    this, "Souvenir déjà présent ou invalide",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        root.addView(title("Contacts connus"))

        val contacts = (
            Store.items.filter { !it.outgoing }.map { it.sender } +
            MemoryStore.all(this).keys
        ).distinct().sorted()

        contacts.forEach { contact ->
            root.addView(button(contact) {
                contactField.setText(contact)
                refresh()
            })
        }

        root.addView(title("Souvenirs enregistrés"))

        val scroll = ScrollView(this).apply {
            addView(root)
        }
        setContentView(scroll)
        refresh()
    }

    private fun refresh() {
        val contact = contactField.text.toString().trim()

        val old = root.findViewWithTag<LinearLayout>("memory_list")
        if (old != null) root.removeView(old)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "memory_list"
        }

        if (contact.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Choisis un contact ou saisis son nom."
                setTextColor(Color.DKGRAY)
            })
        } else {
            val memories = MemoryStore.memories(this, contact)

            if (memories.isEmpty()) {
                list.addView(TextView(this).apply {
                    text = "Aucun souvenir pour $contact."
                    setTextColor(Color.DKGRAY)
                })
            }

            memories.forEachIndexed { index, memory ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                }

                row.addView(TextView(this).apply {
                    text = memory
                    textSize = 14f
                    setTextColor(Color.BLACK)
                })

                row.addView(button("Supprimer ce souvenir") {
                    MemoryStore.delete(this@MemoryActivity, contact, index)
                    refresh()
                })

                list.addView(row)
            }

            if (memories.isNotEmpty()) {
                list.addView(button("Effacer toute la mémoire de $contact") {
                    MemoryStore.deleteContact(this, contact)
                    refresh()
                    Toast.makeText(
                        this, "Mémoire supprimée", Toast.LENGTH_SHORT
                    ).show()
                })
            }
        }

        root.addView(list)
    }
}
