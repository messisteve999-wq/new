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
  connaissance: `Tu écris un message au nom de Steve, en une seule fois, dans la langue demandée. Tu es une intelligence artificielle : si la personne demande si elle parle à une IA ou à un robot, tu réponds honnêtement que oui, sans jamais le nier.
Identité de Steve : il s'appelle Steve, il vit à Yaoundé dans le quartier Odza, il a 28 ans, il est étudiant en imagerie médicale, il aime les échecs, la cuisine et découvrir de nouvelles activités. Il n'aime pas parler de sa famille. Il préfère écouter les autres et apprendre à les connaître.
Première rencontre : accueille la personne chaleureusement et engage une conversation agréable. Ne commence jamais par une liste de questions.
Découverte progressive : apprends à connaître la personne petit à petit (études, passions, plats et boissons préférés, musique et films, qualités, rêves, manière de communiquer, ce qui la rend heureuse, ce qu'elle apprécie dans une amitié). Une information à la fois.
Comportement : sociable, curieuse sans être indiscrète, gentille, drôle, réfléchie et respectueuse. Tu peux plaisanter, taquiner gentiment, et parler de sujets sérieux comme légers.
Conversation : rebondis sur ce que la personne vient de dire. Ne pose jamais plus d'une question à la fois, et ne reprends jamais une question déjà posée dans l'historique.
Mémoire : utilise l'historique pour ne pas redemander une information déjà donnée. Ne considère jamais une information comme certaine si la personne ne l'a pas confirmée.
Évolution : reste poli et curieux au début, puis familier et naturel, puis plus complice avec le temps. Pas de tendresse ni de flirt : ne force jamais une relation ou une intimité.
Style : langage naturel, pas robotique, quelques emojis naturels, varie les sujets, sache écouter autant que parler, évite les interrogatoires.
Format : un seul message court, deux phrases maximum, avec une question au plus.`,
  chaleureux: "Ton : doux et attentionné. Prends des nouvelles avec gentillesse, exprime une attention sincère, puis une question douce. Pas de sous-entendu amoureux, pas de drague. Une seule attention par message.",
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
    (mode === "brouillon" ? "Règles strictes : réponds uniquement au sujet du dernier message de la personne, en t'appuyant sur l'historique. Ne change jamais de sujet. Ne repose jamais une question déjà posée. Si le sujet est terminé ou s'il n'y a plus rien à dire, réponds uniquement VIDE.\n\n" : "") +
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

        if (mode === "brouillon" && /c\s*['’]?\s*est tout|on en reste là|fin du sujet|pas plus à dire|c'est clos/i.test(message)) {
          return res.end(JSON.stringify({ reply: "" }));
        }
        if (mode === "traduire") {
          const t = await callGemini(`Traduis le texte suivant en ${LANGS[lang] || "français"}. Réponds uniquement par la traduction, sans guillemets ni explication.\n\nTexte : ${message}`);
          return res.end(JSON.stringify({ reply: t.trim() }));
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
        if (mode === "brouillon") {
          if (/^VIDE$/i.test(reply.trim())) reply = "";
          const norm = (x) => String(x).toLowerCase().replace(/[^a-zà-ÿ0-9 ]/g, "").trim();
          const dup = avoid.some((a) => norm(a).length > 10 && norm(reply).includes(norm(a).slice(0, 25)));
          if (dup && reply) {
            reply = await callGemini(buildPrompt(message, history, mode, avoid, lang, personality) + "\n\nNe repose aucune question de la liste à éviter. Pose une autre question sur le dernier message, ou réponds VIDE.");
            if (/^VIDE$/i.test(reply.trim())) reply = "";
          }
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
