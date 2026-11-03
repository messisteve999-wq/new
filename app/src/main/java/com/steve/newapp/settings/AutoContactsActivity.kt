package com.steve.newapp.settings

import android.app.Activity
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import com.steve.newapp.R

class AutoContactsActivity : Activity() {

    private lateinit var allNames: List<String>
    private lateinit var shown: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private var selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_contacts)

        selected = AutoContactsStore.getAllowed(this).toMutableSet()
        allNames = loadContactNames().distinct().sorted()
        shown = allNames.toMutableList()

        val list = findViewById<ListView>(R.id.contactsList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, shown)
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        list.adapter = adapter
        refreshChecks(list)

        list.setOnItemClickListener { _, _, pos, _ ->
            val name = shown[pos]
            if (selected.contains(name)) selected.remove(name) else selected.add(name)
        }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s?.toString() ?: ""
                shown.clear()
                shown.addAll(allNames.filter { it.contains(q, ignoreCase = true) })
                adapter.notifyDataSetChanged()
                refreshChecks(list)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.btnSelectAll).setOnClickListener {
            selected = allNames.toMutableSet()
            refreshChecks(list)
        }
        findViewById<Button>(R.id.btnSelectNone).setOnClickListener {
            selected.clear()
            refreshChecks(list)
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            AutoContactsStore.setAllowed(this, selected)
            finish()
        }
    }

    private fun refreshChecks(list: ListView) {
        for (i in shown.indices) {
            list.setItemChecked(i, selected.contains(shown[i]))
        }
    }

    private fun loadContactNames(): List<String> {
        val names = mutableListOf<String>()
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                it.getString(0)?.trim()?.takeIf { n -> n.isNotEmpty() }?.let { n -> names.add(n) }
            }
        }
        return names
    }
}
