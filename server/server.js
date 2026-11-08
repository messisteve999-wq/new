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
    const req = https.request(
      {
        hostname: "generativelanguage.googleapis.com",
        path: `/v1beta/models/${MODEL}:generateContent`,
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": KEY,
          "Content-Length": Buffer.byteLength(body),
        },
      },
      (res) => {
        let data = "";
        res.on("data", (c) => (data += c));
        res.on("end", () => {
          try {
            const json = JSON.parse(data);
            const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
            if (!text) return reject(new Error(JSON.stringify(json)));
            resolve(text.trim());
          } catch (e) {
            reject(e);
          }
        });
      }
    );
    req.on("error", reject);
    req.write(body);
    req.end();
  });
}

async function callGemini(prompt, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      return await callOnce(prompt);
    } catch (e) {
      const busy = /UNAVAILABLE|503/.test(e.message);
      if (!busy || i === retries - 1) throw e;
      await new Promise((r) => setTimeout(r, 2000 * (i + 1)));
    }
  }
}

const FAREWELL = /repose[- ]toi|bon courage|au revoir|bonne (journée|soirée|nuit)|excellente (journée|soirée|nuit)|à bientôt|bonne route|bonne continuation/i;
const PERSONA = {
  connaissance: "Objectif : faire connaissance. Commente brièvement la réponse précédente de la personne, puis ouvre vers une autre question différente de celles déjà posées.",
  serieux: "Objectif : garder le contact de façon sérieuse et utile. Réponds ou questionne de façon pertinente en t'appuyant sur l'historique, sans répéter une question déjà posée."
};
const LANGS = { fr: "français", en: "anglais", el: "grec", ja: "japonais" };

function buildPrompt(message, history, mode, avoid = [], lang = "fr", personality = "connaissance") {
  const past = (Array.isArray(history) ? history : [])
    .slice(-20)
    .map((h) => `- ${String(h).slice(0, 300)}`)
    .join("\n");

  const style =
    `Tu réponds à la place de Steve, en ${LANGS[lang] || "français"}. ` +
    "Réponses courtes (1 à 2 phrases), naturelles et polies. " +
    "Ton poli, respectueux et chaleureux. Pas de compliments appuyés, pas de drague, pas de message romantique. " +
    "Ne parle jamais de sa famille. " +
    "Ne termine jamais par une formule d'au revoir (bonne journée, bonne soirée, bonne nuit, à bientôt) : pose plutôt une question ouverte qui relance la conversation. " +
    "Imite son style d'après les messages qu'il a déjà envoyés.";

  const task =
    mode === "questions"
      ? "Ne réponds pas au message. Écris uniquement UNE seule question sincère et respectueuse pour mieux connaître la personne qui écrit, sans insister ni sous-entendu. Sans numérotation, sans introduction ni commentaire."
      : mode === "suggestions"
      ? "Propose UNE seule suggestion de réponse, sans guillemets ni explication."
      : "Écris UNE seule réponse, sans guillemets ni explication.";

  return (
    `${style}\n${task}\n\n` +
    (mode === "brouillon" ? (PERSONA[personality] || PERSONA.connaissance) + "\n\n" : "") +
    (avoid.length ? "Ne répète jamais ces questions ou idées déjà posées à cette personne :\n" + avoid.slice(-40).map((a) => "- " + String(a).slice(0, 200)).join("\n") + "\n\n" : "") +
    (past ? `Historique récent des notifications :\n${past}\n\n` : "") +
    (mode === "questions" ? "Écris la question maintenant." : `Message reçu : ${message}`)
  );
}

const server = http.createServer((req, res) => {
  res.setHeader("Content-Type", "application/json; charset=utf-8");

  if (req.method === "GET" && req.url === "/") {
    return res.end(JSON.stringify({ status: "ok", model: MODEL, version: "V4" }));
  }

  if (req.method === "POST" && req.url === "/reply") {
    let raw = "";
    req.on("data", (c) => (raw += c));
    req.on("end", async () => {
      try {
        const { message, history = [], mode = "reply", from_me = false, avoid = [], lang = "fr", personality = "connaissance" } =
          JSON.parse(raw || "{}");

        // Ignore les messages envoyés par Steve
        if (from_me) return res.end(JSON.stringify({ reply: "" }));

        if (!message) {
          res.statusCode = 400;
          return res.end(JSON.stringify({ error: "message manquant" }));
        }

        let reply = await callGemini(buildPrompt(message, history, mode, avoid, lang, personality));
        if (FAREWELL.test(reply)) {
          reply = await callGemini(
            buildPrompt(message, history, mode, avoid, lang, personality) +
            "\n\nNe termine pas par une formule d'au revoir : pose une question ouverte."
          );
        }
        if (FAREWELL.test(reply)) {
          reply = reply.split(/(?<=[.!?])\s+/).filter((x) => !FAREWELL.test(x)).join(" ").trim();
        }
        if (mode === "questions") {
          // Ne garde que les questions, sans phrase d'introduction ni réponse au message
          const found = (reply.match(/[^.!?\n]*\?/g) || [])
            .map((x) => x.trim())
            .filter((x) => x.length > 3 && !avoid.includes(x));
          reply = found.length ? found[found.length - 1] : "";
        }
        res.end(JSON.stringify({ reply }));
      } catch (e) {
        res.statusCode = 500;
        res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  res.statusCode = 404;
  res.end(JSON.stringify({ error: "route introuvable" }));
});

server.listen(PORT, "127.0.0.1", () =>
  console.log(`New serveur V4 sur http://127.0.0.1:${PORT}`)
);
