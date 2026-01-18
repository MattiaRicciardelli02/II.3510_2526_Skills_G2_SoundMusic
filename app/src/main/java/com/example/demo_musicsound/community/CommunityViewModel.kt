package com.example.demo_musicsound.community

import CommunityBeat
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo_musicsound.data.LocalBeatDao
import com.example.demo_musicsound.data.LocalBeatEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
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

    // ✅ NEW: label autore per ownerId
    val authorLabels: Map<String, String> = emptyMap(),

    val isLoggedIn: Boolean = false,
    val uid: String? = null,
    val authRequired: Boolean = false,
    val userProfile: UserProfile? = null
)
class CommunityViewModel(
    private val repo: FirebaseCommunityRepository,
    private val beatDao: LocalBeatDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _ui = MutableStateFlow(
        CommunityUiState(
            isLoggedIn = auth.currentUser != null,
            uid = auth.currentUser?.uid
        )
    )
    val ui: StateFlow<CommunityUiState> = _ui

    private val itunesRepo = ItunesRepository()

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

    private fun requireAuth(message: String) {
        _ui.value = _ui.value.copy(
            loading = false,
            authRequired = true,
            message = message,
            myBeats = emptyList(),
            communityBeats = emptyList(),
            coverUrls = emptyMap(),
            authorLabels = emptyMap() // ✅
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

                val all = (mine + comm).distinctBy { it.id }

                // -------- covers --------
                val coverMap = mutableMapOf<String, String>()
                for (b in all) {
                    val url = repo.getCoverDownloadUrl(b.coverPath)
                    if (!url.isNullOrBlank()) coverMap[b.id] = url
                }

                // -------- author labels (NEW) --------
                val ownerIds = all.map { it.ownerId }
                    .filter { it.isNotBlank() }
                    .distinct()

                val authorMap = mutableMapOf<String, String>()
                for (ownerId in ownerIds) {
                    val p = try { repo.getUserProfile(ownerId) } catch (_: Throwable) { null }

                    val label =
                        p?.username?.trim().orEmpty()
                            .ifBlank { p?.displayName?.trim().orEmpty() }
                            .ifBlank { ownerId.take(10) + "…" }

                    authorMap[ownerId] = label
                }

                _ui.value = _ui.value.copy(
                    coverUrls = coverMap,
                    authorLabels = authorMap
                )

            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Failed to load community: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun download(context: Context, beat: CommunityBeat, onDone: (File) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    authRequired = true,
                    message = "Login required to download."
                )
                return@launch
            }

            _ui.value = _ui.value.copy(message = null)

            try {
                // 1) Download file locally into /exports (stable filename)
                val file = repo.downloadToLocalExports(context, beat)

                // 2) Persist into Room so RecordScreen can show it immediately
                // Use a deterministic id to avoid duplicates on repeated downloads
                val localId = "dl_${beat.id}"

                beatDao.upsert(
                    LocalBeatEntity(
                        id = localId,
                        ownerId = uid,
                        title = beat.title.ifBlank { file.nameWithoutExtension },
                        filePath = file.absolutePath,
                        createdAt = if (beat.createdAt > 0L) beat.createdAt else System.currentTimeMillis()
                    )
                )

                // 3) Optional but recommended: add to Firebase private library
                // This makes the downloaded beat appear in your library sync too.
                try {
                    repo.addToLibrary(
                        ownerId = uid,
                        beatId = localId,
                        localBeatFile = file,
                        title = beat.title.ifBlank { file.nameWithoutExtension },
                        createdAt = if (beat.createdAt > 0L) beat.createdAt else System.currentTimeMillis()
                    )
                } catch (_: Throwable) {
                    // Best-effort: local Room save is what makes RecordScreen work
                }

                _ui.value = _ui.value.copy(message = "Downloaded: ${beat.title.ifBlank { file.nameWithoutExtension }}")
                onDone(file)

            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    message = "Download failed: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun searchReferenceTracks(query: String, onResult: (List<ReferenceTrack>) -> Unit) {
        viewModelScope.launch {
            try {
                val results = itunesRepo.searchTracks(query, limit = 10)
                onResult(results)
                if (results.isEmpty()) {
                    _ui.value = _ui.value.copy(message = "No results found.")
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    message = "Search failed: ${t.message ?: "unknown error"}"
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
        reference: ReferenceTrack?,
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
                    reference = reference
                )

                _ui.value = _ui.value.copy(message = "Published successfully.")
                load()
                onDone()

            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    message = "Publish failed: ${t.message ?: "unknown error"}"
                )
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val profile = repo.getUserProfile(uid)
                _ui.value = _ui.value.copy(userProfile = profile)
            } catch (_: Throwable) {
                // ignore
            }
        }
    }
}