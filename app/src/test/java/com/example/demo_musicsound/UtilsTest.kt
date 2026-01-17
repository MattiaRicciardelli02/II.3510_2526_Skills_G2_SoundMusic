package com.example.demo_musicsound

import com.example.demo_musicsound.community.CommunityBeat
import com.example.demo_musicsound.data.LocalBeatEntity
import org.junit.Assert.*
import org.junit.Test
import com.example.demo_musicsound.ui.screen.formatDurationMs

class UtilsTest {

    @Test
    fun localBeatEntity_shouldStoreAllFieldsCorrectly() {
        val entity = LocalBeatEntity(
            id = "uuid-123",
            ownerId = "guest",
            title = "My Beat",
            filePath = "/storage/emulated/0/Android/data/.../exports/my_beat.wav",
            createdAt = 123456789L
        )

        assertEquals("uuid-123", entity.id)
        assertEquals("guest", entity.ownerId)
        assertEquals("My Beat", entity.title)
        assertEquals("/storage/emulated/0/Android/data/.../exports/my_beat.wav", entity.filePath)
        assertEquals(123456789L, entity.createdAt)
    }

    @Test
    fun localBeatEntity_copyShouldChangeOnlyRequestedFields() {
        val original = LocalBeatEntity(
            id = "uuid-123",
            ownerId = "guest",
            title = "Old Title",
            filePath = "/path/old.wav",
            createdAt = 10L
        )

        val updated = original.copy(title = "New Title")

        assertEquals("uuid-123", updated.id)
        assertEquals("guest", updated.ownerId)
        assertEquals("New Title", updated.title)
        assertEquals("/path/old.wav", updated.filePath)
        assertEquals(10L, updated.createdAt)
    }

    @Test
    fun communityBeat_defaultsShouldBeEmpty() {
        val b = CommunityBeat()

        assertEquals("", b.id)
        assertEquals("", b.ownerId)
        assertEquals("", b.title)
        assertEquals("", b.audioPath)
        assertEquals("", b.coverPath)
        assertEquals(0L, b.createdAt)

        assertEquals("", b.description)
        assertEquals("", b.spotifyTrackId)
        assertEquals("", b.spotifyTrackName)
        assertEquals("", b.spotifyTrackArtist)
        assertEquals("", b.spotifyUrl)
    }

    @Test
    fun communityBeat_shouldKeepProvidedValues() {
        val b = CommunityBeat(
            id = "beat1",
            ownerId = "uid123",
            title = "Trap Beat",
            audioPath = "library/uid123/beat1.wav",
            createdAt = 99L,
            description = "test"
        )

        assertEquals("beat1", b.id)
        assertEquals("uid123", b.ownerId)
        assertEquals("Trap Beat", b.title)
        assertEquals("library/uid123/beat1.wav", b.audioPath)
        assertEquals(99L, b.createdAt)
        assertEquals("test", b.description)
    }

    @Test
    fun formatDurationMs_null_returnsDash() {
        val result = formatDurationMs(null)
        assertEquals("—", result)
    }

    @Test
    fun formatDurationMs_secondsOnly() {
        val result = formatDurationMs(5_000)
        assertEquals("00:05", result)
    }

    @Test
    fun formatDurationMs_minutesAndSeconds() {
        val result = formatDurationMs(65_000)
        assertEquals("01:05", result)
    }

    @Test
    fun formatDurationMs_overOneHour_formatsAsTotalMinutesAndSeconds() {
        val result = formatDurationMs(3_661_000)
        assertEquals("61:01", result)
    }
}