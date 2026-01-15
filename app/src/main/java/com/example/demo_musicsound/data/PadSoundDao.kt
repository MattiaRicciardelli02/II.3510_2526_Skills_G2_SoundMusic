package com.example.demo_musicsound.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PadSoundDao {

    // UI osservabile: quando cambi label/uri, Compose si aggiorna da solo
    @Query("SELECT * FROM pad_sounds ORDER BY slotId")
    fun observeAll(): Flow<List<PadSoundEntity>>

    @Query("SELECT * FROM pad_sounds WHERE slotId = :slotId LIMIT 1")
    fun observe(slotId: String): Flow<PadSoundEntity?>

    // Utili per init / operazioni one-shot (come stai già facendo nel Main)
    @Query("SELECT * FROM pad_sounds")
    suspend fun getAll(): List<PadSoundEntity>

    @Query("SELECT * FROM pad_sounds WHERE slotId = :slotId LIMIT 1")
    suspend fun get(slotId: String): PadSoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PadSoundEntity)
}