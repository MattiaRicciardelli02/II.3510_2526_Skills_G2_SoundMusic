package com.example.demo_musicsound.community

data class CommunityBeat(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val audioPath: String = "",
    val coverPath: String = "",
    val createdAt: Long = 0L,

    // NEW (per il publish)
    val description: String = "",
    val spotifyTrackId: String = "",
    val spotifyTrackName: String = "",
    val spotifyTrackArtist: String = "",
    val spotifyUrl: String = "",

    val refProvider: String = "",
    val refTrackId: String = "",
    val refTrackName: String = "",
    val refArtistName: String = "",
    val refUrl: String = "",
    val refPreviewUrl: String = "",
    val refArtworkUrl: String = ""

)
