from pathlib import Path

p = Path("app/src/main/java/com/steve/newapp/MsgViews.kt")
s = p.read_text()

old = '            setOnClickListener { openChat = k; Store.refresh() }\n'
new = '''            setOnClickListener {
                openChat = k
                val first = msgs.first()
                Store.markConversationRead(first.pkg, first.sender)
                Store.refresh()
            }
'''

if s.count(old) != 1:
    raise SystemExit("ARRET : clic conversation introuvable.")

s = s.replace(old, new, 1)

old = """private fun renderChat(ctx: Context, parent: LinearLayout, k: String) {
    val msgs = Store.items.filter { conversationKey(it) == k }.sortedBy { it.time }
"""

new = """private fun renderChat(ctx: Context, parent: LinearLayout, k: String) {
    val firstMessage = Store.items.firstOrNull { conversationKey(it) == k }
    if (firstMessage != null) {
        Store.markConversationRead(firstMessage.pkg, firstMessage.sender)
    }
    val msgs = Store.items.filter { conversationKey(it) == k }.sortedBy { it.time }
"""

if s.count(old) != 1:
    raise SystemExit("ARRET : début de renderChat introuvable.")

s = s.replace(old, new, 1)

p.write_text(s)
print("OK : ouverture d'une conversation = messages lus.")
