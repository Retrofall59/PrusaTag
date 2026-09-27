import com.tomyn.prusatag.AnalyseurTag
import com.tomyn.prusatag.ChampsOpenPrintTag
import java.io.File

fun main() {
    val dump = File("dump_prusament_galaxy_black.bin").readBytes()

    println("=== URI ===")
    println(AnalyseurTag.extraireUri(dump))

    println()
    println("=== Donnees OpenPrintTag decodees ===")
    val payload = AnalyseurTag.extraireCbor(dump)
    if (payload == null) {
        println("ECHEC : record OpenPrintTag introuvable")
        return
    }
    val donnees = AnalyseurTag.extraireDonneesPrincipales(payload)
    if (donnees == null) {
        println("ECHEC : impossible de decoder la zone principale")
        return
    }
    for ((cle, valeur) in donnees) {
        val nom = ChampsOpenPrintTag.nomChamp(cle.toInt())
        val valeurAffichee = when {
            nom == "material_class" -> ChampsOpenPrintTag.materialClass[(valeur as Long).toInt()] ?: valeur
            nom == "material_type" -> ChampsOpenPrintTag.materialType[(valeur as Long).toInt()] ?: valeur
            nom == "tags" -> (valeur as List<Any?>).map { ChampsOpenPrintTag.nomsTags[(it as Long).toInt()] ?: it }
            nom == "primary_color" -> {
                val octets = valeur as ByteArray
                "#" + octets.joinToString("") { String.format("%02x", it) }
            }
            else -> valeur
        }
        println("$nom = $valeurAffichee")
    }
}
