from pathlib import Path

p = Path("app/src/main/java/com/steve/newapp/BubbleService.kt")
s = p.read_text()

changes = [
(
"""    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var panelRoot: LinearLayout? = null
""",
"""    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var badgeView: TextView
    private lateinit var badgeParams: WindowManager.LayoutParams
    private var panelRoot: LinearLayout? = null
"""
),
(
"""    private val refresher: () -> Unit = { panelList?.let { renderMessages(this, it, true) } }
""",
"""    private val refresher: () -> Unit = { panelList?.let { renderMessages(this, it, true) } }
    private val badgeRefresher: () -> Unit = { updateUnreadBadge() }
"""
),
(
"""                    wm.updateViewLayout(bubbleView, bubbleParams)
                    true
""",
"""                    wm.updateViewLayout(bubbleView, bubbleParams)
                    badgeParams.x = bubbleParams.x + dp(38)
                    badgeParams.y = (bubbleParams.y - dp(4)).coerceAtLeast(0)
                    wm.updateViewLayout(badgeView, badgeParams)
                    true
"""
),
(
"""        wm.addView(bubbleView, bubbleParams)
    }

    private fun togglePanel() {
""",
"""        wm.addView(bubbleView, bubbleParams)

        Store.init(this)
        val badgeSizeW = dp(30)
        val badgeSizeH = dp(22)
        badgeView = TextView(this).apply {
            text = "0"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#FFFF1744"))
                setStroke(dp(1), Color.WHITE)
            }
        }
        badgeParams = WindowManager.LayoutParams(
            badgeSizeW, badgeSizeH, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams.x + dp(38)
            y = (bubbleParams.y - dp(4)).coerceAtLeast(0)
        }
        wm.addView(badgeView, badgeParams)
        Store.listeners.add(badgeRefresher)
        updateUnreadBadge()
    }

    private fun updateUnreadBadge() {
        val count = Store.unreadCount(this)
        badgeView.text = if (count > 99) "99+" else count.toString()
        badgeView.visibility = if (count > 0) android.view.View.VISIBLE
                               else android.view.View.GONE
    }

    private fun togglePanel() {
"""
),
(
"""    private fun bringBubbleToFront() {
        wm.removeView(bubbleView)
        wm.addView(bubbleView, bubbleParams)
    }
""",
"""    private fun bringBubbleToFront() {
        wm.removeView(bubbleView)
        wm.addView(bubbleView, bubbleParams)
        wm.removeView(badgeView)
        wm.addView(badgeView, badgeParams)
    }
"""
),
(
"""        Store.listeners.remove(refresher)
        panelRoot?.let { wm.removeView(it) }
        wm.removeView(bubbleView)
""",
"""        Store.listeners.remove(refresher)
        Store.listeners.remove(badgeRefresher)
        panelRoot?.let { wm.removeView(it) }
        wm.removeView(badgeView)
        wm.removeView(bubbleView)
"""
)
]

for old, new in changes:
    n = s.count(old)
    if n != 1:
        raise SystemExit(f"ARRET : ancre introuvable ou non unique ({n}) : {old[:90]!r}")
    s = s.replace(old, new, 1)

p.write_text(s)
print("OK : badge rouge du robot ajouté.")
