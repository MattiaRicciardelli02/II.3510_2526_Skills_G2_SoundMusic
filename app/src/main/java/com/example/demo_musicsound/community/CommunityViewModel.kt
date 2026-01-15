package com.example.demo_musicsound.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CommunityUiState(
    val loading: Boolean = false,
    val myBeats: List<CommunityBeat> = emptyList(),
    val communityBeats: List<CommunityBeat> = emptyList(),
    val coverUrls: Map<String, String> = emptyMap(),
    val message: String? = null
)

class CommunityViewModel(
    private val repo: FirebaseCommunityRepository,
    private val spotifyRepo: SpotifyRepository = SpotifyRepositoryStub()
) : ViewModel() {


    private val _ui = MutableStateFlow(CommunityUiState())
    val ui: StateFlow<CommunityUiState> = _ui

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, message = null)

            try {
                val mine = repo.getMyPublished()
                val comm = repo.getFromCommunity()

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
                _ui.value = _ui.value.copy(message = "Spotify search failed: ${t.message ?: "unknown error"}")
                onResult(emptyList())
            }
        }
    }

    fun publish(
        context: Context,
        localBeatFile: File,
        title: String,
        description: String,
        coverUri: android.net.Uri?,
        spotifyRef: SpotifyTrackRef?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(message = null)
            try {
                repo.publishBeat(
                    context = context,
                    localBeatFile = localBeatFile,
                    title = title,
                    description = description,
                    coverUri = coverUri,
                    spotifyRef = spotifyRef
                )
                _ui.value = _ui.value.copy(message = "Published successfully.")
                load() // refresh community lists
                onDone()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(message = "Publish failed: ${t.message ?: "unknown error"}")
            }
        }
    }

}
