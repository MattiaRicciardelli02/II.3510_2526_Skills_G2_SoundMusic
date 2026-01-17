package com.example.demo_musicsound.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalBeatDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LocalBeatDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // OK only in tests
            .build()
        dao = db.localBeatDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun upsertAndGetByOwner_returnsInsertedBeatsInDescOrder() = kotlinx.coroutines.runBlocking {
        // Arrange
        dao.upsert(
            LocalBeatEntity(
                id = "b1",
                ownerId = "guest",
                title = "First",
                filePath = "/tmp/first.wav",
                createdAt = 100L
            )
        )
        dao.upsert(
            LocalBeatEntity(
                id = "b2",
                ownerId = "guest",
                title = "Second",
                filePath = "/tmp/second.wav",
                createdAt = 200L
            )
        )

        // Act
        val rows = dao.getByOwner("guest")

        // Assert
        assertEquals(2, rows.size)
        assertEquals("b2", rows[0].id) // newest first
        assertEquals("b1", rows[1].id)
    }

    @Test
    fun moveGuestToOwner_movesAllGuestRowsToGivenUid() = kotlinx.coroutines.runBlocking {
        // Arrange
        dao.upsert(
            LocalBeatEntity(
                id = "g1",
                ownerId = "guest",
                title = "GuestBeat1",
                filePath = "/tmp/g1.wav",
                createdAt = 10L
            )
        )
        dao.upsert(
            LocalBeatEntity(
                id = "g2",
                ownerId = "guest",
                title = "GuestBeat2",
                filePath = "/tmp/g2.wav",
                createdAt = 20L
            )
        )

        // Act
        dao.moveGuestToOwner("UID_123")

        // Assert
        val guestRows = dao.getByOwner("guest")
        assertTrue(guestRows.isEmpty())

        val uidRows = dao.getByOwner("UID_123")
        assertEquals(2, uidRows.size)
        assertEquals(setOf("g1", "g2"), uidRows.map { it.id }.toSet())
    }

    @Test
    fun moveGuestToOwner_movesAllGuestBeats() = kotlinx.coroutines.runBlocking {
        // Insert two guest beats
        dao.upsert(LocalBeatEntity("beat1","guest","Guest Beat 1","/tmp/beat1.wav",1L))
        dao.upsert(LocalBeatEntity("beat2","guest","Guest Beat 2","/tmp/beat2.wav",2L))

        // Insert a beat that already belongs to a user
        dao.upsert(LocalBeatEntity("beat3","user123","User Beat","/tmp/beat3.wav",3L))

        // Act: move guest beats to user123 (returns Unit)
        dao.moveGuestToOwner("user123")

        // Assert: now user has 3 beats total
        val userBeats = dao.getByOwner("user123")
        assertEquals(3, userBeats.size)

        // Assert: no guest beats remain
        val guestBeats = dao.getGuest()
        assertTrue(guestBeats.isEmpty())
    }
}