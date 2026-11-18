from pathlib import Path

p = Path("app/src/main/java/com/steve/newapp/MsgViews.kt")
s = p.read_text()

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
    raise SystemExit("ARRET : bloc de liste introuvable ou déjà modifié.")

s = s.replace(old, new, 1)
p.write_text(s)
print("OK : onglets et filtrage ajoutés.")
