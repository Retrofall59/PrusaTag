# Changelog

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
