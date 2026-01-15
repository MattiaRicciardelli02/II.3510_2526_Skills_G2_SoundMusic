package com.example.demo_musicsound.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(
        AuthUiState(
            isLoggedIn = auth.currentUser != null,
            uid = auth.currentUser?.uid
        )
    )
    val ui: StateFlow<AuthUiState> = _ui

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        val u = a.currentUser
        _ui.value = _ui.value.copy(
            isLoggedIn = u != null,
            uid = u?.uid
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
}