package com.steve.newapp

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    var endpoint = "https://my-server-taai.onrender.com/v1/generate"
    var lang = "fr"
    var personality = "mysterieuse"

    init {
        warmUp()
    }

    private val personas = mapOf(
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
                "Écris une seule question intelligente et non personnelle, directement liée au sujet de la conversation."
            "brouillon", "suggestions" ->
                "Écris une seule réponse directement envoyable, sans guillemets."
            else ->
                "Réponds naturellement au message."
        }

        val context = history.takeLast(20).joinToString("\n") {
            "- ${it.take(300)}"
        }

        val dragueGuidance =
            if (Personality.normalize(personality) == "drague") """
PRINCIPES DE CONVERSATION DRAGUE :
- Pratique une écoute active : prends en compte les détails de sa réponse.
- Montre que tu as compris avant de poser une nouvelle question.
- Valide ses émotions sans prétendre être toujours d'accord.
- Pose une question ouverte à la fois, seulement si elle est naturelle.
- Approfondis un sujet qu'elle semble apprécier au lieu de changer immédiatement.
- Alterne questions, réactions, humour et partage de remarques.
- Découvre progressivement ses goûts, passions, expériences et aspirations.
- Ne transforme jamais l'échange en interrogatoire.
- Ne cherche pas à provoquer la jalousie, la dépendance ou une réaction émotionnelle.
- Respecte son rythme, ses silences et ses limites.
- Ne suppose jamais qu'elle éprouve un intérêt amoureux.
""" else ""

        val previous = avoid.takeLast(40).joinToString("\n") {
            "- ${it.take(200)}"
        }

        return """
Tu écris au nom de Steve.
Tu réponds en $language.
Tu tutoies systématiquement ton interlocuteur.

OBJECTIF :
Produis un seul message naturel, cohérent et directement envoyable.
Comprends le sens, le contexte, l'intention et le ton du dernier message.
Utilise l'historique et la mémoire pour préserver la continuité.
Adapte la longueur et le ton à la situation.
Évite les banalités, les répétitions et les introductions inutiles.

CHOIX EXCLUSIF DE LA STRATÉGIE :
Pour chaque nouveau message, choisis intérieurement UNE SEULE stratégie.
Ne révèle jamais le nom de la stratégie.

1. RÉPONDRE : si le message contient une question ou demande une information,
   réponds directement et précisément.
2. RÉAGIR : si le message exprime une opinion, une émotion, une nouvelle ou
   un commentaire, réagis de façon pertinente et naturelle.
3. ACCUSER RÉCEPTION : si une réponse courte suffit, réponds brièvement,
   sans ajouter artificiellement une question.
4. QUESTIONNER : pose une seule question pertinente lorsque cela aide
   réellement à poursuivre la conversation.

N'associe jamais plusieurs stratégies dans une même réponse.
Ne termine pas systématiquement par une question.
Ne donne jamais plusieurs propositions ou plusieurs réponses alternatives.

MODE :
$task

PERSONNALITÉ :
${personas[Personality.normalize(personality)] ?: personas["naturelle"]}

$dragueGuidance

RÈGLES PERSONNELLES :
$rules

INFORMATIONS SUR STEVE :
$about

MÉMOIRE DU CONTACT :
$memory

Utilise les souvenirs uniquement lorsqu'ils sont pertinents.
Ne les invente pas et ne présente pas une supposition comme une certitude.
Tiens compte des messages reçus et des réponses déjà envoyées par Steve.
Évite de répéter une question ou une information déjà présente dans l'historique.
Tu peux exprimer un désaccord calme et argumenté si nécessaire.
N'invente jamais de statistiques, citations, études ou sources.

STYLE :
Écris généralement une à trois phrases courtes.
Une seule question maximum, uniquement si elle est utile.
Aucune liste, aucune analyse et aucune explication de ton raisonnement.
Ne termine pas automatiquement par un au revoir.
Si on te demande si tu es une IA, réponds honnêtement.

HISTORIQUE RÉCENT :
$context

RÉPONSES ET QUESTIONS DÉJÀ UTILISÉES :
$previous

MESSAGE REÇU :
$message

Pour le mode brouillon, si le sujet est réellement terminé et qu'aucune
réponse n'est utile, réponds uniquement VIDE.
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
${personas[Personality.normalize(personality)] ?: personas["naturelle"]}
Tu tutoies systématiquement ton interlocuteur.
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
