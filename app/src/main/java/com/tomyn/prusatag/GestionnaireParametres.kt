package com.tomyn.prusatag

import android.content.Context

object GestionnaireParametres {

    private const val FICHIER = "prusatag_parametres"
    private const val CLE_VUE_DETAILLEE = "vue_detaillee"

    fun lireVueDetaillee(context: Context): Boolean =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getBoolean(CLE_VUE_DETAILLEE, false)

    fun ecrireVueDetaillee(context: Context, active: Boolean) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putBoolean(CLE_VUE_DETAILLEE, active).apply()
    }
}
