package com.steve.newapp

import android.content.Context

object Prefs {
    private const val NAME = "new"
    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    // manual (défaut) | suggest | auto (réponse automatique, seulement si choisi)
    fun mode(c: Context): String = sp(c).getString("mode", "manual") ?: "manual"
    fun setMode(c: Context, m: String) = sp(c).edit().putString("mode", m).apply()
    fun bg(c: Context): String = sp(c).getString("bg", "#FFFFFF") ?: "#FFFFFF"
    fun setBg(c: Context, v: String) = sp(c).edit().putString("bg", v).apply()
    fun bool(c: Context, k: String, d: Boolean = false): Boolean = sp(c).getBoolean(k, d)
    fun setBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun text(c: Context, k: String, d: String = ""): String = sp(c).getString(k, d) ?: d
    fun setText(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
}
