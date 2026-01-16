package com.example.demo_musicsound.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalBeatDao {

    @Query("SELECT * FROM local_beats WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getByOwner(ownerId: String): List<LocalBeatEntity>

    @Query("SELECT * FROM local_beats WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeByOwner(ownerId: String): Flow<List<LocalBeatEntity>>

    @Query("SELECT * FROM local_beats WHERE ownerId = 'guest' ORDER BY createdAt DESC")
    suspend fun getGuest(): List<LocalBeatEntity>

    @Query("SELECT * FROM local_beats WHERE ownerId = 'guest' ORDER BY createdAt DESC")
    fun observeGuest(): Flow<List<LocalBeatEntity>>

    @Query("UPDATE local_beats SET ownerId = :newOwnerId WHERE ownerId = 'guest'")
    suspend fun moveGuestToOwner(newOwnerId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: LocalBeatEntity)

    @Query("DELETE FROM local_beats WHERE ownerId = :ownerId")
    suspend fun deleteByOwner(ownerId: String)
}