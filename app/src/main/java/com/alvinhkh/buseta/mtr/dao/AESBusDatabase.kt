package com.alvinhkh.buseta.mtr.dao

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.alvinhkh.buseta.mtr.model.AESBusDistrict
import com.alvinhkh.buseta.mtr.model.AESBusRoute
import com.alvinhkh.buseta.mtr.model.AESBusStop

@Database(entities = [(AESBusRoute::class), (AESBusStop::class), (AESBusDistrict::class)], version = 2)
@TypeConverters(RoomConverterNullString::class)
abstract class AESBusDatabase : RoomDatabase() {
    abstract fun aesBusDao(): AESBusDao
}
