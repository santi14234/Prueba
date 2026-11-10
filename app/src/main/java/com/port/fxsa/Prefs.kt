package com.port.fxsa

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("fx", Context.MODE_PRIVATE)
    var enabled: Boolean get() = sp.getBoolean("on", false); set(v) = sp.edit().putBoolean("on", v).apply()
    var global: Boolean get() = sp.getBoolean("global", true); set(v) = sp.edit().putBoolean("global", v).apply()
    var bass: Int get() = sp.getInt("bass", 0); set(v) = sp.edit().putInt("bass", v).apply()          // 0..100
    var virt: Int get() = sp.getInt("virt", 0); set(v) = sp.edit().putInt("virt", v).apply()          // 0..100
    var reverbOn: Boolean get() = sp.getBoolean("rvOn", false); set(v) = sp.edit().putBoolean("rvOn", v).apply()
    var reverb: Int get() = sp.getInt("rv", 40); set(v) = sp.edit().putInt("rv", v).apply()           // 0..100
    var amp: Int get() = sp.getInt("amp", 0); set(v) = sp.edit().putInt("amp", v).apply()             // dB 0..12
    var compensate: Boolean get() = sp.getBoolean("comp", true); set(v) = sp.edit().putBoolean("comp", v).apply()
    var reverbStatus: String get() = sp.getString("rvStatus", "") ?: ""; set(v) = sp.edit().putString("rvStatus", v).apply()
    /** 10 bandas virtuales, en milibelios (-1500..1500). */
    var bands: List<Int>
        get() { val l = (sp.getString("bands10", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }; return List(10) { l.getOrElse(it) { 0 } } }
        set(v) = sp.edit().putString("bands10", v.joinToString(",")).apply()
}
