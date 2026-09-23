package com.jumincho.cvpass.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Local storage for the venue-side visitor log and the visitor's own history. */
@Database(entities = [CheckInEntity::class, VisitEntity::class], version = 1, exportSchema = true)
abstract class CvPassDatabase : RoomDatabase() {

    abstract fun checkInDao(): CheckInDao

    abstract fun visitDao(): VisitDao

    companion object {
        /** Opens the app's database file. */
        fun build(context: Context): CvPassDatabase =
            Room.databaseBuilder(context, CvPassDatabase::class.java, "cvpass.db").build()
    }
}

/** A row of the venue-side visitor log. */
@Entity(tableName = "check_ins", indices = [Index("venue_id", "checked_in_at"), Index("checked_in_at")])
data class CheckInEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "venue_id") val venueId: String,
    @ColumnInfo(name = "venue_name") val venueName: String,
    @ColumnInfo(name = "visitor_name") val visitorName: String,
    val phone: String,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "checked_in_at") val checkedInAt: Long,
)

/** A row of the visitor's own history. */
@Entity(tableName = "visits", indices = [Index("venue_id", "visited_at"), Index("visited_at")])
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "venue_id") val venueId: String,
    @ColumnInfo(name = "venue_name") val venueName: String,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "visited_at") val visitedAt: Long,
)

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
