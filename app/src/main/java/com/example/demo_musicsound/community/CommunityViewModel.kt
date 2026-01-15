package com.example.demo_musicsound.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CommunityUiState(
    val loading: Boolean = false,
    val myBeats: List<CommunityBeat> = emptyList(),
    val communityBeats: List<CommunityBeat> = emptyList(),
    val coverUrls: Map<String, String> = emptyMap(),
    val message: String? = null,

    // auth state for UI
    val isLoggedIn: Boolean = false,
    val uid: String? = null,

    // UI can show a popup when true
    val authRequired: Boolean = false
)

class CommunityViewModel(
    private val repo: FirebaseCommunityRepository,
    private val spotifyRepo: SpotifyRepository = SpotifyRepositoryStub(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(
        CommunityUiState(
            isLoggedIn = auth.currentUser != null,
            uid = auth.currentUser?.uid
        )
    )
    val ui: StateFlow<CommunityUiState> = _ui

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

    fun dismissAuthRequired() {
        _ui.value = _ui.value.copy(authRequired = false)
    }

    fun requireAuth(message: String = "You must be logged in to access Community.") {
        _ui.value = _ui.value.copy(
            loading = false,
            authRequired = true,
            message = message,
            // opzionale: pulisci dati quando non loggato
            myBeats = emptyList(),
            communityBeats = emptyList(),
            coverUrls = emptyMap()
        )
    }

    fun load() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                requireAuth("Login or register to access Community.")
                return@launch
            }

            _ui.value = _ui.value.copy(loading = true, message = null, authRequired = false)

            try {
                val mine = repo.getMyPublished(uid)
                val comm = repo.getFromCommunity(uid)

                _ui.value = _ui.value.copy(
                    loading = false,
                    myBeats = mine,
                    communityBeats = comm
                )

                // Resolve cover URLs (best-effort)
                val all = (mine + comm).distinctBy { it.id }
                val coverMap = mutableMapOf<String, String>()

                for (b in all) {
                    val url = repo.getCoverDownloadUrl(b.coverPath)
                    if (!url.isNullOrBlank()) {
                        coverMap[b.id] = url
                    }
                }

                _ui.value = _ui.value.copy(coverUrls = coverMap)

            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Failed to load community: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun download(context: Context, beat: CommunityBeat, onDone: (File) -> Unit = {}) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                requireAuth("Login required to download.")
                return@launch
            }

            _ui.value = _ui.value.copy(message = null)
            try {
                val file = repo.downloadToLocalExports(context, beat)
                _ui.value = _ui.value.copy(message = "Downloaded to local: ${file.name}")
                onDone(file)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    message = "Download failed: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun searchSpotify(query: String, onResult: (List<SpotifyTrackRef>) -> Unit) {
        viewModelScope.launch {
            try {
                val results = spotifyRepo.searchTracks(query.trim())
                if (results.isEmpty()) {
                    _ui.value = _ui.value.copy(
                        message = "Spotify search is not configured yet (stub)."
                    )
                }
                onResult(results)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    message = "Spotify search failed: ${t.message ?: "unknown error"}"
                )
                onResult(emptyList())
            }
        }
    }

    fun publish(
        context: Context,
        localBeatFile: File,
        title: String,
        description: String,
        coverUri: Uri?,
        spotifyRef: SpotifyTrackRef?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                requireAuth("Login required to publish.")
                return@launch
            }

            _ui.value = _ui.value.copy(message = null)

            try {
                repo.publishBeat(
                    context = context,
                    ownerId = uid,
                    localBeatFile = localBeatFile,
                    title = title,
                    description = description,
                    coverUri = coverUri,
                    spotifyRef = spotifyRef
                )
                _ui.value = _ui.value.copy(message = "Published successfully.")
                load()
                onDone()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(message = "Publish failed: ${t.message ?: "unknown error"}")
            }
        }
    }
}