# Tests — décodage OpenPrintTag

## Lancer le test

```
cd tests
kotlinc ../app/src/main/java/com/tomyn/prusatag/ChampsOpenPrintTag.kt \
        ../app/src/main/java/com/tomyn/prusatag/DecodeurCbor.kt \
        ../app/src/main/java/com/tomyn/prusatag/AnalyseurTag.kt \
        TestDecodage.kt -include-runtime -d test.jar
java -jar test.jar
```

## Contenu

- **`dump_prusament_galaxy_black.bin`** — dump brut réel (320 octets) d'une bobine Prusament
  PLA Galaxy Black, transcrit à la main depuis des captures d'écran de l'appli NFC Cool
  (fournies par jcjames_13009 sur le forum). Sert de cas de référence pour valider tout
  changement futur au décodeur NDEF/CBOR.
- **`TestDecodage.kt`** — décode ce dump et affiche tous les champs. Le résultat attendu
  (vérifié champ par champ contre l'appli OpenPrintTag et l'implémentation Python officielle
  de la spec) :

```
gtin = 8594173675001
brand_specific_instance_id = 654b66fa0e
material_class = FFF
material_type = PLA
material_name = PLA Prusa Galaxy Black
brand_name = Prusament
manufactured_date = 1765902077 (16 décembre 2025)
nominal_netto_full_weight = 1000
actual_netto_full_weight = 1068
empty_container_weight = 277
primary_color = #3d3e3d
tags = [glitter, industrially_compostable]
density = 1.24
min_print_temperature = 205 / max = 225
preheat_temperature = 170
min_bed_temperature = 40 / max = 60
min_chamber_temperature = 18 / max = 40 / cible = 20
container_width = 67, outer_diameter = 200, inner_diameter = 101, hole_diameter = 51
nominal_full_length = 338604, actual_full_length = 361754
```

Avant de toucher au décodeur (`DecodeurCbor.kt`, `AnalyseurTag.kt`), relance ce test et vérifie
que ces valeurs ne changent pas.
