package com.example.a1234567889.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.a1234567889.R

/**
 * Lab item 6 — "Звуковые эффекты. Класс SoundPool. Воспроизведение, остановка,
 * управление громкостью".
 *
 * A tiny singleton wrapper around [SoundPool] that loads a handful of short
 * synthesized effects (see res/raw/sfx_*.wav) and exposes play / stop / volume
 * control used across the game (tile clicks, victory, defeat, achievements).
 */
class SoundManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<String, Int>()
    private val loadedIds = mutableSetOf<Int>()
    private val activeStreams = mutableListOf<Int>()

    /** 0f..1f, mirrors [com.example.a1234567889.models.GameSettings.sfxVolume]. */
    @Volatile var volume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Mirrors [com.example.a1234567889.models.GameSettings.isSfxEnabled]. */
    @Volatile var isEnabled: Boolean = true

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds.add(sampleId)
        }
        soundIds["click"] = soundPool.load(appContext, R.raw.sfx_click, 1)
        soundIds["win"] = soundPool.load(appContext, R.raw.sfx_win, 1)
        soundIds["error"] = soundPool.load(appContext, R.raw.sfx_error, 1)
        soundIds["achievement"] = soundPool.load(appContext, R.raw.sfx_achievement, 1)
    }

    private fun play(key: String, volumeOverride: Float? = null) {
        if (!isEnabled) return
        val id = soundIds[key] ?: return
        if (id !in loadedIds) return // not finished decoding yet — fail silently, no crash
        val v = (volumeOverride ?: volume).coerceIn(0f, 1f)
        val streamId = soundPool.play(id, v, v, 1, 0, 1f)
        if (streamId != 0) {
            activeStreams.add(streamId)
            if (activeStreams.size > 16) activeStreams.removeAt(0)
        }
    }

    fun playClick() = play("click")
    fun playWin() = play("win")
    fun playError() = play("error")
    fun playAchievement() = play("achievement")

    /** Stops every effect that is currently playing — demonstrates SoundPool.stop(). */
    fun stopAll() {
        activeStreams.forEach { soundPool.stop(it) }
        activeStreams.clear()
    }

    fun release() {
        soundPool.release()
    }

    companion object {
        @Volatile private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context).also { instance = it }
            }

        /** Short haptic "tick" used alongside clicks when vibration is enabled in settings. */
        fun vibrateTick(context: Context, durationMs: Long = 12L) {
            try {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator == null || !vibrator.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            } catch (_: Exception) {
                // Vibration is a nice-to-have; never crash the game over it.
            }
        }

        /** A slightly longer celebratory vibration pattern, used for wins/achievements. */
        fun vibrateSuccess(context: Context) {
            try {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator == null || !vibrator.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 60, 60, 60, 60, 120)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 60, 60, 60, 60, 120), -1)
                }
            } catch (_: Exception) {
            }
        }
    }
}
