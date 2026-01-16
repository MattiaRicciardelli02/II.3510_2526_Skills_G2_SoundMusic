package com.example.demo_musicsound.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PadSoundEntity::class,
        LocalBeatEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAO già esistente
    abstract fun padSoundDao(): PadSoundDao

    // ✅ NUOVO DAO per i beat locali
    abstract fun localBeatDao(): LocalBeatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mybeat.db"
                )
                    // ⚠️ SOLO PER SVILUPPO
                    // evita crash quando cambi schema
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}