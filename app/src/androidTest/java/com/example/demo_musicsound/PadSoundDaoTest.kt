package com.example.demo_musicsound.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PadSoundDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PadSoundDao

    @Before
    fun setup() {
        // Create an in-memory Room database for testing
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.padSoundDao()
    }

    @After
    fun tearDown() {
        // Close database after each test
        db.close()
    }

    @Test
    fun upsert_insertsPadSound() = runBlocking {
        // Insert a pad sound
        dao.upsert(
            PadSoundEntity(
                slotId = "A0",
                soundKey = "kick",
                label = "Kick",
                uri = null
            )
        )

        // Verify that the pad was inserted
        val allPads = dao.getAll()
        assertEquals(1, allPads.size)
        assertEquals("A0", allPads.first().slotId)
    }

    @Test
    fun upsert_sameSlotId_replacesExistingPad() = runBlocking {
        // Insert initial pad sound
        dao.upsert(
            PadSoundEntity(
                slotId = "A0",
                soundKey = "kick",
                label = "Kick",
                uri = null
            )
        )

        // Insert another pad with the same slotId
        dao.upsert(
            PadSoundEntity(
                slotId = "A0",
                soundKey = "kick",
                label = "Custom Kick",
                uri = "content://custom/kick.wav"
            )
        )

        // Verify that the pad was replaced
        val allPads = dao.getAll()
        assertEquals(1, allPads.size)
        assertEquals("Custom Kick", allPads.first().label)
    }

    @Test
    fun observeAll_emitsUpdatedList() = runBlocking {
        // Observe initial empty state
        val initial = dao.observeAll().first()
        assertTrue(initial.isEmpty())

        // Insert a pad sound
        dao.upsert(
            PadSoundEntity(
                slotId = "A1",
                soundKey = "snare",
                label = "Snare",
                uri = null
            )
        )

        // Verify that the Flow emits the updated list
        val updated = dao.observeAll().first()
        assertEquals(1, updated.size)
        assertEquals("A1", updated.first().slotId)
    }
}