package com.jumincho.cvpass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries on [CheckInEntity] rows; times are epoch milliseconds, ranges half-open. */
@Dao
interface CheckInDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(checkIn: CheckInEntity)

    @Query(
        "SELECT * FROM check_ins WHERE venue_id = :venueId AND checked_in_at >= :from AND checked_in_at < :until " +
            "ORDER BY checked_in_at",
    )
    suspend fun between(venueId: String, from: Long, until: Long): List<CheckInEntity>

    @Query("SELECT COUNT(*) FROM check_ins WHERE venue_id = :venueId AND checked_in_at >= :from AND checked_in_at < :until")
    fun countBetween(venueId: String, from: Long, until: Long): Flow<Int>

    @Query("DELETE FROM check_ins WHERE checked_in_at < :cutoff")
    suspend fun deleteBefore(cutoff: Long)
}
