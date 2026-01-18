package com.example.demo_musicsound.UI

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore
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

    // Repo mocked
    private val fakeRepo = mockk<FirebaseCommunityRepository>(relaxed = true)

    private val fakeAuth = mockk<FirebaseAuth>(relaxed = true)
    private val fakeFirestore = mockk<FirebaseFirestore>(relaxed = true)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        vm = AuthViewModel(
            localBeatDao = db.localBeatDao(),
            repo = fakeRepo,
            auth = fakeAuth,
            db = fakeFirestore
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

        // Questi esistono solo in REGISTER: se non ci sono, ignoro
        runCatching { composeRule.onNodeWithTag("confirmField").performTextReplacement("") }
        runCatching { composeRule.onNodeWithTag("firstNameField").performTextReplacement("") }
        runCatching { composeRule.onNodeWithTag("lastNameField").performTextReplacement("") }
        runCatching { composeRule.onNodeWithTag("usernameField").performTextReplacement("") }

        composeRule.waitForIdle()
    }

    private fun typeLogin(email: String?, password: String?) {
        clearAllFields()
        if (email != null) composeRule.onNodeWithTag("emailField").performTextReplacement(email)
        if (password != null) composeRule.onNodeWithTag("passwordField").performTextReplacement(password)
        composeRule.waitForIdle()
    }

    private fun typeRegister(
        email: String?,
        password: String?,
        confirm: String?,
        firstName: String?,
        lastName: String?,
        username: String?
    ) {
        clearAllFields()

        if (email != null) composeRule.onNodeWithTag("emailField").performTextReplacement(email)
        if (password != null) composeRule.onNodeWithTag("passwordField").performTextReplacement(password)
        if (confirm != null) composeRule.onNodeWithTag("confirmField").performTextReplacement(confirm)

        if (firstName != null) composeRule.onNodeWithTag("firstNameField").performTextReplacement(firstName)
        if (lastName != null) composeRule.onNodeWithTag("lastNameField").performTextReplacement(lastName)
        if (username != null) composeRule.onNodeWithTag("usernameField").performTextReplacement(username)

        composeRule.waitForIdle()
    }

    private fun submit() {
        composeRule.onNodeWithTag("submitButton")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun assertErrorContains(expected: String) {
        composeRule.onNodeWithTag("errorChip")
            .assertIsDisplayed()
            .assertTextContains(expected)
    }

    // ------------------------------------------------------------
    // LOGIN - invalid cases (uguali a prima)
    // ------------------------------------------------------------

    @Test
    fun login_emptyEmailAndPassword_showsError() {
        goToLoginTab()
        typeLogin(email = "", password = "")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_emailOnly_showsError() {
        goToLoginTab()
        typeLogin(email = "test@mail.com", password = "")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_passwordOnly_showsError() {
        goToLoginTab()
        typeLogin(email = "", password = "123456")
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun login_blankSpaces_showsError() {
        goToLoginTab()
        typeLogin(email = "   ", password = "   ")
        submit()
        assertErrorContains("Email and password required.")
    }

    // ------------------------------------------------------------
    // REGISTER - invalid cases (aggiornati)
    // ------------------------------------------------------------

    @Test
    fun register_emptyFields_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "",
            password = "",
            confirm = "",
            firstName = "",
            lastName = "",
            username = ""
        )
        submit()
        assertErrorContains("Email and password required.")
    }

    @Test
    fun register_passwordTooShort_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123",
            confirm = "123",
            firstName = "Mario",
            lastName = "Rossi",
            username = "mario.rossi"
        )
        submit()
        assertErrorContains("Password must be at least 6 characters.")
    }

    @Test
    fun register_passwordsDoNotMatch_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "abcdef",
            firstName = "Mario",
            lastName = "Rossi",
            username = "mario.rossi"
        )
        submit()
        assertErrorContains("Passwords do not match.")
    }

    @Test
    fun register_missingConfirm_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "",
            firstName = "Mario",
            lastName = "Rossi",
            username = "mario.rossi"
        )
        submit()
        assertErrorContains("Passwords do not match.")
    }

    @Test
    fun register_missingFirstNameLastNameUsername_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "",
            lastName = "",
            username = ""
        )
        submit()
        assertErrorContains("First name, last name and username are required.")
    }

    @Test
    fun register_missingFirstNameOnly_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "",
            lastName = "Rossi",
            username = "mario.rossi"
        )
        submit()
        assertErrorContains("First name, last name and username are required.")
    }

    @Test
    fun register_missingLastNameOnly_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "Mario",
            lastName = "",
            username = "mario.rossi"
        )
        submit()
        assertErrorContains("First name, last name and username are required.")
    }

    @Test
    fun register_missingUsernameOnly_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "Mario",
            lastName = "Rossi",
            username = ""
        )
        submit()
        assertErrorContains("First name, last name and username are required.")
    }

    @Test
    fun register_usernameInvalidChars_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "Mario",
            lastName = "Rossi",
            username = "mario rossi!" // spazio + !
        )
        submit()
        assertErrorContains("Username must be 3-20 chars (letters/numbers/._).")
    }

    @Test
    fun register_usernameTooShort_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "Mario",
            lastName = "Rossi",
            username = "ab" // < 3
        )
        submit()
        assertErrorContains("Username must be 3-20 chars (letters/numbers/._).")
    }

    @Test
    fun register_usernameTooLong_showsError() {
        goToRegisterTab()
        typeRegister(
            email = "test@mail.com",
            password = "123456",
            confirm = "123456",
            firstName = "Mario",
            lastName = "Rossi",
            username = "this_username_is_way_too_long_123" // > 20
        )
        submit()
        assertErrorContains("Username must be 3-20 chars (letters/numbers/._).")
    }
}