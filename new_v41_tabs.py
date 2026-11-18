from pathlib import Path

p = Path("app/src/main/java/com/steve/newapp/MsgViews.kt")
s = p.read_text()

old = """private var openChat: String? = null
// Vrai quand le texte a déjà été traduit à la main : on l'envoie sans nouvelle traduction
private var skipTranslate = false
"""

new = """private var openChat: String? = null
private var selectedApp: String? = null
// Vrai quand le texte a déjà été traduit à la main : on l'envoie sans nouvelle traduction
private var skipTranslate = false

private val appLabels = linkedMapOf(
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "com.facebook.orca" to "Messenger",
    "com.facebook.katana" to "Facebook",
    "com.snapchat.android" to "Snapchat",
    "com.google.android.apps.messaging" to "SMS",
    "com.android.mms" to "MMS",
    "com.samsung.android.messaging" to "Messages"
)
"""

if s.count(old) != 1:
    raise SystemExit("ARRET : emplacement des variables introuvable.")

s = s.replace(old, new, 1)
p.write_text(s)

print("OK : premier bloc des onglets ajouté.")

old = """    if (Store.items.isEmpty()) {
        parent.addView(TextView(ctx).apply {
            text = "Aucune discussion pour le moment"
            setTextColor(sub)
            setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), 0)
        })
        return
    }
    for ((k, msgs) in Store.items.groupBy { conversationKey(it) }) {
"""

new = """    val enabled = Store.enabledApps(ctx)
    val tabs = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val scroll = HorizontalScrollView(ctx).apply {
        isHorizontalScrollBarEnabled = false
        addView(tabs)
    }
    parent.addView(scroll)

    fun addTab(label: String, pkg: String?, count: Int) {
        val active = selectedApp == pkg
        tabs.addView(TextView(ctx).apply {
            text = if (count > 0) "$label ($count)" else label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
            setTextColor(if (active) Color.WHITE else sub)
            background = rounded(
                ctx,
                Color.parseColor(if (active) RED else "#20202B"),
                18
            )
            setOnClickListener {
                selectedApp = pkg
                Store.refresh()
            }
        })
    }

    addTab("Toutes", null, Store.unreadCount(ctx))
    enabled.forEach { pkg ->
        val label = appLabels[pkg] ?: return@forEach
        addTab(label, pkg, Store.unreadCount(ctx, pkg))
    }

    val visibleItems = Store.items.filter {
        it.pkg in enabled && (selectedApp == null || it.pkg == selectedApp)
    }

    if (visibleItems.isEmpty()) {
        parent.addView(TextView(ctx).apply {
            text = "Aucune discussion pour cette application"
            setTextColor(sub)
            setPadding(ctx.dp(16), ctx.dp(24), ctx.dp(16), 0)
        })
        return
    }
    for ((k, msgs) in visibleItems.groupBy { conversationKey(it) }) {
"""

if s.count(old) != 1:
    raise SystemExit("ARRET : bloc de liste introuvable.")

s = s.replace(old, new, 1)
p.write_text(s)
print("OK : onglets et filtrage ajoutés.")
