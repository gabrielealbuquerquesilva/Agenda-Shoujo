package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isGoogleEvent = 1")
    fun getGoogleEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isGoogleEvent = 0 ORDER BY dateTime ASC")
    fun getLocalEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Int)

    @Query("DELETE FROM events WHERE isGoogleEvent = 1")
    suspend fun clearGoogleEvents()
}
