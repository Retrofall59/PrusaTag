# PrusaTag

Appli Android pour lire les tags NFC OpenPrintTag des bobines Prusament (Prusa), et afficher
matériau, couleur, poids, températures et dimensions du conteneur.

**Projet séparé de [BambuRfidReader](https://github.com/Retrofall59/BambuRfidReader)** : technologie
différente (NFC-V/ISO15693 + NDEF standard, contre MIFARE Classic pour Bambu), format différent
(OpenPrintTag, une spec ouverte, contre le format propriétaire Bambu).

## Fonctionnement

Approche le dos du téléphone d'une bobine Prusament. L'appli lit le tag NFC-V, en extrait
l'enregistrement NDEF "application/vnd.openprinttag", décode les données CBOR qu'il contient,
et affiche :
- Marque, matériau, couleur
- Poids nominal / réel / du conteneur vide
- Températures d'impression, de préchauffe, de plateau, de chambre
- Dimensions du conteneur, longueur de filament nominale/réelle
- Tags matériau (glitter, compostable, etc.)

## Format OpenPrintTag

[OpenPrintTag](https://openprinttag.org) est une spécification ouverte (licence MIT) pour
identifier les bobines de filament par NFC. Le décodeur de cette appli (`DecodeurCbor.kt`,
`AnalyseurTag.kt`, `ChampsOpenPrintTag.kt`) est écrit à partir de zéro en Kotlin pur, sans
dépendance externe, et validé contre l'implémentation Python officielle de la spec ainsi qu'un
vrai dump de bobine Prusament (voir le dossier `tests/`).

## Distribution

Comme pour BambuRfidReader : chaque mise à jour est livrée sous forme de zip complet du dépôt à
uploader sur GitHub, ce qui déclenche automatiquement la compilation de l'APK via GitHub Actions.

## Historique des versions

Voir [CHANGELOG.md](CHANGELOG.md).

## Remerciements

Merci à jcjames_13009 sur le forum lesimprimantes3d.fr pour avoir fourni le dump de test réel
qui a permis de valider tout le décodeur.
