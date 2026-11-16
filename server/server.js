const http = require("http");
const https = require("https");

const PORT = process.env.PORT || 3001;
const KEY = process.env.GEMINI_API_KEY;
const MODEL = process.env.GEMINI_MODEL || "gemini-3.5-flash-lite";

if (!KEY) {
  console.error("GEMINI_API_KEY manquante");
  process.exit(1);
}

function callOnce(prompt) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] });
    const req = https.request({
      hostname: "generativelanguage.googleapis.com",
      path: `/v1beta/models/${MODEL}:generateContent`,
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": KEY,
        "Content-Length": Buffer.byteLength(body),
      },
    }, (res) => {
      let data = "";
      res.on("data", c => data += c);
      res.on("end", () => {
        try {
          const json = JSON.parse(data);
          const answer = json.candidates?.[0]?.content?.parts?.[0]?.text;
          if (!answer) return reject(new Error(JSON.stringify(json)));
          resolve(answer.trim());
        } catch (e) { reject(e); }
      });
    });
    req.on("error", reject);
    req.write(body);
    req.end();
  });
}

async function callGemini(prompt, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try { return await callOnce(prompt); }
    catch (e) {
      const busy = /UNAVAILABLE|503/.test(e.message);
      if (!busy || i === retries - 1) throw e;
      await new Promise(r => setTimeout(r, 2000 * (i + 1)));
    }
  }
}

const LANGS = { fr: "français", en: "anglais", el: "grec", ja: "japonais" };

const PERSONA = {
  connaissance: `PERSONNALITÉ : FAIRE CONNAISSANCE.
Tu es sociable, curieux et attentif. Tu veux découvrir la personne progressivement, sans transformer la discussion en interrogatoire.
Rebondis d'abord sur ce qu'elle vient de dire. Pose au maximum une question simple et pertinente à la fois. Varie les sujets : loisirs, musique, cuisine, études, travail, voyages, rêves et petites habitudes.
Mémorise le contexte fourni dans l'historique et ne redemande pas une information déjà donnée. Ne prétends jamais connaître une information non confirmée. Sois naturel, ouvert et respectueux.`,
  drague: `PERSONNALITÉ : DRAGUE AMICALE.
Tu es charmeur, joueur et complice, avec une séduction légère et élégante. Utilise parfois une taquinerie gentille, un compliment précis ou une petite touche d'humour.
Adapte toujours ton niveau de flirt à la réponse de l'autre personne. Ne sois jamais lourd, insistant, vulgaire ou possessif. Ne suppose jamais que l'autre personne est intéressée. Si elle pose une limite ou n'est pas réceptive, reviens immédiatement à une conversation amicale.
Garde une conversation vivante, spontanée et respectueuse.`,
  romance: `PERSONNALITÉ : ROMANTIQUE.
Tu es tendre, attentionné, délicat et expressif. Tu sais employer des mots affectueux et des images poétiques avec naturel, sans exagération.
Montre de l'intérêt pour les émotions et les petites choses qui comptent pour l'autre. Fais progresser la tendresse selon la confiance et la réciprocité. Ne déclare pas un amour profond à une personne que tu connais à peine et ne crée jamais de pression affective.
Reste sincère, doux et respectueux.`,
  mysterieux: `PERSONNALITÉ : MYSTÉRIEUX.
Tu es calme, subtil, observateur et intriguant. Tes réponses sont réfléchies, parfois légèrement énigmatiques, avec une pointe de profondeur.
Tu peux utiliser une formule originale ou une métaphore courte, mais reste toujours compréhensible et réponds réellement au message reçu. Ne sois ni froid, ni obscur, ni artificiellement dramatique.
Laisse une petite ouverture qui donne envie de poursuivre la conversation, sans jouer avec les sentiments.`,
  amical: `PERSONNALITÉ : AMICAL.
Tu es un ami agréable, accessible, spontané et fiable. Tu écoutes, encourages et partages une énergie positive sans en faire trop.
Tu peux plaisanter naturellement, utiliser une petite taquinerie bienveillante et parler aussi bien de sujets légers que sérieux. Montre de l'intérêt sans être indiscret.
Adapte ton langage à la personne et garde une complicité saine et respectueuse.`,
};

function buildPrompt(message, history, mode, avoid = [], lang = "fr", personality = "connaissance") {
  const past = (Array.isArray(history) ? history : [])
    .slice(-20).map(h => `- ${String(h).slice(0, 300)}`).join("\n");
  const persona = PERSONA[personality] || PERSONA.connaissance;

  const style =
    `Tu écris au nom de Steve, en ${LANGS[lang] || "français"}. ` +
    "Steve a 28 ans, vit à Yaoundé (Odza), étudie l’imagerie médicale, aime les échecs, la cuisine et découvrir de nouvelles activités. " +
    "Écris un seul message final, naturel et cohérent, généralement en une ou deux phrases. " +
    "Réponds au dernier message et respecte le ton de la conversation. " +
    "Ne donne ni analyse, ni explication, ni liste, ni plusieurs variantes. " +
    "Ne termine pas automatiquement par une formule d'au revoir. " +
    "Ne parle jamais de sa famille. Si on te demande si tu es une IA, réponds honnêtement.";

  const task = mode === "questions"
    ? "Écris une seule question sincère et respectueuse pour mieux connaître la personne. Sans introduction."
    : mode === "suggestions"
      ? "Écris une seule suggestion de réponse, directement envoyable, sans guillemets."
      : "Écris une seule réponse directement envoyable, sans guillemets.";

  const avoidText = avoid.length
    ? "Évite de répéter ces messages ou questions déjà utilisés :\n" +
      avoid.slice(-40).map(x => "- " + String(x).slice(0, 200)).join("\n") + "\n"
    : "";

  const contextRules = mode === "brouillon"
    ? "Règles supplémentaires : réponds uniquement au sujet du dernier message. Ne change pas de sujet. Ne repose pas une question déjà posée dans l'historique. Si le sujet est réellement terminé, réponds uniquement VIDE. "
    : "";

  return `${style}\n\n${persona}\n\n${contextRules}${task}\n\n` +
    (past ? `Historique récent :\n${past}\n\n` : "") +
    avoidText + `Dernier message reçu : ${message}\n\n` +
    "Réponds maintenant avec un seul message. N'ajoute aucun préambule.";
}

const FAREWELL = /repose[- ]toi|au revoir|bonne (journée|soirée|nuit)|excellente (journée|soirée|nuit)|à bientôt|bonne continuation/i;

const server = http.createServer((req, res) => {
  res.setHeader("Content-Type", "application/json; charset=utf-8");

  if (req.method === "GET" && req.url === "/") {
    return res.end(JSON.stringify({ status: "ok", model: MODEL, version: "V30" }));
  }

  if (req.method === "POST" && req.url === "/reply") {
    let raw = "";
    req.on("data", c => raw += c);
    req.on("end", async () => {
      try {
        const {
          message, history = [], mode = "reply", from_me = false,
          avoid = [], lang = "fr", personality = "connaissance"
        } = JSON.parse(raw || "{}");

        if (from_me) return res.end(JSON.stringify({ reply: "" }));
        if (!message && mode !== "questions") {
          res.statusCode = 400;
          return res.end(JSON.stringify({ error: "message manquant" }));
        }

        if (mode === "traduire") {
          const translated = await callGemini(
            `Traduis en ${LANGS[lang] || "français"}. Réponds uniquement par la traduction :\n${message}`
          );
          return res.end(JSON.stringify({ reply: translated.trim() }));
        }

        let prompt = buildPrompt(message || "", history, mode, avoid, lang, personality);
        let reply = await callGemini(prompt);
        reply = reply.replace(/\r/g, "").split(/\n+/).map(x => x.trim()).filter(Boolean).join(" ");
        reply = reply.replace(/^["'«]+|["'»]+$/g, "").trim();

        if (mode === "brouillon" && /^VIDE$/i.test(reply)) reply = "";
        if (mode === "brouillon" && reply && Array.isArray(avoid)) {
          const norm = value => String(value).toLowerCase().replace(/[^a-zà-ÿ0-9 ]/g, "").trim();
          const repeated = avoid.some(item => {
            const old = norm(item);
            return old.length > 12 && norm(reply).includes(old.slice(0, 28));
          });
          if (repeated) {
            reply = await callGemini(prompt + "\\n\\nPropose une réponse différente, sans reprendre les questions ou formulations déjà utilisées. Si nécessaire, réponds VIDE.");
            reply = reply.replace(/\r/g, "").split(/\n+/).map(x => x.trim()).filter(Boolean).join(" ").trim();
            if (/^VIDE$/i.test(reply)) reply = "";
          }
        }

        if (mode === "questions") {
          const questions = reply.match(/[^.!?\n]*\?/g) || [];
          reply = questions.length ? questions[0].trim() : "";
        }
        if (FAREWELL.test(reply)) {
          reply = reply.split(/(?<=[.!?])\s+/).filter(x => !FAREWELL.test(x)).join(" ").trim();
        }
        if (/^VIDE$/i.test(reply)) reply = "";

        return res.end(JSON.stringify({ reply }));
      } catch (e) {
        res.statusCode = 500;
        return res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  res.statusCode = 404;
  res.end(JSON.stringify({ error: "route introuvable" }));
});

server.listen(PORT, "127.0.0.1", () =>
  console.log(`New serveur V30 sur http://127.0.0.1:${PORT}`)
);
