package com.example.demo_musicsound.Audio

import android.content.Context
import android.media.MediaScannerConnection
import androidx.annotation.RawRes
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * OfflineExporter (pro)
 * - Loads 16-bit PCM WAV from res/raw (mono or stereo, any sample-rate)
 * - Downmixes to MONO, resamples to 44.1 kHz
 * - Mixes one bar (N steps) into a single WAV file
 */
object OfflineExporter {

    /** Raw sample (mono 16-bit) with its source SR */
    data class Sample(val data: ShortArray, val sr: Int)

    /** Track to mix: name just for reference, step pattern, and sample */
    data class TrackMix(
        val resName: String,
        val pattern: List<Boolean>,
        val sample: Sample
    )

    /** Load 16-bit PCM WAV from res/raw (supports mono/stereo). Returns MONO data. */
    fun loadWavPCM16(context: Context, @RawRes resId: Int): Sample {
        context.resources.openRawResource(resId).use { input ->
            val hdr = ByteArray(44)
            require(input.read(hdr) == 44) { "Invalid WAV header" }

            fun leInt(off: Int) = ByteBuffer.wrap(hdr, off, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int
            fun leShort(off: Int) = ByteBuffer.wrap(hdr, off, 2)
                .order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

            val audioFmt     = leShort(20)      // 1 = PCM
            val numChannels  = leShort(22)      // 1 or 2
            val sampleRate   = leInt(24)        // e.g. 44100
            val bitsPerSamp  = leShort(34)      // 16

            require(audioFmt == 1)    { "WAV not PCM" }
            require(bitsPerSamp == 16){ "Only 16-bit supported" }
            require(numChannels in 1..2) { "Channels must be 1 or 2" }

            val payload = input.readBytes()
            val shorts = ShortArray(payload.size / 2)
            ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            // Downmix to mono if stereo
            val mono: ShortArray = if (numChannels == 1) {
                shorts
            } else {
                val frames = shorts.size / 2
                val out = ShortArray(frames)
                var si = 0
                for (i in 0 until frames) {
                    val l = shorts[si++].toInt()
                    val r = shorts[si++].toInt()
                    out[i] = ((l + r) / 2).coerceIn(-32768, 32767).toShort()
                }
                out
            }

            return Sample(mono, sampleRate)
        }
    }

    /** Simple linear resampler (MONO 16-bit) to dstSr */
    private fun resampleLinear(src: ShortArray, srcSr: Int, dstSr: Int): ShortArray {
        if (srcSr == dstSr) return src
        val ratio = dstSr.toDouble() / srcSr
        val dstLen = max(1, floor(src.size * ratio).toInt())
        val out = ShortArray(dstLen)
        for (i in 0 until dstLen) {
            val x = i / ratio
            val x0 = floor(x).toInt().coerceIn(0, src.lastIndex)
            val x1 = min(x0 + 1, src.lastIndex)
            val t = (x - floor(x)).toFloat()
            val s = src[x0] * (1f - t) + src[x1] * t
            out[i] = s.roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Write a mono 16-bit PCM WAV @ dstSr */
    private fun writeWavPCM16(file: File, pcm: ShortArray, dstSr: Int) {
        val bytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { bytes.putShort(it) }

        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { out ->
            fun w(s: String) = out.writeBytes(s)
            fun iLE(v: Int) = out.write(byteArrayOf(
                (v).toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()
            ))
            fun sLE(v: Int) = out.write(byteArrayOf((v).toByte(), (v shr 8).toByte()))

            val dataSize = bytes.capacity()
            val riffSize = 36 + dataSize
            val byteRate = dstSr * 2 // mono 16-bit

            w("RIFF"); iLE(riffSize); w("WAVE")
            w("fmt "); iLE(16); sLE(1); sLE(1); iLE(dstSr); iLE(byteRate); sLE(2); sLE(16)
            w("data"); iLE(dataSize); out.write(bytes.array())
        }
    }

    /**
     * Mix one bar to WAV file (mono 44.1 kHz).
     * - steps: number of sequencer steps (usually 16)
     * - bpm: tempo
     */
    fun exportBeatToWav(
        context: Context,
        bpm: Int,
        steps: Int = 16,
        dstSr: Int = 44100,
        tracks: List<TrackMix>
    ): File {
        val stepSec = (60.0 / bpm) / 4.0
        val totalSamples = (steps * stepSec * dstSr).toInt()

        // Sum in 32-bit then clip to 16-bit
        val acc = IntArray(totalSamples)
        val clip = 32767

        tracks.forEach { t ->
            val mono = if (t.sample.sr != dstSr)
                resampleLinear(t.sample.data, t.sample.sr, dstSr)
            else t.sample.data

            val hitLen = mono.size
            for (i in 0 until steps) {
                if (!t.pattern.getOrNull(i).orFalse()) continue
                val start = (i * stepSec * dstSr).toInt()
                val maxCopy = min(totalSamples - start, hitLen)
                var k = 0
                while (k < maxCopy) {
                    acc[start + k] += mono[k].toInt()
                    k++
                }
            }
        }

        val pcm = ShortArray(totalSamples) { idx -> acc[idx].coerceIn(-clip, clip).toShort() }

        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val out = File(outDir, "beat_${System.currentTimeMillis()}.wav")
        writeWavPCM16(out, pcm, dstSr)
        MediaScannerConnection.scanFile(context, arrayOf(out.absolutePath), arrayOf("audio/wav"), null)
        return out
    }

    private fun Boolean?.orFalse() = this == true
}