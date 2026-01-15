package com.example.demo_musicsound.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pad_sounds")
data class PadSoundEntity(
    @PrimaryKey val slotId: String,   // "A0".."A5", "B0".."B5"
    val soundKey: String,             // "kick", "snare", ...
    val label: String,                // testo sul bottone
    val uri: String? = null           // null = usa il raw di default
)