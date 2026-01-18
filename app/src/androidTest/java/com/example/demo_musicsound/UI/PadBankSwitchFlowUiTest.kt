package com.example.demo_musicsound.UI

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.demo_musicsound.Audio.Sequencer
import com.example.demo_musicsound.Audio.SoundManager
import com.example.demo_musicsound.ui.screen.PadScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PadBankSwitchFlowUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchBank_toggleStep_switchBack() {
        val seq = Sequencer()
        lateinit var sound: SoundManager

        rule.setContent {
            sound = SoundManager(rule.activity)
            PadScreen(sound = sound, seq = seq)
        }

        // Wait for initial composition
        rule.waitForIdle()

        // ---- Switch to Bank B ----
        rule.onNodeWithTag("bank_B", useUnmergedTree = true)
            .assertExists()
            .performClick()

        rule.waitForIdle()

        // ---- Toggle a step in Bank B ----
        rule.onNodeWithTag("seq_rim_0")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        rule.waitForIdle()

        // ---- Switch back to Bank A ----
        rule.onNodeWithTag("bank_A", useUnmergedTree = true)
            .assertExists()
            .performClick()

        rule.waitForIdle()

        // ---- Toggle kick step ----
        rule.onNodeWithTag("seq_kick_0")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // ---- Final sanity check ----
        // Verify the toggled step is still present
        rule.onNodeWithTag("seq_kick_0", useUnmergedTree = true)
            .assertExists()
    }
}