package com.jumincho.cvpass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Local storage for the venue-side visitor log and the visitor's own history. */
@Database(entities = [CheckInEntity::class, VisitEntity::class], version = 1, exportSchema = true)
abstract class CvPassDatabase : RoomDatabase() {

    abstract fun checkInDao(): CheckInDao

    abstract fun visitDao(): VisitDao

    companion object {
        /** Opens the app's database file. */
        fun build(context: Context): CvPassDatabase =
            Room.databaseBuilder(context, CvPassDatabase::class.java, FILE_NAME).build()

        /** Never rename: existing installs keep their visitor log and history in this file. */
        private const val FILE_NAME = "cvpass.db"
    }
}
