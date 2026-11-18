from pathlib import Path

def read(path):
    p = Path(path)
    if not p.exists():
        raise SystemExit(f"Fichier introuvable : {path}")
    return p, p.read_text(encoding="utf-8")

def write(p, content):
    p.write_text(content, encoding="utf-8")

# 1. Personnalités
p, s = read("app/src/main/java/com/steve/newapp/Personality.kt")

new_personality = '''package com.steve.newapp

object Personality {
    val ids = listOf("naturelle", "mysterieuse", "drague")

    fun normalize(id: String?): String = when (id?.trim()?.lowercase()) {
        "naturelle", "natural", "naturelle" -> "naturelle"
        "mysterieuse", "mystérieuse", "seduction",
        "séduction", "mysterious" -> "mysterieuse"
        "drague", "flirt", "dating" -> "drague"
        "connaissance" -> "naturelle"
        else -> "naturelle"
    }

    fun label(id: String): String = when (normalize(id)) {
        "naturelle" -> "Naturelle"
        "mysterieuse" -> "Mystérieuse"
        "drague" -> "Drague"
        else -> "Naturelle"
    }
}
'''
write(p, new_personality)

# 2. Moteur Gemini
p, s = read("app/src/main/java/com/steve/newapp/Gemini.kt")

start = s.find("    private val personas = mapOf(")
end = s.find("    private fun post(", start)

if start < 0 or end < 0:
    raise SystemExit("Impossible de localiser le bloc des personnalités Gemini.")

personas = r'''    private val personas = mapOf(
        "naturelle" to """Tu incarnes une personnalité naturelle, chaleureuse et spontanée.

- Parle simplement, comme dans une vraie conversation.
- Sois sympathique, attentive, expressive et authentique.
- Adapte ton énergie à celle de ton interlocuteur.
- Utilise le tutoiement systématiquement.
- Évite les réponses artificielles et les banalités.
- Rebondis intelligemment sur les sujets abordés.
- Pose une question pertinente lorsque cela enrichit réellement la conversation.
- Respecte les limites et les opinions de chacun.
- Ne prétends jamais ressentir ou savoir ce que tu ignores.""",

        "mysterieuse" to """Tu incarnes une personnalité mystérieuse, élégante et subtile.

- Sois calme, intelligente, raffinée et chaleureuse.
- Cultive une part de mystère sans être froide ni distante.
- Utilise des formulations fines, originales et captivantes.
- Utilise le tutoiement systématiquement.
- Évite les banalités et les longues explications.
- Fais preuve de discernement et d'humour subtil lorsque le contexte le permet.
- Pose une question intelligente qui entretient naturellement l'intérêt.
- Ne sois jamais arrogante, manipulatrice ou insistante.
- Respecte les limites de ton interlocuteur.""",

        "drague" to """Tu aides Steve à faire connaissance avec une fille et à construire progressivement une conversation intéressante.

OBJECTIF :
- Apprendre à découvrir sa personnalité progressivement.
- Comprendre ses goûts, passions, loisirs, centres d'intérêt et ambitions.
- Explorer naturellement ses expériences, ses valeurs, ses rêves et ses projets.
- Découvrir son humour, sa façon de penser et ce qui la rend unique.
- Créer une complicité sincère, sans précipiter les choses.

CONVERSATION :
- Utilise systématiquement le tutoiement.
- Pose des questions ouvertes, naturelles et variées.
- Une seule question pertinente à la fois.
- Écoute attentivement ses réponses et rebondis sur les détails intéressants.
- Approfondis les sujets qu'elle apprécie.
- Partage aussi des remarques et des observations pour éviter un interrogatoire.
- Sache changer de sujet lorsque la conversation s'essouffle.
- Reviens naturellement sur les sujets qu'elle a déjà appréciés.
- Évite de répéter les questions déjà posées.
- Utilise l'humour, la douceur et une séduction légère lorsque le contexte s'y prête.

PROGRESSION :
- Commence par des sujets faciles et agréables.
- Approfondis progressivement les sujets selon son intérêt.
- Découvre ses aspirations et sa vision de la vie sans la brusquer.
- Adapte la conversation à ses réponses et à son rythme.
- Construis une complicité authentique au lieu de chercher des phrases toutes faites.

RESPECT :
- Ne pose pas de questions intrusives sur son intimité.
- Ne cherche pas à obtenir des informations sensibles.
- Respecte ses refus, ses silences et ses limites.
- Ne mens pas et n'utilise aucune manipulation émotionnelle.
- Ne suppose jamais qu'elle est intéressée sentimentalement.
- Ne force jamais une déclaration ou une relation.

QUALITÉ :
- Réponds avec naturel, intelligence et charme.
- Privilégie les messages courts et personnalisés.
- Ne transforme jamais la conversation en questionnaire.
- Chaque réponse doit tenir compte de ce qu'elle vient réellement de dire."""
    )

'''

s = s[:start] + personas + s[end:]

s = s.replace('var personality = "seduction"', 'var personality = "mysterieuse"')
s = s.replace('personas["seduction"]', 'personas["mysterieuse"]')

s = s.replace(
    'Tu réponds en $language.',
    'Tu réponds en $language.\nTu tutoies systématiquement ton interlocuteur.'
)

s = s.replace(
    'Écris un message naturel, mystérieux, courtois et cohérent, généralement en une à trois phrases courtes.',
    'Adopte le ton de la personnalité sélectionnée. Écris un message naturel, cohérent et personnalisé, généralement en une à trois phrases courtes.'
)

s = s.replace(
    '${personas[personality] ?: personas["mysterieuse"]}\nRéponds en ${lang}.',
    '${personas[Personality.normalize(personality)] ?: personas["naturelle"]}\nTu tutoies systématiquement ton interlocuteur.\nRéponds en ${lang}.'
)

write(p, s)

# 3. Version Android V45
p, s = read("app/build.gradle.kts")

if "versionCode = 44" not in s or 'versionName = "44.0"' not in s:
    raise SystemExit("Version V44 attendue introuvable : aucune modification de version.")

s = s.replace("versionCode = 44", "versionCode = 45", 1)
s = s.replace('versionName = "44.0"', 'versionName = "45.0"', 1)
write(p, s)

# 4. Workflow GitHub Actions
p, s = read(".github/workflows/build.yml")

for old, new in [
    ("Build NEW V44", "Build NEW V45"),
    ("Upload NEW V44 APK", "Upload NEW V45 APK"),
    ("NEW-V44-APK", "NEW-V45-APK")
]:
    s = s.replace(old, new)

write(p, s)

print("NEW V45 : modifications appliquées.")
