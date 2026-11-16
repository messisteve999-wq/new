NEW V30 — patch de mise à jour

Le patch contient les fichiers V30 à remplacer et un script de migration qui modifie
les préférences de personnalités déjà enregistrées sans écraser tes autres fonctions.

Installation dans Termux :
  cd ~/new
  unzip -o ~/storage/downloads/NEW-V30-PATCH.zip -d .
  python apply-v30-migration.py
  git status

Vérifie que MainActivity.kt et MsgViews.kt restent présents et que leurs autres fonctions
n'ont pas été supprimées. Puis :
  git add app/src/main/java/com/steve/newapp/Personality.kt app/src/main/java/com/steve/newapp/NotifService.kt server/server.js app/build.gradle.kts app/src/main/java/com/steve/newapp/MainActivity.kt app/src/main/java/com/steve/newapp/MsgViews.kt
  git commit -m "NEW V30 - personnalités et réponses sans doublons"
  git push origin main

Lancer la compilation GitHub Actions :
  gh workflow run build.yml --ref main
  gh run list --limit 3
  # Pour suivre la compilation : gh run watch

Télécharger l'APK après réussite :
  mkdir -p ~/storage/downloads/NEW-V30
  gh run download --name new-apk --dir ~/storage/downloads/NEW-V30
  ls -lh ~/storage/downloads/NEW-V30
