package com.glyphpay.nfc

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("glyph_pay", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var style: GlyphAnimations.Style
        get() = runCatching { GlyphAnimations.Style.valueOf(sp.getString("style", "PAY")!!) }
            .getOrDefault(GlyphAnimations.Style.PAY)
        set(v) = sp.edit().putString("style", v.name).apply()

    /** Яркость, 10..100 % */
    var brightness: Int
        get() = sp.getInt("brightness", 100)
        set(v) = sp.edit().putInt("brightness", v).apply()

    /** Дополнительные package name приложений через запятую */
    var extraPackages: String
        get() = sp.getString("extra_packages", "") ?: ""
        set(v) = sp.edit().putString("extra_packages", v).apply()

    var lastEvent: String
        get() = sp.getString("last_event", "") ?: ""
        set(v) = sp.edit().putString("last_event", v).apply()
}
