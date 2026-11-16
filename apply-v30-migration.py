from pathlib import Path

main = Path("app/src/main/java/com/steve/newapp/MainActivity.kt")
if main.exists():
    s = main.read_text(encoding="utf-8")
    old = 'Gemini.personality = Prefs.text(this, "personality", "connaissance")'
    new = 'Gemini.personality = Personality.normalize(Prefs.text(this, "personality", "connaissance"))'
    if old in s:
        s = s.replace(old, new)
        main.write_text(s, encoding="utf-8")
        print("MainActivity.kt : anciennes préférences globales migrées.")
    elif new in s:
        print("MainActivity.kt : déjà migré.")
    else:
        print("ATTENTION : ligne de personnalité non trouvée dans MainActivity.kt.")

views = Path("app/src/main/java/com/steve/newapp/MsgViews.kt")
if views.exists():
    s = views.read_text(encoding="utf-8")
    old = 'Prefs.text(ctx, "persona_$contactName", Gemini.personality)'
    new = 'Personality.normalize(Prefs.text(ctx, "persona_$contactName", Gemini.personality))'
    s = s.replace(old, new)
    views.write_text(s, encoding="utf-8")
    print("MsgViews.kt : personnalités enregistrées par contact migrées.")
else:
    print("ATTENTION : MsgViews.kt introuvable.")
