package com.example.demo_musicsound.Audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import androidx.annotation.RawRes

class SoundManager(private val context: Context) {

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setAudioAttributes(attrs)
        .setMaxStreams(24)
        .build()

    private val sounds = mutableMapOf<String, Int>() // key -> sampleId
    private val loaded = mutableSetOf<Int>()         // sampleId pronti

    init {
        pool.setOnLoadCompleteListener { _, sid, status ->
            if (status == 0) loaded.add(sid)
            else Log.e("SoundManager", "Load failed sid=$sid status=$status")
        }
    }

    // -------------------------------
    // RAW preload (come prima)
    // -------------------------------
    fun preload(key: String, @RawRes resId: Int) {
        if (key !in sounds) {
            val sid = pool.load(context, resId, 1)
            sounds[key] = sid
        }
    }

    // -------------------------------
    // NEW: load from URI and replace
    // -------------------------------
    /**
     * Carica un suono da Uri (content:// o file://) e lo associa alla key.
     * Se esiste già un suono con la stessa key, lo sostituisce e scarica il precedente.
     */
    fun replaceFromUri(key: String, uri: Uri): Boolean {
        return try {
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            if (afd == null) {
                Log.e("SoundManager", "openAssetFileDescriptor returned null for $uri")
                return false
            }

            afd.use {
                val oldSid = sounds[key]

                // carica il nuovo (async)
                val newSid = pool.load(it, 1)
                sounds[key] = newSid

                // scarica il vecchio
                if (oldSid != null) {
                    loaded.remove(oldSid)
                    pool.unload(oldSid)
                }
            }

            true
        } catch (t: Throwable) {
            Log.e("SoundManager", "replaceFromUri failed for $uri", t)
            false
        }
    }

    /** Variante comoda: passa una String uri salvata nel DB. */
    fun replaceFromUriString(key: String, uriString: String): Boolean {
        return replaceFromUri(key, Uri.parse(uriString))
    }

    /** Alias (se ti piace tenerlo) */
    fun loadOrReplaceFromUri(key: String, uri: Uri): Boolean {
        return replaceFromUri(key, uri)
    }

    // -------------------------------
    // play (come prima)
    // -------------------------------
    fun play(key: String, gain: Float = 1f, rate: Float = 1f) {
        val sid = sounds[key] ?: return
        if (sid !in loaded) return
        pool.play(sid, gain, gain, 1, 0, rate)
    }

    fun release() = pool.release()
}