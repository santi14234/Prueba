package com.port.fxsa

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var p: Prefs
    private val bandBars = mutableListOf<SeekBar>()
    private var bands = mutableListOf<Int>()
    private lateinit var status: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun push() = EffectsService.push(this)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = Prefs(this)
        bands = p.bands.toMutableList()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        setContentView(ScrollView(this).apply { addView(root) })

        root.addView(Switch(this).apply {
            text = "Activar efectos"; isChecked = p.enabled; textSize = 18f
            setOnCheckedChangeListener { _, on -> p.enabled = on; push() }
        })
        root.addView(Switch(this).apply {
            text = "Salida global (todas las apps)"; isChecked = p.global
            setOnCheckedChangeListener { _, on -> p.global = on; push() }
        })

        root.addView(label("Preset del ecualizador"))
        val names = Engine.presetNames
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            var first = true
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (first) { first = false; return }
                    bands = Engine.curve10(names[pos]).toMutableList()
                    p.bands = bands
                    bandBars.forEachIndexed { i, s -> s.progress = bands[i] - Engine.MIN }
                    push()
                }
                override fun onNothingSelected(a: AdapterView<*>?) {}
            }
        })

        root.addView(label("Ecualizador de 10 bandas"))
        for (i in 0 until 10) {
            val f = Engine.VFREQ[i]
            val name = if (f >= 1000) "${f / 1000} kHz" else "$f Hz"
            val bar = slider(name, Engine.MIN, Engine.MAX, bands[i], { v -> "${v / 100f} dB" }) { v ->
                bands[i] = v; p.bands = bands; push()
            }
            root.addView(bar.first); bandBars.add(bar.second)
        }

        root.addView(Switch(this).apply {
            text = "Compensar volumen (no bajar al cortar bandas)"; isChecked = p.compensate
            setOnCheckedChangeListener { _, on -> p.compensate = on; push() }
        })

        root.addView(label("Amplificador, graves y 3D"))
        root.addView(slider("Amplificador", 0, 12, p.amp, { "+$it dB" }) { p.amp = it; push() }.first)
        root.addView(slider("Graves", 0, 100, p.bass, { "$it%" }) { p.bass = it; push() }.first)
        root.addView(slider("Virtualizador 3D", 0, 100, p.virt, { "$it%" }) { p.virt = it; push() }.first)

        root.addView(label("Sala de Concierto (reverb)"))
        root.addView(Switch(this).apply {
            text = "Activar reverb"; isChecked = p.reverbOn
            setOnCheckedChangeListener { _, on -> p.reverbOn = on; push(); status.postDelayed({ refreshStatus() }, 600) }
        })
        root.addView(slider("Nivel", 0, 100, p.reverb, { "$it%" }) { p.reverb = it; push() }.first)
        status = TextView(this); root.addView(status)
        refreshStatus()
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refreshStatus() }

    private fun refreshStatus() { status.text = "Estado del reverb: ${p.reverbStatus.ifEmpty { "—" }}" }

    private fun label(t: String) = TextView(this).apply { text = t; textSize = 16f; setPadding(0, dp(16), 0, dp(4)) }

    private fun slider(title: String, min: Int, max: Int, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit): Pair<View, SeekBar> {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tv = TextView(this).apply { text = "$title: ${fmt(value)}" }
        val sb = SeekBar(this).apply {
            this.max = max - min; progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, pr: Int, user: Boolean) {
                    tv.text = "$title: ${fmt(pr + min)}"
                    if (user) onChange(pr + min)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        box.addView(tv); box.addView(sb)
        return box to sb
    }
}
