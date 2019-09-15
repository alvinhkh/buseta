package com.alvinhkh.buseta.mtr.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alvinhkh.buseta.mtr.model.*

@Database(entities = [(MtrBusRoute::class), (MtrBusRouteLine::class), (MtrBusStop::class),
    (MtrBusFare::class), (MtrBusFrequence::class)], version = 2)
@TypeConverters(RoomConverterNullString::class)
abstract class MtrBusDatabase : RoomDatabase() {
    abstract fun mtrBusDao(): MtrBusDao
}
