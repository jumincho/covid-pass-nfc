package com.jumincho.cvpass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries on [VisitEntity] rows; times are epoch milliseconds. */
@Dao
interface VisitDao {

    @Insert
    suspend fun insert(visit: VisitEntity)

    @Query("SELECT MAX(visited_at) FROM visits WHERE venue_id = :venueId")
    suspend fun lastVisitAt(venueId: String): Long?

    @Query("SELECT * FROM visits ORDER BY visited_at DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<VisitEntity>>

    @Query("DELETE FROM visits WHERE visited_at < :cutoff")
    suspend fun deleteBefore(cutoff: Long)
}
