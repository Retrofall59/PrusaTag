package com.tomyn.prusatag

/**
 * Decodeur CBOR minimal (RFC 8949), suffisant pour lire le format OpenPrintTag.
 * Porte depuis un script Python valide manuellement contre un vrai dump Prusa
 * (voir /tests dans ce depot) : tous les types rencontres dans un tag reel passent.
 *
 * Types geres : uint, negint, bytes, texte, array (dont indefinie), map (dont indefinie),
 * tag CBOR, simple/bool/null, float16/32/64.
 * Ne gere PAS : les types CBOR exotiques absents du format OpenPrintTag (BigNum, etc.) —
 * suffisant pour ce format specifique, pas un decodeur CBOR generaliste.
 */
class DecodeurCbor(private val donnees: ByteArray) {
    private var position = 0

    private fun octetSuivant(): Int {
        val v = donnees[position].toInt() and 0xFF
        position++
        return v
    }

    private fun octets(n: Int): ByteArray {
        val r = donnees.copyOfRange(position, position + n)
        position += n
        return r
    }

    private fun lireLongueur(info: Int): Long? = when {
        info < 24 -> info.toLong()
        info == 24 -> octetSuivant().toLong()
        info == 25 -> {
            val b = octets(2)
            ((b[0].toLong() and 0xFF) shl 8) or (b[1].toLong() and 0xFF)
        }
        info == 26 -> {
            val b = octets(4)
            var v = 0L
            for (o in b) v = (v shl 8) or (o.toLong() and 0xFF)
            v
        }
        info == 27 -> {
            val b = octets(8)
            var v = 0L
            for (o in b) v = (v shl 8) or (o.toLong() and 0xFF)
            v
        }
        info == 31 -> null // longueur indefinie
        else -> throw IllegalArgumentException("info CBOR invalide : $info")
    }

    /** Point d'entree : decode un seul item CBOR a la position courante. */
    fun lireItem(): Any? {
        val premier = octetSuivant()
        val typeMajeur = premier shr 5
        val info = premier and 0x1F

        return when (typeMajeur) {
            0 -> lireLongueur(info) // uint
            1 -> -1L - (lireLongueur(info) ?: 0L) // negint
            2 -> { // bytes
                val n = (lireLongueur(info) ?: 0L).toInt()
                octets(n)
            }
            3 -> { // texte UTF-8
                val n = (lireLongueur(info) ?: 0L).toInt()
                String(octets(n), Charsets.UTF_8)
            }
            4 -> { // array
                val n = lireLongueur(info)
                val liste = mutableListOf<Any?>()
                if (n == null) {
                    while (donnees[position].toInt() and 0xFF != 0xFF) liste.add(lireItem())
                    position++ // consomme le break (0xFF)
                } else {
                    repeat(n.toInt()) { liste.add(lireItem()) }
                }
                liste
            }
            5 -> { // map
                val n = lireLongueur(info)
                val carte = LinkedHashMap<Any?, Any?>()
                if (n == null) {
                    while (donnees[position].toInt() and 0xFF != 0xFF) {
                        val cle = lireItem()
                        val valeur = lireItem()
                        carte[cle] = valeur
                    }
                    position++ // consomme le break (0xFF)
                } else {
                    repeat(n.toInt()) {
                        val cle = lireItem()
                        val valeur = lireItem()
                        carte[cle] = valeur
                    }
                }
                carte
            }
            6 -> { // tag CBOR : on l'ignore et on decode l'item suivant directement
                lireLongueur(info)
                lireItem()
            }
            7 -> when (info) { // simple/bool/null/float
                20 -> false
                21 -> true
                22 -> null
                25 -> lireFloat16()
                26 -> Float.fromBits(
                    octets(4).fold(0) { acc, o -> (acc shl 8) or (o.toInt() and 0xFF) }
                )
                27 -> Double.fromBits(
                    octets(8).fold(0L) { acc, o -> (acc shl 8) or (o.toLong() and 0xFF) }
                )
                else -> null
            }
            else -> throw IllegalArgumentException("type majeur CBOR invalide : $typeMajeur")
        }
    }

    private fun lireFloat16(): Float {
        val b = octets(2)
        val h = ((b[0].toInt() and 0xFF) shl 8) or (b[1].toInt() and 0xFF)
        val signe = (h shr 15) and 1
        val exposant = (h shr 10) and 0x1F
        val fraction = h and 0x3FF
        val valeur = if (exposant == 0) {
            (fraction / 1024.0) * Math.pow(2.0, -14.0)
        } else {
            (1 + fraction / 1024.0) * Math.pow(2.0, (exposant - 15).toDouble())
        }
        return (if (signe == 1) -valeur else valeur).toFloat()
    }

    companion object {
        /** Decode l'integralite du tableau d'octets donne comme un seul item CBOR racine. */
        fun decoder(donnees: ByteArray): Any? = DecodeurCbor(donnees).lireItem()

        /** Decode un item CBOR et indique aussi combien d'octets il a consomme (utile pour
         * les payloads OpenPrintTag qui concatenent plusieurs items CBOR a la suite). */
        fun decoderAvecTaille(donnees: ByteArray): Pair<Any?, Int> {
            val d = DecodeurCbor(donnees)
            val valeur = d.lireItem()
            return Pair(valeur, d.position)
        }
    }
}
