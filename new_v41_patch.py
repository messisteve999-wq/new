from pathlib import Path

p = Path("app/src/main/java/com/steve/newapp/NotifService.kt")
s = p.read_text()

changes = [
    (
        "var translation: String? = null, var pending: Runnable? = null\n",
        "var translation: String? = null, var pending: Runnable? = null,\n        var read: Boolean = outgoing\n"
    ),
    (
        "outgoing = o.getBoolean(\"outgoing\")\n",
        "outgoing = o.getBoolean(\"outgoing\"),\n                    read = o.optBoolean(\"read\", true)\n"
    ),
    (
        '.put("pkg", m.pkg).put("time", m.time).put("outgoing", m.outgoing))',
        '.put("pkg", m.pkg).put("time", m.time).put("outgoing", m.outgoing)\n                .put("read", m.read))'
    ),
    (
        "fun add(m: Msg) { items.add(0, m); save(); refresh() }\n",
        """fun add(m: Msg) { items.add(0, m); save(); refresh() }

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
"""
    )
]

for old, new in changes:
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"ARRET : ancre introuvable ou non unique ({count}) : {old[:70]!r}")
    s = s.replace(old, new, 1)

p.write_text(s)
print("OK : suivi des messages lus/non lus ajouté.")
