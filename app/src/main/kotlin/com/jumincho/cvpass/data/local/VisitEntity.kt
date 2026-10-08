package com.jumincho.cvpass.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A row of the visitor's own history. */
@Entity(tableName = "visits", indices = [Index("venue_id", "visited_at"), Index("visited_at")])
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "venue_id") val venueId: String,
    @ColumnInfo(name = "venue_name") val venueName: String,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "visited_at") val visitedAt: Long,
)
