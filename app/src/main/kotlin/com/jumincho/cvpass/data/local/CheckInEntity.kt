package com.jumincho.cvpass.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
