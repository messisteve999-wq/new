package com.steve.newapp

object Personality {
    val ids = listOf("naturelle", "mysterieuse", "drague")

    fun normalize(id: String?): String = when (id?.trim()?.lowercase()) {
        "naturelle", "natural", "naturelle" -> "naturelle"
        "mysterieuse", "mystérieuse", "seduction",
        "séduction", "mysterious" -> "mysterieuse"
        "drague", "flirt", "dating" -> "drague"
        "connaissance" -> "naturelle"
        else -> "naturelle"
    }

    fun label(id: String): String = when (normalize(id)) {
        "naturelle" -> "Naturelle"
        "mysterieuse" -> "Mystérieuse"
        "drague" -> "Drague"
        else -> "Naturelle"
    }
}
