package com.tomyn.prusatag

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var zoneInvite: android.view.View
    private lateinit var zoneResultat: android.view.View
    private lateinit var texteInvite: TextView
    private lateinit var carreCouleur: android.view.View
    private lateinit var texteMateriau: TextView
    private lateinit var texteMarque: TextView
    private lateinit var texteDetails: TextView

    private val nomsChampsAffiches = listOf(
        "gtin", "brand_specific_instance_id", "manufactured_date",
        "nominal_netto_full_weight", "actual_netto_full_weight", "empty_container_weight",
        "min_print_temperature", "max_print_temperature", "preheat_temperature",
        "min_bed_temperature", "max_bed_temperature",
        "min_chamber_temperature", "max_chamber_temperature", "chamber_temperature",
        "container_width", "container_outer_diameter", "container_inner_diameter", "container_hole_diameter",
        "nominal_full_length", "actual_full_length", "density", "tags"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        zoneInvite = findViewById(R.id.zoneInvite)
        zoneResultat = findViewById(R.id.zoneResultat)
        texteInvite = findViewById(R.id.texteInvite)
        carreCouleur = findViewById(R.id.carreCouleur)
        texteMateriau = findViewById(R.id.texteMateriau)
        texteMarque = findViewById(R.id.texteMarque)
        texteDetails = findViewById(R.id.texteDetails)

        traiterIntentEventuel(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        traiterIntentEventuel(intent)
    }

    private fun traiterIntentEventuel(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action != android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED) return

        @Suppress("DEPRECATION")
        val tag = intent.getParcelableExtra<Tag>(android.nfc.NfcAdapter.EXTRA_TAG) ?: return
        lireTag(tag)
    }

    private fun lireTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            Toast.makeText(this, "Ce tag ne supporte pas la lecture NDEF", Toast.LENGTH_LONG).show()
            return
        }

        try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            if (message == null) {
                Toast.makeText(this, "Tag vide ou illisible", Toast.LENGTH_LONG).show()
                return
            }

            var payloadOpenPrintTag: ByteArray? = null
            var uri: String? = null

            for (record in message.records) {
                if (record.tnf == NdefRecord.TNF_MIME_MEDIA &&
                    String(record.type, Charsets.US_ASCII) == "application/vnd.openprinttag"
                ) {
                    payloadOpenPrintTag = record.payload
                }
                if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                    record.type.contentEquals(NdefRecord.RTD_URI)
                ) {
                    uri = record.toUri()?.toString()
                }
            }

            if (payloadOpenPrintTag == null) {
                Toast.makeText(this, "Ce tag n'est pas au format OpenPrintTag", Toast.LENGTH_LONG).show()
                return
            }

            val donnees = AnalyseurTag.extraireDonneesPrincipales(payloadOpenPrintTag)
            if (donnees == null) {
                Toast.makeText(this, "Données OpenPrintTag illisibles (tag endommagé ?)", Toast.LENGTH_LONG).show()
                return
            }

            afficherResultats(donnees, uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de lecture : ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try { ndef.close() } catch (e: Exception) { /* rien a faire */ }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun afficherResultats(donnees: Map<Long, Any?>, uri: String?) {
        fun valeur(nomChamp: String): Any? {
            val cle = ChampsOpenPrintTag.nomsChamps.entries.firstOrNull { it.value == nomChamp }?.key
            return cle?.let { donnees[it.toLong()] }
        }

        val materialName = valeur("material_name") as? String ?: "Matériau inconnu"
        val brandName = valeur("brand_name") as? String ?: ""
        val materialType = (valeur("material_type") as? Long)?.toInt()
            ?.let { ChampsOpenPrintTag.materialType[it] } ?: ""
        val materialClass = (valeur("material_class") as? Long)?.toInt()
            ?.let { ChampsOpenPrintTag.materialClass[it] } ?: ""

        texteMateriau.text = if (brandName.isNotEmpty()) "$brandName $materialName" else materialName
        texteMarque.text = listOfNotNull(materialClass.ifEmpty { null }, materialType.ifEmpty { null }).joinToString(" · ")

        val couleurOctets = valeur("primary_color") as? ByteArray
        if (couleurOctets != null && couleurOctets.size >= 3) {
            val r = couleurOctets[0].toInt() and 0xFF
            val g = couleurOctets[1].toInt() and 0xFF
            val b = couleurOctets[2].toInt() and 0xFF
            (carreCouleur.background as? GradientDrawable)?.setColor(Color.rgb(r, g, b))
        }

        val details = StringBuilder()
        for (nomChamp in nomsChampsAffiches) {
            val v = valeur(nomChamp) ?: continue
            val texteValeur = when {
                nomChamp == "tags" -> (v as? List<Any?>)
                    ?.mapNotNull { (it as? Long)?.toInt()?.let { c -> ChampsOpenPrintTag.nomsTags[c] } }
                    ?.joinToString(", ") ?: v.toString()
                nomChamp == "manufactured_date" -> {
                    val date = java.util.Date((v as Long) * 1000)
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(date)
                }
                nomChamp == "density" -> String.format(java.util.Locale.FRANCE, "%.2f g/cm³", v as Float)
                else -> v.toString()
            }
            details.appendLine("$nomChamp : $texteValeur")
        }
        if (uri != null) details.appendLine("\nurl : $uri")
        texteDetails.text = details.toString().trim()

        zoneInvite.visibility = android.view.View.GONE
        zoneResultat.visibility = android.view.View.VISIBLE
    }
}
