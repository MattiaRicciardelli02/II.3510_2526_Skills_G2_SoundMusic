package com.example.demo_musicsound.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_beats")
data class LocalBeatEntity(
    @PrimaryKey val id: String,          // uuid
    val ownerId: String,                 // uid Firebase oppure "guest"
    val title: String,
    val filePath: String,                // path assoluto del wav (cache locale)
    val createdAt: Long,

    // ✅ nuovi campi (minimo per sync)
    val remoteAudioPath: String? = null, // path su Firebase Storage
    val synced: Boolean = false          // true se caricato su Firebase
)