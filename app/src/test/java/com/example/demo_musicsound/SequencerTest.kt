package com.example.demo_musicsound

import com.example.demo_musicsound.Audio.Sequencer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SequencerTest {

    private lateinit var seq: Sequencer

    @Before
    fun setup() {
        seq = Sequencer()
    }

    @Test
    fun toggle_onEmptyTrack_setsStepToTrue() {
        // We toggle a step on a new track and expect it to become active.
        val key = "kick"

        val before = seq.pattern(key).toList()
        assertTrue("Pattern should not be empty", before.isNotEmpty())
        assertFalse("Step 0 should start as false", before[0])

        seq.toggle(key, 0)

        val after = seq.pattern(key).toList()
        assertTrue("Step 0 should become true after toggle", after[0])
    }

    @Test
    fun toggle_twiceOnSameStep_returnsToFalse() {
        // Toggling the same step twice should restore the original state.
        val key = "snare"

        seq.toggle(key, 3)
        assertTrue(seq.pattern(key)[3])

        seq.toggle(key, 3)
        assertFalse(seq.pattern(key)[3])
    }

    @Test
    fun clear_resetsAllStepsForGivenKeys() {
        // We activate some steps across multiple tracks, then clear them.
        val keys = listOf("kick", "snare")

        seq.toggle("kick", 0)
        seq.toggle("kick", 1)
        seq.toggle("snare", 2)

        // Sanity check: some steps are active
        assertTrue(seq.pattern("kick")[0])
        assertTrue(seq.pattern("kick")[1])
        assertTrue(seq.pattern("snare")[2])

        seq.clear(keys)

        // After clear, all steps must be false
        keys.forEach { k ->
            val p = seq.pattern(k)
            assertEquals(true, p.all { step -> step == false })
        }
    }
}