package com.example.demo_musicsound.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo_musicsound.community.FirebaseCommunityRepository
import com.example.demo_musicsound.data.LocalBeatDao
import com.google.firebase.auth.FirebaseAuth
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
    val message: String? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val uid: String? = null
)

class AuthViewModel(
    private val localBeatDao: LocalBeatDao,
    private val repo: FirebaseCommunityRepository?, // Optional: used to upload guest beats to Firebase library
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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
            email = u?.email ?: ""   // ✅ così MainActivity può usare authState.email sempre
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

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, message = null)
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()

                val uid = auth.currentUser?.uid
                if (!uid.isNullOrBlank()) {
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
        _ui.value = _ui.value.copy(message = "Logged out.")
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