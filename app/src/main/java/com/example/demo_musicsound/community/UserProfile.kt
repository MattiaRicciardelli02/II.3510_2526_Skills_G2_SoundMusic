package com.example.demo_musicsound.community

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val createdAt: Long = 0L
)
