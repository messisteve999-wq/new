package com.steve.newapp

object Personality {
    val ids = listOf(
        "connaissance",
        "chaleureux",
        "romance",
        "drague",
        "amical",
        "humour",
        "professionnel",
        "mysterieux"
    )

    fun label(id: String): String = when (id) {
        "connaissance" -> "Faire connaissance"
        "chaleureux" -> "Chaleureux"
        "romance" -> "Romantique"
        "drague" -> "Drague amicale"
        "amical" -> "Amical"
        "humour" -> "Humoristique"
        "professionnel" -> "Professionnel"
        "mysterieux" -> "Mystérieux"
        else -> "Faire connaissance"
    }
}
