package com.alvinhkh.buseta.mtr.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alvinhkh.buseta.mtr.model.MtrBusFare
import com.alvinhkh.buseta.mtr.model.MtrBusRoute
import com.alvinhkh.buseta.mtr.model.MtrBusRouteLine
import com.alvinhkh.buseta.mtr.model.MtrBusStop

@Database(entities = [(MtrBusRoute::class), (MtrBusRouteLine::class), (MtrBusStop::class), (MtrBusFare::class)], version = 1)
@TypeConverters(RoomConverterNullString::class)
abstract class MtrBusDatabase : RoomDatabase() {
    abstract fun mtrBusDao(): MtrBusDao
}
