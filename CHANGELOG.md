# Changelog

## v1.3 (build 4)

**Corrige pour de bon le conflit avec l'appli officielle Prusa** (la v1.2 n'avait réglé qu'une partie du problème).
- Cause complète : le tag contient deux enregistrements NDEF, le lien `https://3dtag.org/s/...` en premier, puis les données OpenPrintTag ensuite. Android résout le dispatch sur le **premier** enregistrement qu'une application sait gérer — l'appli Prusa officielle répond au lien et l'emporte avant même qu'Android examine le second enregistrement, celui que visait notre filtre v1.2. Confirmé par jcjames_13009 : après désinstallation de l'appli Prusa, c'est le navigateur qui s'ouvrait sur ce lien, jamais PrusaTag.
- PrusaTag fait maintenant aussi concurrence directement sur ce lien (`3dtag.org`), pas seulement sur le type de données OpenPrintTag : Android devrait désormais proposer un choix entre les deux applis dès le premier enregistrement.

## v1.2 (build 3)

**Corrige l'échec de compilation de la v1.1** (`Unresolved reference: nomsChampsAffiches`). Du travail sur le menu Paramètres, mis en pause en cours de route pour revenir sur BambuRfidReader, avait été livré à moitié fini. Terminé proprement cette fois :
- Le bouton Paramètres (engrenage) ouvre bien l'écran Paramètres.
- L'interrupteur "Affichage détaillé" fonctionne : décoché, seuls les champs essentiels sont affichés (poids, températures, dimensions...) ; coché, les champs techniques (GTIN, identifiant d'instance) s'ajoutent.
- Le bouton "Copier les résultats" copie le résultat affiché dans le presse-papier.
- Cette fois, l'intégralité du code (y compris l'écran, que je ne peux normalement pas compiler ici) a été vérifiée par une vraie compilation locale avant livraison, pas seulement relue à l'œil.

## v1.1 (build 2)

- Corrige : au scan d'une bobine, c'est l'appli officielle Prusa (déjà installée) qui s'ouvrait systématiquement à la place de PrusaTag. Cause : l'appli Prusa déclare très probablement un filtre NDEF précis sur le type `application/vnd.openprinttag` (son propre format), plus prioritaire côté Android que notre filtre générique `TECH_DISCOVERED` seul. PrusaTag déclare maintenant le même filtre : Android proposera un choix entre les deux applis au scan, au lieu d'imposer Prusa à chaque fois. Signalé par jcjames_13009 sur le forum (premier vrai test sur une bobine Prusament).

## v1.0 (build 1)

Première version.

- Lecture NFC-V/NDEF des tags OpenPrintTag (bobines Prusament).
- Décodeur CBOR/NDEF écrit à partir de zéro (`DecodeurCbor.kt`, `AnalyseurTag.kt`), validé
  champ par champ contre l'implémentation Python officielle de la spec OpenPrintTag et un vrai
  dump de bobine (Prusament PLA Galaxy Black, fourni par jcjames_13009).
- Affiche : marque, matériau, couleur, poids (nominal/réel/conteneur vide), températures
  (impression, préchauffe, plateau, chambre), dimensions du conteneur, longueur de filament,
  densité, tags matériau.
- Non testé sur un vrai lecteur NFC-V en conditions réelles (seul le décodage logiciel a pu
  être vérifié en local) — premier vrai test à faire sur le terrain.
