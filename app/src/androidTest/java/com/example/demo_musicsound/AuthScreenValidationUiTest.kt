package com.example.demo_musicsound

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.demo_musicsound.auth.AuthViewModel
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.example.demo_musicsound.data.AppDatabase
import com.example.demo_musicsound.ui.screen.AuthScreen
import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenValidationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase
    private lateinit var vm: AuthViewModel

    // We keep repo mocked: we only test invalid inputs (no Firebase call should happen).
    private val fakeRepo = mockk<FirebaseCommunityRepository>(relaxed = true)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        vm = AuthViewModel(
            localBeatDao = db.localBeatDao(),
            repo = fakeRepo,
            auth = FirebaseAuth.getInstance()
        )

        composeRule.setContent {
            AuthScreen(
                vm = vm,
                startOnRegister = false,
                onDone = {}
            )
        }

        composeRule.waitForIdle()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun goToLoginTab() {
        composeRule.onNodeWithTag("tabLogin")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun goToRegisterTab() {
        composeRule.onNodeWithTag("tabRegister")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun clearAllFields() {
        composeRule.onNodeWithTag("emailField").assertIsDisplayed().performTextReplacement("")
        composeRule.onNodeWithTag("passwordField").assertIsDisplayed().performTextReplacement("")
        // confirm exists only in register mode
        runCatching {
            composeRule.onNodeWithTag("confirmField").assertIsDisplayed().performTextReplacement("")
        }
        composeRule.waitForIdle()
    }

    private fun typeLogin(email: String?, password: String?) {
        clearAllFields()
        if (email != null) composeRule.onNodeWithTag("emailField").performTextReplacement(email)
        if (password != null) composeRule.onNodeWithTag("passwordField").performTextReplacement(password)
        composeRule.waitForIdle()
    }

    private fun typeRegister(email: String?, password: String?, confirm: String?) {
        clearAllFields()
        if (email != null) composeRule.onNodeWithTag("emailField").performTextReplacement(email)
        if (password != null) composeRule.onNodeWithTag("passwordField").performTextReplacement(password)
        if (confirm != null) composeRule.onNodeWithTag("confirmField").performTextReplacement(confirm)
        composeRule.waitForIdle()
    }

    private fun submit() {
        composeRule.onNodeWithTag("submitButton")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun assertErrorContains(expected: String) {
        // Error is shown in the chip with tag "errorChip"
        composeRule.onNodeWithTag("errorChip")
            .assertIsDisplayed()
            .assertTextContains(expected)
    }

    // ------------------------------------------------------------
    // LOGIN - invalid cases
    // ------------------------------------------------------------

    @Test
    fun login_emptyEmailAndPassword_showsError() {
        // Empty fields should be rejected before hitting Firebase.
        goToLoginTab()
        typeLogin(email = "", password = "")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_emailOnly_showsError() {
        // Email provided, missing password -> validation error.
        goToLoginTab()
        typeLogin(email = "test@mail.com", password = "")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_passwordOnly_showsError() {
        // Password provided, missing email -> validation error.
        goToLoginTab()
        typeLogin(email = "", password = "123456")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_blankSpaces_showsError() {
        // Whitespaces should be treated as blank.
        goToLoginTab()
        typeLogin(email = "   ", password = "   ")
        submit()
        assertErrorContains("Email and password required.")
    }

    // ------------------------------------------------------------
    // REGISTER - invalid cases
    // ------------------------------------------------------------

    @Test
    fun register_emptyFields_showsError() {
        // Register requires email + password.
        goToRegisterTab()
        typeRegister(email = "", password = "", confirm = "")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun register_passwordTooShort_showsError() {
        // Password must be >= 6 chars (ViewModel rule).
        goToRegisterTab()
        typeRegister(email = "test@mail.com", password = "123", confirm = "123")
        submit()
        assertErrorContains("Password must be at least 6 characters.")
    }

    @Test
    fun register_passwordsDoNotMatch_showsError() {
        // Confirm mismatch -> error.
        goToRegisterTab()
        typeRegister(email = "test@mail.com", password = "123456", confirm = "abcdef")
        submit()
        assertErrorContains("Passwords do not match.")
    }

    @Test
    fun register_missingConfirm_showsError() {
        // Confirm empty -> mismatch.
        goToRegisterTab()
        typeRegister(email = "test@mail.com", password = "123456", confirm = "")
        submit()
        assertErrorContains("Passwords do not match.")
    }
}