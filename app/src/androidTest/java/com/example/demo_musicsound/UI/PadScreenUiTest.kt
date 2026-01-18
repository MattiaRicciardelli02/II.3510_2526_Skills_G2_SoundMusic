package com.example.demo_musicsound.UI

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.demo_musicsound.MainActivity
import org.junit.Rule
import org.junit.Test

class PadScreenUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_showsBottomNavigationItems() {
        // Verify bottom navigation labels are visible
        rule.onNodeWithText("Pad").assertIsDisplayed()
        rule.onNodeWithText("Record").assertIsDisplayed()
        rule.onNodeWithText("Community").assertIsDisplayed()
    }
}