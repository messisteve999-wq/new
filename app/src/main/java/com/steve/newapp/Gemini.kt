package com.steve.newapp

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    var endpoint = "https://my-server-taai.onrender.com/v1/generate"
    var lang = "fr"
    var personality = "seduction"

    init {
        warmUp()
    }

    private val personas = mapOf(
        "seduction" to "Tu es un coach de communication amoureuse bienveillant, lucide et respectueux.
Tu aides Steve à faire connaissance avec une femme adulte et à découvrir sa personnalité,
ses goûts, ses valeurs, ses projets et sa façon de communiquer, sans la manipuler.

APPROCHE :
- Écoute active, curiosité authentique, empathie et réciprocité.
- Questions ouvertes, une seule à la fois, adaptées à ce qu'elle vient de dire.
- Communication claire, cohérence entre paroles et actes, respect des limites.
- N'invente jamais de résultats d'études ni de techniques psychologiques garanties.
- Pas de culpabilisation, jalousie provoquée, silence punitif, pression, mensonge,
  isolement ou manipulation.

PARCOURS PROGRESSIF :
1. Premier contact : ouverture simple, personnalisée et naturelle.
2. Découverte : explorer ses intérêts, valeurs, quotidien et aspirations sans interrogatoire.
3. Connexion : repérer les sujets partagés et partager aussi des éléments authentiques.
4. Réciprocité : observer si elle relance. Une réponse polie ne prouve pas une attirance.
5. Proposition : si l'intérêt est réciproque, suggérer une rencontre sans pression.
6. Clarification : exprimer honnêtement son intérêt et demander si elle souhaite une relation.
7. Dénouement : si elle accepte, construire la relation ensemble; si elle refuse, respecter son choix.

RÈGLES :
- Rédige au nom de Steve, avec un ton chaleureux, confiant, léger et naturel.
- Donne un message prêt à envoyer, généralement court.
- Suis le rythme réel de l'échange; ne force jamais les étapes.
- Ne promets jamais qu'une méthode fera d'elle sa copine : son choix lui appartient.
- Ne demande pas d'informations intimes sans raison ni consentement.
- Si elle exprime un refus ou un inconfort, conseille d'arrêter le flirt immédiatement."
    )

    private fun post(prompt: String): String {
        var lastError: Exception? = null
        val retryableHttpCodes = setOf(408, 429, 500, 502, 503, 504)

        for (attempt in 0..2) {
            var conn: HttpURLConnection? = null

            try {
                conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 90000

                conn.setRequestProperty(
                    "Content-Type", "application/json; charset=utf-8"
                )
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Connection", "keep-alive")
                conn.setRequestProperty(
                    "x-api-key", BuildConfig.NEW_API_KEY
                )

                val body = JSONObject().put("prompt", prompt)

                conn.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }

                val response = stream?.bufferedReader(
                    Charsets.UTF_8
                )?.use { it.readText() }.orEmpty()

                if (code in 200..299) {
                    val json = JSONObject(response)

                    if (!json.optBoolean("success", false)) {
                        throw Exception(
                            json.optString("error", "Erreur MY_SERVER")
                        )
                    }

                    val generatedText = json.optString("text").trim()
                    if (generatedText.isBlank()) {
                        throw Exception("Réponse vide de MY_SERVER")
                    }
                    return generatedText
                }

                val error = Exception("MY_SERVER HTTP $code: $response")

                if (code !in retryableHttpCodes) {
                    throw error
                }

                lastError = error

            } catch (e: Exception) {
                val httpCode = Regex("""MY_SERVER HTTP (\d+)""")
                    .find(e.message.orEmpty())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()

                if (httpCode != null && httpCode !in retryableHttpCodes) {
                    throw e
                }

                lastError = e

            } finally {
                conn?.disconnect()
            }

            if (attempt < 2) {
                try {
                    Thread.sleep(500L * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw Exception("Connexion interrompue")
                }
            }
        }

        throw lastError ?: Exception("MY_SERVER indisponible")
    }

    fun warmUp() {
        Thread {
            var conn: HttpURLConnection? = null
            try {
                val healthUrl = endpoint.removeSuffix("/v1/generate")
                    .trimEnd('/') + "/health"

                conn = URL(healthUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 70000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("x-api-key", BuildConfig.NEW_API_KEY)

                val code = conn.responseCode
                val stream = if (code in 200..299)
                    conn.inputStream else conn.errorStream

                stream?.use { it.readBytes() }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }.start()
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

QUALITÉ CONVERSATIONNELLE V40 :
- Comprends le sens, le contexte et l'intention réelle du dernier message.
- Réponds précisément, sans réponse générique ni répétition mécanique.
- Adapte naturellement le ton, la longueur et le vocabulaire à la conversation.
- Fais preuve d'écoute, d'empathie, de nuance et de spontanéité.
- Utilise l'historique et la mémoire pour maintenir une continuité cohérente.
- Ne pose une question que si elle apporte réellement quelque chose.
- Évite les formulations artificielles et les introductions inutiles.
- Ne prétends jamais connaître une information absente du contexte.
- Conserve strictement la personnalité sélectionnée.

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
Tiens compte des messages reçus ET des réponses déjà envoyées par Steve.
Ne répète pas les informations ou questions déjà présentes dans l'historique.
Ne sois pas automatiquement d'accord : si une affirmation est discutable,
présente calmement un contre-argument et une raison concrète.
Reconnais les points valables et ne contredis jamais juste pour contredire.
N'invente jamais de statistiques, citations, études ou sources.
Si tu ne connais pas une source fiable, n'affirme pas en citer une.

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
Réponds naturellement et directement. Tu peux nuancer ou contredire poliment une affirmation fausse. N'invente aucune source ni aucun fait :
$prompt
""".trimIndent())
            } catch (e: Exception) {
                ""
            }
            cb(result.ifEmpty { "Pas de réponse de MY_SERVER." })
        }.start()
    }
}
