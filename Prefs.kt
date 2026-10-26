package com.mohamed.phone

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object Prefs {
    private lateinit var sp: SharedPreferences

    /** 0 = زي النظام، 1 = فاتح، 2 = داكن */
    val theme = MutableStateFlow(0)
    val amoled = MutableStateFlow(false)
    /** -1 = ألوان الخلفية (Material You) */
    val accent = MutableStateFlow(-1)
    val haptic = MutableStateFlow(true)
    val keypadTones = MutableStateFlow(true)

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        theme.value = sp.getInt("theme", 0)
        amoled.value = sp.getBoolean("amoled", false)
        accent.value = sp.getInt("accent", -1)
        haptic.value = sp.getBoolean("haptic", true)
        keypadTones.value = sp.getBoolean("tones", true)
    }

    fun setTheme(v: Int) { theme.value = v; sp.edit().putInt("theme", v).apply() }
    fun setAmoled(v: Boolean) { amoled.value = v; sp.edit().putBoolean("amoled", v).apply() }
    fun setAccent(v: Int) { accent.value = v; sp.edit().putInt("accent", v).apply() }
    fun setHaptic(v: Boolean) { haptic.value = v; sp.edit().putBoolean("haptic", v).apply() }
    fun setTones(v: Boolean) { keypadTones.value = v; sp.edit().putBoolean("tones", v).apply() }
}
