package com.randallengineering.finances.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object DuolingoSoundEffects {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a crisp, melodic ascending chime (C5 -> E5 -> G5 -> C6) for XP reward claims and successes.
     */
    fun playSuccessChime(context: Context? = null) {
        context?.let { performSuccessHaptic(it) }
        audioScope.launch {
            playFrequencies(
                listOf(
                    Tone(523.25, 60),  // C5
                    Tone(659.25, 60),  // E5
                    Tone(783.99, 80),  // G5
                    Tone(1046.50, 180) // C6
                )
            )
        }
    }

    /**
     * Plays a triumphant celebratory fanfare for Level-Up and Boss Battle victories.
     */
    fun playLevelUpFanfare(context: Context? = null) {
        context?.let { performLevelUpHaptic(it) }
        audioScope.launch {
            playFrequencies(
                listOf(
                    Tone(523.25, 70),  // C5
                    Tone(659.25, 70),  // E5
                    Tone(783.99, 70),  // G5
                    Tone(1046.50, 90), // C6
                    Tone(1318.51, 90), // E6
                    Tone(1567.98, 250) // G6
                )
            )
        }
    }

    /**
     * Plays a snappy combo click chime for rapid queue verification.
     */
    fun playComboChime(combo: Int, context: Context? = null) {
        context?.let { performClickHaptic(it) }
        val baseFreq = 440.0 + (combo * 80.0)
        audioScope.launch {
            playFrequencies(
                listOf(
                    Tone(baseFreq, 50),
                    Tone(baseFreq * 1.25, 90)
                )
            )
        }
    }

    /**
     * Generates and plays pure sine wave tones using AudioTrack.
     */
    private fun playFrequencies(tones: List<Tone>) {
        try {
            val sampleRate = 44100
            val totalDurationMs = tones.sumOf { it.durationMs }
            val totalSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
            val generatedSnd = ShortArray(totalSamples)

            var sampleIdx = 0
            for (tone in tones) {
                val toneSamples = (sampleRate * (tone.durationMs / 1000.0)).toInt()
                for (i in 0 until toneSamples) {
                    if (sampleIdx >= totalSamples) break
                    // Sine wave calculation with smooth attack/decay envelope
                    val envelope = when {
                        i < toneSamples * 0.1 -> i / (toneSamples * 0.1)
                        i > toneSamples * 0.7 -> (toneSamples - i) / (toneSamples * 0.3)
                        else -> 1.0
                    }
                    val angle = 2.0 * Math.PI * i / (sampleRate / tone.frequency)
                    generatedSnd[sampleIdx] = (sin(angle) * 32767 * 0.45 * envelope).toInt().toShort()
                    sampleIdx++
                }
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performClickHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vibrator.vibrate(15)
        }
    }

    fun performSuccessHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrator.vibrate(40)
        }
    }

    fun performLevelUpHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 40, 70, 50, 120)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(120)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private data class Tone(val frequency: Double, val durationMs: Long)
}
