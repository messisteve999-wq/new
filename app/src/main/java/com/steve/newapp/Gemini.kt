package com.steve.newapp

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    var endpoint = "http://127.0.0.1:8787/v1/generate"
    var lang = "fr"
    var personality = "connaissance"

    private val personas = mapOf(
        "connaissance" to """
Tu es sociable, curieux et attentif.
Découvre progressivement la personne sans l'interroger.
Pose au maximum une question pertinente à la fois.
Ne redemande pas une information déjà donnée.
""",
        "drague" to """
Tu es charmeur, joueur et complice.
Utilise parfois une taquinerie gentille ou un compliment précis.
Adapte le flirt à la réciprocité.
Ne sois jamais insistant, vulgaire ou possessif.
Respecte immédiatement les limites de l'autre personne.
""",
        "romance" to """
Tu es tendre, attentionné et délicat.
Utilise des mots affectueux avec naturel.
Respecte le rythme et les sentiments de l'autre personne.
Ne crée aucune pression affective.
""",
        "mysterieux" to """
Tu es calme, subtil et observateur.
Tes réponses sont réfléchies et légèrement énigmatiques.
Reste compréhensible et réponds réellement au message.
""",
        "amical" to """
Tu es un ami agréable, spontané et fiable.
Tu écoutes, encourages et plaisantes naturellement.
Garde une complicité saine et respectueuse.
"""
    )

    private fun post(prompt: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-api-key", BuildConfig.NEW_API_KEY)

        val body = JSONObject().put("prompt", prompt)
        conn.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val stream = if (conn.responseCode in 200..299)
            conn.inputStream else conn.errorStream

        val response = stream.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }
        conn.disconnect()

        val json = JSONObject(response)
        if (!json.optBoolean("success", false)) {
            throw Exception(json.optString("error", "Erreur MY_SERVER"))
        }
        return json.optString("text").trim()
    }

    private fun buildPrompt(
        message: String,
        history: List<String>,
        mode: String,
        avoid: List<String>,
        lang: String,
        personality: String,
        rules: String,
        about: String,
        memory: String
    ): String {
        val language = when (lang) {
            "en" -> "anglais"
            "el" -> "grec"
            "ja" -> "japonais"
            else -> "français"
        }

        val task = when (mode) {
            "questions" ->
                "Écris une seule question sincère pour mieux connaître la personne."
            "brouillon", "suggestions" ->
                "Écris une seule réponse directement envoyable, sans guillemets."
            else ->
                "Réponds naturellement au message."
        }

        val context = history.takeLast(20).joinToString("\n") {
            "- ${it.take(300)}"
        }

        val previous = avoid.takeLast(40).joinToString("\n") {
            "- ${it.take(200)}"
        }

        return """
Tu écris au nom de Steve.
Tu réponds en $language.
${personas[personality] ?: personas["connaissance"]}

RÈGLES PERSONNELLES :
$rules

INFORMATIONS SUR STEVE :
$about

MÉMOIRE DU CONTACT :
$memory

Utilise ces souvenirs uniquement lorsqu'ils sont utiles.
Ne les invente pas et ne les présente pas comme des certitudes
s'ils ne sont pas pertinents pour le message actuel.

STYLE :
Écris un message naturel et cohérent, généralement en une ou deux phrases.
Réponds au dernier message.
Ne donne aucune analyse, explication ou liste.
Ne termine pas automatiquement par un au revoir.
Si on te demande si tu es une IA, réponds honnêtement.

$task

HISTORIQUE RÉCENT :
$context

RÉPONSES ET QUESTIONS DÉJÀ UTILISÉES :
$previous

MESSAGE REÇU :
$message

Pour le mode brouillon, si le sujet est réellement terminé,
réponds uniquement VIDE.
N'ajoute aucun préambule.
""".trimIndent()
    }

    fun ask(
        message: String,
        history: List<String> = emptyList(),
        fromMe: Boolean = false,
        mode: String = "suggestions",
        avoid: List<String> = emptyList(),
        lang: String = Gemini.lang,
        personality: String = Gemini.personality,
        rules: String = "",
        about: String = "",
        memory: String = "",
        cb: (List<String>) -> Unit
    ) {
        Thread {
            val items = try {
                if (fromMe) emptyList()
                else {
                    val prompt = buildPrompt(
                        message, history, mode, avoid,
                        lang, personality, rules, about, memory
                    )
                    val reply = post(prompt)
                    if (reply.isBlank() || reply.equals("VIDE", true))
                        emptyList()
                    else listOf(reply)
                }
            } catch (e: Exception) {
                emptyList()
            }
            cb(items)
        }.start()
    }

    fun translate(text: String, target: String, cb: (String) -> Unit) {
        Thread {
            val result = try {
                post("Traduis en $target. Réponds uniquement par la traduction :\n$text")
            } catch (e: Exception) {
                ""
            }
            cb(result)
        }.start()
    }

    fun chat(prompt: String, cb: (String) -> Unit) {
        Thread {
            val result = try {
                post("""
${personas[personality] ?: personas["connaissance"]}
Réponds en ${lang}.
Réponds naturellement et directement :
$prompt
""".trimIndent())
            } catch (e: Exception) {
                ""
            }
            cb(result.ifEmpty { "Pas de réponse de MY_SERVER." })
        }.start()
    }
}
