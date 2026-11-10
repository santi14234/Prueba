package com.port.fxsa

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder

/**
 * Mantiene los efectos vivos. Modo "Salida global": sesión 0 (como Eqfy "Global").
 * Además escucha las sesiones que abren los reproductores para (a) procesar solo esa sesión si
 * el modo global está apagado, o (b) aplicar el reverb por sesión cuando el reverb global no funciona.
 */
class EffectsService : Service() {
    private val sets = HashMap<Int, EffectSet>()
    private val sessions = LinkedHashSet<Int>()

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val s = i.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (s <= 0) return
            if (i.action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) sessions.add(s)
            else { sessions.remove(s); sets.remove(s)?.release() }
            sync()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val f = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        registerReceiver(sessionReceiver, f)
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notification())
        if (!Prefs(this).enabled) { releaseAll(); stopForeground(true); stopSelf(); return START_NOT_STICKY }
        sync()
        return START_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val b = if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("fx", "Efectos de sonido", NotificationManager.IMPORTANCE_MIN))
            Notification.Builder(this, "fx")
        } else Notification.Builder(this)
        return b.setContentTitle("FxPort Sound activo").setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)).build()
    }

    private fun sync() {
        val p = Prefs(this)
        var status = "Apagado"
        var globalOk = false

        if (p.global) {
            val g = sets.getOrPut(0) { EffectSet(0, false) }
            status = g.apply(p)
            globalOk = status.startsWith("Activo") || !p.reverbOn
        } else sets.remove(0)?.release()

        for (s in sessions.toList()) {
            val needed = if (p.global) (p.reverbOn && !globalOk) else true
            if (!needed) { sets.remove(s)?.release(); continue }
            val set = sets.getOrPut(s) { EffectSet(s, p.global) }
            if (set.reverbOnly != p.global) { set.release(); sets.remove(s); sets[s] = EffectSet(s, p.global) }
            val st = sets.getValue(s).apply(p)
            if (!globalOk && st.startsWith("Activo")) status = "$st — por sesión"
        }
        p.reverbStatus = status
    }

    private fun releaseAll() { sets.values.forEach { it.release() }; sets.clear() }

    override fun onDestroy() {
        try { unregisterReceiver(sessionReceiver) } catch (_: Throwable) {}
        releaseAll(); super.onDestroy()
    }

    companion object {
        fun push(ctx: Context) {
            val i = Intent(ctx, EffectsService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }
}
