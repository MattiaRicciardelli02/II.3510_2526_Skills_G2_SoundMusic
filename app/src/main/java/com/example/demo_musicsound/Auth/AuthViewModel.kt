package com.example.demo_musicsound.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.example.demo_musicsound.data.LocalBeatDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

data class AuthUiState(
    val loading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    // ✅ NEW (register fields)
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",

    val message: String? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val uid: String? = null
)

class AuthViewModel(
    private val localBeatDao: LocalBeatDao,
    private val repo: FirebaseCommunityRepository?, // Optional: used to upload guest beats to Firebase library
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(
        AuthUiState(
            isLoggedIn = auth.currentUser != null,
            uid = auth.currentUser?.uid,
            email = auth.currentUser?.email ?: ""
        )
    )
    val ui: StateFlow<AuthUiState> = _ui

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        val u = a.currentUser
        _ui.value = _ui.value.copy(
            isLoggedIn = u != null,
            uid = u?.uid,
            email = u?.email ?: "" // ✅ così MainActivity può usare authState.email sempre
        )
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    fun setEmail(v: String) {
        _ui.value = _ui.value.copy(email = v, error = null, message = null)
    }

    fun setPassword(v: String) {
        _ui.value = _ui.value.copy(password = v, error = null, message = null)
    }

    fun setConfirmPassword(v: String) {
        _ui.value = _ui.value.copy(confirmPassword = v, error = null, message = null)
    }

    // ✅ NEW setters
    fun setFirstName(v: String) {
        _ui.value = _ui.value.copy(firstName = v, error = null, message = null)
    }

    fun setLastName(v: String) {
        _ui.value = _ui.value.copy(lastName = v, error = null, message = null)
    }

    fun setUsername(v: String) {
        _ui.value = _ui.value.copy(username = v, error = null, message = null)
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null, error = null)
    }

    fun login() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password

        if (email.isBlank() || pass.isBlank()) {
            _ui.value = _ui.value.copy(error = "Email and password required.")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, message = null)
            try {
                auth.signInWithEmailAndPassword(email, pass).await()

                val uid = auth.currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    migrateGuestBeatsToUser(uid)
                }

                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Logged in.",
                    password = "",
                    confirmPassword = ""
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = t.message ?: "Login failed."
                )
            }
        }
    }

    fun register() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password
        val confirm = _ui.value.confirmPassword

        val firstName = _ui.value.firstName.trim()
        val lastName = _ui.value.lastName.trim()
        val username = _ui.value.username.trim()

        if (email.isBlank() || pass.isBlank()) {
            _ui.value = _ui.value.copy(error = "Email and password required.")
            return
        }
        if (pass.length < 6) {
            _ui.value = _ui.value.copy(error = "Password must be at least 6 characters.")
            return
        }
        if (pass != confirm) {
            _ui.value = _ui.value.copy(error = "Passwords do not match.")
            return
        }

        // ✅ NEW validation (minima)
        if (firstName.isBlank() || lastName.isBlank() || username.isBlank()) {
            _ui.value = _ui.value.copy(error = "First name, last name and username are required.")
            return
        }
        // username: semplice regola “safe”
        val usernameOk = Regex("^[a-zA-Z0-9._]{3,20}$").matches(username)
        if (!usernameOk) {
            _ui.value = _ui.value.copy(error = "Username must be 3-20 chars (letters/numbers/._).")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, message = null)
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()

                val uid = auth.currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    // ✅ Save profile in Firestore: users/{uid}
                    saveUserProfile(
                        uid = uid,
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        username = username
                    )

                    migrateGuestBeatsToUser(uid)
                }

                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Account created.",
                    password = "",
                    confirmPassword = ""
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = t.message ?: "Registration failed."
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
        // reset “session-only” fields
        _ui.value = _ui.value.copy(
            message = "Logged out.",
            password = "",
            confirmPassword = ""
        )
    }

    private suspend fun saveUserProfile(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        username: String
    ) {
        // NB: qui faccio un set() merge per non distruggere eventuali altri campi.
        val data = hashMapOf(
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username,
            "displayName" to "$firstName $lastName".trim(),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    private suspend fun migrateGuestBeatsToUser(uid: String) {
        withContext(Dispatchers.IO) {
            // 1) Read guest beats before changing ownerId
            val guestRows = localBeatDao.getGuest()

            // 2) Move guest beats to this user in Room
            localBeatDao.moveGuestToOwner(uid)

            // 3) If repository is available, upload beats to Firebase private library
            val r = repo ?: return@withContext
            for (row in guestRows) {
                val f = File(row.filePath)
                if (!f.exists() || !f.isFile) continue

                try {
                    r.addToLibrary(
                        ownerId = uid,
                        beatId = row.id,
                        localBeatFile = f,
                        title = row.title,
                        createdAt = row.createdAt
                    )
                } catch (_: Throwable) {
                    // Best-effort: ignore single beat failures
                }
            }
        }
    }
}