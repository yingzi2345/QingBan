package com.qingban.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun get(id: String): EventEntity?

    @Query(
        """
        SELECT * FROM events
        WHERE startEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY startEpochDay ASC,
                 CASE WHEN startMinutes IS NULL THEN 1 ELSE 0 END ASC,
                 startMinutes ASC,
                 createdAt ASC
        """,
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startEpochDay ASC, startMinutes ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startEpochDay ASC, startMinutes ASC")
    suspend fun getAll(): List<EventEntity>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface CountdownDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(countdown: CountdownEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countdowns: List<CountdownEntity>)

    @Update
    suspend fun update(countdown: CountdownEntity)

    @Query("SELECT * FROM countdowns WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CountdownEntity?

    @Query("SELECT * FROM countdowns ORDER BY pinned DESC, targetEpochDay ASC, createdAt ASC")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns ORDER BY pinned DESC, targetEpochDay ASC, createdAt ASC")
    suspend fun getAll(): List<CountdownEntity>

    @Query("UPDATE countdowns SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM countdowns")
    suspend fun deleteAll()
}
