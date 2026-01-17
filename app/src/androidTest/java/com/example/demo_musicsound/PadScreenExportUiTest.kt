package com.example.demo_musicsound

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager
import com.example.demo_musicsound.ui.screen.PadScreen
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class PadScreenExportUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createPattern_thenExport_showsExportedSnackbar() {
        // We capture the exported file from the callback without touching production code.
        val exportedFileRef = AtomicReference<File?>(null)

        rule.setContent {
            val context = rule.activity
            val sound = SoundManager(context)     // uses RAW resources in your app
            val seq = Sequencer()

            PadScreen(
                sound = sound,
                seq = seq,
                padLabels = emptyMap(),
                onPadSoundPicked = { _, _, _ -> },
                onBeatExported = { file, _bpm ->
                    exportedFileRef.set(file)
                }
            )
        }

        // 1) Make sure we're on Bank A (stable for tags and sound keys)
        rule.onNodeWithTag("bank_A").performClick()

        // 2) Activate 2 steps on 2 different tracks (kick + snare)
        // These tags come from your SequencerGrid: "seq_${pad.soundKey}_$i"
        rule.onNodeWithTag("seq_kick_0").performClick()
        rule.onNodeWithTag("seq_snare_4").performClick()

        // 3) Export
        rule.onNodeWithTag("btn_export").performClick()


        // 4) Type filename in dialog
        rule.onNodeWithTag("field_beat_name").performTextClearance()
        rule.onNodeWithTag("field_beat_name").performTextInput("ui_test_beat")

        // 5) Save
        rule.onNodeWithTag("btn_save_export").performClick()

        // Wait until the export callback receives a file (max 10s)
        rule.waitUntil(timeoutMillis = 10_000) {
            exportedFileRef.get() != null
        }

        rule.waitUntil(timeoutMillis = 10_000) {
            try {
                rule.onNodeWithText("Exported:", substring = true).assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // 7) Validate: callback received a file
        val f = exportedFileRef.get()
        assert(f != null) { "Expected exported file callback to be invoked." }
        assert(f!!.name.contains("ui_test_beat")) { "Expected exported file name to contain ui_test_beat, was=${f!!.name}" }
    }
}