package com.steve.newapp.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.steve.newapp.R

class AutoContactsActivity : Activity() {

    private val permReq = 101
    private var allNames: List<String> = emptyList()
    private val shown = mutableListOf<String>()
    private var modes = mutableMapOf<String, String>()
    private var currentQuery = ""
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_contacts)

        modes = AutoContactsStore.getModes(this)
        val list = findViewById<ListView>(R.id.contactsList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ -> cycleMode(pos) }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                applyFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // « Tout sélectionner » met tous les contacts en suggestion (jamais en auto)
        findViewById<Button>(R.id.btnSelectAll).setOnClickListener {
            allNames.forEach { modes.putIfAbsent(it, AutoContactsStore.MODE_SUGGEST) }
            applyFilter(currentQuery)
        }
        findViewById<Button>(R.id.btnSelectNone).setOnClickListener {
            modes.clear()
            applyFilter(currentQuery)
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            AutoContactsStore.setModes(this, modes)
            Toast.makeText(this, "Contacts enregistrés", Toast.LENGTH_SHORT).show()
            finish()
        }

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), permReq)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permReq) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                Toast.makeText(this, "Permission des contacts refusée", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadContacts() {
        allNames = readContactNames().distinct().sorted()
        applyFilter("")
    }

    private fun cycleMode(pos: Int) {
        val name = shown[pos]
        when (modes[name]) {
            null -> modes[name] = AutoContactsStore.MODE_SUGGEST
            AutoContactsStore.MODE_SUGGEST -> modes[name] = AutoContactsStore.MODE_AUTO
            else -> modes.remove(name)
        }
        applyFilter(currentQuery)
    }

    private fun labelFor(name: String): String = when (modes[name]) {
        AutoContactsStore.MODE_AUTO -> "$name  —  Auto IA"
        AutoContactsStore.MODE_SUGGEST -> "$name  —  Suggestion (fenêtre)"
        else -> "$name  —  Désactivé"
    }

    private fun applyFilter(q: String) {
        currentQuery = q
        shown.clear()
        shown.addAll(allNames.filter { it.contains(q, ignoreCase = true) })
        adapter.clear()
        adapter.addAll(shown.map { labelFor(it) })
        adapter.notifyDataSetChanged()
    }

    private fun readContactNames(): List<String> {
        val names = mutableListOf<String>()
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val n = it.getString(0)?.trim()
                if (!n.isNullOrEmpty()) names.add(n)
            }
        }
        return names
    }
}
