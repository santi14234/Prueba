package com.port.fxsa

import android.media.audiofx.*
import android.os.Build

/** Conjunto de efectos atados a una sesión de audio (0 = global). */
class EffectSet(val session: Int, val reverbOnly: Boolean) {
    private var eq: Equalizer? = null
    private var dp: DynamicsProcessing? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var reverb: AudioEffect? = null
    private var loud: LoudnessEnhancer? = null
    private var dpFailed = false

    private fun <T> safe(f: () -> T): T? = try { f() } catch (t: Throwable) { null }

    /** @return estado del reverb para mostrar en pantalla. */
    fun apply(p: Prefs): String {
        val v10 = p.bands
        if (!reverbOnly) {
            val compDb = if (p.compensate) maxOf(0f, -(v10.average().toFloat() / 100f)) else 0f
            val gainDb = (p.amp + compDb).coerceIn(0f, 24f)
            val usedDp = Build.VERSION.SDK_INT >= 28 && !dpFailed && applyDp(v10, gainDb)
            if (!usedDp) {
                applyHardwareEq(v10)
                if (loud == null) loud = safe { LoudnessEnhancer(session) }
                loud?.let { l -> safe { l.setTargetGain((gainDb * 100).toInt()); l.enabled = gainDb > 0f } }
            }
            if (bass == null) bass = safe { BassBoost(0, session) }
            bass?.let { b -> safe { if (b.isStrengthSupported) b.setStrength((p.bass * 10).toShort()); b.enabled = p.bass > 0 } }
            if (virt == null) virt = safe { Virtualizer(0, session) }
            virt?.let { v -> safe { if (v.isStrengthSupported) v.setStrength((p.virt * 10).toShort()); v.enabled = p.virt > 0 } }
        }
        return applyReverb(p)
    }

    private fun applyHardwareEq(v10: List<Int>) {
        val hw = Engine.eqInfo()
        val g = Engine.mapToHardware(v10, hw)
        if (eq == null) eq = safe { Equalizer(0, session) }
        eq?.let { e -> safe {
            e.enabled = true
            g.forEachIndexed { i, mb -> e.setBandLevel(i.toShort(), mb.toShort()) }
        } }
    }

    /** 10 bandas reales + limitador con ganancia (amplificador sin recortes) — Android 9+. */
    private fun applyDp(v10: List<Int>, gainDb: Float): Boolean = try {
        if (dp == null) {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
                true, 10, false, 0, false, 0, true).build()
            dp = DynamicsProcessing(0, session, cfg)
        }
        val d = dp!!
        v10.forEachIndexed { i, mb ->
            val cutoff = minOf(Engine.VFREQ[i] * 1.41f, 20000f)
            d.setPreEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, cutoff, mb / 100f))
        }
        d.setLimiterAllChannelsTo(DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -2f, gainDb))
        d.enabled = true
        d.hasControl()
    } catch (t: Throwable) {
        safe { dp?.release() }; dp = null; dpFailed = true; false
    }

    /** Reverb "Sala de Concierto": prueba EnvironmentalReverb y, si el equipo lo rechaza, PresetReverb(LARGEHALL). */
    private fun applyReverb(p: Prefs): String {
        if (!p.reverbOn) { safe { reverb?.enabled = false }; return "Apagado" }
        if (reverb == null) {
            reverb = safe { EnvironmentalReverb(0, session) }?.let { r ->
                if (r.hasControl()) r else { r.release(); null }
            }
            if (reverb == null) reverb = safe { PresetReverb(0, session) }?.let { r ->
                if (r.hasControl()) r else { r.release(); null }
            }
        }
        val r = reverb ?: return "No soportado en esta sesión"
        return when (r) {
            is EnvironmentalReverb -> safe {
                r.roomLevel = -1000; r.roomHFLevel = -400; r.decayTime = 3200; r.decayHFRatio = 800
                r.reflectionsLevel = -1200; r.reflectionsDelay = 25
                r.reverbLevel = (-3500 + p.reverb * 30).toShort(); r.reverbDelay = 45
                r.diffusion = 1000; r.density = 1000
                r.enabled = true
                "Activo (reverb ambiental)"
            } ?: "Error al aplicar"
            is PresetReverb -> safe {
                r.preset = PresetReverb.PRESET_LARGEHALL
                r.enabled = true
                "Activo (sala grande del sistema)"
            } ?: "Error al aplicar"
            else -> "Desconocido"
        }
    }

    fun release() {
        listOf(eq, dp, bass, virt, reverb, loud).forEach { safe { it?.enabled = false; it?.release() } }
        eq = null; dp = null; bass = null; virt = null; reverb = null; loud = null
    }
}
