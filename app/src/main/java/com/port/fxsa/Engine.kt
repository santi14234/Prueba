package com.port.fxsa

import android.media.audiofx.Equalizer
import kotlin.math.ln

data class EqInfo(val bands: Int, val min: Int, val max: Int, val freqs: List<Int>)

object Engine {
    /** 10 bandas tipo Eqfy / ecualizador gráfico. */
    val VFREQ = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    const val MIN = -1500
    const val MAX = 1500

    /** Ecualizador "hardware" del sistema (normalmente 5 bandas): se usa si no hay DynamicsProcessing. */
    fun eqInfo(): EqInfo = try {
        val e = Equalizer(0, 0)
        val n = e.numberOfBands.toInt()
        val r = e.bandLevelRange
        val f = List(n) { e.getCenterFreq(it.toShort()) / 1000 }
        e.release()
        EqInfo(n, r[0].toInt(), r[1].toInt(), f)
    } catch (t: Throwable) {
        EqInfo(5, -1500, 1500, listOf(60, 230, 910, 3600, 14000))
    }

    /** Convierte las 10 bandas virtuales a las N bandas reales del equipo (interpolación logarítmica). */
    fun mapToHardware(v10: List<Int>, hw: EqInfo): List<Int> = hw.freqs.map { f ->
        val x = (ln(f.coerceAtLeast(1) / 31.25) / ln(2.0)).coerceIn(0.0, 9.0)
        val a = x.toInt().coerceIn(0, 8)
        val g = v10[a] + (v10[a + 1] - v10[a]) * (x - a)
        g.toInt().coerceIn(hw.min, hw.max)
    }

    private val base: Map<String, List<Float>> = linkedMapOf(
        "Plano" to listOf(0f, 0f, 0f, 0f, 0f),
        "Graves" to listOf(6f, 4f, 0f, 0f, 0f),
        "Voz" to listOf(-2f, 0f, 3f, 3f, 0f),
        "Brillante" to listOf(0f, 0f, 1f, 4f, 6f),
        "Concierto" to listOf(3f, 1f, -1f, 2f, 4f),
        "Rock" to listOf(4f, 2f, -2f, 2f, 4f)
    )
    val presetNames get() = base.keys.toList()

    fun curve10(name: String): List<Int> {
        val pts = base[name] ?: base.getValue("Plano")
        return List(10) { i ->
            val pos = i * 4f / 9f
            val a = pos.toInt().coerceIn(0, 3)
            ((pts[a] + (pts[a + 1] - pts[a]) * (pos - a)) * 100).toInt().coerceIn(MIN, MAX)
        }
    }
}
