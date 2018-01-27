package com.alvinhkh.buseta.mtr.dao

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.alvinhkh.buseta.mtr.model.AESBusDistrict
import com.alvinhkh.buseta.mtr.model.AESBusRoute
import com.alvinhkh.buseta.mtr.model.AESBusStop

@Database(entities = [(AESBusRoute::class), (AESBusStop::class), (AESBusDistrict::class)], version = 1)
abstract class AESBusDatabase : RoomDatabase() {
    abstract fun aesBusDao(): AESBusDao
}
