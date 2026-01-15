package com.example.demo_musicsound.community

interface SpotifyRepository {
    suspend fun searchTracks(query: String): List<SpotifyTrackRef>
}

class SpotifyRepositoryStub : SpotifyRepository {
    override suspend fun searchTracks(query: String): List<SpotifyTrackRef> = emptyList()
}
