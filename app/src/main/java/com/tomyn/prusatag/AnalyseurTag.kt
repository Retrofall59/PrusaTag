package com.tomyn.prusatag

/**
 * Analyse un dump brut de tag NFC-V (ISO15693) au format NFC Type 5 Tag, localise les
 * enregistrements NDEF qu'il contient, et en extrait ceux qui nous interessent :
 * - le record URI (TNF=1, type='U') -> l'URL de reference (ex: https://3dtag.org/s/...)
 * - le record MIME "application/vnd.openprinttag" (TNF=2) -> les donnees CBOR OpenPrintTag
 *
 * Structure verifiee manuellement contre un vrai dump Prusament (voir /tests) :
 * [0..3]   Capability Container (CC)
 * [4]      Tag TLV = 0x03 (NDEF Message)
 * [5]      Longueur (0xFF = format etendu, longueur reelle sur les 2 octets suivants)
 * [...]    Message NDEF : un ou plusieurs enregistrements NDEF a la suite
 */
object AnalyseurTag {

    data class RecordNdef(val tnf: Int, val type: String, val payload: ByteArray)

    /** Localise le debut et la longueur du message NDEF dans le dump brut (apres la CC + TLV). */
    private fun localiserMessageNdef(dump: ByteArray): Pair<Int, Int>? {
        if (dump.size < 6) return null
        var i = 4 // apres la Capability Container (4 octets)
        while (i < dump.size) {
            val tag = dump[i].toInt() and 0xFF
            if (tag == 0x00) { i++; continue } // padding NULL TLV
            if (tag == 0xFE) return null // fin de zone TLV, pas de message NDEF trouve
            if (tag == 0x03) {
                var longueur = dump[i + 1].toInt() and 0xFF
                var debutMessage = i + 2
                if (longueur == 0xFF) {
                    longueur = ((dump[i + 2].toInt() and 0xFF) shl 8) or (dump[i + 3].toInt() and 0xFF)
                    debutMessage = i + 4
                }
                return Pair(debutMessage, longueur)
            }
            // TLV inconnu : on saute Tag+Longueur+Valeur pour continuer a chercher
            val longueur = dump[i + 1].toInt() and 0xFF
            i += 2 + longueur
        }
        return null
    }

    /** Decoupe le message NDEF en enregistrements individuels. */
    private fun decouperRecords(dump: ByteArray, debut: Int, longueur: Int): List<RecordNdef> {
        val records = mutableListOf<RecordNdef>()
        var pos = debut
        val fin = debut + longueur
        while (pos < fin) {
            val header = dump[pos].toInt() and 0xFF
            val sr = (header and 0x10) != 0        // Short Record
            val il = (header and 0x08) != 0        // ID Length present
            val tnf = header and 0x07
            pos++

            val typeLength = dump[pos].toInt() and 0xFF
            pos++

            val payloadLength: Int
            if (sr) {
                payloadLength = dump[pos].toInt() and 0xFF
                pos++
            } else {
                payloadLength = ((dump[pos].toInt() and 0xFF) shl 24) or
                        ((dump[pos + 1].toInt() and 0xFF) shl 16) or
                        ((dump[pos + 2].toInt() and 0xFF) shl 8) or
                        (dump[pos + 3].toInt() and 0xFF)
                pos += 4
            }

            val idLength = if (il) { val v = dump[pos].toInt() and 0xFF; pos++; v } else 0

            val type = String(dump.copyOfRange(pos, pos + typeLength), Charsets.US_ASCII)
            pos += typeLength
            pos += idLength // on ignore l'ID, pas utilise par OpenPrintTag

            val payload = dump.copyOfRange(pos, pos + payloadLength)
            pos += payloadLength

            records.add(RecordNdef(tnf, type, payload))
        }
        return records
    }

    /** Retourne les octets CBOR du record OpenPrintTag, ou null si absent/format inattendu. */
    fun extraireCbor(dump: ByteArray): ByteArray? {
        val (debut, longueur) = localiserMessageNdef(dump) ?: return null
        val records = decouperRecords(dump, debut, longueur)
        val record = records.firstOrNull { it.tnf == 2 && it.type == "application/vnd.openprinttag" }
        return record?.payload
    }

    /**
     * Le payload OpenPrintTag concatene plusieurs zones CBOR a la suite (meta, puis main,
     * puis eventuellement aux) plutot que d'etre un seul objet CBOR. La zone "meta" (toujours
     * en premier) indique via ses champs optionnels ou commencent/finissent les autres zones ;
     * a defaut, "main" comble l'espace entre la fin de "meta" et le debut de "aux" (ou la fin
     * du payload si "aux" est absente). Voir meta_fields.yaml dans la specification officielle.
     *
     * @return la zone "main" decodee (map cle CBOR -> valeur), ou null si le format est invalide.
     */
    @Suppress("UNCHECKED_CAST")
    fun extraireDonneesPrincipales(payloadOpenPrintTag: ByteArray): Map<Long, Any?>? {
        val (metaBrut, tailleMeta) = DecodeurCbor.decoderAvecTaille(payloadOpenPrintTag)
        val meta = metaBrut as? Map<Long, Any?> ?: emptyMap()

        val debutMain = (meta[0L] as? Long)?.toInt() ?: tailleMeta
        val debutAux = (meta[2L] as? Long)?.toInt()
        val finMain = when {
            meta[1L] != null -> debutMain + (meta[1L] as Long).toInt()
            debutAux != null -> debutAux
            else -> payloadOpenPrintTag.size
        }

        if (debutMain >= finMain || finMain > payloadOpenPrintTag.size) return null

        val octetsMain = payloadOpenPrintTag.copyOfRange(debutMain, finMain)
        val donneesMain = DecodeurCbor.decoder(octetsMain) as? Map<Long, Any?>
        return donneesMain
    }

    /** Retourne l'URL du record URI (TNF=1, type='U'), prefixe NFC deja resolu, ou null si absent. */
    fun extraireUri(dump: ByteArray): String? {
        val (debut, longueur) = localiserMessageNdef(dump) ?: return null
        val records = decouperRecords(dump, debut, longueur)
        val record = records.firstOrNull { it.tnf == 1 && it.type == "U" } ?: return null
        if (record.payload.isEmpty()) return null

        val prefixes = arrayOf(
            "", "http://www.", "https://www.", "http://", "https://",
            "tel:", "mailto:"
            // liste volontairement partielle : suffisant pour OpenPrintTag qui utilise "https://"
        )
        val code = record.payload[0].toInt() and 0xFF
        val prefixe = prefixes.getOrElse(code) { "" }
        val reste = String(record.payload.copyOfRange(1, record.payload.size), Charsets.UTF_8)
        return prefixe + reste
    }
}
