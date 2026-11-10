# FxPort Sound
Ecualizador global + reverb Sala de Concierto + virtualizador + graves + amplificador.
Java/Kotlin puro (sin código nativo): un solo APK funciona en 32 bits (armeabi-v7a) y 64 bits (arm64-v8a).
Subir a GitHub -> Actions -> Build FxPort Sound -> descargar artefacto `FxPort-Sound` (APK).

- 10 bandas reales con DynamicsProcessing (Android 9+). En Android 8.1 (J2 Core) el sistema solo ofrece ~5 bandas:
  las 10 bandas se mapean por interpolación a las del equipo.
- Modo "Salida global" (sesión 0) o por sesión (escucha OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION).
- Reverb: EnvironmentalReverb -> PresetReverb(GRAN SALA) -> por sesión; la app muestra el estado real.
- Config guardada en SharedPreferences y restaurada al arrancar.
