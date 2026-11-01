# new (v1)
Messagerie reliée aux notifications WhatsApp / WhatsApp Business.
- Bulle flottante « S » noir sur blanc : ouvre/ferme la fenêtre de messages.
- Modes : manuel, suggestions Gemini, réponse automatique (uniquement si choisi).
- Gemini : serveur existant gemini-server-steve.onrender.com (POST /reply).
- Accès aux notifications : Paramètres > Applications > new > ⋮ > Autoriser les paramètres restreints, puis activer l'accès.
Non inclus en v1 : assistant IA, paramètres de confidentialité et avancés, envoi de photos/vidéos, icône « Steve » rouge définitive.

## v2
- Assistant IA : discussion avec Gemini (mode "chat" sur le serveur). Règles et infos personnelles dans Paramètres > Assistant IA.
- Paramètres : confidentialité (masquer le texte, partager les contacts avec l'IA), avancé (WhatsApp Business, adresse du serveur).
- Le serveur doit accepter mode "chat" et répondre {"reply": "..."}.

## v3
- Photos et vidéos : bouton « Joindre photo ou vidéo » sur chaque message, puis « Envoyer la pièce jointe ».
- Limite : WhatsApp n'accepte pas les médias dans une réponse de notification. L'appli ouvre donc l'écran de partage de WhatsApp avec le fichier et votre texte ; vous choisissez le contact et appuyez sur envoyer.

## v4
- Icône : fond blanc, mot « Steve » en rouge (dessiné en vecteur, sans police).

## v5
- Bulle : glisser pour la déplacer ; un appui ouvre ou ferme la fenêtre.
- Paramètres > Bulle flottante : interrupteur pour afficher ou arrêter la bulle.
