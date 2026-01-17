package com.example.demo_musicsound.community


data class ReferenceTrack(
    val provider: String = "itunes",
    val trackId: String = "",
    val trackName: String = "",
    val artistName: String = "",
    val trackViewUrl: String = "",
    val previewUrl: String = "",
    val artworkUrl: String = ""
)
