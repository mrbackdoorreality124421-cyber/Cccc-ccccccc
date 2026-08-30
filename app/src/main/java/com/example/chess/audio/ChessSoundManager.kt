package com.example.chess.audio

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class HapticType {
    LIGHT, MEDIUM, HEAVY, CHECK, CHECKMATE
}

/**
 * ASMR-grade Acoustic Chess Sound & Haptic Manager.
 * Synthesizes ultra high-fidelity acoustic wood piece physics on 44.1kHz 16-bit PCM mono
 * with instantaneous zero-latency playback and contextual tactile vibrations.
 */
class ChessSoundManager {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var isEnabled: Boolean = true
    var volume: Float = 1.0f

    // Pre-generate all sound buffers for zero-latency instant playback
    private val moveSound by lazy { generateMoveSound() }
    private val captureSound by lazy { generateCaptureSound() }
    private val castleSound by lazy { generateCastleSound() }
    private val checkSound by lazy { generateCheckSound() }
    private val checkmateSound by lazy { generateCheckmateSound() }
    private val promotionSound by lazy { generatePromotionSound() }
    private val illegalSound by lazy { generateIllegalSound() }
    private val gameStartSound by lazy { generateGameStartSound() }
    private val gameEndSound by lazy { generateGameEndSound() }
    private val hintReadySound by lazy { generateHintReadySound() }
    private val correctMoveSound by lazy { generateCorrectMoveSound() }

    fun playMove() = play(moveSound)
    fun playCapture() = play(captureSound)
    fun playCastle() = play(castleSound)
    fun playCheck() = play(checkSound)
    fun playCheckmate() = play(checkmateSound)
    fun playPromotion() = play(promotionSound)
    fun playIllegal() = play(illegalSound)
    fun playGameStart() = play(gameStartSound)
    fun playGameEnd() = play(gameEndSound)
    fun playHintReady() = play(hintReadySound)
    fun playCorrectMove() = play(correctMoveSound)

    // Legacy method aliases for backward compatibility
    fun playMoveSound() = playMove()
    fun playCaptureSound() = playCapture()
    fun playCastleSound() = playCastle()
    fun playCheckSound() = playCheck()
    fun playGameOverSound() = playCheckmate()
    var isSoundEnabled: Boolean
        get() = isEnabled
        set(value) { isEnabled = value }

    private fun play(buffer: ShortArray) {
        if (!isEnabled) return
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
                audioTrack.setVolume(volume)
                audioTrack.play()

                val durationMs = (buffer.size.toDouble() / sampleRate * 1000).toLong() + 50
                delay(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Gracefully ignore audio hardware glitches
            }
        }
    }

    // ===== ASMR PROCEDURAL AUDIO SYNTHESIS =====

    /**
     * MOVE SOUND: Soft wooden "thud" with subtle body resonance.
     * Feels like a weighted wooden Staunton piece placed onto a polished walnut board.
     */
    private fun generateMoveSound(): ShortArray {
        val duration = 0.12 // 120ms
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-35.0 * t) * (1.0 - exp(-200.0 * t))

            val f1 = 180.0  // Main wood resonance
            val f2 = 420.0  // Mid body tone
            val f3 = 680.0  // High surface snap

            val body = sin(2 * PI * f1 * t)
            val mid = 0.3 * sin(2 * PI * f2 * t)
            val snap = 0.15 * sin(2 * PI * f3 * t) * exp(-80.0 * t)

            // Tactile surface contact noise in the initial 5ms
            val contactNoise = if (t < 0.005) {
                (Random.nextDouble() * 2.0 - 1.0) * 0.2 * exp(-300.0 * t)
            } else 0.0

            val sample = (body + mid + snap + contactNoise) * envelope
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return buffer
    }

    /**
     * CAPTURE SOUND: Crisp tactile clack with deep body resonance.
     * High-speed piece-on-piece wooden collision.
     */
    private fun generateCaptureSound(): ShortArray {
        val duration = 0.15 // 150ms
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate

            val impact = exp(-150.0 * t)
            val ring = exp(-25.0 * t) * sin(2 * PI * 280.0 * t)

            val f1 = 1200.0 // Sharp contact
            val f2 = 800.0  // Body strike
            val f3 = 350.0  // Deep resonance

            val sharp = sin(2 * PI * f1 * t) * impact
            val body = 0.6 * sin(2 * PI * f2 * t) * impact
            val deep = 0.4 * sin(2 * PI * f3 * t) * ring

            val transient = if (t < 0.003) {
                (Random.nextDouble() * 2.0 - 1.0) * 0.5 * exp(-400.0 * t)
            } else 0.0

            val sample = (sharp + body + deep + transient) * 0.8
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 32000).toInt().toShort()
        }
        return buffer
    }

    /**
     * CASTLE SOUND: Two rapid staggered wood thuds (King then Rook).
     */
    private fun generateCastleSound(): ShortArray {
        val duration = 0.25 // 250ms
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        val move1Time = 0.0
        val move2Time = 0.11 // 110ms gap

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            val t1 = t - move1Time
            if (t1 >= 0) {
                val env1 = exp(-35.0 * t1) * (1.0 - exp(-200.0 * t1))
                sample += (sin(2 * PI * 180.0 * t1) + 0.3 * sin(2 * PI * 420.0 * t1)) * env1 * 0.7
            }

            val t2 = t - move2Time
            if (t2 >= 0) {
                val env2 = exp(-35.0 * t2) * (1.0 - exp(-200.0 * t2))
                sample += (sin(2 * PI * 200.0 * t2) + 0.3 * sin(2 * PI * 450.0 * t2)) * env2 * 0.7
            }

            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return buffer
    }

    /**
     * CHECK SOUND: Double alert bell tone with subtle low tension drone.
     */
    private fun generateCheckSound(): ShortArray {
        val duration = 0.25
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate

            val tone1 = if (t < 0.08) sin(2 * PI * 880.0 * t) * exp(-20.0 * t) else 0.0
            val tone2 = if (t in 0.08..0.16) sin(2 * PI * 1100.0 * (t - 0.08)) * exp(-20.0 * (t - 0.08)) else 0.0
            val drone = sin(2 * PI * 220.0 * t) * 0.15 * exp(-5.0 * t)

            val sample = tone1 + tone2 + drone
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return buffer
    }

    /**
     * CHECKMATE SOUND: Triumphant C major chord fanfare.
     */
    private fun generateCheckmateSound(): ShortArray {
        val duration = 0.65
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-7.0 * t)

            val c4 = sin(2 * PI * 261.63 * t)
            val e4 = 0.8 * sin(2 * PI * 329.63 * t)
            val g4 = 0.6 * sin(2 * PI * 392.00 * t)
            val c5 = 0.4 * sin(2 * PI * 523.25 * t)

            val sample = (c4 + e4 + g4 + c5) / 2.8 * envelope
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return buffer
    }

    /**
     * PROMOTION SOUND: Ascending shimmer sparkle sweep.
     */
    private fun generatePromotionSound(): ShortArray {
        val duration = 0.4
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration

            val freq = 400.0 + progress * 800.0
            val shimmer = sin(2 * PI * freq * t) * (1.0 - progress)

            val sparkle = if (Random.nextDouble() > 0.7) {
                (Random.nextDouble() * 2.0 - 1.0) * 0.3 * (1.0 - progress)
            } else 0.0

            val sample = (shimmer + sparkle) * 0.7
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return buffer
    }

    /**
     * ILLEGAL MOVE SOUND: Soft dull wood bump + low rejected buzz.
     */
    private fun generateIllegalSound(): ShortArray {
        val duration = 0.15
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-40.0 * t)

            val thunk = sin(2 * PI * 150.0 * t) * envelope
            val buzz = (Random.nextDouble() * 2.0 - 1.0) * 0.2 * envelope

            val sample = thunk + buzz
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 20000).toInt().toShort()
        }
        return buffer
    }

    /**
     * GAME START: Clean bright chime (C5).
     */
    private fun generateGameStartSound(): ShortArray {
        val duration = 0.3
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = (1.0 - exp(-50.0 * t)) * exp(-10.0 * t)
            val sample = sin(2 * PI * 523.25 * t) * envelope
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 25000).toInt().toShort()
        }
        return buffer
    }

    /**
     * GAME END: Resolving harmonious G-major chord.
     */
    private fun generateGameEndSound(): ShortArray {
        val duration = 0.8
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-5.5 * t)

            val g3 = sin(2 * PI * 196.00 * t)
            val b3 = 0.7 * sin(2 * PI * 246.94 * t)
            val d4 = 0.5 * sin(2 * PI * 293.66 * t)
            val g4 = 0.3 * sin(2 * PI * 392.00 * t)

            val sample = (g3 + b3 + d4 + g4) / 2.5 * envelope
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return buffer
    }

    /**
     * HINT READY: Soft, subtle ascending harmonic chime.
     */
    private fun generateHintReadySound(): ShortArray {
        val duration = 0.18
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-18.0 * t) * (1.0 - exp(-100.0 * t))
            val freq = 880.0 + t * 500.0 // Smooth upward frequency sweep
            val sample = (sin(2 * PI * freq * t) + 0.4 * sin(2 * PI * (freq * 1.5) * t)) * envelope * 0.35
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 22000).toInt().toShort()
        }
        return buffer
    }

    /**
     * CORRECT MOVE: Pleasant positive feedback chord when following assistant/hint move.
     */
    private fun generateCorrectMoveSound(): ShortArray {
        val duration = 0.22
        val samples = (duration * sampleRate).toInt()
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-12.0 * t)
            val sample = (sin(2 * PI * 1046.5 * t) + 0.5 * sin(2 * PI * 1318.5 * t) + 0.3 * sin(2 * PI * 1567.98 * t)) * envelope * 0.45
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 24000).toInt().toShort()
        }
        return buffer
    }

    companion object {
        fun performHaptic(context: Context, type: HapticType) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                } ?: return

                if (!vibrator.hasVibrator()) return

                when (type) {
                    HapticType.LIGHT -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(12)
                        }
                    }
                    HapticType.MEDIUM -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(28, 120))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(28)
                        }
                    }
                    HapticType.HEAVY -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, 180))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(50)
                        }
                    }
                    HapticType.CHECK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0, 35, 45, 35),
                                    intArrayOf(0, 140, 0, 160),
                                    -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 35, 45, 35), -1)
                        }
                    }
                    HapticType.CHECKMATE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0, 50, 60, 80),
                                    intArrayOf(0, 180, 0, 240),
                                    -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 50, 60, 80), -1)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore vibration hardware failures
            }
        }
    }
}
