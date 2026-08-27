package com.example.chess.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-fidelity acoustic chess sound generator that produces realistic
 * wooden chess piece sounds on-the-fly without external audio assets.
 */
class ChessSoundManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val sampleRate = 44100
    var isSoundEnabled: Boolean = true

    // Pre-generated PCM audio buffers for instant zero-latency playback
    private val moveBuffer: ShortArray by lazy { generateWoodMoveSound() }
    private val captureBuffer: ShortArray by lazy { generateCaptureSound() }
    private val checkBuffer: ShortArray by lazy { generateCheckSound() }
    private val castleBuffer: ShortArray by lazy { generateCastleSound() }
    private val victoryBuffer: ShortArray by lazy { generateGameOverSound() }

    fun playMoveSound() {
        if (!isSoundEnabled) return
        playSoundBuffer(moveBuffer)
    }

    fun playCaptureSound() {
        if (!isSoundEnabled) return
        playSoundBuffer(captureBuffer)
    }

    fun playCheckSound() {
        if (!isSoundEnabled) return
        playSoundBuffer(checkBuffer)
    }

    fun playCastleSound() {
        if (!isSoundEnabled) return
        playSoundBuffer(castleBuffer)
    }

    fun playGameOverSound() {
        if (!isSoundEnabled) return
        playSoundBuffer(victoryBuffer)
    }

    private fun playSoundBuffer(buffer: ShortArray) {
        scope.launch {
            try {
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Release track after playback completes
                val durationMs = (buffer.size.toDouble() / sampleRate * 1000).toLong() + 50
                kotlinx.coroutines.delay(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Graceful fallback
            }
        }
    }

    /**
     * Generates a realistic wooden piece placement "thud/knock" on a wooden board.
     */
    private fun generateWoodMoveSound(): ShortArray {
        val duration = 0.08 // 80 ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Exponential rapid decay
            val decay = exp(-45.0 * t)
            // Wood resonant frequencies: 220Hz fundamental + 480Hz second harmonic + 820Hz snap
            val fundamental = sin(2.0 * PI * 220.0 * t)
            val harmonic = 0.4 * sin(2.0 * PI * 480.0 * t)
            val click = 0.25 * sin(2.0 * PI * 850.0 * t) * exp(-120.0 * t)
            // Initial impact noise
            val noise = if (i < (0.006 * sampleRate)) (Random.nextDouble() * 2.0 - 1.0) * 0.3 * exp(-300.0 * t) else 0.0

            val sample = (fundamental + harmonic + click + noise) * decay
            val clamped = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
            buffer[i] = clamped
        }
        return buffer
    }

    /**
     * Generates a crisp piece capture sound: sharp contact transient + deep body resonance.
     */
    private fun generateCaptureSound(): ShortArray {
        val duration = 0.095 // 95 ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-38.0 * t)
            // Sharp snap at 1200Hz + resonant strike at 280Hz + body at 160Hz
            val snap = 0.6 * sin(2.0 * PI * 1250.0 * t) * exp(-160.0 * t)
            val body = sin(2.0 * PI * 280.0 * t)
            val lowThud = 0.5 * sin(2.0 * PI * 160.0 * t)
            val impactNoise = if (i < (0.008 * sampleRate)) (Random.nextDouble() * 2.0 - 1.0) * 0.45 * exp(-250.0 * t) else 0.0

            val sample = (snap + body + lowThud + impactNoise) * decay
            val clamped = (sample.coerceIn(-1.0, 1.0) * 31000).toInt().toShort()
            buffer[i] = clamped
        }
        return buffer
    }

    /**
     * Generates an alert double-tap for Check warning.
     */
    private fun generateCheckSound(): ShortArray {
        val duration = 0.14 // 140 ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Tap 1 at t=0, Tap 2 at t=0.055
            val tap1Time = t
            val tap2Time = t - 0.055

            var sample = 0.0
            if (tap1Time >= 0) {
                val decay1 = exp(-60.0 * tap1Time)
                sample += (sin(2.0 * PI * 587.33 * tap1Time) + 0.4 * sin(2.0 * PI * 880.0 * tap1Time)) * decay1
            }
            if (tap2Time >= 0) {
                val decay2 = exp(-50.0 * tap2Time)
                sample += (sin(2.0 * PI * 880.0 * tap2Time) + 0.5 * sin(2.0 * PI * 1174.66 * tap2Time)) * decay2
            }

            val clamped = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
            buffer[i] = clamped
        }
        return buffer
    }

    /**
     * Generates a rapid two-step wood placement for Castling.
     */
    private fun generateCastleSound(): ShortArray {
        val duration = 0.16 // 160 ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val t1 = t
            val t2 = t - 0.075

            var sample = 0.0
            if (t1 >= 0) {
                val decay1 = exp(-50.0 * t1)
                sample += sin(2.0 * PI * 240.0 * t1) * decay1
            }
            if (t2 >= 0) {
                val decay2 = exp(-45.0 * t2)
                sample += (sin(2.0 * PI * 210.0 * t2) + 0.3 * sin(2.0 * PI * 440.0 * t2)) * decay2
            }

            val clamped = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
            buffer[i] = clamped
        }
        return buffer
    }

    /**
     * Generates an elegant harmonic completion chime for Checkmate / Game Victory.
     */
    private fun generateGameOverSound(): ShortArray {
        val duration = 0.35 // 350 ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-12.0 * t)
            // Major chord: C5 (523.25Hz) + E5 (659.25Hz) + G5 (783.99Hz)
            val note1 = sin(2.0 * PI * 523.25 * t)
            val note2 = 0.7 * sin(2.0 * PI * 659.25 * t)
            val note3 = 0.5 * sin(2.0 * PI * 783.99 * t)

            val sample = (note1 + note2 + note3) / 2.2 * decay
            val clamped = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
            buffer[i] = clamped
        }
        return buffer
    }
}
