package com.example.demo_musicsound

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.demo_musicsound.Audio.OfflineExporter
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.data.AppDatabase
import com.example.demo_musicsound.data.LocalBeatDao
import com.example.demo_musicsound.data.LocalBeatEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BeatExportAndSaveTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var beatDao: LocalBeatDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // In-memory DB for fast, isolated tests
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // OK in tests
            .build()

        beatDao = db.localBeatDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun sequencer_exportWav_and_saveToRoom() = runBlocking {
        // ---------------------------
        // 1) Arrange: build a small pattern with Sequencer
        // ---------------------------
        val seq = Sequencer()

        // We use your raw sample keys. These MUST exist in res/raw.
        val key = "kick"
        seq.ensureAll(listOf(key))

        // Make a simple "4 on the floor" pattern on 16 steps
        // (0, 4, 8, 12)
        seq.toggle(key, 0)
        seq.toggle(key, 4)
        seq.toggle(key, 8)
        seq.toggle(key, 12)

        val pattern = seq.pattern(key).toList()
        assertTrue("Pattern should have active steps", pattern.count { it } > 0)

        val steps = pattern.size

        // Load PCM from res/raw using your OfflineExporter helper
        val resId = context.resources.getIdentifier(key, "raw", context.packageName)
        assertTrue("raw/$key must exist", resId != 0)

        val track = OfflineExporter.TrackMix(
            resName = key,
            pattern = pattern,
            sample = OfflineExporter.loadWavPCM16(context, resId)
        )

        // ---------------------------
        // 2) Act: export to WAV on device filesystem
        // ---------------------------
        val outFile: File = OfflineExporter.exportBeatToWav(
            context = context,
            bpm = 120,
            steps = steps,
            dstSr = 44100,
            tracks = listOf(track)
        )

        // ---------------------------
        // 3) Assert: file exists and is non-empty
        // ---------------------------
        assertTrue("Exported file should exist", outFile.exists())
        assertTrue("Exported file should not be empty", outFile.length() > 44) // WAV header is 44 bytes

        // ---------------------------
        // 4) Act: save metadata in Room
        // ---------------------------
        val ownerId = "guest"
        val beatId = "test_beat_1"

        beatDao.upsert(
            LocalBeatEntity(
                id = beatId,
                ownerId = ownerId,
                title = "Test Beat",
                filePath = outFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )

        // ---------------------------
        // 5) Assert: Room contains the beat
        // ---------------------------
        val rows = beatDao.getByOwner(ownerId)
        assertTrue("DB should contain at least 1 beat", rows.isNotEmpty())

        val saved = rows.first { it.id == beatId }
        assertEquals("Test Beat", saved.title)
        assertEquals(outFile.absolutePath, saved.filePath)

        // Extra: the saved path still points to a real file
        assertTrue(File(saved.filePath).exists())
    }

    @Test
    fun sequencer_exportWav_twoTracks_and_saveToRoom() = runBlocking {
        // 1) Arrange: two tracks on the sequencer
        val seq = Sequencer()

        val kick = "kick"
        val snare = "snare"
        seq.ensureAll(listOf(kick, snare))

        // Kick: 0,4,8,12
        listOf(0, 4, 8, 12).forEach { seq.toggle(kick, it) }

        // Snare: 4,12 (classic backbeat, but depends on your step grid)
        listOf(4, 12).forEach { seq.toggle(snare, it) }

        val kickPattern = seq.pattern(kick).toList()
        val snarePattern = seq.pattern(snare).toList()
        val steps = kickPattern.size

        // Sanity checks
        require(steps == snarePattern.size) { "Patterns must have same length" }
        assertTrue(kickPattern.any { it })
        assertTrue(snarePattern.any { it })

        fun resIdOf(name: String): Int =
            context.resources.getIdentifier(name, "raw", context.packageName)

        val kickRes = resIdOf(kick)
        val snareRes = resIdOf(snare)

        assertTrue("raw/$kick must exist", kickRes != 0)
        assertTrue("raw/$snare must exist", snareRes != 0)

        val kickTrack = OfflineExporter.TrackMix(
            resName = kick,
            pattern = kickPattern,
            sample = OfflineExporter.loadWavPCM16(context, kickRes)
        )

        val snareTrack = OfflineExporter.TrackMix(
            resName = snare,
            pattern = snarePattern,
            sample = OfflineExporter.loadWavPCM16(context, snareRes)
        )

        // 2) Act: export mixed beat
        val outFile = OfflineExporter.exportBeatToWav(
            context = context,
            bpm = 120,
            steps = steps,
            dstSr = 44100,
            tracks = listOf(kickTrack, snareTrack)
        )

        // Assert: duration is approximately correct
        val durationSec = wavDurationSeconds(outFile)

        // Expected duration: 16 steps @ 120 BPM ≈ 2.0 seconds
        val expected = 2.0
        val tolerance = 0.15 // allow small encoder rounding errors

        assertTrue(
            "Expected duration ~${expected}s, but was ${durationSec}s",
            kotlin.math.abs(durationSec - expected) <= tolerance
        )

        // 3) Assert: file exists and is non-empty
        assertTrue(outFile.exists())
        assertTrue(outFile.length() > 44)

        // 4) Save to Room
        val ownerId = "guest"
        val beatId = "test_beat_2tracks"

        beatDao.upsert(
            LocalBeatEntity(
                id = beatId,
                ownerId = ownerId,
                title = "Test Beat (2 tracks)",
                filePath = outFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )

        // 5) Assert: DB contains the saved beat
        val rows = beatDao.getByOwner(ownerId)
        val saved = rows.first { it.id == beatId }

        assertEquals("Test Beat (2 tracks)", saved.title)
        assertEquals(outFile.absolutePath, saved.filePath)
        assertTrue(File(saved.filePath).exists())
    }
}

private fun wavDurationSeconds(file: File): Double {
    val mmr = android.media.MediaMetadataRetriever()
    mmr.setDataSource(file.absolutePath)
    val durationMs =
        mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLong() ?: 0L
    mmr.release()
    return durationMs / 1000.0
}