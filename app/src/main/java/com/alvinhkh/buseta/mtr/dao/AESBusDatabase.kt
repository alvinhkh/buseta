package com.alvinhkh.buseta.mtr.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alvinhkh.buseta.mtr.model.AESBusDistrict
import com.alvinhkh.buseta.mtr.model.AESBusRoute
import com.alvinhkh.buseta.mtr.model.AESBusStop

@Database(entities = [(AESBusRoute::class), (AESBusStop::class), (AESBusDistrict::class)], version = 2)
@TypeConverters(RoomConverterNullString::class)
abstract class AESBusDatabase : RoomDatabase() {
    abstract fun aesBusDao(): AESBusDao
}
