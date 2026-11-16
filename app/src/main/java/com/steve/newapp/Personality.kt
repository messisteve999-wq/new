package com.steve.newapp

object Personality {
    val ids = listOf("connaissance", "drague", "romance", "mysterieux", "amical")

    fun normalize(id: String?): String =
        if (id != null && id in ids) id else "connaissance"

    fun label(id: String): String = when (normalize(id)) {
        "connaissance" -> "Faire connaissance"
        "drague" -> "Drague amicale"
        "romance" -> "Romantique"
        "mysterieux" -> "Mystérieux"
        "amical" -> "Amical"
        else -> "Faire connaissance"
    }
}
