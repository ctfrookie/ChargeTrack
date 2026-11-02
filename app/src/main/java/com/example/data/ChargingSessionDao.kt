package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ChargingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ChargingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSession): Long

    @Update
    suspend fun updateSession(session: ChargingSession)

    @Delete
    suspend fun deleteSession(session: ChargingSession)

    @Query("DELETE FROM charging_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM charging_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM charging_sessions")
    suspend fun getSessionCount(): Int
}
